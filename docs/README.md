# Docs

Project documentation is split by purpose:

| File | Purpose |
| --- | --- |
| [`synth-format-spec.md`](synth-format-spec.md) | Binary `.synth` layout, reader/writer behavior, and synthesis notes. |
| [`../schemas/synth.schema.json`](../schemas/synth.schema.json) | JSON interchange schema used by CLI and package bindings. |
| [`../references/json/`](../references/json/) | JSON generated from every reference `.synth` file. |

Keep format behavior in `synth-format-spec.md`. Keep machine-readable JSON rules in `schemas/synth.schema.json`.
