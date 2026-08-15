"""Runtime configuration, all overridable via environment variables.

Values can be set as real environment variables or placed in a ``.env`` file in the
backend folder (easiest on Windows — no need to fiddle with `set`/System Properties).
See ``.env.example``.
"""
from __future__ import annotations

import os
from datetime import date

# Load a .env file from the backend folder if python-dotenv is available (it ships with
# uvicorn[standard]). This runs before Settings reads any variable.
try:
    from dotenv import load_dotenv

    _here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # backend/
    load_dotenv(os.path.join(_here, ".env"))
except Exception:
    pass


def _env_set(name: str) -> set[str]:
    raw = os.getenv(name, "")
    return {x.strip() for x in raw.split(",") if x.strip()}


class Settings:
    HTTP_USER_AGENT = os.getenv(
        "HTTP_USER_AGENT", "GroceryCompare/0.1 (+contact@example.com)"
    )
    REQUEST_TIMEOUT_S = float(os.getenv("REQUEST_TIMEOUT_S", "15"))
    PER_HOST_RPS = float(os.getenv("PER_HOST_RPS", "1"))  # polite: 1 req/sec/host

    # Marktguru
    MARKTGURU_BASE = os.getenv("MARKTGURU_BASE", "https://api.marktguru.de/api/v1")
    MARKTGURU_WEB = os.getenv("MARKTGURU_WEB", "https://www.marktguru.de")
    MARKTGURU_CLIENT_KEY = os.getenv("MARKTGURU_CLIENT_KEY")  # pin a known-good key
    MARKTGURU_API_KEY = os.getenv("MARKTGURU_API_KEY")

    DEFAULT_PLZ = os.getenv("DEFAULT_PLZ", "80331")  # Munich
    DB_PATH = os.getenv("DB_PATH", "grocery.db")

    DISABLED_CHAINS = _env_set("DISABLED_CHAINS")

    # Data source toggle: prefer the aggregator (covers all chains) unless disabled.
    USE_AGGREGATOR = os.getenv("USE_AGGREGATOR", "1") == "1"

    # Demo mode: auto-seed packaged sample offers for any searched PLZ that has no data,
    # so the app works out of the box with no keys and no manual seeding.
    DEMO_MODE = os.getenv("DEMO_MODE", "0") == "1"


settings = Settings()


def offer_week(d: date | None = None) -> str:
    """ISO-week string used as the cache/version key, e.g. ``2026-W34``.

    German discounter offers run Mon–Sat and refresh for the new week; keying by ISO
    week is the natural cache-busting version.
    """
    d = d or date.today()
    iso = d.isocalendar()
    return f"{iso.year}-W{iso.week:02d}"
