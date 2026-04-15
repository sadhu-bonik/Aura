# Workflow — branching, commits, PRs

---

## Branches

Two permanent branches:

- `main` — stable, demo-ready. Only team leads merge here, only from `develop`, only at agreed milestones.
- `develop` — integration branch. Everything lands here first. No direct commits.

All work happens on short-lived branches off `develop`:

| Prefix | Purpose | Example |
|---|---|---|
| `feature/` | new functionality | `feature/creator-profile-ui` |
| `bugfix/` | fix a bug in `develop` | `bugfix/login-null-pointer` |
| `backend/` | Firebase / repo logic with no UI | `backend/firestore-deal-creation` |
| `docs/` | docs-only changes | `docs/add-firestore-indexes` |
| `hotfix/` | urgent fix directly off `main` (rare) | `hotfix/crash-on-startup` |

Rules:
- lowercase, hyphens, no underscores or spaces
- be specific: `feature/deal-inbox` not `feature/stuff`
- hotfixes merge into **both** `main` and `develop`

### Lifecycle

```bash
# start
git checkout develop
git pull origin develop
git checkout -b feature/creator-profile-ui

# during work — commit often, push early
git push origin feature/creator-profile-ui

# stay current
git checkout develop && git pull origin develop
git checkout feature/creator-profile-ui
git merge develop   # resolve conflicts

# submit PR: base = develop, compare = your branch
# after merge
git checkout develop && git pull origin develop
git branch -d feature/creator-profile-ui
```

---

## Commit format (Conventional Commits)

```
<type>: <imperative summary>
```

- lowercase type and summary
- imperative mood: "add", "fix", "update" — not "added", "fixing"
- no trailing period
- ≤50 chars on the summary line
- one logical change per commit

### Types

| Type | For |
|---|---|
| `feat` | new feature or screen |
| `fix` | bug fix |
| `ui` | layout / visual changes only |
| `refactor` | restructure without behavior change |
| `data` | data model changes |
| `firebase` | Firestore rules, queries, config |
| `docs` | documentation only |
| `chore` | Gradle, dependencies, tooling |
| `test` | adding / updating tests |
| `nav` | navigation graph changes |

Optional scope: `feat(auth): add role selection after registration`.

### Good

```
feat: add creator profile fragment layout
fix: resolve crash when portfolio list is empty
ui: apply typography scale to auth screens
refactor: extract deal logic into DealRepository
firebase: add security rules for deals collection
chore: add firebase bom and nav component
```

### Bad

```
fixed stuff
WIP
update
feat: Added creator profile.          (capitalized, past tense, period)
feat: creator profile done + fixed login + cleaned up imports   (many things)
```

### Multi-line body (optional, for complex commits)

```
feat: add portfolio item upload with firebase storage

Uploads image/video to /portfolioItems/{userId}/{filename}.
Updates portfolioItems collection with the download URL.
Shows upload progress in the UI.
```

---

## PRs

- **Base**: `develop` (never `main` except for hotfixes).
- **Title**: same format as a commit message (`<type>: <summary>`).
- **Description**: what changed, why, how you tested. If the PR implements a feature, link its spec at `docs/features/<name>.md`.
- **Size**: prefer small PRs. If it takes >30 min to review, split it.
- **Review**: at least one teammate approval before merging.
- **Merge**: squash-merge so `develop` stays linear.

### Before opening the PR

- [ ] App builds (`./gradlew assembleDebug`)
- [ ] Tested on an emulator
- [ ] No debug logs, commented-out code, or test data committed
- [ ] No `google-services.json` or secrets staged
- [ ] Commit messages follow the format above
- [ ] Feature spec committed in `docs/features/` (if this is a feature PR)

---

## Parallel work

When two people are working on connected pieces (e.g. chat UI + chat repository):

1. **Agree on the shared data class first** — merge `data/model/Message.kt` before either side starts.
2. **Use stub data** until the other branch lands.
3. **Announce merges** in the group chat so the other person can pull `develop` and rebase.
4. **Never import from each other's unmerged branches** — always go through `develop`.

---

## Amending

If you just committed and need to fix the message (not pushed yet):

```bash
git commit --amend -m "feat: add creator profile fragment"
```

If you already pushed, do **not** force-push a shared branch — just add a follow-up commit.
