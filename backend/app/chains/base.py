"""Chain parser interface — the isolation boundary.

Every data source (an aggregator advertiser, or a chain's first-party feed) implements
``ChainParser``. The scheduler runs each in its own try/except, so one breaking parser
marks only *that* chain stale and never takes the others (or the cached data) down.
"""
from __future__ import annotations

import abc
from datetime import date, datetime
from typing import Optional

import httpx

from ..config import settings
from ..models import Chain, NormalizedOffer
from ..ratelimit import HostRateLimiter
from ..robots import robots_allows

_limiter = HostRateLimiter(settings.PER_HOST_RPS)


class ParserError(RuntimeError):
    pass


class ChainParser(abc.ABC):
    #: which normalized chain(s) this parser produces
    chain: Chain
    #: stable source id recorded on every offer, e.g. "marktguru"
    source: str
    #: flip to disable without deleting code
    enabled: bool = True

    def __init__(self) -> None:
        self.last_success: Optional[datetime] = None
        self.last_error: Optional[str] = None

    @property
    def is_enabled(self) -> bool:
        return self.enabled and self.chain.value not in settings.DISABLED_CHAINS

    async def _get(self, client: httpx.AsyncClient, url: str, **kw) -> httpx.Response:
        """Rate-limited, robots-respecting, identified GET."""
        if not await robots_allows(url):
            raise ParserError(f"robots.txt disallows {url}")
        await _limiter.acquire(url)
        headers = {"User-Agent": settings.HTTP_USER_AGENT, **kw.pop("headers", {})}
        resp = await client.get(url, headers=headers, **kw)
        resp.raise_for_status()
        return resp

    @abc.abstractmethod
    async def fetch(self, plz: str) -> list[NormalizedOffer]:
        """Fetch + normalize all current offers for a region. Must be idempotent."""
        raise NotImplementedError

    # ---- shared helpers for subclasses ----
    @staticmethod
    def _parse_date(value: str | None) -> Optional[date]:
        if not value:
            return None
        for fmt in ("%Y-%m-%d", "%d.%m.%Y", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%dT%H:%M:%SZ"):
            try:
                return datetime.strptime(value[: len(fmt) + 4], fmt).date()
            except ValueError:
                continue
        try:
            return datetime.fromisoformat(value.replace("Z", "+00:00")).date()
        except ValueError:
            return None
