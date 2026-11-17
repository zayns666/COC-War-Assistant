# V1.3 repair candidate — NOT final AI scanner

## Actual changes
- Capture surface resize is transactional with rollback attempts; dimensions and reader change only after successful surface assignment.
- Scan discards frames older than capture request and retries for approximately one second.
- Capture success is shown separately from missing AI model.
- Meta update accepts only raw.githubusercontent.com paths under zayns666/COC-War-Assistant (still NOT wired to automatic updates).
- Existing validator, unit-test definitions and CI test gate retained from V1.3 preflight.

## Still not implemented
- No trained model, no image detection, no real-time tactical strategy or online auto-updates.
- Frame timestamps rely on Android Image timestamp monotonic clock convention; test on target Android device.
- Recovery from partial VirtualDisplay resize failure must be device-tested.

## Verification
- Static source assertions and ZIP CRC only. Gradle build, Android runtime and model accuracy are NOT verified here.
- Work on a development branch. Do not push to main until device tests pass.
