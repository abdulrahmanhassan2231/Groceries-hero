"""Store locator + distance. Haversine distance from device location to nearest branch.

The branch dataset is a pluggable seed (``stores.json``); in production it comes from each
chain's store-finder endpoint or an OSM extract. Distance is computed per-request from the
device's lat/lon, so it is never baked into the cache.
"""
from __future__ import annotations

import json
import math
import os
from dataclasses import dataclass
from typing import Optional

from ..models import Chain

_STORES_PATH = os.getenv(
    "STORES_PATH", os.path.join(os.path.dirname(__file__), "stores.json")
)


@dataclass
class Store:
    chain: Chain
    address: str
    lat: float
    lon: float


def _load_stores() -> list[Store]:
    try:
        with open(_STORES_PATH, encoding="utf-8") as fh:
            data = json.load(fh)
    except FileNotFoundError:
        return []
    out = []
    for s in data:
        try:
            out.append(Store(Chain(s["chain"]), s["address"], float(s["lat"]), float(s["lon"])))
        except (KeyError, ValueError):
            continue
    return out


_STORES = _load_stores()


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlmb = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dlmb / 2) ** 2
    return round(2 * r * math.asin(math.sqrt(a)), 2)


def nearest(chain: Chain, lat: float, lon: float) -> Optional[tuple[float, str]]:
    """Return (distance_km, address) of the nearest branch of ``chain``, or None."""
    best: Optional[tuple[float, str]] = None
    for st in _STORES:
        if st.chain != chain:
            continue
        d = haversine_km(lat, lon, st.lat, st.lon)
        if best is None or d < best[0]:
            best = (d, st.address)
    return best
