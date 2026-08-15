"""Lidl first-party parser (fallback / cross-check to the aggregator).

Strategy (see docs/01-data-source-research.md):
  1. JSON first — Lidl's web app is backed by JSON product/offer services; capture the
     XHR that powers the weekly-offers page and map it here.
  2. Leaflet PDF/HTML fallback if the JSON shape changes.

Disabled until its fixture-driven parser test is in place and the live shape is verified.
"""
from __future__ import annotations

from ..models import Chain, NormalizedOffer
from .base import ChainParser


class LidlParser(ChainParser):
    chain = Chain.LIDL
    source = "lidl_json"
    enabled = False  # flip on after verifying live endpoint + adding a fixture test

    async def fetch(self, plz: str) -> list[NormalizedOffer]:
        # TODO: GET the Lidl weekly-offers JSON for the region, then map each item with
        # normalize_unit_price() exactly as MarktguruParser.parse_response does.
        raise NotImplementedError("Lidl first-party connector not yet verified")

    @classmethod
    def parse_response(cls, data: dict, plz: str) -> list[NormalizedOffer]:
        raise NotImplementedError
