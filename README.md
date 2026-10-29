# FACEIT Stats Widget

Android home-screen widget for FACEIT CS2 profile `Elka390` (editable in app).

## Build APK
GitHub Actions builds a debug APK on every push. Open **Actions → Android APK Build → latest run → Artifacts** and download `faceit-stats-widget-debug-apk`.

## Setup on phone
1. Install the downloaded APK.
2. Open **FACEIT Widget** and enter your FACEIT Data API key. The key is saved only in the app's local preferences and is not stored in this repository.
3. Long-press an empty area on the Android home screen → **Widgets** → **FACEIT Stats**.
4. Tap ↻ on the widget to refresh. Android may schedule periodic updates approximately every 30 minutes.

Create/manage a key through the FACEIT Developer Portal. Never commit API keys to this public repository. Availability and names of stats depend on FACEIT API permissions and data.
