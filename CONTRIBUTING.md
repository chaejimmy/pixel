# Contributing

## Canonical package root

**All Kotlin code must live under `com.shourov.apps.pacedream`.**

This is the canonical package root for the project. The evidence:

- The application's `applicationId` is `com.shourov.apps.pacedream` (`app/build.gradle.kts`).
- The `:app` module `namespace` is `com.shourov.apps.pacedream`.
- The generated `BuildConfig`, the design system (`com.shourov.apps.pacedream.designsystem`),
  and 38 of 40 Gradle modules already declare a `com.shourov.apps.pacedream.*` namespace.

Historically the codebase grew a second root, `com.pacedream.*`
(`com.pacedream.app.*`, `com.pacedream.common.*`, `com.pacedream.notifications`).
This dual root makes module ownership ambiguous and forces a "which root?"
decision for every new file. We are consolidating onto the canonical root
module-by-module; **do not introduce new files under `com.pacedream.*`.**

### Rules for new code

- New packages and files go under `com.shourov.apps.pacedream`.
- A feature module `:feature:foo` uses `com.shourov.apps.pacedream.feature.foo`.
- A core module `:core:foo` uses `com.shourov.apps.pacedream.core.foo`.
- Keep each module's source package root aligned with its `namespace` in
  `build.gradle.kts`.

### Migration status

Migration is incremental and tracked as follow-up tickets so that no single
PR carries a high-risk, repo-wide rename. See the "Remaining modules to
migrate" section in the platform tracking PR. Modules still on the legacy
`com.pacedream.*` root will be moved over in dependency order.
