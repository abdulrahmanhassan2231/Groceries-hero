# GroceryCompare backend

Scheduled scraper + normalizer that serves a clean, PLZ-keyed API of weekly German
discounter offers. **The phone never scrapes — this server does.**

## Quickest start (demo mode — zero manual steps)
No API keys, no manual seeding. The server auto-loads sample offers for whatever zipcode
you search.

- **Windows:** double-click `run_demo.bat` (or run it in a terminal from the `backend` folder).
- **macOS/Linux:** `bash run_demo.sh`

Then open **http://localhost:8000/docs**, expand **GET /search**, click **Try it out**, set
`q = Kartoffeln` and `plz =` *your zipcode*, and **Execute**. Results appear immediately.

To pre-warm specific regions instead: `python seed.py 80331 85354`.
To use real (non-demo) data, start without `DEMO_MODE=1` (see below).

## Run (manual)
```bash
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

# start the API (also starts the weekly scheduler)
uvicorn app.main:app --reload --port 8000

# trigger a refresh manually (dev)
curl -X POST 'http://localhost:8000/admin/refresh?plz=80331'

# search
curl 'http://localhost:8000/search?q=Kartoffeln&plz=80331&lat=48.137&lon=11.575'
```

## Tests
```bash
cd backend && python3 -m pytest -q
```
`test_units.py` and `test_matcher.py` cover the normalizer (the hard part);
`test_marktguru_parser.py` validates the primary connector against a captured fixture —
the template every other connector must pass before its `enabled` flag flips.

## Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `/search?q=&plz=&lat=&lon=&max_distance_km=&limit=` | Ranked offers for one item |
| GET | `/autocomplete?q=&plz=` | Search suggestions |
| GET | `/basket?items=&items=&plz=` | Best single store vs optimal split |
| GET | `/health` | Per-chain connector status |
| POST | `/admin/refresh?plz=` | Manual refresh (protect in prod) |

## Configuration (env vars)
`HTTP_USER_AGENT`, `PER_HOST_RPS`, `DEFAULT_PLZ`, `DB_PATH`, `DISABLED_CHAINS`,
`USE_AGGREGATOR`, `MARKTGURU_CLIENT_KEY`, `MARKTGURU_API_KEY`, `STORES_PATH`.
See `app/config.py`.

## Adding a chain connector
1. Capture the live request/response (DevTools) and save a redacted sample to
   `tests/fixtures/<chain>_sample.json`.
2. Implement `parse_response()` + `fetch()` in `app/chains/<chain>.py` (subclass
   `ChainParser`).
3. Write a fixture-driven test mirroring `test_marktguru_parser.py`.
4. Record the robots.txt/ToS decision in `docs/legal-and-robustness.md`.
5. Flip `enabled = True`.

See `docs/01-data-source-research.md` for per-chain source notes and
`docs/legal-and-robustness.md` for the compliance policy.
