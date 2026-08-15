# GroceryCompare 🇩🇪 — where's this grocery item cheapest this week?

> Type or speak a grocery item (e.g. *"Kartoffeln"*) and get a **ranked list** of where
> it's cheapest this week across **Lidl, Aldi Süd, Aldi Nord, Netto Marken-Discount and
> Kaufland** — with store, product name, **€/kg or €/l unit price**, offer validity dates,
> whether it's a regular price or a weekly *Angebot*, and distance to the nearest branch.

> ⚠️ This feature branch (`claude/grocery-price-comparison-ei3i9c`) repurposes the
> repository for the GroceryCompare project. The legacy WhatsApp bulk-message script
> (`script.py`, `chromedriver.exe`, `Customer bulk email data.xlsx`) is left in place and is
> unrelated to this project.

## What's here

| Path | What | Status |
|---|---|---|
| [`docs/01-data-source-research.md`](docs/01-data-source-research.md) | **Deliverable 1** — per-chain data-source research, stability & legal notes | ✅ |
| [`backend/`](backend/) | **Deliverable 2** — FastAPI scraper + normalizer, one chain (Marktguru) end-to-end; the other four scaffolded (**Deliverable 4**) | ✅ backend + primary chain working & tested |
| [`android/`](android/) | **Deliverable 3** — Kotlin/Compose app skeleton, search wired to the backend | ✅ skeleton (build in Android Studio) |
| [`docs/architecture.md`](docs/architecture.md) | System architecture diagram + decisions | ✅ |
| [`docs/legal-and-robustness.md`](docs/legal-and-robustness.md) | robots.txt/ToS/rate-limit/isolation policy | ✅ |

## The core idea
- **One aggregator covers all five chains.** Marktguru exposes a PLZ-keyed JSON
  offer API used by its own web app. That's the primary source and the end-to-end slice.
  Each chain's first-party leaflet feed is an isolated **fallback**, disabled until verified.
- **Normalization is the hard part** and it's done deterministically on the server:
  parse German package sizes (`2 kg`, `6 x 1,5 l`, `500-750 g`, `12 Eier`), convert to
  **€/kg / €/l**, and fuzzy-match German synonyms (Kartoffeln ↔ Speisekartoffeln ↔
  festkochend). Fully unit-tested.
- **The phone never scrapes.** All fetching/parsing runs server-side on a weekly schedule
  (Sun night / Mon morning); the app just reads a clean normalized API and caches it in
  Room for offline use.
- **Everything is keyed by PLZ + offer week** — regional pricing and the Aldi Süd/Nord
  split demand it.

## Quick start (backend)
```bash
cd backend
pip install -r requirements.txt
python3 -m pytest -q                 # 30 tests: normalizer + primary connector + service
uvicorn app.main:app --port 8000     # serves /search /autocomplete /basket /health
```
See [`backend/README.md`](backend/README.md) and [`android/README.md`](android/README.md).

## Status against the deliverables
1. ✅ **Data-source research per chain** — availability, stability, legal notes.
2. ✅ **Backend scraper + normalizer for ONE chain, end to end** — Marktguru connector
   (covers all 5 advertisers) → normalizer → PLZ/week SQLite cache → `/search` API, with a
   fixture-driven test proving the full path.
3. ✅ **Android skeleton with search against that chain** — MVVM/Compose/Hilt/Retrofit/Room/
   WorkManager; search, autocomplete, voice, distance, watch, offline.
4. 🚧 **Remaining chains** — Lidl, Aldi Süd/Nord, Netto, Kaufland parsers
   scaffolded and isolated; each is disabled until its live endpoint is captured and a
   fixture test is added (the research doc records how). Marktguru already returns offers
   for all five today.

> **Disclaimer shown in-app:** *Prices are indicative — verify in store.*
