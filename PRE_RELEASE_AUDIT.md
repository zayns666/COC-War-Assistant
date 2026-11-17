# V1.3 Pre-release audit — NOT READY FOR MAIN

Source baseline: user-provided `COC-War-Assistant-main (1).zip`.

## Changes in this candidate
- Remote meta feed now validates version, unique IDs, TH ranges, status, and guidance before caching.
- Remote URL parsing requires HTTPS, valid host and no embedded credentials; redirects remain disabled and response size capped.
- Stale Gemini/App Check error text removed from ViewModel.
- CI workflow now runs unit tests before assembling the debug APK.
- Added four validator unit tests (NOT executed in this environment).

## Critical blockers (NOT resolved)
1. `BaseAnalysisRepository` intentionally does not perform computer vision; real trained and evaluated COC detection model is missing.
2. No validated current-patch strategies for each TH. Bundled strategy is explicitly an unverified example.
3. MediaProjection capture, overlay interactions, and TTS require device testing.
4. Online updater is not wired to app startup/UI and has no verified publication endpoint.
5. Unit tests and Android build need to run in GitHub Actions; this environment has no Android SDK configured.

## Release decision
NO-GO for `main`. Treat this as an isolated preflight candidate, not a working AI scanner. Test in a separate branch, only merge after device and model tests pass.
