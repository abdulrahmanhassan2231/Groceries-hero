"""Search + shopping-list service tests, exercising the cache end to end."""
import json
import os

from app.chains.marktguru import MarktguruParser
from app.config import offer_week
from app.models import Chain
from app.search import best_basket, search
from app.store import db

FIXTURE = os.path.join(os.path.dirname(__file__), "fixtures", "marktguru_sample.json")


def _seed():
    db.init_db()
    with open(FIXTURE, encoding="utf-8") as fh:
        offers = MarktguruParser.parse_response(json.load(fh), plz="80331")
    db.replace_offers("80331", offer_week(), offers)


def test_search_ranks_cheapest_kartoffeln_first():
    _seed()
    res = search("Kartoffeln", "80331")
    assert res.results, "expected matches"
    # both potato offers should match and beat milk/water
    top = res.results[0]
    assert "artoffeln" in top.product_name.lower()
    # cheapest €/kg first: Aldi Süd 0.745 €/kg vs Kaufland 0.598 €/kg -> Kaufland first
    assert top.chain == Chain.KAUFLAND
    assert res.disclaimer.startswith("Prices are indicative")


def test_search_distance_filter():
    _seed()
    # Munich Marienplatz-ish coords; all seeded stores are in Munich (<10 km)
    res = search("Kartoffeln", "80331", lat=48.137, lon=11.575, max_distance_km=50)
    assert all(
        o.nearest_store_distance_km is None or o.nearest_store_distance_km <= 50
        for o in res.results
    )


def test_basket_single_vs_split():
    _seed()
    out = best_basket(["Kartoffeln", "Milch"], "80331")
    assert out["optimal_split"]["total"] > 0
    assert out["single_store"] is not None
    assert "chain" in out["single_store"]
