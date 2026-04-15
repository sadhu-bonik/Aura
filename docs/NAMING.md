# Naming

Conventions for Kotlin, Firestore, and Android resources. Consistency matters more than cleverness — pick the name the next teammate would guess.

---

## Kotlin

| Kind | Convention | Example |
|---|---|---|
| Package | lowercase, dot-separated | `com.aura.app.ui.creator.profile` |
| Class | PascalCase | `CreatorProfileFragment`, `DealRepository` |
| Function | camelCase, verb-first | `getCreatorProfile()`, `onAcceptDealClicked()` |
| Variable | camelCase, noun | `creatorProfile`, `dealList` |
| Boolean | `is…` / `has…` / `can…` | `isLoading`, `hasAcceptedDeal`, `canSendMessage` |
| Constant | SCREAMING_SNAKE_CASE | `COLLECTION_USERS`, `ROLE_CREATOR` |
| LiveData backing field | `_name` private + `name` public | `private val _isLoading; val isLoading: LiveData<Boolean> = _isLoading` |

### Class name suffixes

| Role | Suffix |
|---|---|
| Screen | `…Fragment` (e.g. `LoginFragment`) |
| State holder | `…ViewModel` |
| Firebase I/O | `…Repository` |
| Firestore-mapped DTO | *no suffix* (`User`, `Deal`, `Message`) |
| RecyclerView adapter | `…Adapter` |
| Firebase wrapper | `…Manager` (`FirebaseAuthManager`, `FirestoreManager`) |

### Function naming patterns

```kotlin
// Repository reads — return Flow<T> or suspend one-shot
fun getDealsForCreator(creatorId: String): Flow<List<Deal>>
suspend fun getCreatorProfile(userId: String): Result<CreatorProfile>

// Repository writes — return Result
suspend fun acceptDeal(dealId: String): Result<Unit>
suspend fun sendMessage(message: Message): Result<Unit>

// ViewModel loaders (no args, side-effect only)
fun loadDealInbox()
fun loadRecommendations()

// ViewModel event handlers — on…Clicked / on…Changed
fun onAcceptDealClicked(dealId: String)
fun onSearchQueryChanged(query: String)

// Extension utilities in utils/Extensions.kt
fun String.isValidEmail(): Boolean
fun Long.toCurrencyString(): String    // 5000 → "$50.00"
fun View.show() / hide() / gone()
```

---

## Firestore

- **Collections**: camelCase, plural — `users`, `creatorProfiles`, `portfolioItems`
- **Fields**: camelCase — `profileImageUrl`, `createdAt`, `isAvailable`, `chatUnlocked`
- **Document IDs**: auto-generated *unless* the ID is semantically meaningful (e.g. `creatorProfiles/{userId}` uses the Auth UID)

Matching Kotlin is required for `toObject()` to work:

```kotlin
data class Deal(
    val dealId: String = "",
    val brandId: String = "",
    val creatorId: String = "",
    val status: String = "",
    val chatUnlocked: Boolean = false,
    val createdAt: Timestamp? = null,
)
```

Every property defaults — no exceptions. This is how Firestore deserializes via the no-arg constructor.

---

## XML layouts

File names: `<prefix>_<snake_case>.xml`

| Type | Prefix | Example |
|---|---|---|
| Activity layout | `activity_` | `activity_main.xml` |
| Fragment layout | `fragment_` | `fragment_creator_profile.xml` |
| RecyclerView item | `item_` | `item_deal_card.xml` |
| Reusable include | `layout_` | `layout_empty_state.xml` |
| Dialog | `dialog_` | `dialog_confirm_deal.xml` |
| Custom view | `view_` | `view_rating_bar.xml` |

---

## View IDs (snake_case, view-type prefix)

| Type | Prefix | Examples |
|---|---|---|
| Button | `btn_` | `btn_continue`, `btn_accept_deal` |
| TextView | `tv_` | `tv_creator_name`, `tv_deal_status` |
| EditText | `et_` | `et_email`, `et_password` |
| TextInputLayout | `til_` | `til_email` |
| ImageView | `iv_` | `iv_avatar`, `iv_portfolio_image` |
| RecyclerView | `rv_` | `rv_creator_feed`, `rv_messages` |
| ProgressBar | `pb_` | `pb_loading` |
| ConstraintLayout | `cl_` | `cl_root`, `cl_profile_header` |
| CardView | `cv_` | `cv_creator_card` |
| Toolbar | `tb_` | `tb_main` |
| BottomNavigationView | `bnv_` | `bnv_creator`, `bnv_brand` |
| Switch | `sw_` | `sw_availability` |
| CheckBox | `cb_` | `cb_accept_terms` |
| Chip | `chip_` | `chip_niche_fashion` |
| FloatingActionButton | `fab_` | `fab_add_portfolio` |

---

## String resources (snake_case, context prefix)

```xml
<string name="label_email">Email</string>
<string name="hint_password">Enter your password</string>
<string name="btn_continue">Continue</string>
<string name="title_creator_profile">My Profile</string>
<string name="error_invalid_email">Please enter a valid email</string>
<string name="empty_deals">No deals yet. Brands will reach out here.</string>
```

Prefixes: `label_`, `hint_`, `btn_`, `title_`, `error_`, `empty_`, `msg_`, `confirm_`.

---

## Drawables (snake_case, type prefix)

| Type | Prefix | Example |
|---|---|---|
| Vector icon | `ic_` | `ic_home.xml`, `ic_send.xml` |
| Background / shape | `bg_` | `bg_card_rounded.xml` |
| Selector | `selector_` | `selector_tab_icon.xml` |
| Illustration | `img_` | `img_empty_deals.png` |
| Logo | `logo_` | `logo_aura.xml` |

---

## Constants (`utils/Constants.kt`)

```kotlin
object Constants {
    // Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_CREATOR_PROFILES = "creatorProfiles"
    const val COLLECTION_BRAND_PROFILES = "brandProfiles"
    const val COLLECTION_PORTFOLIO_ITEMS = "portfolioItems"
    const val COLLECTION_CAMPAIGNS = "campaigns"
    const val COLLECTION_DEALS = "deals"
    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_SHORTLISTS = "shortlists"
    const val COLLECTION_REVIEWS = "reviews"
    const val COLLECTION_RECOMMENDATIONS = "recommendations"

    // Roles
    const val ROLE_CREATOR = "creator"
    const val ROLE_BRAND = "brand"

    // Deal statuses
    const val STATUS_PENDING = "pending"
    const val STATUS_ACCEPTED = "accepted"
    const val STATUS_REJECTED = "rejected"
    const val STATUS_COMPLETED = "completed"
    const val STATUS_CANCELLED = "cancelled"
    const val STATUS_EXPIRED = "expired"

    // Storage paths
    const val STORAGE_PROFILE_IMAGES = "profileImages"
    const val STORAGE_PORTFOLIO_ITEMS = "portfolioItems"
    const val STORAGE_BRAND_LOGOS = "brandLogos"
    const val STORAGE_CAMPAIGN_ASSETS = "campaignAssets"
}
```

Never inline collection or status strings in repository code — always reference `Constants`.
