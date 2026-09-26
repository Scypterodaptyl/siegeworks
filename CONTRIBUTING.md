# Contributing

## Source layout

`src` is the source of truth for every supported target. Use Stonecutter branches only where Minecraft or loader APIs differ; do not edit generated sources under `versions/*/build`.

Forge-only integrations stay out of targets where their dependencies do not exist.

## Optional integrations

Code that references an optional mod belongs in its integration package and must only be initialized after checking that the mod is loaded.

## Mechanics, configuration and data

- Engine and projectile combat values belong in data definitions.
- Server pacing and policy belong in the server config.
- Loading order, controls, aiming constraints and animation events remain code when they define the mechanic itself.

Do not duplicate a value in code when it already comes from a data definition or server config.

## Verification

Collision, terrain movement, deployment, ladders, siege-tower ramps and projectile behavior have deterministic tests or GameTests. Changes in those areas should update the relevant coverage rather than rely only on a manual client check.

## Assets

Third-party assets need a traceable source and license entry in `THIRD_PARTY_NOTICES.md`.
