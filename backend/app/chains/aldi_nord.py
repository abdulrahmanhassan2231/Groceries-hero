"""Aldi Nord first-party parser (northern/eastern Germany).

Strategy mirrors Aldi Süd: JSON/asset feed -> leaflet PDF -> OCR. Disabled until verified.
"""
from __future__ import annotations

from ..models import Chain, NormalizedOffer
from .base import ChainParser


class AldiNordParser(ChainParser):
    chain = Chain.ALDI_NORD
    source = "aldi_nord"
    enabled = False

    async def fetch(self, plz: str) -> list[NormalizedOffer]:
        raise NotImplementedError("Aldi Nord first-party connector not yet verified")
