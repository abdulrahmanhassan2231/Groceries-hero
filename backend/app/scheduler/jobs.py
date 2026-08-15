"""Scheduled refresh jobs.

German discounter weeks refresh Mon–Sat; new offers land for the new week, so we refresh
Sunday night and Monday morning. Each connector runs isolated: one failure marks only that
connector stale and never blocks the others or clobbers last-known-good cache.
"""
from __future__ import annotations

import asyncio
import logging

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger

from ..chains import active_parsers
from ..config import offer_week, settings
from ..models import NormalizedOffer
from ..store import db

log = logging.getLogger("scheduler")

# PLZ regions to keep warm. Munich (home) first; extend per user base.
WARM_PLZ = [settings.DEFAULT_PLZ]


async def refresh_region(plz: str) -> int:
    """Run every active connector for one region and persist the merged result."""
    week = offer_week()
    all_offers: list[NormalizedOffer] = []
    for parser in active_parsers():
        try:
            offers = await parser.fetch(plz)
            all_offers.extend(offers)
            parser.last_error = None
            db.set_connector_state(parser.source, "ok")
            log.info("connector %s: %d offers for %s", parser.source, len(offers), plz)
        except Exception as exc:  # isolation: never let one break the batch
            parser.last_error = str(exc)
            db.set_connector_state(parser.source, "stale", str(exc))
            log.warning("connector %s failed for %s: %s", parser.source, plz, exc)

    if all_offers:
        written = db.replace_offers(plz, week, all_offers)
        log.info("persisted %d offers for %s %s", written, plz, week)
        return written
    log.warning("no offers fetched for %s; keeping last-known-good cache", plz)
    return 0


async def refresh_all() -> None:
    for plz in WARM_PLZ:
        await refresh_region(plz)


def start_scheduler() -> AsyncIOScheduler:
    sched = AsyncIOScheduler(timezone="Europe/Berlin")
    # Sunday 23:30 and Monday 06:00 local — around when the new offer week starts.
    sched.add_job(lambda: asyncio.create_task(refresh_all()),
                  CronTrigger(day_of_week="sun", hour=23, minute=30), id="sun_night")
    sched.add_job(lambda: asyncio.create_task(refresh_all()),
                  CronTrigger(day_of_week="mon", hour=6, minute=0), id="mon_morning")
    sched.start()
    return sched
