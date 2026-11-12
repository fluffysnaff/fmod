# Working defaults

These are revisable defaults, not acceptance gates. Follow current owner intent and explicit constraints within higher-priority instructions and access controls. Depart when a better approach serves the result; explain consequential departures briefly.

## Ownership

- Translate rough wording into the intended outcome; do not mistake terminology for a technical prescription. Preserve explicit constraints. Validate consequential assumptions, numbers and inherited decisions, whoever supplied them.
- Own the result across the project and machine. The repository root, existing code and prior documents do not bound the solution. Make relevant changes wherever authorized; preserve unrelated work.
- Decide routine technical questions independently. Continue authorized work to completion. Ask only for consequential ambiguity, missing information, access or authorization. Planning and review alone do not authorize implementation.

## Execution and code

- Establish current behavior and observable success. Inspect the owning code, interfaces and relevant evidence before editing. Compare alternatives only for consequential tradeoffs; choose the simplest sufficient design and fix the cause.
- Use conventional patterns, precise names, cohesive functions and explicit dependencies. Reuse established functionality and consolidate duplicated knowledge. Add abstractions or dependencies only when they solve a current problem and justify their cost.
- Remove obsolete paths when replacing them. Preserve required contracts and unrelated behavior. Favor clarity over cleverness or minimum line count.
- Where correctness depends on them, make ownership, units, ordering, failure and cancellation explicit. Validate input at boundaries; bound queues, retries and background work. Release or abort must stop output. Keep diagnostics off critical paths.
- Comment only for non-obvious intent, constraints or hazards. Do not narrate code, add decorative banners or duplicate explanations.

## Token and output budget

- Spend tokens on decisions and evidence. Keep required updates to meaningful findings, decisions or blockers. Use plain language. Final responses need only the result, essential verification and material limitations; add a next step when needed. Omit routine narration and repeated summaries.
- Start with scoped `rg` searches, filenames and narrow line ranges. Expand only to resolve a specific uncertainty. Reuse gathered facts; reread when changed or unresolved. Batch independent reads; keep dependent actions sequential.
- Use quiet flags and small output limits sufficient for the result. For noisy builds/tests, save full output to a temporary log and return exit status plus a short result. On failure, inspect relevant excerpts and expand as needed. Preserve failure status; never infer success from silence or truncation. Keep secrets out of logs.
- Avoid duplicate plans, reports and receipt files. Keep one concise checkpoint only when work needs resuming: decisions, exact working state, essential evidence and next action. Use skills, research and reviews only when they resolve a concrete need; stop when actionable uncertainty is resolved.

## Verification and progress

- Verify changed behavior through the actual integration path when practical, including relevant failure and boundary cases. Use proportionate checks; inspection can suffice for simple edits. Add tests for meaningful contracts and regressions, not implementation details.
- Run necessary checks once; repeat or broaden for changes, failures, remaining risk or explicit requirements. Investigate the first loss of progress before adding workarounds.
- Before further experiments, preserve a reproducible working candidate, including uncommitted source, build, settings and results. Compare observable behavior; long runs must advance the product or resolve a decision. Define a stopping condition and clean up owned processes.
- Claim only observed results. Distinguish inference and unverified behavior. Passing tests or low latency alone do not establish product quality.

## Git

