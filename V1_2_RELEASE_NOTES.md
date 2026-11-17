# V1.2 — HUD Mini + Voice Guidance (no API key)

Changes:
- HUD starts collapsed, can be expanded via shield/title, and remains draggable.
- New voice-guidance action uses built-in Android Text-to-Speech (Indonesian locale if available).
- Voice advice is explicitly general/manual; no fabricated building detection or live analysis.
- Removed stale Firebase AppCheck calls from MainActivity.
- Retained screenshot/MediaProjection and tactical manual-marking flows.
- Existing MetaUpdater remains a standalone foundation, **not connected to automatic periodic syncing**.

Limitations:
- No trained on-device computer vision detector is bundled. Automatic base scanning still reports this limitation.
- No claim of live voice coaching based on actual game events.
- Google Play Services/Firebase API key is not required for the manual workflow.

Build: push project root contents to your GitHub repository; wait for a NEW Actions run.
