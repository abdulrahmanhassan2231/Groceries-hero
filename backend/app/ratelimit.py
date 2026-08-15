"""A tiny per-host token-bucket rate limiter (async)."""
from __future__ import annotations

import asyncio
import time
from collections import defaultdict
from urllib.parse import urlparse


class HostRateLimiter:
    def __init__(self, rps: float):
        self.min_interval = 1.0 / rps if rps > 0 else 0.0
        self._last: dict[str, float] = defaultdict(float)
        self._locks: dict[str, asyncio.Lock] = defaultdict(asyncio.Lock)

    async def acquire(self, url: str) -> None:
        host = urlparse(url).netloc
        async with self._locks[host]:
            wait = self._last[host] + self.min_interval - time.monotonic()
            if wait > 0:
                await asyncio.sleep(wait)
            self._last[host] = time.monotonic()
