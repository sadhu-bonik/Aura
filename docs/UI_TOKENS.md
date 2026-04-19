# UI Tokens

Visual language: colors, spacing, typography, component rules. All tokens live in `res/values/` — never hardcode in layouts.

---

## 1. Colors (`res/values/colors.xml`)

Dark theme by default.

| Token | Hex | Usage |
|---|---|---|
| `colorPrimary` | `#B1A1FF` | Primary actions, active state |
| `colorPrimaryVariant` | `#7757FA` | Pressed state, gradients |
| `colorSecondary` | `#69F6B8` | Accents, active deals indicator |
| `colorSecondaryVariant` | `#58E7AB` | Pressed accent |
| `colorBackground` | `#0B0E14` | App background |
| `colorSurface` | `#0B0E14` | Cards, sheets, dialogs base |
| `colorSurfaceVariant` | `#22262F` | Input fields, received messages |
| `colorSurfaceContainer` | `#161A21` | secondary surfaces |
| `colorSurfaceContainerHigh` | `#1C2028` | elevated elements |
| `colorSurfaceContainerHighest`| `#22262F` | highlighted elevated elements |
| `colorOnBackground` | `#ECEDF6` | Primary text on background |
| `colorOnSurface` | `#ECEDF6` | Primary text on cards |
| `colorTextSecondary` | `#A9ABB3` | Subtitles, placeholders |
| `colorTextDisabled` | `#52555C` | Disabled text |
| `colorOnPrimary` | `#2F0095` | Text/icons on primary |
| `colorSuccess` | `#69F6B8` | Accepted / success |
| `colorWarning` | `#FFB300` | Pending / caution |
| `colorError` | `#FF6E84` | Error / rejected |
| `colorDivider` | `#45484F` | Dividers, strokes |

---

## 2. Typography

Font: Roboto (system) or Inter (downloadable). Styles defined in `res/values/styles.xml` — never set `textSize` or `textStyle` in layouts.

| Style | Size | Weight | Usage |
|---|---|---|---|
| `TextAppearance.Aura.DisplayLarge` | 32sp | Bold | Screen titles, hero |
| `TextAppearance.Aura.HeadlineMedium` | 24sp | SemiBold | Section headers |
| `TextAppearance.Aura.TitleLarge` | 20sp | Medium | Card / dialog titles |
| `TextAppearance.Aura.TitleMedium` | 18sp | Medium | Sub-section titles |
| `TextAppearance.Aura.BodyLarge` | 16sp | Regular | Primary body |
| `TextAppearance.Aura.BodyMedium` | 14sp | Regular | Secondary body, cards |
| `TextAppearance.Aura.LabelLarge` | 14sp | Medium | Button labels |
| `TextAppearance.Aura.LabelSmall` | 12sp | Regular | Captions, timestamps |

---

## 3. Spacing (`res/values/dimens.xml`)

| Token | Value |
|---|---|
| `space_xs` | 4dp |
| `space_sm` | 8dp |
| `space_md` | 16dp |
| `space_lg` | 24dp |
| `space_xl` | 32dp |
| `space_xxl` | 48dp |

Corner radii: `corner_radius_sm` 8dp, `corner_radius_md` 12dp, `corner_radius_lg` 20dp, `corner_radius_full` 50dp.

Icon sizes: `icon_size_sm` 20dp, `icon_size_md` 24dp, `icon_size_lg` 32dp, `icon_size_xl` 48dp.

---

## 4. Buttons

**Primary** — `colorPrimary` background, `colorOnPrimary` text, `corner_radius_md`, height `52dp`, full width in forms.

**Outlined / secondary** — transparent background, 1dp `colorPrimary` stroke, `colorPrimary` text.

**Destructive** — `colorError` background, `colorOnPrimary` text. Only for irreversible actions (cancel deal, remove from shortlist).

**Disabled** — `android:enabled="false"` + alpha 0.4. Do not style manually.

Use `com.google.android.material.button.MaterialButton` exclusively.

---

## 5. Cards

Always `MaterialCardView`:

```xml
app:cardBackgroundColor="@color/colorSurface"
app:cardCornerRadius="@dimen/corner_radius_md"
app:cardElevation="0dp"
app:strokeColor="@color/colorDivider"
app:strokeWidth="1dp"
```

No elevation shadow on dark backgrounds — use stroke instead. Internal padding `space_md`.

---

## 6. Inputs

Always `TextInputLayout` (OutlinedBox) + `TextInputEditText`:

```xml
style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
app:boxBackgroundColor="@color/colorSurfaceVariant"
app:boxCornerRadiusTopStart="@dimen/corner_radius_sm"
```

IDs: `til_<name>` for the layout, `et_<name>` for the field. Use `app:helperText` for descriptions and `setError()` for validation — never toast form errors.

---

## 7. Images (Glide)

```kotlin
Glide.with(this)
    .load(url)
    .placeholder(R.drawable.ic_placeholder_avatar)
    .error(R.drawable.ic_placeholder_avatar)
    .centerCrop()
    .into(binding.ivAvatar)
```

- Profile images: circular (`CircleCrop()`).
- Portfolio thumbs: `centerCrop()` inside cards.
- Never call `setImageBitmap` or `setImageURI` for remote URLs.

---

## 8. Loading / Empty / Error states

Every screen or list that loads data must handle all three. Use visibility toggling or `ViewSwitcher`.

| State | Contents |
|---|---|
| Loading | `ProgressBar` (circular, `colorPrimary`), centered |
| Empty | Icon/illustration + title + subtitle (e.g. "No creators found. Try adjusting your filters.") |
| Error | Icon + message + retry button that re-invokes the fetch |

Standard IDs:

```
@+id/pb_loading          loading spinner
@+id/layout_empty        empty container
@+id/tv_empty_title
@+id/tv_empty_subtitle
@+id/layout_error        error container
@+id/tv_error_message
@+id/btn_retry
```

---

## 9. Bottom navigation

- Creator role: Dashboard, Portfolio, Deals, Chat.
- Brand role: Discover, Campaigns, Deals, Chat.
- Active: `colorPrimary` icon + label. Inactive: `colorTextSecondary`. Background: `colorSurface`.
- Swap the menu resource at runtime based on role — don't create two host activities.

---

## 10. Reusable components to build early

Build these before feature work, so every screen can reuse them:

| Component | File |
|---|---|
| Creator card | `item_creator_card.xml` |
| Portfolio card | `item_portfolio_card.xml` |
| Deal card + status badge | `item_deal_card.xml` |
| Chat bubble (sent/received variants) | `item_message.xml` |
| Campaign card | `item_campaign_card.xml` |
| Empty state | `layout_empty_state.xml` |
| Loading overlay | `layout_loading.xml` |

---

## 11. Accessibility

- Every `ImageView` has `android:contentDescription` (or `"@null"` if decorative).
- Minimum touch target 48×48dp.
- Don't use color alone to convey meaning — pair with icon or text (e.g. deal status badge shows color AND word).
- Text contrast ≥ 4.5:1 against background (the palette above is verified).
