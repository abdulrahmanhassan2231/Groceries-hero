# Deliverable 1 — Data-Source Research (Germany, grocery discounters)

> Goal: find the most stable, legally defensible way to obtain **weekly offer prices**
> for **Lidl, Aldi Süd, Aldi Nord, Netto Marken-Discount, Kaufland**, keyed by **PLZ**
> (region), so the backend can normalize them to €/kg or €/l and rank them.
>
> Scope of verification: endpoints and behaviours below were researched from public
> sources (open-source clients, community wikis, app teardowns). **Anything marked
> ⚠️ UNVERIFIED must be re-confirmed against live traffic** (see "How to verify" at the
> end) before shipping — chains change endpoints and keys without notice. Never hard-code
> an endpoint we have not seen respond.

---

## TL;DR / recommendation

1. **Use an aggregator as the primary source: Marktguru.** A single semi-public JSON API
   returns offers for *all five* target chains, already region-filtered by PLZ. This is by
   far the fastest path to a correct end-to-end slice and is what Deliverable 2 targets.
2. **Treat each chain's own leaflet feed as a fallback / cross-check**, isolated behind a
   per-chain parser (so one breaking never takes the app down). Several chains expose their
   own JSON; the rest need PDF/HTML leaflet extraction.
3. **OCR is a last resort** — only for chains that publish offers as flat images with no
   text layer.
4. **Legally**: none of these expose a *contractual public API for third parties*. All
   access is either (a) an aggregator whose ToS must be read, or (b) reverse-engineered
   first-party endpoints. We proceed on the read-only, low-rate, attributed, cache-heavy
   footing described in `docs/legal-and-robustness.md`, show the "verify in store"
   disclaimer, and are prepared to fall back or remove any source on request.

| Source | Coverage | Shape | Region key | Stability | First choice? |
|---|---|---|---|---|---|
| **Marktguru** | All 5 chains | JSON | `zipCode` (PLZ) | Medium (keys rotate) | ✅ Primary |
| Bonial / kaufDA / MeinProspekt | All 5 (as leaflets) | JSON leaflet index + page images | geo/PLZ | Medium | Fallback aggregator |
| Lidl first-party | Lidl only | JSON (`/webapp/…/products`) | store/region | Medium | Fallback |
| Aldi Süd first-party | Aldi Süd only | JSON/HTML (`assets`/Algolia-style) | region | Low–Med | Fallback |
| Aldi Nord first-party | Aldi Nord only | JSON (community-scraped) | region | Low–Med | Fallback |
| Netto MD first-party | Netto only | HTML/JSON leaflet | PLZ/store | Low | Fallback |
| Kaufland first-party | Kaufland only | JSON (`/api`) | region/store | Medium | Fallback |

---

## Primary source: Marktguru

**What it is.** A German/Austrian offer-aggregator (web + app) that indexes weekly
discounter offers. Its web app is backed by a JSON API that supports free-text search
plus a PLZ filter, and returns per-offer price, brand, unit, validity dates and the
advertiser (retailer). Multiple open-source clients exist that document the call shape,
e.g. `Nusscookie/offers-api` (Node), `sydev/marktguru`, `manmal/marktguru-cli`.

**Endpoint (⚠️ verify keys before use).**

```
GET https://api.marktguru.de/api/v1/offers/search
  ?as=web
  &q=<search term, e.g. Kartoffeln>
  &zipCode=<5-digit PLZ, e.g. 80331>
  &limit=<n>
  &offset=<n>
Headers:
  x-clientkey: <extracted from the web bootstrap bundle>
  x-apikey:    <extracted from the web bootstrap bundle>
  User-Agent:  <our identifying UA>
```

**Auth model.** The `x-clientkey` / `x-apikey` pair is **not a secret we own** — it is the
public client key the marktguru *web app itself* ships. The robust way to obtain it (rather
than pinning a literal that will rotate) is to fetch the web app once, find the JS bundle it
loads, and extract the two keys with a regex, then cache them. This is implemented in
`backend/app/chains/marktguru.py::_bootstrap_keys()` with an env-var override
(`MARKTGURU_CLIENT_KEY`, `MARKTGURU_API_KEY`) for pinning a known-good pair.

