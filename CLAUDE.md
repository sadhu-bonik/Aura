# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Read [`AGENTS.md`](./AGENTS.md) first** — it is the authoritative source for domain invariants, coding rules, and docs index. This file adds build mechanics and architectural context that require reading multiple files to derive.

---

## Build commands

Set `JAVA_HOME` before running Gradle or builds will fail:

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew assembleDebug          # build debug APK
./gradlew assembleRelease        # build release APK
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device/emulator required)
./gradlew lint                   # Android Lint
```

`google-services.json` must be present in `app/` (gitignored — ping thongumathoiba@gmail.com).  
YouTube API key goes in `local.properties` as `YOUTUBE_API_KEY=…` (read by `app/build.gradle.kts` into `BuildConfig`).

---

## Architecture

**Single Activity + Jetpack Navigation + MVVM + Repository + Firebase.**

```
Fragment  →  ViewModel  →  Repository  →  Firebase SDK
```

- `MainActivity` (`ui/common/`) is the only Activity. Every screen is a Fragment registered in `res/navigation/nav_graph.xml`.
- ViewModels expose `LiveData` (one-shot results) or `StateFlow` (ongoing state) — never raw Firebase types.
- Repositories (`data/repository/`) are the **only** place Firebase SDK classes appear. Write methods return `Result<T>`; stream methods return `Flow<T>`.
- No DI framework — repositories are constructed manually in ViewModel factories.

### Key data flow patterns

**Streaming (real-time):** Repository returns `Flow<List<T>>` backed by a Firestore snapshot listener → ViewModel collects in `viewModelScope` → Fragment observes `LiveData`.

**One-shot writes:** ViewModel calls `repository.doSomething()` → `Result<Unit>` → ViewModel updates a `LiveData<UiState>`.

**Multi-document atomicity:** Repositories use `firestore.runTransaction { }` or `firestore.batch()` for writes that must be atomic (e.g., flipping `status` + `chatUnlocked` together on deal acceptance).

### Navigation & role gating

Role is fixed at registration (`creator` or `brand`). The bottom nav and available destinations differ per role — role-specific fragments check `SessionManager(context).getUserRole()` and refuse to render if the role doesn't match. Deep links into role-gated screens must also validate role.

### Firestore model conventions

Every `data/model/` data class must have **default values on every property** so Firestore's `toObject()` can deserialize via the no-arg constructor. Field names are camelCase. See `docs/FIRESTORE_SCHEMA.md` for full collection/field listing.

### FCM & notifications

`AuraMessagingService` (`firebase/`) saves the FCM token to `users/{uid}.fcmToken` on refresh. Push notifications from Firestore triggers (Cloud Functions) are not yet wired — in-app notification state is read directly from a `notifications` Firestore collection.

---

## Where new code goes

| What you're adding | Where |
|---|---|
| New screen | Fragment in `ui/<feature>/`, entry in `nav_graph.xml` |
| Shared RecyclerView adapter | `adapters/` |
| Screen-specific adapter | same package as the Fragment |
| Firestore read/write | `data/repository/<Domain>Repository.kt` |
| New Firestore collection | update `docs/FIRESTORE_SCHEMA.md` |
| New string / color / dimen | `res/values/strings.xml`, `colors.xml`, `dimens.xml` |
| Reusable drawable | `res/drawable/` with `bg_` (backgrounds), `ic_` (icons) prefix |

For any non-trivial feature, write `docs/features/<name>.md` before writing code (see `AGENTS.md §7`).
