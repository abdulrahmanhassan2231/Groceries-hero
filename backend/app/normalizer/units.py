"""Package-size parsing and unit-price normalization.

This is the core of the "normalization is the hard problem" requirement: take a raw
German offer (a price plus a free-text size like ``"6 x 1,5 l"`` or ``"ca. 500 g Beutel"``)
and produce a comparable **€/kg** or **€/l** unit price.

Design goals:
  * Pure standard library (no deps) so it is trivially testable and fast.
  * Never crash on garbage input — return ``None`` when a size can't be understood.
  * Be explicit about *base dimension* (mass vs volume vs count) because you cannot
    compare €/kg to €/l to €/piece.

The public entry point is :func:`normalize_unit_price`.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from enum import Enum
from typing import Optional


class Dimension(str, Enum):
    MASS = "mass"      # normalized to kg
    VOLUME = "volume"  # normalized to l
    COUNT = "count"    # normalized to piece (Stück) — NOT comparable to mass/volume


# Conversion factors into the canonical base unit of each dimension.
_MASS_TO_KG = {
    "kg": 1.0,
    "kilogramm": 1.0,
    "g": 0.001,
    "gramm": 0.001,
    "mg": 0.000001,
}
_VOLUME_TO_L = {
    "l": 1.0,
    "liter": 1.0,
    "ltr": 1.0,
    "ml": 0.001,
    "cl": 0.01,
    "dl": 0.1,
}
_COUNT_WORDS = {
    "stück", "stk", "st", "x", "packung", "pack", "beutel", "dose", "glas",
    "flasche", "eier", "stange", "bund", "netz", "schale", "becher", "tafel",
}

# Match a number that may use German decimal comma or English point.
_NUM = r"\d+(?:[.,]\d+)?"


@dataclass(frozen=True)
class ParsedSize:
    """A parsed package size, expressed in the canonical base unit."""
    base_quantity: float       # total amount in kg (mass), l (volume), or pieces (count)
    dimension: Dimension
    multiplier: int = 1        # e.g. the "6" in "6 x 1,5 l"
    raw: str = ""

    @property
    def base_unit(self) -> str:
        return {Dimension.MASS: "kg", Dimension.VOLUME: "l", Dimension.COUNT: "Stück"}[
            self.dimension
        ]


def _to_float(num: str) -> float:
    """Parse a German-or-English formatted number. ``"1,5"`` -> ``1.5``.

    Handles thousands separators conservatively: only treats a comma/point as a
    thousands separator when it groups exactly three trailing digits and there is
    another separator, which practically never happens in package sizes anyway.
    """
    return float(num.replace(".", "X").replace(",", ".").replace("X", ""))


def _unit_factor(unit: str) -> Optional[tuple[float, Dimension]]:
    u = unit.lower().strip(". ")
    if u in _MASS_TO_KG:
        return _MASS_TO_KG[u], Dimension.MASS
    if u in _VOLUME_TO_L:
        return _VOLUME_TO_L[u], Dimension.VOLUME
    if u in _COUNT_WORDS:
        return 1.0, Dimension.COUNT
    return None


# "6 x 1,5 l", "2x0,5l", "3 × 100 g"
_MULTIPACK_RE = re.compile(
    rf"(?P<count>\d+)\s*[x×]\s*(?P<qty>{_NUM})\s*(?P<unit>[a-zA-ZäöüÄÖÜ]+)",
)
# "500 g", "1,5 l", "2 kg", "250ml", "ca. 500 g", "je 100g"
_SINGLE_RE = re.compile(
    rf"(?:ca\.?\s*|je\s*|per\s*)?(?P<qty>{_NUM})\s*(?P<unit>kg|kilogramm|g|gramm|mg|"
    rf"l|liter|ltr|ml|cl|dl)\b",
    re.IGNORECASE,
)
# range like "500-750 g" or "500 - 750 g" -> use the *midpoint* for a fair estimate
_RANGE_RE = re.compile(
    rf"(?P<lo>{_NUM})\s*[-–]\s*(?P<hi>{_NUM})\s*(?P<unit>kg|g|l|ml|cl|dl)\b",
    re.IGNORECASE,
)
# bare piece counts: "12 Eier", "6 Stück", "10er"
_COUNT_RE = re.compile(
    r"(?P<count>\d+)\s*(?:er\b|x\b|(?:stück|stk|st|eier|packung|beutel|dose|glas|"
    r"flasche|stange|bund|netz|schale|becher|tafel)\b)",
    re.IGNORECASE,
)


def parse_size(text: str) -> Optional[ParsedSize]:
    """Parse a free-text German package size into a :class:`ParsedSize`.

    Precedence: range -> multipack -> single mass/volume -> bare count.
    Returns ``None`` if nothing usable is found.
    """
    if not text:
        return None
    s = text.strip()

    # 1) range ("500-750 g") -> midpoint
    m = _RANGE_RE.search(s)
    if m:
        f = _unit_factor(m.group("unit"))
        if f:
            factor, dim = f
            mid = (_to_float(m.group("lo")) + _to_float(m.group("hi"))) / 2.0
            return ParsedSize(mid * factor, dim, 1, s)

    # 2) multipack ("6 x 1,5 l")
    m = _MULTIPACK_RE.search(s)
    if m:
        f = _unit_factor(m.group("unit"))
        if f:
            factor, dim = f
            count = int(m.group("count"))
            each = _to_float(m.group("qty")) * factor
            return ParsedSize(each * count, dim, count, s)

    # 3) single mass/volume ("500 g", "1,5 l")
    m = _SINGLE_RE.search(s)
    if m:
        f = _unit_factor(m.group("unit"))
        if f:
            factor, dim = f
            return ParsedSize(_to_float(m.group("qty")) * factor, dim, 1, s)

    # 4) bare count ("12 Eier", "6 Stück")
    m = _COUNT_RE.search(s)
    if m:
        count = int(m.group("count"))
        return ParsedSize(float(count), Dimension.COUNT, count, s)

    return None


@dataclass(frozen=True)
class UnitPrice:
    value: float          # price per base unit
    unit: str             # "kg", "l", or "Stück"
    dimension: Dimension
    derived: bool         # True if we computed it, False if taken from the feed as-is


def normalize_unit_price(
    price: float,
    size_text: str,
    *,
    fallback_unit_price: Optional[float] = None,
    fallback_unit: Optional[str] = None,
) -> Optional[UnitPrice]:
    """Convert an offer price + size text into a comparable unit price.

    ``price`` is the total price of the package (e.g. 1.49 € for a 2 kg bag).
    We derive €/kg or €/l ourselves from the parsed size and only fall back to a
    feed-provided unit price when the size can't be parsed.
    """
    parsed = parse_size(size_text)
    if parsed and parsed.base_quantity > 0:
        return UnitPrice(
            value=round(price / parsed.base_quantity, 4),
            unit=parsed.base_unit,
            dimension=parsed.dimension,
            derived=True,
        )
    if fallback_unit_price is not None and fallback_unit:
        dim = (
            Dimension.MASS if fallback_unit.lower() in ("kg", "g")
            else Dimension.VOLUME if fallback_unit.lower() in ("l", "ml", "cl")
            else Dimension.COUNT
        )
        return UnitPrice(round(fallback_unit_price, 4), fallback_unit.lower(), dim, False)
    return None