**Region model.** `zipCode` drives which region's offers are returned. This is exactly the
PLZ-keyed caching the brief asks for — cache key = `(chain, plz_prefix, offer_week)`.

**Response (observed field families — ⚠️ names verify against live JSON).**

```jsonc
{
  "totalResults": 42,
  "results": [
    {
      "id": 123456,
      "product": { "name": "Speisekartoffeln festkochend", "brand": {"name": "..."} },
      "description": "festkochend, 2 kg Beutel",
      "price": 1.49,
      "unit": "Beutel",
      "unitPrice": 0.75,           // sometimes present; often must be derived
      "unitPriceUnit": "kg",
      "quantity": "2 kg",          // free text -> we parse this
      "validityDates": [ { "from": "2026-08-17", "to": "2026-08-23" } ],
      "advertisers": [ { "name": "ALDI SÜD", "id": 7 } ]
    }
  ]
}
```

The normalizer never trusts `unitPrice`/`unitPriceUnit` blindly: it re-derives €/kg and €/l
from `price` + parsed `quantity`/`description` and only falls back to the provided unit
price when it can't parse a size. See `backend/app/normalizer/units.py`.

**Stability.** Medium. The endpoint has been stable for years but the API keys rotate, and
field names have shifted across app versions. Mitigation: bootstrap keys dynamically,
parse defensively (tolerate missing fields), pin nightly test fixtures, alert on empty
result rates.

**Legal.** Marktguru is itself an aggregator; its data is third-party retailer content. We
read at low rate, attribute the source, cache aggressively (respecting the weekly cadence),
and expose the "indicative — verify in store" disclaimer. We do **not** redistribute their
page images or claim their branding. If Marktguru objects or publishes ToS forbidding this,
we disable the connector and fall back to first-party per-chain parsers.

---

## Fallback aggregator: Bonial (kaufDA / MeinProspekt)

**What it is.** Bonial International (Axel Springer), the "drive to store" leaflet platform;
German consumer brands **kaufDA** and **MeinProspekt**. Covers all five chains as digital
leaflets (Prospekte).

**Shape.** A JSON leaflet index (brochures per retailer per geo) plus per-page **images**.
Because content is delivered as leaflet page images, extracting structured prices means
OCR — heavier and less reliable than Marktguru's structured offers. Bonial also runs a
**commercial partner/publisher program**; a proper B2B data agreement (rather than
scraping) would be the clean long-term route if the product grows.

**Stability.** Medium for the leaflet index; extraction reliability low (image OCR).

**Legal.** Bonial has commercial terms and a partner program → *prefer a data agreement*.
Absent one, treat as fallback only, low-rate, no image redistribution.

---

## Per-chain first-party sources

Each is isolated behind its own parser (`backend/app/chains/<chain>.py`). Assessment and
the concrete extraction strategy per chain:

### Lidl
- **Available:** Lidl's site/app is backed by JSON web endpoints
  (`/webapp/...` product/offers services) and campaign/leaflet feeds; product data is
  comparatively structured.
- **Region:** by selected store/region.
- **Stability:** Medium. Endpoints exist but change with site releases.
- **Strategy:** JSON first (preferred), leaflet PDF/HTML fallback.
- **Legal:** Standard e-commerce ToS; read-only, rate-limited, attributed.

### Aldi Süd
- **Available:** No documented public offers API for Aldi Süd. The site renders weekly
  offers ("Angebote") from JSON/asset feeds; community clients exist but are brittle.
  Third-party scrapers (ScrapingBee, Actowiz, Pepesto for CH) exist, confirming there is
  **no** official third-party API.
- **Region:** Aldi Süd covers southern Germany (incl. **Munich / 8xxxx → this is our home
  region**). Aldi Süd vs Nord split matters: Munich = **Süd**.
