"""robots.txt compliance: fetch, cache, and check per-host rules."""
from __future__ import annotations

from urllib.parse import urlparse
from urllib.robotparser import RobotFileParser

import httpx

from .config import settings

_cache: dict[str, RobotFileParser] = {}


async def _load(host_root: str) -> RobotFileParser:
    rp = RobotFileParser()
    url = f"{host_root}/robots.txt"
    try:
        async with httpx.AsyncClient(
            timeout=settings.REQUEST_TIMEOUT_S,
            headers={"User-Agent": settings.HTTP_USER_AGENT},
        ) as client:
            resp = await client.get(url)
        if resp.status_code == 200:
            rp.parse(resp.text.splitlines())
        else:
            rp.allow_all = True  # no robots.txt served -> allowed
    except Exception:
        # On network failure be conservative but non-fatal: allow, log upstream.
        rp.allow_all = True
    return rp


async def robots_allows(url: str) -> bool:
    parsed = urlparse(url)
    host_root = f"{parsed.scheme}://{parsed.netloc}"
    if host_root not in _cache:
        _cache[host_root] = await _load(host_root)
    return _cache[host_root].can_fetch(settings.HTTP_USER_AGENT, url)
