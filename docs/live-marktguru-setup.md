# Using live Marktguru data (real prices)

Demo mode ships fake sample offers so the app runs instantly. To get **real** weekly prices,
the backend calls Marktguru's own API. That API needs two keys that Marktguru's website
sends with every request: `x-clientkey` and `x-apikey`. These are **not secrets you own** —
they're the public keys the marktguru.de web app itself uses — but they **rotate**, so we
don't hard-code them.

There are two ways to get them in. Try the easy one first.

---

## Step 1 — just try it (the app may fetch the keys itself)

1. Copy `backend\.env.example` to `backend\.env` (the `run_live.bat` launcher does this for
   you automatically the first time).
2. Run **`backend\run_live.bat`**.
3. Open http://localhost:8000/docs → **POST `/admin/refresh`** → set `plz` to your zipcode →
   **Execute**.
4. Look at the response:
   - `"written": 42` (any number > 0) → 🎉 it worked. Now use **GET `/search`**.
   - `"written": 0` → the auto-fetch of the keys failed. Do Step 2.

The app tries to read the keys straight from marktguru.de on first use. When that works you
never touch DevTools. When Marktguru changes their site it can fail — then capture the keys
manually:

---

## Step 2 — capture the two keys from your browser (5 minutes)

You do this in any desktop browser (Chrome, Edge, Brave, Firefox). You're just copying two
values the website already sends.

1. Open **https://www.marktguru.de** and, if asked, enter your **PLZ** (zipcode).
2. Press **F12** to open Developer Tools. Click the **Network** tab.
3. In the Network filter box, type **`offers`** (and click the **Fetch/XHR** filter).
4. On the website, **search for any product** (e.g. type "Kartoffeln" and hit enter), or
   just reload the offers page. You'll see requests appear — click one whose name starts
   with **`search`** or contains **`offers`** (host `api.marktguru.de`).
5. In the panel that opens, go to **Headers** → scroll to **Request Headers**. Find:
   - `x-clientkey: ....`
   - `x-apikey: ....`
   Copy each value (the part after the colon).
6. Open **`backend\.env`** in Notepad and paste them in, uncommenting the two lines:
   ```
   MARKTGURU_CLIENT_KEY=the-x-clientkey-value
   MARKTGURU_API_KEY=the-x-apikey-value
   DEMO_MODE=0
   ```
   Save the file.
7. Stop the server (CTRL+C) and run **`run_live.bat`** again.
8. http://localhost:8000/docs → **POST `/admin/refresh`** with your zipcode → **Execute**.
   `"written"` should now be > 0. Then **GET `/search`** returns live prices.

> Tip: you can also copy the whole request as cURL (right-click the request → *Copy* →
> *Copy as cURL*) and paste it to me — I can read the exact headers/params/response and
> confirm the connector matches, or fix it if Marktguru changed their field names.

---

## Notes & legal
- Keys rotate every so often; if live refresh starts returning 0 again, repeat Step 2.
- We fetch politely: one identifying User-Agent, rate-limited, cached by PLZ + offer week,
  refreshed weekly — not on every user search. See `docs/legal-and-robustness.md`.
- Marktguru is a third-party aggregator. This is a good-faith, low-volume, personal-use
  setup, not a commercial data agreement. The in-app disclaimer ("prices are indicative —
  verify in store") always applies.
- To switch back to the safe offline demo anytime: run `run_demo.bat` instead.
