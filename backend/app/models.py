"""The clean, normalized API contract served to the Android client.

This is the *only* shape the phone knows about. Each chain's messy source data is mapped
into ``NormalizedOffer`` by its parser, so the app is insulated from upstream changes.
"""
from __future__ import annotations

from datetime import date
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


class Chain(str, Enum):
    LIDL = "lidl"
    ALDI_SUED = "aldi_sued"
    ALDI_NORD = "aldi_nord"
    NETTO = "netto"
    KAUFLAND = "kaufland"


class PriceType(str, Enum):
    REGULAR = "regular"     # Dauerpreis / regular shelf price
    OFFER = "offer"         # weekly Angebot


class NormalizedOffer(BaseModel):
    chain: Chain
    product_name: str
    brand: Optional[str] = None
    price: float = Field(..., description="Total package price in EUR")
    currency: str = "EUR"

    # normalized comparison value; unit is 'kg', 'l', or 'Stück'
    unit_price: Optional[float] = None
    unit_price_unit: Optional[str] = None
    unit_price_derived: bool = True  # False => taken from feed, not recomputed

    package_size: Optional[str] = None       # raw size text, e.g. "2 kg Beutel"
    price_type: PriceType = PriceType.OFFER
    valid_from: Optional[date] = None
    valid_to: Optional[date] = None

    plz: str                                  # region key this offer was fetched for
    source: str                               # e.g. "marktguru" / "lidl_json"
    match_score: float = 0.0                  # relevance to the query (filled by search)

    # nearest-branch distance is filled per-request from device location + store locator
    nearest_store_distance_km: Optional[float] = None
    nearest_store_address: Optional[str] = None


class SearchResponse(BaseModel):
    query: str
    plz: str
    offer_week: str                          # ISO week, e.g. "2026-W34"
    results: list[NormalizedOffer]
    disclaimer: str = "Prices are indicative — verify in store."


class ChainHealth(BaseModel):
    chain: Chain
    status: str                              # "ok" | "stale" | "disabled"
    last_success: Optional[str] = None
    last_error: Optional[str] = None
    offer_count: int = 0


class HealthResponse(BaseModel):
    status: str
    offer_week: str
    chains: list[ChainHealth]
