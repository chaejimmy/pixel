<!--
PaceDream Android — PR template

The checkboxes below are MANDATORY for any UI-touching change. Tick them
yourself before requesting review — reviewers will bounce a PR that ships
with unchecked boxes (or with the section deleted). Tag-only / non-UI
changes can mark a box `[N/A]` with a one-line note.

See `DESIGN_QA_REPORT_ANDROID.md` §4 ("testTag scaffolding") and
`DESIGN_SYSTEM_README.md` for the underlying rationale.
-->

## Summary

<!-- 1–3 bullet points. Lead with WHY, not WHAT. -->

-

## Test plan

<!-- How a reviewer can verify this locally. Bulleted markdown checklist. -->

- [ ]

## Design-system & QA guardrails

- [ ] Light + dark `@Preview` added for any new screen composable
- [ ] `testTag` applied to new interactive elements (root + key surfaces),
      registered in the feature's `<Feature>TestTags` registry
- [ ] No new `Color(0x…)` / `.sp` / `RoundedCornerShape(N.dp)` in `feature/**`
      (use `PaceDreamColors.*`, `PaceDreamTypography.*`,
      `PaceDreamRadius.*`)
- [ ] `designSystemCheck` passes locally
- [ ] If this PR adds a **destructive action** (cancel, delete, sign-out,
      refund): confirmation dialog + error surfacing both present
- [ ] If this PR adds a **callback parameter** on a public composable: it is
      NOT defaulting to `{}` unless the call site is preview-only

<!--
Reviewer note: the `designSystemCheck` task lives in `build-logic`. Run
locally with `./gradlew designSystemCheck` before pushing.
-->
