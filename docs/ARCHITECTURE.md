# Architecture

Companion to `AGENTS.md` §2–§5. Read this when adding a new screen or feature, or when deciding where a file belongs.

---

## Layering

```
Fragment (ui/)           ← observes LiveData, forwards user intents
   │
   ▼
ViewModel (ui/)          ← holds UI state, calls repository, exposes LiveData/Flow
   │
   ▼
Repository (data/)       ← the ONLY layer that talks to Firebase
   │
   ▼
Firebase Managers        ← thin wrappers (FirebaseAuthManager, FirestoreManager, StorageManager)
   │
   ▼
Firebase SDK
```

**Rule**: each arrow goes one direction. A Fragment never imports `FirebaseFirestore`. A ViewModel never constructs a `Fragment`. A repository never observes `LiveData`.

---

## Where code goes

| You are writing… | It goes in… |
|---|---|
| A new screen (XML + Kotlin) | `ui/<role-or-area>/<feature>/` |
| The ViewModel for that screen | Same package as the Fragment |
| A Firestore read or write | `data/repository/<Name>Repository.kt` |
| A Firestore-mapped data class | `data/model/<Name>.kt` |
| A Firebase setup helper | `firebase/` |
| A pure Kotlin helper, extension, or constant | `utils/` |
| A RecyclerView adapter used by >1 screen | `adapters/` |
| A RecyclerView adapter used by 1 screen | That screen's package |
| Navigation plumbing | `navigation/` + `res/navigation/nav_graph.xml` |

---

## Single-Activity pattern

- `MainActivity` (in `ui/common/`) is the only `Activity`.
- It hosts a `NavHostFragment` that drives every screen.
- The nav graph is one file: `res/navigation/nav_graph.xml`.
- Bottom navigation varies by role: `res/menu/bottom_nav_creator.xml` and `bottom_nav_brand.xml`. Swap the menu at runtime based on the signed-in user's role.

---

## ViewModel conventions

- Expose state as `LiveData<T>` (or `StateFlow<T>`). Keep the mutable backing field `private val _name`, public `val name = _name` as `LiveData`.
- Load in `init { … }` only if the screen has no arguments to wait for; otherwise expose a `load(args)` method the Fragment calls in `onViewCreated`.
- Surface loading/error as separate `LiveData<Boolean>` / `LiveData<String?>` — every data screen must handle loading/empty/error (see `docs/UI_TOKENS.md` §8).
- Use `viewModelScope` for coroutines.

---

## Repository conventions

- One repository per domain area, not per collection.
- Read methods return `Flow<T>` (from Firestore snapshot listeners) or `suspend fun` one-shots.
- Write methods return `Result<T>` — never throw up to the ViewModel.
- Enforce invariants here (see `AGENTS.md` §4). Example: `DealRepository.acceptDeal(id)` is the one place where `status` flips to `"accepted"` AND `chatUnlocked` flips to `true` — in a single Firestore transaction.

---

## Package rename (one-time)

The Android Studio template created the app under `com.example.aura2`. We are renaming to `com.aura.app`.

Checklist for the person doing the rename:

1. `app/build.gradle.kts` — change `namespace` and `applicationId` to `com.aura.app`.
2. Move `app/src/main/java/com/example/aura2/*` to `app/src/main/java/com/aura/app/ui/common/` (MainActivity lives here).
3. Update the `package` line in every Kotlin file.
4. `AndroidManifest.xml` — the `.MainActivity` reference resolves via namespace; verify the app still builds.
5. Delete the empty `com/example/aura2/` directory.
6. Sync Gradle, run the app once to confirm it launches.

Commit as: `chore: rename package to com.aura.app`.
