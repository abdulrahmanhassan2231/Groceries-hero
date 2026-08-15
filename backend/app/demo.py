"""Demo mode: auto-seed sample offers so the app works with zero manual steps.

When ``DEMO_MODE=1`` and a search hits a PLZ with no cached offers, we load the packaged
sample dataset (``app/demo_offers.json``) for that PLZ on the fly. This lets you type any
zipcode into /search and immediately see results, without keys or a manual seed step.

The sample data is clearly indicative and carries the same "verify in store" disclaimer.
Turn it off (unset DEMO_MODE) to use only real fetched data.
"""
from __future__ import annotations

import json
import os

from .config import offer_week, settings
from .store import db

_DEMO_PATH = os.path.join(os.path.dirname(__file__), "demo_offers.json")


def seed_plz(plz: str) -> int:
    """Seed the packaged demo offers for one PLZ + current offer week. Returns count."""
    # imported here to avoid a circular import at module load
    from .chains.marktguru import MarktguruParser

    with open(_DEMO_PATH, encoding="utf-8") as fh:
        raw = json.load(fh)
    offers = MarktguruParser.parse_response(raw, plz)
    return db.replace_offers(plz, offer_week(), offers)


def ensure_seeded(plz: str) -> int:
    """If demo mode is on and this PLZ has no offers this week, seed it. Returns count."""
    if not settings.DEMO_MODE:
        return 0
    if db.load_offers(plz, offer_week()):
        return 0
    return seed_plz(plz)
