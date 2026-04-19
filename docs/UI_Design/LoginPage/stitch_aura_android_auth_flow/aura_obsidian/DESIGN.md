# Design System Specification: The Architectural Curator

## 1. Overview & Creative North Star
The North Star for this design system is **"The Architectural Curator."** 

Unlike standard marketplaces that feel cluttered or transactional, this system treats digital space like a high-end gallery. It merges the structural integrity of a "business-first" platform with the fluid, expressive soul of a creator’s portfolio. We achieve this through **Editorial Asymmetry**—using generous, intentional whitespace and varying typographic scales to guide the eye, rather than rigid, boxed-in grids. The goal is a "Native Android" experience that feels custom-built and premium, moving away from generic Material components toward a bespoke, high-end digital environment.

---

## 2. Colors: Tonal Depth & Soul
The palette is rooted in a "Sleek Midnight" philosophy. We do not use pure black; we use layers of deep navy and charcoal to create a sense of infinite space.

### The "No-Line" Rule
**Borders are a failure of hierarchy.** Within this system, 1px solid borders for sectioning are strictly prohibited. Boundaries must be defined solely through background color shifts or tonal transitions.
*   **Example:** A `surface-container-low` section sitting on a `surface` background provides all the separation necessary.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers—like stacked sheets of smoked glass.
*   **Base Layer:** `surface` (#0b1326)
*   **Secondary Sectioning:** `surface-container-low` (#131b2e)
*   **Interactive Cards:** `surface-container` (#171f33)
*   **Popovers/Modals:** `surface-container-highest` (#2d3449)

### The "Glass & Gradient" Rule
To inject "soul" into the professional aesthetic, utilize:
*   **Glassmorphism:** For floating navigation bars or header overlays, use `surface` at 70% opacity with a `20px` backdrop blur.
*   **Signature Textures:** Use a subtle linear gradient for primary CTAs (from `primary` #4edea3 to `on_primary_container` #009365) at a 135-degree angle. This adds a "lithographic" depth that flat emerald cannot achieve.

---

## 3. Typography: The Editorial Voice
We use a dual-font strategy to balance authority with modern elegance.

*   **Display & Headlines (Manrope):** Chosen for its geometric precision and modern "tech-meets-luxury" feel. 
    *   *Usage:* Use `display-lg` (3.5rem) with tight letter-spacing (-0.02em) for hero moments to create an editorial, magazine-like impact.
*   **Body & Labels (Inter):** The workhorse of the system. 
    *   *Usage:* `body-md` (0.875rem) is the standard for creator descriptions. Keep line-height generous (1.6) to ensure the "business" side of the marketplace feels readable and sophisticated.

**Hierarchy Tip:** Never center-align long-form text. Maintain a strong left-aligned axis to reinforce the architectural "grid-less" look.

---

## 4. Elevation & Depth: Tonal Layering
Traditional shadows are often too "dirty" for a premium dark theme. We use light to define shape.

*   **The Layering Principle:** Instead of a shadow, place a `surface-container-lowest` (#060e20) card inside a `surface-container-high` (#222a3d) section. This "recessed" look creates depth without visual clutter.
*   **Ambient Shadows:** For high-priority floating elements (e.g., FABs), use a shadow with a `32px` blur, 0px offset, and 6% opacity using the `primary` color (#4edea3) rather than black. This creates a "glow" rather than a "drop."
*   **The "Ghost Border" Fallback:** If a border is required for accessibility, use the `outline-variant` (#45464d) at **15% opacity**. It should be felt, not seen.

---

## 5. Components: Refined Primitives

### Buttons
*   **Primary:** High-rounded (`full`). Gradient fill (`primary` to `on_primary_container`). Text is `on_primary` (#003824), bold.
*   **Secondary:** No fill. "Ghost Border" (15% `outline-variant`).
*   **Interaction:** On press, the button should scale down slightly (98%) and increase in brightness—mimicking a physical tactile switch.

### Cards & Lists
*   **Rule:** **Forbid dividers.** Use `16px` or `24px` of vertical whitespace to separate items.
*   **Creator Cards:** Use `xl` (1.5rem) corner radius. Use a `surface-variant` (#2d3449) subtle header area to house the brand logo, transitioning into the main card body.

### Input Fields
*   **Style:** Filled, not outlined. Use `surface-container-highest` (#2d3449) with a `DEFAULT` (0.5rem) radius. 
*   **Focus State:** The bottom `2px` of the field should illuminate in `tertiary` (#ffb95f) to provide a "gold" highlight of professional trust.

### Marketplace-Specific Components
*   **The "Trust Badge":** A small, `label-sm` chip using `tertiary_container` with `on_tertiary` text. Use this for verified creators to provide a subtle "gold-leaf" stamp of quality.
*   **The "Price Tag":** `title-md` using the `secondary` (#b9c7e0) color to ensure it feels sophisticated and not "salesy."

---

## 6. Do’s and Don’ts

### Do
*   **Do** use `tertiary` (#ffb95f) sparingly as a "financial" or "prestige" accent for high-value actions.
*   **Do** embrace asymmetry. An image can bleed off the right edge of the screen to suggest a larger world of content.
*   **Do** use "Breathing Room." If you think there is enough margin, add 8px more.

### Don’t
*   **Don’t** use pure white text (#FFFFFF). Always use `on_surface` (#dae2fd) to reduce eye strain and maintain the "midnight" mood.
*   **Don’t** use standard Android "Ripple" effects in high-contrast white. Use a subtle `primary` tint for ripples to keep the brand identity consistent.
*   **Don’t** use icons with varying stroke weights. Stick to a "Light" or "Regular" weight (approx 1.5pt) to match the elegant sans-serif typography.

---

## 7. Implementation Summary Table

| Element | Token/Value | Application |
| :--- | :--- | :--- |
| **Corner Radius** | `xl` (1.5rem) | Main content cards, Hero images |
| **Shadows** | Blur: 32px, Opacity: 6%, Color: `primary` | Floating action elements |
| **Borders** | `outline-variant` @ 15% | Only when tonal shift is insufficient |
| **Spacing** | 8dp / 16dp / 24dp / 32dp / 48dp | Incremental "Editorial" spacing |
| **Blur** | 20px - 40px | Backdrop blur for Glassmorphism layers |