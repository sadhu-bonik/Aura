# Design System Strategy: The Ethereal Atelier (Nocturnal Edition)

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Ethereal Atelier."** 

In its current dark mode evolution, the system transforms from a sun-drenched studio into a sophisticated, midnight digital gallery. We maintain a departure from transactional, rigid "grid-of-boxes" aesthetics, opting instead for a mood of quiet luxury and mystery. We utilize **intentional asymmetry** and **controlled compositions** that emphasize depth through luminosity rather than stark contrast.

To achieve this premium feel, we balance the space. While previously expansive, we have transitioned to a **balanced density** (Spacing: 2), ensuring the interface feels focused and intimate without sacrificing its editorial soul.

---

## 2. Colors & Tonal Logic
Our palette is rooted in a deep, atmospheric neutral base, providing a velvet-like canvas that feels more like a private lounge than a standard interface. 

### The "No-Line" Rule
**Standard 1px borders are strictly prohibited for sectioning.** To define boundaries, designers must use tonal shifts within the dark spectrum. 
*   **Surface Transitioning:** Use `surface` as your base dark layer. When a section needs to be distinguished, shift the background to `surface-container-low`. 
*   **Depth through Tone:** By using tonal layering rather than lines, we maintain a seamless, "liquid" interface.

### Surface Hierarchy & Nesting
Think of the UI as layers of obsidian and smoke.
*   **Level 0 (Base):** `surface` — The primary dark background.
*   **Level 1 (Subtle Inset):** `surface-container-low` — Used for secondary content blocks to create a "recessed" feel.
*   **Level 2 (Active Cards):** `surface-container` — The standard for interactive elements.
*   **Level 3 (Prominence):** `surface-container-highest` — Used to draw the eye to specific data, appearing to catch the "light" of the screen.

### The "Glass & Gradient" Rule
To elevate the experience, floating elements (like bottom navigation bars) should utilize **Dark Glassmorphism**. Use `surface` at 80% opacity with a `24px` backdrop-blur to allow colors to bleed through. 
*   **Signature Textures:** For primary CTAs, use a subtle linear gradient involving `primary` (`#7a729a`) at a 135-degree angle. This adds a gemstone-like glow to buttons against the dark background.

---

## 3. Typography: The Editorial Voice
We use **Manrope** across the entire system. It is a modern, geometric sans-serif that balances technical precision with organic warmth, especially legible in light-on-dark configurations.

*   **Display & Headline (The Hook):** Use `display-lg` and `headline-lg` with tight letter-spacing (-0.02em). These create "Moments of Impact" against the dark canvas.
*   **Title (The Navigator):** `title-lg` should be semi-bold to provide a clear anchor for content blocks.
*   **Body (The Story):** `body-lg` is our primary reading grade. Ensure a line-height of at least 1.6 to prevent "haloing" effects common in dark mode reading.
*   **Hierarchy Tip:** Contrast a `display-sm` headline with a `label-md` uppercase subtitle to create a professional, magazine-style layout.

---

## 4. Elevation & Depth
In dark mode, we reject heavy shadows. Depth is achieved through **Luminance Layering**.

*   **The Layering Principle:** Higher elevation levels are represented by lighter tonal values of the surface color. A `surface_container_highest` element looks closer to the user because it is "brighter" than the `surface` below it.
*   **Ambient Glows:** If an element must float, use a subtle, large-radius glow: `y: 12px, blur: 24px, color: primary (5% opacity)`. 
*   **The "Ghost Border" Fallback:** If accessibility requires a border, use the `outline_variant` token at **15% opacity**.
*   **Glassmorphism:** Use `surface` with 70% opacity and a `backdrop-filter: blur(20px)` to maintain the "Aura" effect where brand purples subtly permeate the UI.

---

## 5. Components

### Buttons (The Tactile Interaction)
*   **Primary:** Uses **maximum roundedness** (`roundedness: 3`). Apply the signature purple gradient. Padding is balanced for a sleek, modern touch.
*   **Secondary:** No background. Use a `surface-container-high` background on hover/press. Typography should be `title-sm` in `primary` color.
*   **States:** On "Press," scale the button down to `0.98` for a haptic, physical feel.

### Input Fields (The Elegant Entry)
*   **Styling:** Fields must use **maximum roundedness** and `surface-container-low` background. 
*   **Borders:** No borders in the default state. Upon focus, transition to `surface_container_highest` with a `2px` "Ghost Border" using `primary`.
*   **Labels:** Use `label-md` inside the padding for a compact, modern look.

### Cards & Lists (The Narrative Flow)
*   **Forbidden:** Horizontal dividers (`<hr>`).
*   **Separation:** Use the `spacing: 2` scale (Standard) to separate list items. This creates a more functional density suitable for complex dark-mode interfaces.
*   **Imagery:** All imagery should have a **maximum** corner radius to match the pill-shaped design language.

### Additional Signature Component: The "Aura Chip"
A bespoke filter chip using `tertiary_container` with `tertiary` text. These act as neon-like accents against the dark theme for creator tags and categories.

---

## 6. Do's and Don'ts

### Do:
*   **Embrace Tonal Elevation:** Use lighter shades of dark grey/purple to indicate proximity to the user.
*   **Maintain Legibility:** Ensure `on_surface_variant` has enough contrast against dark backgrounds.
*   **Color as Signal:** Use the `primary` #7a729a purple sparingly for actions and brand moments.

### Don't:
*   **No Pure Black:** Avoid `#000000` for backgrounds to prevent "smearing" on OLED screens; stick to the `neutral` dark palette.
*   **No Heavy Outlines:** Structural separation must be tonal, never a high-contrast stroke.
*   **No Standard Material FABs:** Floating buttons must be pill-shaped (`roundedness: 3`), not circles.
*   **Avoid Over-saturation:** Keep large background areas neutral; save the vibrant `primary` and `tertiary` for interactive components.