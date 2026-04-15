# Aura — Agent Instructions

This file is the single source of truth for every AI agent (Claude Code, Copilot, Antigravity) working on this repo. Read it top-to-bottom before doing anything. Load `docs/*.md` only when a task touches that specific area.

---

## 1. What Aura is

Aura is an Android app that connects **content creators** (Instagram, YouTube, TikTok) with **brands** who want to run sponsored-content deals. A creator publishes a portfolio; a brand browses creators, shortlists them, and sends a **deal** (a collaboration offer with a budget and brief); if the creator accepts, an in-app chat opens for the two parties to coordinate; once the work is done, either side can leave a review.

This is a university project for **CSE 3310, Team 14, Spring 2026**, 4 members, due **2026-04-28**. It is graded against the team's SRA document. Scope does not need to go beyond what the SRA describes — extra polish is welcome, extra features are not.

### Feature set (from SRA §4.1–4.10)

1. **Login / Registration** — email + password via Firebase Auth
2. **Profile setup** — role selection (creator or brand) + role-specific profile
3. **Creator activities** — edit profile, manage portfolio, view incoming deals
4. **Brand activities** — create campaigns, browse creators, send deals
5. **Recommendation engine** — rank creators for a brand by niche/rating/followers
6. **Portfolio analytics** — engagement/consistency score from YouTube Data API v3
7. **Send deal** — brand → creator collaboration request with budget
8. **Deal lifecycle** — `pending → accepted → completed`, or `pending → rejected/cancelled`, or auto-expire after 7 days
9. **Messaging** — real-time chat, unlocked only after a deal is accepted
10. **Deal dashboard / history** — both roles see their past and active deals, can leave reviews on completed ones

---

## 2. Stack and why

| Choice | Why |
|---|---|
| Kotlin, minSdk 24, targetSdk 36 | Already set by Android Studio template; covers ~98% of devices |
| **Firebase** (Auth, Firestore, Storage, FCM) | SRA allows any backend; Firebase removes the need to host a server, gives us real-time chat for free, and handles auth. Replaces the SRA's suggested SQLite |
| MVVM + Repository | Standard Android pattern. Keeps Firebase calls out of Fragments so tests and code review stay sane with 4 people editing in parallel |
| Single-Activity + Jetpack Navigation | One `MainActivity`, every screen is a `Fragment`. Avoids fragmented back-stack bugs and lets us use one `nav_graph.xml` |
| Coroutines + Flow / LiveData | Async without callback hell. Flow for repo streams; LiveData for ViewModel → Fragment |
| Material 3 + ViewBinding | No Compose — team is more comfortable with XML layouts for a 2-week runway |
| Glide | Image loading (profile pics, portfolio thumbnails) |
| YouTube Data API v3 | Only external API, used by the Portfolio Analytics feature |
| No DI framework yet | Manual construction in ViewModel factories. Adding Hilt/Koin for a 2-week project is overkill |

---

## 3. Package layout

