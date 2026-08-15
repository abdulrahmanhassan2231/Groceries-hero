# GroceryCompare Android app

Native Android (Kotlin, Jetpack Compose, min SDK 26) client for the GroceryCompare backend.
Type or speak a grocery item → ranked list of where it's cheapest this week across Lidl,
Aldi Süd/Nord, Netto Marken-Discount and Kaufland, with €/kg or €/l unit prices, offer
validity, offer-vs-regular flag, and distance to the nearest branch.

## Stack
MVVM · Hilt · Retrofit/OkHttp + kotlinx.serialization · Room (offline cache) · WorkManager
(daily price-drop check) · Coroutines/Flow · Compose Material 3 · German + English.

## Build
Requires Android Studio (Koala+) or the Android SDK/command-line tools.

```bash
cd android
# generate the gradle wrapper jar once (not committed):
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```
Create `android/local.properties` with `sdk.dir=/path/to/Android/sdk`.

## Pointing at the backend
`app/build.gradle.kts` sets `API_BASE_URL`:
- Emulator → `http://10.0.2.2:8000/` (host machine).
- Physical device → your machine's LAN IP (uncomment the debug `buildConfigField`).

## Structure
```
data/remote     Retrofit GroceryApi + DTOs (mirror the backend's normalized models)
data/local      Room: OfferEntity/WatchedItemEntity, DAOs, AppDatabase (offline cache)
data/repo       GroceryRepository — offline-first: cache + network refresh, basket, drops
di              Hilt module (Retrofit/OkHttp/Room)
ui/navigation   AppNavigation — bottom-nav + NavHost with animated screen transitions
ui/search       SearchViewModel + SearchScreen (PLZ, autocomplete, voice, ranked cards)
ui/list         ShoppingListViewModel + ShoppingListScreen (best store vs optimal split)
ui/watched      WatchedViewModel + WatchedScreen (watched items + price-drop status)
ui/components    OfferCard, ChainBadge, AnimatedPrice, Shimmer skeletons, EmptyState
ui/theme        Material 3 design system: Color, Type, Theme, per-chain brand visuals
work            PriceRefreshWorker — WorkManager watched-item check + notifications
```

## Design & animations
- **Material 3 design system** with a custom grocery-green palette, tuned typography,
  full light/dark support, edge-to-edge, and a transparent status bar.
- **Per-chain brand identity**: each store gets its brand color and an initials badge.
- **Motion everywhere**:
  - Staggered/animated list item appearance & placement (`Modifier.animateItem()`).
  - **Shimmer skeleton** cards while the first results load.
  - **Animated price counters** that count up when a value appears/changes.
  - **Expandable offer cards** (`animateContentSize`) revealing details + watch toggle.
  - A glowing **"Bester Preis"** ribbon on the cheapest result.
  - **Pull-to-refresh**, animated suggestion chips, and horizontal screen transitions.

## Features wired
- **3 tabs** (Search · Shopping list · Watched) via an animated bottom navigation bar.
- **Search** with **PLZ input**, debounced **autocomplete**, and **voice input**
  (RecognizerIntent, de-DE).
- **Offline-first**: results render from Room instantly; network refresh updates them.
- **Distance** shown per offer (device location → backend store locator).
- **Watch price** (bell toggle) → `PriceRefreshWorker` notifies on drops.
- **Shopping-list mode** via `/basket`: best single store vs optimal per-item split.
- **Disclaimer** shown on the results surface.

> Note: this module is a working skeleton with the full architecture in place. It has not
> been compiled in the delivery environment (no Android SDK); build it in Android Studio.
