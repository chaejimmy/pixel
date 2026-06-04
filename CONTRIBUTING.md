# Contributing

## Canonical package root

This project standardizes on a single Kotlin/Java package root:

```
com.shourov.apps.pacedream
```

**All new files must use this root.** Do not add files under the legacy
`com.pacedream.*` root — it is being migrated out (see below).

### Why this root is canonical

| Evidence | Value |
| --- | --- |
| `app/build.gradle.kts` → `applicationId` | `com.shourov.apps.pacedream` |
| `app/build.gradle.kts` → `namespace` | `com.shourov.apps.pacedream` |
| Source files under `com.shourov.apps.pacedream.*` | 572 |
| Source files under `com.pacedream.*` (legacy) | 160 |
| Modules declaring a `com.shourov.apps.pacedream.*` namespace | nearly all |

The `applicationId` is the published identity of the app and cannot change
without breaking installs and store listings, so it anchors the decision.
`BuildConfig`, the design system, and the icon facade already live under this
root, and the overwhelming majority of modules declare their namespace there.

### Naming convention by module type

| Module kind | Package / namespace pattern | Example |
| --- | --- | --- |
| App | `com.shourov.apps.pacedream` | `:app` |
| Core | `com.shourov.apps.pacedream.core.<name>` | `:core:network` → `...core.network` |
| Feature | `com.shourov.apps.pacedream.feature.<name>` | `:feature:wanted` → `...feature.wanted` |
| Design system | `com.shourov.apps.pacedream.designsystem` | `:core:designsystem` |

When you create a module, set its `namespace` in `build.gradle.kts` to match
the pattern above and place sources under the matching directory.

### Legacy `com.pacedream.*` — being migrated

A second root, `com.pacedream.*`, exists for historical reasons and is being
retired module-by-module (low-risk, one module per PR) to avoid a risky
big-bang refactor. Modules still on the legacy root are tracked as follow-up
tickets in the migration PR. Do not introduce new code under this root.
