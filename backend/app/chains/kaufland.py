"""Kaufland parser (strong regional pricing — key by region/store, not just brand).

Strategy: JSON `/api` endpoints behind the offers page. NOTE: the Kaufland *marketplace*
seller API is a different product (online listings, not weekly in-store Angebote) — do not
use it here. Disabled until verified.
"""
from __future__ import annotations

from ..models import Chain, NormalizedOffer
from .base import ChainParser


class KauflandParser(ChainParser):
    chain = Chain.KAUFLAND
    source = "kaufland"
    enabled = False

    async def fetch(self, plz: str) -> list[NormalizedOffer]:
        raise NotImplementedError("Kaufland connector not yet verified")
