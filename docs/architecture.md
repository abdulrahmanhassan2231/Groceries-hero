# Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  Android app (Kotlin, Compose, min SDK 26)                            │
│  MVVM · Hilt · Retrofit/OkHttp · Room · WorkManager · Coroutines/Flow │
│                                                                        │
│   UI (Compose)  ─▶  ViewModel  ─▶  Repository  ─▶  Retrofit ──┐        │
│                                        │                       │        │
│                                        ▼                       │        │
│                                   Room cache (offline)         │        │
│                                                                │        │
│   WorkManager (daily) ─▶ Repository.checkPriceDrops ─▶ notify  │        │
└────────────────────────────────────────────────────────────┼─┼───────┘
                     the phone NEVER scrapes; it only calls ▼ ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Backend (FastAPI, Python)                                            │
│                                                                        │
│   GET /search /autocomplete /basket /health   ◀── clean normalized    │
│         │                                          API (models.py)     │
│         ▼                                                              │
│   search.py  ── ranks by €/kg, distance, relevance                    │
│         │                                                              │
│         ▼                                                              │
│   SQLite cache  keyed by (chain, plz, offer_week)                     │
│         ▲                                                              │
│         │ weekly (Sun 23:30 / Mon 06:00 Europe/Berlin)                │
│   scheduler/jobs.py ── runs each connector ISOLATED (try/except)      │
│         │                                                              │
│         ▼                                                              │
│   normalizer/  units.py (€/kg,€/l)  matcher.py (fuzzy)  synonyms.py   │
│         ▲                                                              │
│   chains/  base.ChainParser (robots + rate-limit + UA)               │
│      ├─ marktguru.py  ◀── PRIMARY: one API, all 5 chains, PLZ-keyed   │
│      ├─ lidl.py  aldi_sued.py  aldi_nord.py  netto.py  kaufland.py    │
│      └─ (first-party fallbacks; disabled until individually verified) │
└─────────────────────────────────────────────────────────────────────┘
```

## Key decisions
- **Aggregator-first.** Marktguru's PLZ-keyed API covers all five chains, so the
  end-to-end slice ships fast. First-party parsers are isolated fallbacks/cross-checks.
- **Normalization is server-side and deterministic.** `units.py` re-derives €/kg and €/l
  from price + parsed size (multipacks, ranges, ml/cl/l/g/kg, counts). Fully unit-tested.
- **PLZ + offer-week is the cache/version key.** Regional pricing and Aldi Süd/Nord splits
  demand it; the ISO offer week is the natural cache-buster.
- **Isolation.** Each connector runs in its own try/except; failure marks only that chain
  `stale` and never clobbers last-known-good cache. `/health` exposes per-chain status.
- **Offline-first client.** Room holds the last results; the UI renders cache instantly and
  refreshes over the network when available.

## Data flow for one search ("Kartoffeln")
1. App calls `GET /search?q=Kartoffeln&plz=80331&lat=..&lon=..`.
2. Backend loads the region's cached offers (current offer week, else latest cached).
3. Fuzzy-matches the query (synonyms: Kartoffeln↔Speisekartoffeln↔festkochend).
4. Attaches nearest-branch distance from device location + store locator.
5. Ranks by €/kg ascending, returns the normalized list + disclaimer.
6. App persists to Room and renders; later searches work offline.
