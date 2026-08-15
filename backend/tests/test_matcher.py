"""Fuzzy German name matching + synonym tests."""
from app.normalizer import matcher
from app.normalizer.synonyms import canonicalize


def test_synonyms_map_to_canonical():
    assert canonicalize("Speisekartoffeln") == "kartoffeln"
    assert canonicalize("festkochend") == "kartoffeln"
    assert canonicalize("Vollmilch") == "milch"
    assert canonicalize("H-Milch") == "milch"


def test_canonicalize_substring_in_long_name():
    assert canonicalize("Speisekartoffeln festkochend 2kg Beutel") == "kartoffeln"


def test_score_concept_beats_unrelated():
    s_related = matcher.score("Kartoffeln", "Speisekartoffeln festkochend")
    s_unrelated = matcher.score("Kartoffeln", "Frische Vollmilch 3,5%")
    assert s_related > s_unrelated
    assert s_related >= 0.5


def test_typo_tolerance():
    assert matcher.is_match("Kartofeln", "Speisekartoffeln festkochend")


def test_rank_orders_by_relevance():
    products = [
        {"product_name": "Frische Vollmilch 3,5%"},
        {"product_name": "Speisekartoffeln festkochend"},
        {"product_name": "Kartoffeln vorwiegend festkochend"},
    ]
    ranked = matcher.rank("Kartoffeln", products)
    assert "artoffeln" in ranked[0]["product_name"].lower()
    assert ranked[-1]["product_name"] == "Frische Vollmilch 3,5%"
