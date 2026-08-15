"""Marktguru aggregator parser — the primary, end-to-end data source.

One endpoint returns offers for all five target chains, region-filtered by PLZ. We map
each offer's ``advertiser`` to our :class:`Chain` enum and drop anything that isn't one of
our five chains.

The network layer and the *parsing* layer are deliberately separated:
  * :meth:`fetch` does bootstrap + HTTP.
  * :meth:`parse_response` is a pure function over already-fetched JSON, so it is fully
    unit-tested against ``tests/fixtures/marktguru_sample.json`` with no network.

⚠️ Field names below reflect the observed shape of the API but MUST be re-verified against
live traffic (see docs/01-data-source-research.md). The parser is defensive: unknown or
missing fields degrade gracefully rather than crash.
"""
from __future__ import annotations

import re
from typing import Optional

import httpx

from ..config import settings
from ..models import Chain, NormalizedOffer, PriceType
from ..normalizer import normalize_unit_price
from .base import ChainParser, ParserError

# Map marktguru advertiser names -> our Chain enum. Matching is case-insensitive and
# substring-based to tolerate "ALDI SÜD", "Aldi Süd Angebote", etc.
_ADVERTISER_MAP: list[tuple[str, Chain]] = [
    ("aldi süd", Chain.ALDI_SUED),
    ("aldi sued", Chain.ALDI_SUED),
    ("aldi nord", Chain.ALDI_NORD),
    ("lidl", Chain.LIDL),
    ("netto marken", Chain.NETTO),  # NOT plain "netto" (that is the Danish/EDEKA one)
    ("kaufland", Chain.KAUFLAND),
]


def _advertiser_to_chain(name: str) -> Optional[Chain]:
    low = (name or "").lower()
    for needle, chain in _ADVERTISER_MAP:
        if needle in low:
            return chain
    return None


class MarktguruParser(ChainParser):
    # This parser can produce any of the five chains; `chain` is a nominal label used for
    # health reporting of the aggregator connector itself.
    chain = Chain.LIDL  # placeholder; real chain is set per-offer
    source = "marktguru"

    _KEY_RE = re.compile(r"""["'](?:x-)?(?:client|api)key["']\s*[:=]\s*["']([\w\-]{8,})["']""",
                         re.IGNORECASE)

    async def _bootstrap_keys(self, client: httpx.AsyncClient) -> tuple[str, str]:
        """Obtain (client_key, api_key). Prefer env pins; else scrape the web bundle.

        Rotating keys are the main fragility of this source, so we never hard-code a
        literal in source — we either read a pinned env var or extract from the live
        bootstrap JS.
        """
        if settings.MARKTGURU_CLIENT_KEY and settings.MARKTGURU_API_KEY:
            return settings.MARKTGURU_CLIENT_KEY, settings.MARKTGURU_API_KEY

        # Fetch the web app, find its main JS bundle, extract the two keys.
        resp = await self._get(client, settings.MARKTGURU_WEB)
        scripts = re.findall(r'src="([^"]+\.js)"', resp.text)
        for src in scripts:
            js_url = src if src.startswith("http") else f"{settings.MARKTGURU_WEB}{src}"
            try:
                js = (await self._get(client, js_url)).text
            except Exception:
                continue
            keys = self._KEY_RE.findall(js)
            if len(keys) >= 2:
                return keys[0], keys[1]
        raise ParserError("could not bootstrap marktguru keys; set MARKTGURU_*_KEY env")

    async def fetch(self, plz: str) -> list[NormalizedOffer]:
        plz = plz or settings.DEFAULT_PLZ
        async with httpx.AsyncClient(timeout=settings.REQUEST_TIMEOUT_S) as client:
            client_key, api_key = await self._bootstrap_keys(client)
            headers = {"x-clientkey": client_key, "x-apikey": api_key}
            # We fetch a broad sweep for the region and index locally; the app searches the
            # cache. Marktguru requires a query term, so we sweep common staples. In
            # production this list comes from the synonym catalog / search analytics.
            offers: list[NormalizedOffer] = []
            seen: set[tuple] = set()
            for term in ("angebot", "kartoffeln", "milch", "obst", "gemüse", "fleisch"):
                url = (
                    f"{settings.MARKTGURU_BASE}/offers/search"
                    f"?as=web&q={term}&zipCode={plz}&limit=200&offset=0"
                )
                try:
                    resp = await self._get(client, url, headers=headers)
                    data = resp.json()
                except Exception as exc:  # keep going with other terms
                    self.last_error = f"{term}: {exc}"
                    continue
                for off in self.parse_response(data, plz):
                    key = (off.chain, off.product_name, off.price, off.valid_to)
                    if key not in seen:
                        seen.add(key)
                        offers.append(off)
            if not offers and self.last_error:
                raise ParserError(self.last_error)
            return offers

    @classmethod
    def parse_response(cls, data: dict, plz: str) -> list[NormalizedOffer]:
        """Pure: map a raw marktguru search response into NormalizedOffers.

        Only offers whose advertiser is one of our five chains are kept.
        """
        out: list[NormalizedOffer] = []
        for off in (data or {}).get("results", []) or []:
            advertisers = off.get("advertisers") or []
            adv_name = advertisers[0].get("name") if advertisers else off.get("advertiser", "")
            chain = _advertiser_to_chain(adv_name or "")
            if chain is None:
                continue

            product = off.get("product") or {}
            name = product.get("name") or off.get("title") or off.get("description") or ""
            brand = (product.get("brand") or {}).get("name") if isinstance(
                product.get("brand"), dict
            ) else off.get("brand")

            price = off.get("price")
            if price is None:
                continue
            try:
                price = float(price)
            except (TypeError, ValueError):
                continue

            size_text = off.get("quantity") or off.get("unit") or off.get("description") or ""
            up = normalize_unit_price(
                price,
                size_text,
                fallback_unit_price=_maybe_float(off.get("unitPrice")),
                fallback_unit=off.get("unitPriceUnit"),
            )

            validity = (off.get("validityDates") or [{}])[0]
            valid_from = ChainParser._parse_date(validity.get("from") or off.get("validFrom"))
            valid_to = ChainParser._parse_date(validity.get("to") or off.get("validTo"))

            out.append(
                NormalizedOffer(
                    chain=chain,
                    product_name=name.strip(),
                    brand=(brand or None),
                    price=price,
                    unit_price=up.value if up else None,
                    unit_price_unit=up.unit if up else None,
                    unit_price_derived=up.derived if up else True,
                    package_size=size_text or None,
                    price_type=PriceType.OFFER,
                    valid_from=valid_from,
                    valid_to=valid_to,
                    plz=plz,
                    source="marktguru",
                )
            )
        return out


def _maybe_float(v) -> Optional[float]:
    try:
        return float(v) if v is not None else None
    except (TypeError, ValueError):
        return None
