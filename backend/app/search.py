"""Search / ranking service over the cached offers.

Pure-ish orchestration: read cache -> fuzzy-match -> attach distance -> rank by unit price.
Also implements shopping-list mode (best single store vs optimal per-item split).
"""
from __future__ import annotations

from typing import Optional

from .config import offer_week, settings
from .demo import ensure_seeded
from .models import Chain, NormalizedOffer, SearchResponse
from .normalizer import matcher
from .store import db, locator


def _current_offers(plz: str) -> tuple[str, list[NormalizedOffer]]:
    """Offers for the current week, falling back to the latest cached week (offline)."""
    week = offer_week()
    ensure_seeded(plz)  # demo mode: auto-load sample data for a fresh PLZ
    offers = db.load_offers(plz, week)
    if not offers:
        fallback = db.latest_week_for(plz)
        if fallback:
            return fallback, db.load_offers(plz, fallback)
    return week, offers


def _attach_distance(
    offers: list[NormalizedOffer], lat: Optional[float], lon: Optional[float]
) -> list[NormalizedOffer]:
    if lat is None or lon is None:
        return offers
    for o in offers:
        near = locator.nearest(o.chain, lat, lon)
        if near:
            o.nearest_store_distance_km, o.nearest_store_address = near
    return offers


def search(
    query: str,
    plz: str,
    *,
    lat: Optional[float] = None,
    lon: Optional[float] = None,
    max_distance_km: Optional[float] = None,
    threshold: float = 0.35,
    limit: int = 50,
) -> SearchResponse:
    plz = (plz or settings.DEFAULT_PLZ).strip()
    query = query.strip()
    week, offers = _current_offers(plz)

    # fuzzy match + score
    ranked = matcher.rank(query, [o.model_dump() for o in offers], name_key="product_name")
    matched = [
        NormalizedOffer.model_validate(d)
        for d in ranked
        if d["match_score"] >= threshold
    ]

    matched = _attach_distance(matched, lat, lon)

    if max_distance_km is not None:
        matched = [
            o for o in matched
            if o.nearest_store_distance_km is None
            or o.nearest_store_distance_km <= max_distance_km
        ]

    # rank: cheapest comparable unit price first, then match relevance, then absolute price
    def sort_key(o: NormalizedOffer):
        up = o.unit_price if o.unit_price is not None else float("inf")
        return (up, -o.match_score, o.price)

    matched.sort(key=sort_key)
    return SearchResponse(query=query, plz=plz, offer_week=week, results=matched[:limit])


def autocomplete(prefix: str, plz: str, limit: int = 10) -> list[str]:
    """Distinct product-name suggestions matching a prefix, from the region's cache."""
    plz = (plz or settings.DEFAULT_PLZ).strip()
    _, offers = _current_offers(plz)
    pl = prefix.lower().strip()
    seen: list[str] = []
    for o in offers:
        name = o.product_name
        if pl in name.lower() and name not in seen:
            seen.append(name)
        if len(seen) >= limit:
            break
    return seen


def best_basket(
    items: list[str], plz: str, *, lat=None, lon=None, max_distance_km=None
) -> dict:
    """Shopping-list mode.

    Returns both strategies:
      * ``single_store``: the one chain that minimizes the *total* for all found items.
      * ``optimal_split``: the cheapest offer per item regardless of store.
    """
    plz = (plz or settings.DEFAULT_PLZ).strip()
    per_item: dict[str, list[NormalizedOffer]] = {}
    for it in items:
        res = search(it, plz, lat=lat, lon=lon, max_distance_km=max_distance_km, limit=20)
        per_item[it] = res.results

    # optimal split: cheapest (by unit price, then price) per item
    optimal_split = {}
    split_total = 0.0
    for it, offers in per_item.items():
        if offers:
            best = offers[0]  # already sorted cheapest-first by search()
            optimal_split[it] = best.model_dump()
            split_total += best.price

    # single store: for each chain, sum the cheapest matching offer per item it carries
    chain_totals: dict[Chain, dict] = {}
    for it, offers in per_item.items():
        by_chain: dict[Chain, NormalizedOffer] = {}
        for o in offers:
            if o.chain not in by_chain:  # offers sorted cheapest-first
                by_chain[o.chain] = o
        for chain, off in by_chain.items():
            slot = chain_totals.setdefault(chain, {"total": 0.0, "items": {}, "count": 0})
            slot["total"] += off.price
            slot["items"][it] = off.model_dump()
            slot["count"] += 1

    # prefer the chain covering the most items, then lowest total
    best_single = None
    if chain_totals:
        chain, slot = max(
            chain_totals.items(), key=lambda kv: (kv[1]["count"], -kv[1]["total"])
        )
        best_single = {"chain": chain.value, **slot}

    return {
        "plz": plz,
        "single_store": best_single,
        "optimal_split": {"items": optimal_split, "total": round(split_total, 2)},
        "disclaimer": "Prices are indicative — verify in store.",
    }
