"""SQLite-backed offer cache, keyed by (chain, plz, offer_week).

Keeps the last-known-good data so the API keeps serving even when a scrape fails, and so
the weekly refresh only writes when the offer week rolls over.
"""
from __future__ import annotations

import json
import sqlite3
from contextlib import contextmanager
from datetime import datetime, timezone
from typing import Iterable, Optional

from ..config import settings
from ..models import NormalizedOffer

_SCHEMA = """
CREATE TABLE IF NOT EXISTS offers (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    chain        TEXT NOT NULL,
    plz          TEXT NOT NULL,
    offer_week   TEXT NOT NULL,
    product_name TEXT NOT NULL,
    payload      TEXT NOT NULL,          -- full NormalizedOffer as JSON
    fetched_at   TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_offers_key ON offers (plz, offer_week);
CREATE INDEX IF NOT EXISTS idx_offers_name ON offers (product_name);

CREATE TABLE IF NOT EXISTS connector_state (
    source       TEXT PRIMARY KEY,
    status       TEXT NOT NULL,
    last_success TEXT,
    last_error   TEXT
);
"""


@contextmanager
def _conn():
    conn = sqlite3.connect(settings.DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()


def init_db() -> None:
    with _conn() as c:
        c.executescript(_SCHEMA)


def replace_offers(plz: str, offer_week: str, offers: Iterable[NormalizedOffer]) -> int:
    """Atomically replace the cached offers for a (plz, week). Returns count written."""
    now = datetime.now(timezone.utc).isoformat()
    rows = [
        (o.chain.value, plz, offer_week, o.product_name, o.model_dump_json(), now)
        for o in offers
    ]
    with _conn() as c:
        c.execute("DELETE FROM offers WHERE plz=? AND offer_week=?", (plz, offer_week))
        c.executemany(
            "INSERT INTO offers (chain, plz, offer_week, product_name, payload, fetched_at)"
            " VALUES (?,?,?,?,?,?)",
            rows,
        )
    return len(rows)


def load_offers(plz: str, offer_week: str) -> list[NormalizedOffer]:
    with _conn() as c:
        cur = c.execute(
            "SELECT payload FROM offers WHERE plz=? AND offer_week=?", (plz, offer_week)
        )
        return [NormalizedOffer.model_validate_json(r["payload"]) for r in cur.fetchall()]


def latest_week_for(plz: str) -> Optional[str]:
    """Most recent offer week we have cached for a region (for offline fallback)."""
    with _conn() as c:
        cur = c.execute(
            "SELECT offer_week FROM offers WHERE plz=? ORDER BY offer_week DESC LIMIT 1",
            (plz,),
        )
        row = cur.fetchone()
        return row["offer_week"] if row else None


def set_connector_state(source: str, status: str, error: Optional[str] = None) -> None:
    now = datetime.now(timezone.utc).isoformat()
    with _conn() as c:
        existing = c.execute(
            "SELECT last_success FROM connector_state WHERE source=?", (source,)
        ).fetchone()
        last_success = now if status == "ok" else (existing["last_success"] if existing else None)
        c.execute(
            "INSERT INTO connector_state (source, status, last_success, last_error)"
            " VALUES (?,?,?,?) ON CONFLICT(source) DO UPDATE SET"
            " status=excluded.status, last_success=excluded.last_success,"
            " last_error=excluded.last_error",
            (source, status, last_success, error),
        )


def get_connector_states() -> dict[str, sqlite3.Row]:
    with _conn() as c:
        cur = c.execute("SELECT * FROM connector_state")
        return {r["source"]: r for r in cur.fetchall()}
