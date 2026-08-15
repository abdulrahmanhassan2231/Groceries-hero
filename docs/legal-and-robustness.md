# Legal & robustness policy

This project reads publicly visible weekly grocery offers to help a shopper compare prices.
It is not affiliated with any retailer. The rules below are enforced in code where noted.

## Legal footing
- **No official third-party APIs exist** for these chains. We use (a) an aggregator
  (Marktguru) whose ToS we monitor, and (b) reverse-engineered first-party endpoints. This
  is a pragmatic, good-faith footing, not a contractual right. Prefer a real data agreement
  (e.g. Bonial's partner program) if the product grows.
- **robots.txt is respected** per host. `backend/app/robots.py` fetches and caches
  `robots.txt` and every connector calls `robots_allows(url)` before its first request.
- **Identifying User-Agent.** All outbound requests send a real contact UA
  (`GroceryCompare/0.1 (+contact@example.com)`), configurable via `HTTP_USER_AGENT`. No
  spoofing of a browser to evade blocks.
- **Rate limiting.** A shared token-bucket limiter (`backend/app/ratelimit.py`) caps
  requests per host. Scraping runs on a **weekly** cadence, not per user request — the
  phone never scrapes (see below).
- **Caching / minimal load.** Results are cached by `(chain, plz, offer_week)` and only
  refreshed when the offer week rolls over (Sun night / Mon morning). One shopper searching
  100 times triggers zero extra upstream calls.
- **No redistribution of protected assets.** We store normalized price *facts* (not
  copyrightable) — product name, price, unit, dates. We do **not** re-host leaflet page
  images or retailer logos/branding.
- **Disclaimer.** Every client surface shows *"Prices are indicative — verify in store."*
  Offer data can be stale, regional, or misparsed.
- **Kill switch.** Each connector has an `enabled` flag and can be disabled instantly
  (env `DISABLED_CHAINS=aldi_nord,netto`) if a chain objects or its ToS changes.

## Why scraping lives on the server, never the phone
- Avoids per-device IP blocks and CAPTCHAs.
- No battery/data drain on the user's phone.
- Avoids Google Play policy risk around scraping from the app.
- One server fetch serves all users in a region for the whole week.

## Parser isolation (one break ≠ total outage)
- Each chain is a subclass of `ChainParser` in its own module.
- The scheduler runs each connector in a `try/except`; a failure marks that chain `stale`
  and records the error, but the other chains and all cached data keep serving.
- `GET /health` reports per-chain status (`ok` / `stale` / `disabled`) and last success
  time so breakage is visible.
- Every parser is validated against a captured fixture in `backend/tests/fixtures/`.
