# Phase 4 — Bottom Nav Bar (Stitch match)

## Context

Phase 3.5 work is paused. User is jumping ahead to the bottom nav refresh so the app's shell matches the finalized Stitch design (`stitch_deal_lifecycle_management/deal_dashboard_updated_navigation/screen.png`).

**What's already on the branch (teammate's work, needs replacing, not rebuilding from zero):**
- `HomeContainerFragment` (`app/src/main/java/com/aura/app/ui/main/HomeContainerFragment.kt`) — a top-level fragment that hosts a 4-tab `BottomNavigationView` wired to a child `NavHostFragment` via `setupWithNavController`. Structurally correct — keep.
- `fragment_home_container.xml` — `NavHostFragment` above, 1dp divider, `BottomNavigationView` at bottom. Keep, restyle.
- `nav_home.xml` — child graph with 4 destinations: **Home** (`VideoFeedFragment`), **Discover** (`DiscoverFragment`), **Deals** (`DealsFragment` — teammate's placeholder), **Profile** (`ProfileFragment`).
- `menu/bottom_nav_menu.xml` — 4 items (Home, Discover, Deals, Profile) in that order.
- `color/color_nav_item.xml` — selector: checked = `colorPrimary`, unchecked = white @ 60%.
- `drawable/ic_nav_home.xml`, `ic_nav_discover.xml`, `ic_nav_deals.xml`, `ic_nav_profile.xml` — outline-style vectors.

**What the Stitch design wants (the target):**
- Tab order: **Profile / Deals / Feed / Search** (Profile left, Search right).
- Active tab visual: a solid green rounded-rect "active indicator" pill sits behind the icon; the icon fills in; the label turns green/teal. Inactive tabs: outline icon, gray label, no pill.
- Dark navy background with a 1dp top divider (same as current).
- Labels always visible (Stitch shows every label, not just the active one).

**Confirmed user decisions (2026-04-19):**
1. Rename **Discover → Search** but *keep* `DiscoverFragment` as the destination. Just swap the menu label + the icon. Any real search behavior is the teammate's to build.
2. **Replace `DealsFragment` with `DealDashboardFragment`** behind the Deals tab. Delete teammate's `DealsFragment.kt` + `fragment_deals.xml`.

## Deliverables

### 1. Reorder + relabel tabs in the nav menu

Rewrite `app/src/main/res/menu/bottom_nav_menu.xml` so it defines four items in the new order:

| Position | `id` | `title` string key | Icon (drawable) |
|---|---|---|---|
| 1 | `navigation_profile` | `tab_profile` ("Profile") | `ic_nav_profile` (outline person) |
| 2 | `navigation_deals` | `tab_deals` ("Deals") | `ic_nav_deals` (handshake; filled-on-active via selector — see §4) |
| 3 | `navigation_feed` | `tab_feed` ("Feed") | `ic_nav_feed` (wifi/broadcast glyph — new drawable) |
| 4 | `navigation_search` | `tab_search` ("Search") | `ic_nav_search` (magnifier — new drawable) |

- The existing menu item ids `navigation_home` and `navigation_discover` are renamed to `navigation_feed` and `navigation_search` respectively so the destination `android:id` in `nav_home.xml` matches (Navigation auto-wires by matching menu-item-id to destination-id).
- String keys `tab_profile` / `tab_deals` / `tab_feed` / `tab_search` go in `strings.xml` (add only if missing — grep first).

### 2. Rewire `nav_home.xml`

Rewrite `app/src/main/res/navigation/nav_home.xml`:

```xml
<navigation
    android:id="@+id/nav_home"
    app:startDestination="@id/navigation_deals">

    <fragment
        android:id="@+id/navigation_profile"
        android:name="com.aura.app.ui.main.ProfileFragment"
        android:label="@string/tab_profile"
        tools:layout="@layout/fragment_profile" />

    <fragment
        android:id="@+id/navigation_deals"
        android:name="com.aura.app.ui.chat.DealDashboardFragment"
        android:label="@string/tab_deals"
        tools:layout="@layout/fragment_deal_dashboard" />

    <fragment
        android:id="@+id/navigation_feed"
        android:name="com.aura.app.ui.feed.VideoFeedFragment"
        android:label="@string/tab_feed"
        tools:layout="@layout/fragment_video_feed" />

    <fragment
        android:id="@+id/navigation_search"
        android:name="com.aura.app.ui.main.DiscoverFragment"
        android:label="@string/tab_search"
        tools:layout="@layout/fragment_discover" />

</navigation>
```

- **Start destination = `navigation_deals`** so opening Home lands on the dashboard (matches the Stitch target screen).
- **DiscoverFragment kept** — only the surrounding `android:id` + `android:label` change. The fragment class itself is untouched.
- **DealsFragment swapped out** — destination class now `com.aura.app.ui.chat.DealDashboardFragment`.

### 3. Fold the outer-graph `dealDashboardFragment` destination into the tab

Right now `nav_graph.xml` has two entry points to the dashboard: (a) `homeContainerFragment` hosts the bottom nav, (b) a standalone `dealDashboardFragment` destination at lines 248-257 with actions `action_dashboard_to_chat` and `action_dashboard_to_history`. Once `DealDashboardFragment` is reachable as a *nested* destination under `nav_home`, outer-graph navigation to the dashboard (from login, registration, etc.) should land on `homeContainerFragment` instead — the bottom nav then auto-selects the Deals tab because it's the start destination.

**Edits in `nav_graph.xml`:**
- Keep the `homeContainerFragment` destination (line 241-245) — no change.
- **Move the `action_dashboard_to_chat` and `action_dashboard_to_history` actions into `nav_home.xml`** under `<fragment android:id="@+id/navigation_deals">`. Remove them from the outer-graph `dealDashboardFragment` entry.
- **Delete the outer-graph `dealDashboardFragment` destination** (lines 247-257) entirely. Any outer-graph actions currently pointing at `@+id/dealDashboardFragment` (lines 48, 150, 218, 231 per earlier grep) repoint to `@+id/homeContainerFragment`.
- Verify no `findNavController().navigate(R.id.dealDashboardFragment)` calls exist in code; if they do, replace with `R.id.homeContainerFragment` (the container auto-opens its start destination, which is Deals).

**Nested nav actions:** from inside the Deals tab, `findNavController()` returns the *child* NavController (the one hosted by `HomeContainerFragment`), so `action_dashboard_to_chat` / `action_dashboard_to_history` must be defined in `nav_home.xml`, not the outer graph. Chat and history themselves must be reachable from the child graph — simplest: `<include app:graph="@navigation/nav_graph"/>` isn't quite right since chat is already in nav_graph. Cleanest fix:

- Add `<fragment android:id="@+id/chatFragment"/>` and `<fragment android:id="@+id/dealHistoryFragment"/>` as destinations in `nav_home.xml` too (cross-graph links aren't supported for nested host fragments in the way we'd need). Move their class refs and actions from `nav_graph.xml` into `nav_home.xml`.

**Actually — simpler alternative** (recommended): keep `chatFragment` + `dealHistoryFragment` in the *outer* `nav_graph.xml` where they already are, and have the Deals tab navigate to them by traversing up. In `DealDashboardFragment`, replace `findNavController().navigate(R.id.action_dashboard_to_chat, ...)` with `findNavController(parent navigator)` — specifically:

```kotlin
requireParentFragment().requireParentFragment().findNavController()
    .navigate(R.id.action_homeContainer_to_chat, bundleOf("dealId" to dealId))
```

This walks from `DealDashboardFragment` → its `NavHostFragment` → `HomeContainerFragment` → outer NavHostFragment. Add two actions on the outer graph's `homeContainerFragment`:
```xml
<action android:id="@+id/action_homeContainer_to_chat" app:destination="@id/chatFragment"/>
<action android:id="@+id/action_homeContainer_to_history" app:destination="@id/dealHistoryFragment"/>
```

This keeps chat + history as full-screen destinations (they hide the bottom nav while open, which is correct — chat shouldn't show the bottom nav).

**Chosen approach: the second one** (outer graph actions + double-parent traversal). Write a small extension to avoid the `requireParentFragment().requireParentFragment()` stutter:

```kotlin
// app/src/main/java/com/aura/app/utils/NavExt.kt (new file)
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

fun Fragment.rootNavController(): NavController =
    NavHostFragment.findNavController(requireActivity().supportFragmentManager.findFragmentById(com.aura.app.R.id.nav_host_fragment)!!)
```

Then `DealDashboardFragment` calls `rootNavController().navigate(R.id.action_homeContainer_to_chat, ...)`. Ditto for history.

Update the three other outer-graph destinations that currently point at `dealDashboardFragment` (lines 48, 150, 218, 231) to point at `homeContainerFragment` instead.

### 4. Active-indicator pill + filled-icon selector styling

**BottomNavigationView active indicator** — Material 3 supports a pill behind the active icon via `itemActiveIndicatorStyle`. Add a style:

```xml
<!-- res/values/styles.xml (or themes.xml — wherever other Widget.* styles live) -->
<style name="Widget.Aura.BottomNav.ActiveIndicator" parent="Widget.Material3.BottomNavigationView.ActiveIndicator">
    <item name="android:color">@color/colorPrimary</item>
    <item name="android:width">64dp</item>
    <item name="android:height">32dp</item>
    <item name="shapeAppearance">@style/ShapeAppearance.Material3.Corner.Full</item>
    <item name="marginHorizontal">8dp</item>
</style>
```

Apply in `fragment_home_container.xml` on the `BottomNavigationView`:
```xml
app:itemActiveIndicatorStyle="@style/Widget.Aura.BottomNav.ActiveIndicator"
```

**Filled-when-active icon selectors** — the Deals handshake is outline when idle and filled when selected. Create per-icon selectors:

- `res/drawable/nav_icon_deals.xml` (state selector): `state_checked="true"` → `@drawable/ic_nav_deals_filled`, default → `@drawable/ic_nav_deals`.
- Create filled variants: `ic_nav_deals_filled.xml`, `ic_nav_profile_filled.xml`, `ic_nav_feed_filled.xml`, `ic_nav_search_filled.xml` — or just use the outline in both states for Profile/Feed/Search if the filled handshake is the only one the Stitch design treats specially. Looking at the screenshot, only Deals has a visibly filled active icon; the others keep their outline appearance (the green pill + colored label carry the active state). **Recommend: only create the Deals filled variant; keep Profile/Feed/Search using the outline icon in both states.**

`bottom_nav_menu.xml` then references `@drawable/nav_icon_deals` for the Deals item (the selector), and the other items reference their plain outline drawables.

**Color selector update** — `res/color/color_nav_item.xml`:
```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/colorOnPrimary" android:state_checked="true" />
    <item android:color="@color/colorTextSecondary" android:state_checked="false" />
</selector>
```
- `colorOnPrimary` makes the icon+label contrast against the primary-colored pill when active.
- `colorTextSecondary` (muted gray) for the inactive state, matching the Stitch reference.

Verify both tokens exist (explorer report earlier confirmed they do).

**Also in `fragment_home_container.xml`:**
- `app:labelVisibilityMode="labeled"` stays (every tab shows its label, per Stitch).
- `android:background="@color/colorBackground"` stays.
- Height (`@dimen/bottom_nav_height`) — verify it's ≥ 64dp so the pill fits comfortably; if smaller, bump in `dimens.xml`.

### 5. New / replaced drawables

| Drawable | Purpose | Source |
|---|---|---|
| `ic_nav_feed.xml` (new) | Wifi/broadcast-style glyph for Feed tab | Material Symbols `rss_feed` outline OR `dynamic_feed` outline — pick whichever matches Stitch more closely |
| `ic_nav_search.xml` (new) | Magnifier for Search tab | Material Symbols `search` outline |
| `ic_nav_deals_filled.xml` (new) | Filled handshake for selected Deals tab | Material Symbols `handshake` filled |
| `nav_icon_deals.xml` (new, selector) | State-based swap between outline + filled handshake | Wraps the two above |

`ic_nav_home.xml` and `ic_nav_discover.xml` can stay on disk unused (no cleanup risk) or be deleted — **recommend: delete both** since the user said "replace" and we don't want stale drawables.

### 6. Delete teammate's `DealsFragment` + its layout

- `app/src/main/java/com/aura/app/ui/main/DealsFragment.kt` — delete.
- `app/src/main/res/layout/fragment_deals.xml` — delete.
- Grep for any lingering references (imports, nav actions, layout includes): expected zero after §2. If any hit, swap for `DealDashboardFragment` / `fragment_deal_dashboard`.

### 7. Hide the bottom nav on full-screen destinations

Chat, history, registration steps, and video playback should not show the bottom nav. Since the bottom nav lives inside `HomeContainerFragment` (a nested fragment) and the outer `NavHostFragment` owns chat/history/etc., they're already siblings in the view hierarchy — navigating to chat replaces `HomeContainerFragment` wholesale, hiding its nav automatically. **No extra work needed here** — just confirm during smoke test.

### 8. Build + smoke test

1. `./gradlew assembleDebug` clean.
2. Launch. App opens to the Home container, Deals tab selected (dashboard visible), bottom nav shows Profile / Deals / Feed / Search.
3. **Visual checks (match Stitch `deal_dashboard_updated_navigation/screen.png`):**
   - Deals tab has a green pill behind a filled handshake icon + green "Deals" label.
   - Other three tabs: outline icons, gray labels, no pill.
   - 1dp divider separates content from nav.
4. **Behavior checks:**
   - Tap Profile → `ProfileFragment` opens.
   - Tap Feed → `VideoFeedFragment` opens.
   - Tap Search → `DiscoverFragment` opens (relabeled Search, original content).
   - Tap Deals → returns to dashboard.
   - Inside dashboard: tapping a pending card → chat opens full-screen, **bottom nav disappears** (correct).
   - From chat → back → dashboard reappears with bottom nav + Deals tab still highlighted.
   - From dashboard → history icon → history screen opens full-screen without bottom nav.
5. **Regression checks:**
   - Login → lands on `homeContainerFragment` (Deals tab), not the old standalone `dealDashboardFragment`.
   - Registration flows (outer-graph actions that targeted `dealDashboardFragment` at lines 48, 150, 218, 231) now land on `homeContainerFragment`.

## Critical files

### Edits
| Purpose | Path |
|---|---|
| Menu item order + labels + icon refs + ids | `app/src/main/res/menu/bottom_nav_menu.xml` |
| Nested graph: reorder destinations, swap DealsFragment → DealDashboardFragment, add chat/history actions | `app/src/main/res/navigation/nav_home.xml` |
| Delete outer `dealDashboardFragment` destination; add `action_homeContainer_to_chat` + `action_homeContainer_to_history`; repoint 4 outer callers from `dealDashboardFragment` → `homeContainerFragment` | `app/src/main/res/navigation/nav_graph.xml` |
| Restyle BottomNavigationView: `itemActiveIndicatorStyle`, confirm labelVisibility + background + height | `app/src/main/res/layout/fragment_home_container.xml` |
| Selected / unselected color states match Stitch | `app/src/main/res/color/color_nav_item.xml` |
| Tab label strings (`tab_profile`, `tab_deals`, `tab_feed`, `tab_search`) if missing | `app/src/main/res/values/strings.xml` |
| Widget.Aura.BottomNav.ActiveIndicator style | `app/src/main/res/values/styles.xml` (or `themes.xml` — follow existing convention) |
| `DealDashboardFragment` chat/history nav calls → root nav controller | `app/src/main/java/com/aura/app/ui/chat/DealDashboardFragment.kt` (+ NewDealsTab / ActiveDealsTab / PastDealsTab as needed — wherever `findNavController().navigate(R.id.action_dashboard_to_chat, ...)` or `action_dashboard_to_history` is called today) |

### New files
| Purpose | Path |
|---|---|
| Feed glyph | `app/src/main/res/drawable/ic_nav_feed.xml` |
| Search glyph | `app/src/main/res/drawable/ic_nav_search.xml` |
| Filled handshake (active Deals) | `app/src/main/res/drawable/ic_nav_deals_filled.xml` |
| State selector: outline ↔ filled handshake | `app/src/main/res/drawable/nav_icon_deals.xml` |
| Cross-graph root-nav-controller helper | `app/src/main/java/com/aura/app/utils/NavExt.kt` |

### Deletes
| Path | Reason |
|---|---|
| `app/src/main/java/com/aura/app/ui/main/DealsFragment.kt` | Replaced by `DealDashboardFragment` |
| `app/src/main/res/layout/fragment_deals.xml` | Replaced |
| `app/src/main/res/drawable/ic_nav_home.xml` | Unused after rename |
| `app/src/main/res/drawable/ic_nav_discover.xml` | Unused after rename |

## Reused / already-in-place (do NOT re-implement)

- `HomeContainerFragment` + `fragment_home_container.xml` structural wiring — keep.
- `BottomNavigationView.setupWithNavController` pattern — keep; it still auto-resolves menu-id → destination-id by matching ids.
- `DealDashboardFragment` + its tabs (Active / New / Completed / Past) — unchanged.
- `ChatFragment`, `DealHistoryFragment`, `VideoFeedFragment`, `DiscoverFragment`, `ProfileFragment` class files — unchanged.
- `ic_nav_profile.xml`, `ic_nav_deals.xml` (as the outline/default handshake) — keep.
- `colorPrimary`, `colorOnPrimary`, `colorTextSecondary`, `colorBackground` — all already in `colors.xml`.

## Out of scope

- Real implementation of Search behavior (still just shows `DiscoverFragment`'s current placeholder content — that's the teammate's work).
- Animating the active-indicator pill expand/contract (Material 3 handles basic crossfade; fancy animation = polish).
- Badge dots on tab icons (red dot for pending New Deals etc.) — future milestone.
- Replacing VideoFeedFragment internals for the Feed tab — content unchanged, only label + tab icon update.
- Dark/light theme variants for the nav bar — current app is dark-only (confirmed in theme files).

## Risks & notes

- **Nested navigation + back stack:** `HomeContainerFragment` is a destination in the outer graph, but it owns its own NavController for the four tabs. Pressing back from a tab should exit the app (or whatever the outer graph's back behavior is), not cycle through previously-selected tabs. `setupWithNavController` handles this correctly — no manual `OnBackPressedCallback` needed.
- **`findNavController()` ambiguity inside nested fragments:** Calls from `DealDashboardFragment` (and its children) by default resolve to the *child* NavController. That's why chat/history navigation goes through `rootNavController()`. Verify in smoke test; if a stray call grabs the wrong controller the nav will silently fail.
- **Menu item id vs destination id:** `setupWithNavController` matches them by raw id value. Rename both symmetrically or nothing works. The plan above renames them consistently in both `bottom_nav_menu.xml` + `nav_home.xml`.
- **Delete order:** do the nav_home + nav_graph edits *first*, then delete DealsFragment. Deleting the class while it's still referenced in nav_home crashes the build mid-edit.
- **Icon visual weight:** Material Symbols `handshake` (filled weight 700) reads more decisive in a small pill than weight 400. Author the filled drawable with heavier weight if possible.
- **Outer-graph callers:** nav_graph lines 48, 150, 218, 231 all target `@+id/dealDashboardFragment`. Every one becomes `@+id/homeContainerFragment`. Miss one and that flow 404s at runtime.

## Verification checklist before declaring done

- [ ] Build passes.
- [ ] Bottom nav visually matches Stitch screenshot (order, active pill, colors, labels).
- [ ] Each tab opens the right fragment.
- [ ] Chat + history hide the bottom nav.
- [ ] Back navigation behaves: chat → dashboard (nav returns); dashboard → back → exits or honors outer graph.
- [ ] No `R.id.dealDashboardFragment` references remain in Kotlin or XML.
- [ ] No references to `DealsFragment` or `fragment_deals.xml` remain.
- [ ] `ic_nav_home.xml` + `ic_nav_discover.xml` deleted and no lint warnings about missing drawables.
