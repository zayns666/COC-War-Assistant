# ZAYNS online meta updater (starter)

This adds `MetaUpdater` (bundled fallback, HTTPS fetch, JSON schema check, 500 KB limit, persistent cache) and `VoiceCommander` (Indonesian Android TTS).

**Integration status:** These are reusable components, NOT wired into the existing dashboard/overlay yet. No continuous frame recognition, live voice decisions, strategy auto-selection, scheduled polling, or proven meta ranking is implemented. Existing Firebase AI code remains and may require Firebase configuration; no claim of fully API-free app yet.

To host a feed, publish `app/src/main/assets/meta/strategies.json` to a public GitHub raw HTTPS URL (or GitHub Pages). Call `MetaUpdater(context).refresh(url)` from a coroutine when online, and `cached()` for offline fallback. The bundled examples are explicitly unverified, not claims about current meta. Validate feeds before publishing; an internet connection alone does not prove accuracy.

Voice: create `VoiceCommander(context)`, call `speak(text)` for a verified instruction and `shutdown()` on owner teardown. Never auto-tap or control Clash of Clans.

## Build
Push to GitHub and run Actions -> Build WarStrat AI APK -> Run workflow. Download the `WarStrat-AI-debug-apk` artifact if build succeeds. A Firebase config or other Gradle issues may require fixes. APK not built or tested in this environment.
