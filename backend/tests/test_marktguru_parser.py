"""End-to-end parser test for the primary chain source, against a captured fixture.

This is the template every other chain parser must follow before its `enabled` flag flips.
"""
import json
import os

from app.chains.marktguru import MarktguruParser
from app.models import Chain, PriceType

FIXTURE = os.path.join(os.path.dirname(__file__), "fixtures", "marktguru_sample.json")


def _load():
    with open(FIXTURE, encoding="utf-8") as fh:
        return json.load(fh)


def test_parses_only_target_chains():
    offers = MarktguruParser.parse_response(_load(), plz="80331")
    chains = {o.chain for o in offers}
    # REWE in the fixture must be dropped; the four target advertisers kept.
    assert Chain.ALDI_SUED in chains
    assert Chain.KAUFLAND in chains
    assert Chain.LIDL in chains
    assert Chain.NETTO in chains
    assert all(o.source == "marktguru" for o in offers)
    assert len(offers) == 4  # REWE excluded


def test_unit_prices_are_derived_correctly():
    offers = MarktguruParser.parse_response(_load(), plz="80331")
    by_name = {o.product_name: o for o in offers}

    kartoffeln = by_name["Speisekartoffeln festkochend"]
    assert kartoffeln.chain == Chain.ALDI_SUED
    assert kartoffeln.unit_price_unit == "kg"
    assert abs(kartoffeln.unit_price - 1.49 / 2.0) < 1e-3  # derived, not the feed's 0.75
    assert kartoffeln.unit_price_derived is True

    water = by_name["Mineralwasser Classic"]
    assert water.chain == Chain.NETTO
    assert water.unit_price_unit == "l"
    assert abs(water.unit_price - 3.29 / 9.0) < 1e-3  # 6 x 1,5 l = 9 l


def test_validity_dates_parsed():
    offers = MarktguruParser.parse_response(_load(), plz="80331")
    o = offers[0]
    assert str(o.valid_from) == "2026-08-17"
    assert str(o.valid_to) == "2026-08-23"
    assert o.price_type == PriceType.OFFER


def test_defensive_against_missing_fields():
    bad = {"results": [{"advertisers": [{"name": "Lidl"}]}]}  # no price
    assert MarktguruParser.parse_response(bad, "80331") == []
    assert MarktguruParser.parse_response({}, "80331") == []
    assert MarktguruParser.parse_response({"results": None}, "80331") == []
