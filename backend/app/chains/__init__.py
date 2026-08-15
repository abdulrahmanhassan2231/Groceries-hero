"""Registry of all chain connectors.

The aggregator (Marktguru) is primary and covers all five chains. The per-chain
first-party parsers are fallbacks/cross-checks, disabled until individually verified.
"""
from __future__ import annotations

from ..config import settings
from .aldi_nord import AldiNordParser
from .aldi_sued import AldiSuedParser
from .base import ChainParser
from .kaufland import KauflandParser
from .lidl import LidlParser
from .marktguru import MarktguruParser
from .netto import NettoParser

# order matters only for health display
ALL_PARSERS: list[ChainParser] = [
    MarktguruParser(),
    LidlParser(),
    AldiSuedParser(),
    AldiNordParser(),
    NettoParser(),
    KauflandParser(),
]


def active_parsers() -> list[ChainParser]:
    """Parsers to actually run this cycle."""
    parsers = [p for p in ALL_PARSERS if p.is_enabled]
    if settings.USE_AGGREGATOR:
        return parsers  # aggregator + any enabled first-party ones
    return [p for p in parsers if p.source != "marktguru"]


__all__ = ["ALL_PARSERS", "active_parsers", "ChainParser"]
