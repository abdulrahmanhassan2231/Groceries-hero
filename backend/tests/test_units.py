"""Unit-price normalization tests — the core 'hard problem'."""
import math

import pytest

from app.normalizer.units import Dimension, normalize_unit_price, parse_size


@pytest.mark.parametrize(
    "text,expected_base,dim",
    [
        ("2 kg", 2.0, Dimension.MASS),
        ("500 g", 0.5, Dimension.MASS),
        ("250g", 0.25, Dimension.MASS),
        ("1,5 l", 1.5, Dimension.VOLUME),
        ("750 ml", 0.75, Dimension.VOLUME),
        ("ca. 500 g Beutel", 0.5, Dimension.MASS),
        ("je 100g", 0.1, Dimension.MASS),
        ("6 x 1,5 l", 9.0, Dimension.VOLUME),      # multipack total
        ("2x0,5l", 1.0, Dimension.VOLUME),
        ("500-750 g", 0.625, Dimension.MASS),      # range midpoint
        ("12 Eier", 12.0, Dimension.COUNT),
        ("6 Stück", 6.0, Dimension.COUNT),
        ("10er", 10.0, Dimension.COUNT),
    ],
)
def test_parse_size(text, expected_base, dim):
    p = parse_size(text)
    assert p is not None, text
    assert math.isclose(p.base_quantity, expected_base, rel_tol=1e-6)
    assert p.dimension == dim


def test_parse_size_garbage_returns_none():
    assert parse_size("") is None
    assert parse_size("frisch aus der Region") is None


def test_unit_price_mass():
    up = normalize_unit_price(1.49, "2 kg")
    assert up.unit == "kg"
    assert up.dimension == Dimension.MASS
    assert math.isclose(up.value, 0.745, rel_tol=1e-3)
    assert up.derived is True


def test_unit_price_multipack_volume():
    # 3.29 € for 6 x 1.5 l = 9 l  -> ~0.3656 €/l
    up = normalize_unit_price(3.29, "6 x 1,5 l")
    assert up.unit == "l"
    assert math.isclose(up.value, 3.29 / 9.0, rel_tol=1e-3)


def test_unit_price_falls_back_to_feed_when_unparsable():
    up = normalize_unit_price(2.0, "frisch", fallback_unit_price=1.25, fallback_unit="kg")
    assert up.value == 1.25
    assert up.derived is False


def test_unit_price_none_when_no_size_and_no_fallback():
    assert normalize_unit_price(2.0, "lose Ware") is None