- Before editing, check branch, working-tree status and affected-file changes. Preserve unrelated tracked and untracked work. Follow current authorization for commits, pushes and destructive operations.
- Stage exact paths or hunks; inspect the staged diff. Exclude secrets, logs, local state, plans, prompts and generated artifacts unless explicitly requested.
- Subject: `<type>: <lower-case imperative summary>`. Types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`, `revert`.
- Omit the body when the subject suffices. Otherwise use concise lowercase `-` bullets for useful context or rationale. No scope, trailers, attribution, authorship/tool references, process narration or filler.

## FMod project

FMod is a client-side Fabric addon for Meteor Client. Its focus is stash hunting, travel helpers, coordinate hiding and utilities. Use [README.md](README.md) for installation and product context, and [docs/FEATURES.md](docs/FEATURES.md) for the feature list and optional-mod requirements.

### Toolchain and commands

- The current target is Minecraft 26.3, Meteor `26.3-SNAPSHOT` and Java 25. Use Mojang class and method names; this project does not use Yarn mappings or the old `modImplementation` dependency setup.
- Treat `gradle.properties`, `build.gradle.kts` and `gradle/wrapper/gradle-wrapper.properties` as the version sources of truth. Use the checked-in Gradle wrapper; set `JAVA_HOME` to a JDK 25 installation. Do not put machine-specific JDK paths in tracked files.
- Dependency or Minecraft upgrades must keep the Java toolchain, wrapper/Loom compatibility, resource metadata, mixin targets, CI Java version and relevant documentation consistent.

| Task | PowerShell / Windows | macOS / Linux |
| --- | --- | --- |
| Unit tests | `.\gradlew.bat test` | `bash ./gradlew test` |
| Build and checks | `.\gradlew.bat build` | `bash ./gradlew build` |
| Package-only changes | `.\gradlew.bat jar` | `bash ./gradlew jar` |
| Development client | `.\gradlew.bat runClient` | `bash ./gradlew runClient` |

The distributable is `build/libs/fmod-<version>.jar`. Test reports are in `build/reports/tests/test/`.

### Code map and conventions

| Location | Responsibility |
| --- | --- |
| `src/main/java/addon/fmod/FMod.java` | Meteor entrypoint, FMod category/HUD group, core modules and commands |
| `src/main/java/addon/fmod/modules/` | Original FMod modules, including `CoordHider` and `FrameDupe` |
| `src/main/java/addon/fmod/stashhunting/StashHunting.java` | Ported module/HUD registration and optional-mod guards |
| `src/main/java/addon/fmod/stashhunting/` | Stashhunting modules, route modes, HUD elements and webhook support |
| `src/main/java/addon/fmod/mixin/` | Minecraft/Meteor mixins and optional stashhunting integration mixins |
| `src/main/java/addon/fmod/commands/` | Meteor commands and Brigadier argument types |
| `src/main/resources/` | Fabric metadata, mixin configuration and packaged assets |
| `src/test/java/` | JUnit Jupiter tests for meaningful behavior and regressions |

- Follow nearby Java formatting, including the existing upstream style in ported files. Keep unrelated formatting out of functional changes.
- Register new core modules in `FMod`; register stashhunting modules and their dependency conditions in `StashHunting`. Use `FMod.CATEGORY` and `FMod.HUD_GROUP` for addon features. Keep public feature documentation in `docs/FEATURES.md` current.
- Add or remove mixins in `src/main/resources/fmod.mixins.json` alongside their source. Verify target methods/descriptors against the actual dependency version; successful compilation does not prove a mixin can apply.
- TpMine, LiveWalk, InstaMine, InfReach, FrozenWalk and their dedicated hooks/HUD were intentionally removed. Pitch40Util, GrimAirPlace, ElytraFlyPlusPlus, ChestIndex and AFKVanillaFly were also removed; TrailFollower integrates with Meteor's built-in ElytraFly instead. Preserve that decision unless the owner requests their return; unrelated teleport/clip commands still exist.

### Optional integrations and runtime behavior

- Xaero's World Map, Xaero's Minimap, XaeroPlus and Baritone are optional `compileOnly` integrations. FMod must start without them. Keep registration/class loading guarded, and keep cross-mod mixins optional. Do not bundle these mods or make them mandatory as a shortcut.
- Read the dependency predicates in `StashHunting.register()` and the tested versions in `docs/FEATURES.md` before changing integration behavior. Registering a module is not evidence that its pathfinding or world interaction works.
- Account for null players/worlds at startup, disconnect and teardown. Resolve modules after registration, or pass an explicit owner as the search-area modes do. Release simulated input and restore owned state when a module stops or changes modes.
- Keep render/tick/packet callbacks responsive. Webhooks go through `WebhookClient`'s bounded background worker; preserve timeouts, bounded admission, JSON escaping and token-safe error reporting. URLs stay empty by default. Test notification payloads without sending messages to real services.
- Coordinate hiding and randomization are display-only contracts. Synthetic values use `SecureRandom` independently of actual positions, stay coherent across supported displays, refresh on session/regeneration events and are never persisted. Keep Hide mode and suppress unsupported coordinate HUD syntax. It must not alter player positions, packets, route data, exports or configured webhook contents. Cover F3 and Meteor rendering changes with appropriate checks, and document formats/overlays that remain visible instead of claiming complete anonymization.

### Validation and local data

- Use focused unit tests for changed pure behavior, then the relevant Gradle build. For documentation-only edits, check facts, paths and the diff; a game launch is unnecessary. Packaging/metadata edits need a JAR check for expanded metadata, referenced assets, mixin classes and license/notices.
- Mixin, startup or optional-integration changes need a development-client smoke check when practical. Test both absent and installed optional dependencies when the affected code crosses that boundary. Use an isolated game directory and clean up owned test clients.
- Exercise changed world/inventory/network behavior through the game where practical. Report startup, unit tests and actual gameplay as separate evidence. Do not present a local build as verification against a multiplayer server.
- `run/`, `.gradle/`, `build/` and IDE state are local/ignored. Keep game profiles, account settings, server lists, saved routes/stashes, logs, screenshots and webhook URLs out of commits and release artifacts. Do not reuse a player's normal Minecraft profile for tests.
- The logo source/README assets live in `docs/assets/readme/`; the packaged icon is `src/main/resources/assets/fmod/icon.png`. Keep asset paths and filename case correct for Linux builds and JAR lookup.

### Licensing and repository delivery

- The primary license is GPL-3.0-only. Keep `LICENSE`, Fabric metadata, docs and the JAR's `LICENSE_fmod`/`THIRD_PARTY_NOTICES.md` consistent. Preserve upstream copyright and attribution.
- Read `THIRD_PARTY_NOTICES.md` before importing or relicensing source. The stashhunting port's upstream revision has no published license; do not claim that applying FMod's GPL license resolves the outstanding upstream permission issue.
- `origin` is `https://github.com/fluffysnaff/fmod.git`; `gitlab` is the historical GitLab remote. Verify the destination before an authorized push. GitHub is the active project; do not change or unarchive GitLab as part of routine delivery.
- Preserve `archive/26.3`, `archive/1.21.4` and historical release tags as snapshots unless explicitly instructed otherwise. Their versions, features and licensing may differ from current `main`.
- A push to `main` triggers `.github/workflows/dev_build.yml`, which builds and updates the `snapshot` prerelease. Pull requests use `pull_request.yml`. Account for these publishing effects when pushing; archive tags must not trigger development-release publication.