Root package: **`com.aura.app`** (the Android Studio default `com.example.aura2` is being renamed — see `docs/ARCHITECTURE.md` if you're doing the rename).

```
com.aura.app/
├── ui/
│   ├── auth/              login / register / role selection
│   ├── creator/           profile, portfolio, dashboard (creator-only screens)
│   ├── brand/             profile, campaigns, discovery, shortlist (brand-only screens)
│   ├── deals/             deal inbox & detail — shared by both roles
│   ├── chat/              chat list & chat screen — shared, gated by deal status
│   └── common/            MainActivity, splash, onboarding
├── data/
│   ├── model/             Kotlin data classes mapped to Firestore documents
│   └── repository/        all Firebase I/O goes through here
├── firebase/              thin wrappers around Firebase SDK (Auth / Firestore / Storage managers)
├── utils/                 Constants, Extensions, ValidationUtils, DateUtils
├── navigation/            helpers around Jetpack Navigation
└── adapters/              shared RecyclerView adapters (screen-specific adapters stay in the screen's package)
```

The layering rule: **`ui` → `data.repository` → `firebase`**. UI never talks to Firebase directly.

---

## 4. Domain invariants — MUST hold regardless of where you write the code

These are rules about the *business problem*, not about specific files. Whoever implements a feature is responsible for enforcing the relevant invariants.

1. **Chat is locked until acceptance.** A user can only send or read messages for a deal whose status is `accepted` (or `completed`, for history). The `deals/{id}.chatUnlocked` flag tracks this and MUST flip to `true` only on the `pending → accepted` transition.
2. **Reviews require completion.** A `reviews` document can only be created when its target `deals/{id}.status == "completed"`.
3. **Deals auto-expire.** A deal left in `pending` for 7 days transitions to `expired` (SRA §4.7/§4.8). Client or scheduled function may do this; the UI must treat expired deals as read-only.
4. **Role is fixed at registration.** A user is either a creator or a brand. Role-specific screens must refuse to render if the role doesn't match.
5. **Recommendations are read-only from the client.** The scoring logic is not edited from the app — the `recommendations` collection is populated by a backend process (or, for this project, a manual/admin path).
6. **Firestore fields use camelCase** and Kotlin data classes mapped to Firestore MUST have default values on every property (so Firestore's `toObject()` can deserialize via the no-arg constructor).
7. **Secrets never enter git.** `google-services.json`, API keys, service-account files are gitignored. Each teammate drops their own copy into `app/` locally. **Teammates: ping the repo owner (thongumathoiba@gmail.com) for the current `google-services.json` — the app will not build without it.**

---

## 5. Coding rules — MUST follow when writing code

1. **Fragments are view-only.** Observe `LiveData`/`StateFlow` from a `ViewModel`; forward user actions to the ViewModel. No Firebase, no business branching, no I/O.
2. **Firebase calls only inside `data/repository/`.** ViewModels call repository methods; they do not touch `FirebaseFirestore` / `FirebaseAuth` / `FirebaseStorage` directly.
3. **One `Activity`.** `MainActivity` in `ui/common/` is the host. New screens are Fragments added to `nav_graph.xml`.
4. **No hardcoded strings, colors, or dimens in XML.** Reference `@string/…`, `@color/…`, `@dimen/…`. Tokens are defined in `docs/UI_TOKENS.md`.
5. **Coroutine scope.** Use `viewModelScope` in ViewModels and `lifecycleScope` in Fragments. Never `GlobalScope`.
6. **Commits**: Conventional Commits, lowercase, imperative mood, ≤50 chars. Full rules in `docs/WORKFLOW.md`.
7. **Branching**: `feature/…`, `bugfix/…`, `backend/…`, `docs/…` branched from `develop`. Never push to `main` or `develop` directly. See `docs/WORKFLOW.md`.

---

## 6. Docs index — load on demand

`AGENTS.md` (this file) is always loaded. The following live under `docs/` and are loaded only when the current task touches that area:

| File | Load when the task involves |
|---|---|
| `docs/ARCHITECTURE.md` | adding a new screen or feature; deciding where a file goes; the package rename |
| `docs/FIRESTORE_SCHEMA.md` | any Firestore read/write; adding, removing, or renaming a field or collection |
| `docs/NAMING.md` | naming a class, function, variable, XML id, string, or drawable resource |
| `docs/UI_TOKENS.md` | writing a layout; picking colors, spacing, typography, button/card styles |
| `docs/WORKFLOW.md` | creating a branch, writing a commit message, or opening a PR |
| `docs/features/<name>.md` | implementing that feature (the implementer writes the spec *before* coding and commits it in the same PR) |

---

## 7. Feature spec workflow

Before a non-trivial feature is coded, whoever is implementing it creates `docs/features/<feature>.md` with, at minimum:

- **User story** — who does what, and what they see when it works
- **Screens / flow** — which Fragments are involved and how navigation works
- **Firestore reads/writes** — which collections, which fields, any new indexes
- **Edge cases** — empty/loading/error, offline, expiry, permission denied
- **Invariants touched** — from §4 above

The spec lands in the same PR as the code. Agents implementing the feature read the spec plus `AGENTS.md` + whichever `docs/` files §6 calls out.

---

## 8. What NOT to do

- ❌ Put Firebase SDK calls in a Fragment or Activity
- ❌ Hardcode strings, colors, or dimensions in XML
- ❌ Create a second `Activity` instead of a Fragment destination
- ❌ Allow `messages` writes for a deal whose `chatUnlocked` is false
- ❌ Use `GlobalScope` or `runBlocking`
- ❌ Commit `google-services.json` or any API key
- ❌ Push directly to `main` or `develop`
- ❌ Skip writing the feature spec in `docs/features/` for a non-trivial feature
