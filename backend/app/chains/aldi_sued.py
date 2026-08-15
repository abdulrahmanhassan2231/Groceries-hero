"""Aldi Süd first-party parser (covers Munich / 8xxxx — our home region).

Strategy: no official API. Try the site's JSON/asset offer feed; fall back to the weekly
leaflet PDF -> pdfplumber text extraction; OCR only as last resort. Region matters: Süd vs
Nord are different companies with different prices. Disabled until verified.
"""
from __future__ import annotations

from ..models import Chain, NormalizedOffer
from .base import ChainParser


class AldiSuedParser(ChainParser):
    chain = Chain.ALDI_SUED
    source = "aldi_sued"
    enabled = False

    async def fetch(self, plz: str) -> list[NormalizedOffer]:
        raise NotImplementedError("Aldi Süd first-party connector not yet verified")
