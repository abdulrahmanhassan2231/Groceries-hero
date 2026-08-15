"""On-demand live refresh: fetch a region's offers the first time it's searched.

In live mode, if a searched PLZ has no offers cached for the current offer week, we fetch
them from the connectors before serving. A per-PLZ throttle stops a failing fetch (bad
keys / network) from re-running on every keystroke.

Demo mode is handled separately (see demo.ensure_seeded), so this is a no-op there.
"""
from __future__ import annotations

import time

from .config import offer_week, settings
from .store import db

# minimum seconds between live fetch attempts for the same PLZ
_MIN_INTERVAL = 600
_last_attempt: dict[str, float] = {}


async def ensure_fresh(plz: str) -> None:
    if settings.DEMO_MODE:
        return
    if db.load_offers(plz, offer_week()):
        return  # already have this week's data
    now = time.monotonic()
    if now - _last_attempt.get(plz, 0.0) < _MIN_INTERVAL:
        return  # backed off after a recent (possibly failed) attempt
    _last_attempt[plz] = now
    # imported here to avoid importing the scheduler at module load
    from .scheduler import refresh_region

    await refresh_region(plz)