- **Stability:** Low–Medium.
- **Strategy:** first-party JSON/asset feed if reachable, else weekly leaflet PDF → text
  extraction, else OCR. Also covered by Marktguru (advertiser "ALDI SÜD").

### Aldi Nord
- **Available:** Community-scraped JSON (e.g. `parse.bot` marketplace lists an "Aldi Nord
  API"); no official public API.
- **Region:** northern/eastern Germany. Not our home region but required for coverage.
- **Stability:** Low–Medium.
- **Strategy:** as Aldi Süd. Marktguru advertiser "ALDI Nord".

### Netto Marken-Discount
- **Available:** Netto (the black-yellow-red *Marken-Discount*, **not** the Danish
  Netto/EDEKA-owned) publishes weekly offers as HTML pages + leaflet; some JSON behind the
  store finder and offer pages.
- **Region:** by PLZ/store.
- **Stability:** Low (page structure changes; heavy marketing markup).
- **Strategy:** HTML/JSON scrape of the weekly offers page, leaflet fallback. Marktguru
  advertiser "Netto Marken-Discount". **Disambiguation guard** needed vs "Netto" (Dansk).

### Kaufland
- **Available:** Kaufland runs a modern stack with JSON `/api` endpoints powering its
  offers pages; regionalized pricing.
- **Region:** strong regional pricing differences — must key by region/store, not just
  brand.
- **Stability:** Medium.
- **Strategy:** JSON first. Marktguru advertiser "Kaufland".
- **Note:** Kaufland also runs an actual *marketplace* (Kaufland.de / real,- successor)
  with a real seller API — that is a different product (marketplace listings, not weekly
  in-store Angebote) and is **not** what we want here.

---

## The regional split cheat-sheet (why PLZ is the cache key)

- **Aldi**: Germany is divided between **Aldi Süd** and **Aldi Nord** — different
  companies, different assortments and prices. Munich (8xxxx) = **Süd**.
- **Kaufland / Netto / Lidl**: single company but **regionally differentiated offers**;
  the same product/week can have different prices by region.
- Therefore every cache row and every API response is keyed by **PLZ** (we bucket by the
  first 1–2 digits where a chain's regions are coarse, full 5 digits where needed), plus
  the **offer week** (ISO week; Mon–Sat cadence, refreshed Sun night / Mon morning).

---

## How to verify each ⚠️ before shipping a connector

1. Open the chain's web offers page in a desktop browser, DevTools → Network → filter XHR.
2. Search an item / change PLZ; capture the request that returns prices (URL, headers,
   query params, response JSON).
3. Save a **redacted** sample response into `backend/tests/fixtures/<chain>_sample.json`
   and write a fixture-driven parser test (see `test_marktguru_parser.py` as the template).
4. Confirm `robots.txt` and ToS for that host; record the decision in
   `docs/legal-and-robustness.md`.
5. Only then flip the connector's `enabled = True`.

This keeps every parser honest: it is validated against a real captured payload, and it is
isolated so a fourth chain breaking cannot break the other three.

## Sources
- Marktguru community clients: [Nusscookie/offers-api](https://github.com/Nusscookie/offers-api),
  [sydev/marktguru](https://github.com/sydev/marktguru),
  [manmal/marktguru-cli](https://github.com/manmal/marktguru-cli)
- [Bonial company / kaufDA](https://www.bonial.com/en/company),
  [kaufDA on Google Play](https://play.google.com/store/apps/details?id=com.bonial.kaufda)
- Aldi API landscape (no official Süd API):
  [Aldi Nord via parse.bot](https://parse.bot/marketplace/397bcd36-69f3-489f-a98d-49e56e4a80a7/aldi-de-api),
  [ScrapingBee Aldi](https://www.scrapingbee.com/scrapers/aldi-api/),
  [Pepesto Aldi CH](https://www.pepesto.com/supermarkets/aldi-ch/)
