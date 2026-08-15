"""Netto Marken-Discount parser (the black/yellow/red one — NOT Danish Netto).

Strategy: scrape the weekly offers HTML/JSON keyed by PLZ/store; leaflet fallback. Guard
hard against confusing it with EDEKA-owned "Netto" (Dansk). Disabled until verified.
"""
from __future__ import annotations

from ..models import Chain, NormalizedOffer
from .base import ChainParser


class NettoParser(ChainParser):
    chain = Chain.NETTO
    source = "netto"
    enabled = False

    async def fetch(self, plz: str) -> list[NormalizedOffer]:
        raise NotImplementedError("Netto Marken-Discount connector not yet verified")
