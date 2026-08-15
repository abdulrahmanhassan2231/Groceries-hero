"""Fuzzy matching of a user's query against product names.

Combines three signals:
  1. canonical/synonym match (exact concept hit)  -> strongest
  2. token overlap
  3. fuzzy string similarity (typo tolerance)

Uses ``rapidfuzz`` when available for speed/quality; falls back to the stdlib
``difflib`` so tests and lightweight deployments need no native dependency.
"""
from __future__ import annotations

import re
from typing import Iterable

from .synonyms import _SURFACE_TO_CANON, canonicalize

try:  # optional dependency
    from rapidfuzz import fuzz

    def _similarity(a: str, b: str) -> float:
        return fuzz.token_set_ratio(a, b) / 100.0

    def _partial(a: str, b: str) -> float:
        return fuzz.partial_ratio(a, b) / 100.0

    def _ratio(a: str, b: str) -> float:
        return fuzz.ratio(a, b) / 100.0
except Exception:  # pragma: no cover - fallback path
    from difflib import SequenceMatcher

    def _similarity(a: str, b: str) -> float:
        return SequenceMatcher(None, a, b).ratio()

    _partial = _similarity
    _ratio = _similarity


_TOKEN_RE = re.compile(r"[a-zA-ZäöüÄÖÜß0-9]+")

# similarity above which a (typo'd) token is treated as the same concept
_CONCEPT_FUZZ = 0.82


def _tokens(s: str) -> set[str]:
    return {t.lower() for t in _TOKEN_RE.findall(s)}


def _fuzzy_canon(term: str) -> str:
    """Canonicalize with typo tolerance: exact synonym first, else nearest surface form."""
    exact = canonicalize(term)
    if exact in _SURFACE_TO_CANON.values():
        return exact
    best_canon, best = exact, 0.0
    for tok in _tokens(term):
        for surface, canon in _SURFACE_TO_CANON.items():
            sim = _ratio(tok, surface)
            if sim > best:
                best, best_canon = sim, canon
    return best_canon if best >= _CONCEPT_FUZZ else exact


def score(query: str, product_name: str) -> float:
    """Return a 0..1 relevance score for ``product_name`` given ``query``."""
    if not query or not product_name:
        return 0.0

    concept = 1.0 if _fuzzy_canon(query) == canonicalize(product_name) else 0.0

    q_tok, p_tok = _tokens(query), _tokens(product_name)
    overlap = len(q_tok & p_tok) / len(q_tok) if q_tok else 0.0

    # best-token fuzzy handles typos against long compound German names
    fuzzy = max(
        _similarity(query.lower(), product_name.lower()),
        max((_partial(query.lower(), t) for t in p_tok), default=0.0),
    )

    # weighted blend: concept dominates, then overlap, then fuzzy typo-tolerance
    return round(0.55 * concept + 0.25 * overlap + 0.20 * fuzzy, 4)


def is_match(query: str, product_name: str, threshold: float = 0.35) -> bool:
    return score(query, product_name) >= threshold


def rank(query: str, products: Iterable[dict], name_key: str = "product_name") -> list[dict]:
    """Attach a ``match_score`` to each product dict and return them sorted desc.

    Non-mutating: returns new dicts.
    """
    scored = []
    for p in products:
        s = score(query, p.get(name_key, ""))
        scored.append({**p, "match_score": s})
    scored.sort(key=lambda d: d["match_score"], reverse=True)
    return scored
