# Aura

Aura is an Android app that connects content creators with brands for sponsored-content collaborations. Creators publish a portfolio backed by their public YouTube data; brands browse, shortlist, and send paid deals; once a deal is accepted, an in-app chat opens so the two parties can coordinate the work; on completion either side can leave a review.

---

## Features

- **Email/password authentication** with role-based onboarding (creator or brand).
- **Creator profiles & portfolios** — bio, niches, social handles, and a media gallery.
- **Portfolio analytics** — engagement and consistency scores derived from the YouTube Data API v3.
- **Brand campaigns & creator discovery** — niche-, rating-, and follower-aware ranking.
- **Deal lifecycle** — `pending → accepted → completed`, with `rejected`, `cancelled`, and 7-day auto-`expired` terminal states.
- **Real-time chat** — gated to deals in `accepted` or `completed` state; backed by Firestore listeners.
- **Reviews** — writeable only against completed deals; aggregated into the recipient's rating.
- **Push notifications** — FCM token persisted per user for deal and message events.
- **Role-aware navigation** — bottom navigation, deep links, and screen access all validate the signed-in user's role.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language / SDK | Kotlin, `minSdk` 24, `targetSdk` 36 |
| UI | XML layouts, Material 3, ViewBinding, Jetpack Navigation (single-Activity) |
| Architecture | MVVM + Repository, manual DI via ViewModel factories |
| Async | Coroutines, `Flow`, `LiveData` |
| Backend | Firebase Auth, Firestore, Storage, Cloud Messaging |
| Media | Glide (images), Media3/ExoPlayer (video), ViewPager2 |
| External APIs | YouTube Data API v3 via Retrofit + Gson |

---

## Architecture

```
Fragment  →  ViewModel  →  Repository  →  Firebase SDK
```

- `MainActivity` is the only Activity. Every screen is a `Fragment` registered in `res/navigation/nav_graph.xml`.
- ViewModels expose `LiveData` (one-shot results) or `StateFlow` (ongoing state). They never expose raw Firebase types.
- Repositories in `data/repository/` are the only place Firebase SDK classes are referenced. Write methods return `Result<T>`; stream methods return `Flow<T>`. Multi-document writes use Firestore transactions or batches.
- Real-time screens (chat, deal inbox, notifications) are backed by Firestore snapshot listeners surfaced as cold `Flow`s.

### Package layout (`com.aura.app`)

```
ui/
  ├── auth/      login, register, role selection
  ├── creator/   creator-only screens (profile, portfolio, dashboard)
  ├── brand/     brand-only screens (campaigns, discovery, shortlist)
  ├── deals/     shared deal inbox and detail
  ├── chat/      shared chat list and chat screen
  └── common/    MainActivity, splash, onboarding
data/
  ├── model/     Firestore-mapped data classes
  └── repository/
firebase/        thin wrappers over the Firebase SDK
adapters/        shared RecyclerView adapters
navigation/      navigation helpers
utils/           constants, extensions, validation, date helpers
```

---

## Getting started

### Prerequisites

- Android Studio (Hedgehog or newer)
- JDK 11+ — Android Studio's bundled JBR works
- A Firebase project with Auth, Firestore, Storage, and Cloud Messaging enabled
- A Google Cloud API key with the YouTube Data API v3 enabled

### Configuration

Two files are required and are gitignored:

1. **`app/google-services.json`** — download from your Firebase project's Android app settings and drop it into `app/`.
2. **`local.properties`** — add your YouTube key:
   ```properties
   YOUTUBE_API_KEY=your_key_here
   ```
   It is read into `BuildConfig.YOUTUBE_API_KEY` by `app/build.gradle.kts`.

### Build

Set `JAVA_HOME` to your JDK, then:

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device or emulator required)
./gradlew lint                   # Android Lint
```

---

## Project documentation

The `docs/` directory holds the working specs:

| File | Contents |
|---|---|
| `AGENTS.md` | Domain invariants, coding rules, and the docs index |
| `docs/ARCHITECTURE.md` | Where new code goes; navigation and module boundaries |
| `docs/FIRESTORE_SCHEMA.md` | Collections, documents, and field reference |
| `docs/NAMING.md` | Naming conventions for code and resources |
| `docs/UI_TOKENS.md` | Color, spacing, typography, and component tokens |
| `docs/WORKFLOW.md` | Branching, commits, and PR process |
| `docs/features/` | Per-feature specs (user story, screens, reads/writes, edge cases) |

---

## Contributing

- Branch from `develop`: `feature/…`, `bugfix/…`, `backend/…`, `docs/…`. Never push directly to `main` or `develop`.
- Conventional Commits, lowercase, imperative mood, ≤50 characters on the subject line.
- Non-trivial features land with a spec in `docs/features/<feature>.md` in the same PR.
- Fragments are view-only — Firebase access goes through a repository.
- No hardcoded strings, colors, or dimens in XML; reference `@string/…`, `@color/…`, `@dimen/…`.

---

## License

Proprietary. All rights reserved.
