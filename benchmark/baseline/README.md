# Render benchmark baseline

`RenderBenchmark.renderReferenceSynths` renders every file in `references/synths/` once per op.
These reports come from an Apple Silicon Mac (`Apple M1 Max`, macOS 27.0).
The scores depend on the machine, so compare only runs from comparable hardware.

```bash
./gradlew :benchmark:benchmark :benchmark:checkBenchmarkBaseline   # fails on a regression
./gradlew :benchmark:updateBenchmarkBaseline                       # adopt the newest report
```

The check fails only when the new `score + scoreError` is below the baseline `score - scoreError`.
