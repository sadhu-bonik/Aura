---
name: Hyper-Definition Dark
colors:
  surface: '#151313'
  surface-dim: '#151313'
  surface-bright: '#3c3839'
  surface-container-lowest: '#100e0e'
  surface-container-low: '#1e1b1b'
  surface-container: '#221f1f'
  surface-container-high: '#2c2929'
  surface-container-highest: '#373434'
  on-surface: '#e8e1e1'
  on-surface-variant: '#cac4cf'
  inverse-surface: '#e8e1e1'
  inverse-on-surface: '#332f30'
  outline: '#938f99'
  outline-variant: '#48454e'
  surface-tint: '#cdbef6'
  primary: '#cdbef6'
  on-primary: '#342957'
  primary-container: '#645889'
  on-primary-container: '#e0d4ff'
  inverse-primary: '#635788'
  secondary: '#c6c6c7'
  on-secondary: '#2f3131'
  secondary-container: '#454747'
  on-secondary-container: '#b4b5b5'
  tertiary: '#c8c6c5'
  on-tertiary: '#313030'
  tertiary-container: '#605f5f'
  on-tertiary-container: '#dcd9d9'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e8ddff'
  primary-fixed-dim: '#cdbef6'
  on-primary-fixed: '#1f1340'
  on-primary-fixed-variant: '#4b3f6f'
  secondary-fixed: '#e2e2e2'
  secondary-fixed-dim: '#c6c6c7'
  on-secondary-fixed: '#1a1c1c'
  on-secondary-fixed-variant: '#454747'
  tertiary-fixed: '#e5e2e1'
  tertiary-fixed-dim: '#c8c6c5'
  on-tertiary-fixed: '#1c1b1b'
  on-tertiary-fixed-variant: '#474646'
  background: '#151313'
  on-background: '#e8e1e1'
  surface-variant: '#373434'
typography:
  headline-xl:
    fontFamily: Manrope
    fontSize: 48px
    fontWeight: '800'
    lineHeight: '1.1'
    letterSpacing: -0.04em
  headline-lg:
    fontFamily: Manrope
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '700'
    lineHeight: '1.3'
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Manrope
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
    letterSpacing: '0'
  body-md:
    fontFamily: Manrope
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
    letterSpacing: '0'
  label-lg:
    fontFamily: Manrope
    fontSize: 14px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Manrope
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: 0.02em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  unit: 8px
  gutter: 24px
  margin: 32px
  container-max: 1280px
---

## Brand & Style

This design system is defined by high-octane energy and uncompromising clarity. It targets a high-performance audience that values precision, speed, and a premium "after-dark" aesthetic. By stripping away the softness of gradients and blurs, the system achieves a state of hyper-definition.

The visual style is a fusion of **High-Contrast Bold** and **Modern Minimalism**. It relies on the aggressive juxtaposition of true black voids against saturated, solid pigments. The emotional response is one of immediate focus and digital sophistication—cutting through the noise with sharp boundaries and a "liquid-glass" smoothness provided by the full-radius geometry.

## Colors

The palette is engineered for maximum optical impact. The foundation is a "True Black" (#000000) base, which eliminates the gray-wash typical of most dark modes and allows secondary elements to pop with tactile intensity.

- **Primary (#645889):** A high-saturation violet used for core actions and brand markers. It must remain solid; no transparency or gradients.
- **Surface (#121212):** Used for primary containers to create subtle but sharp separation from the true black background.
- **Secondary (#FFFFFF):** Reserved for high-priority typography and icons to ensure a brutalist level of legibility.
- **Neutral Accent (#fff7f7):** A bright, near-white neutral used for high-contrast borders and structural definition against the dark background.
- **Success/Warning/Error:** Use pure, neon-adjacent solids (e.g., #00FF66, #FFD600, #FF003D) to maintain the high-energy threshold.

## Typography

Manrope is utilized for its geometric balance and modern technical feel. To support the high-energy narrative, headings use heavy weights (700-800) with tight letter-spacing to create dense, impactful "blocks" of text. 

Body copy maintains a generous line height (1.6) to ensure readability against the high-contrast background. Labels and small metadata should utilize semi-bold weights and slight tracking increases to prevent "ink-clogging" visual artifacts on OLED screens.

## Layout & Spacing

The layout follows a strict 12-column fixed grid for desktop, transitioning to a fluid model for mobile. Spacing is governed by an 8px rhythmic scale. 

To maintain the high-impact visual hierarchy, use generous white space (or "black space") between major sections. This ensures that the vibrant violet elements have room to breathe and command attention. Elements should align strictly to the grid to maintain the "sharp boundary" philosophy.

## Elevation & Depth

In this design system, depth is communicated through **Tonal Layering** and **High-Contrast Outlines** rather than shadows. 

1. **Level 0 (Base):** #000000.
2. **Level 1 (Surface):** #121212.
3. **Level 2 (Active/Floating):** #121212 with a 1px solid border of #fff7f7 or #645889 at low opacity (20-40%).

Shadows are entirely omitted to prevent "fuzziness." Hierarchy is instead established by the brightness of the stroke and the clear separation of dark surfaces.

## Shapes

The design system utilizes **Full (Pill-shaped)** roundedness across all interactive components. This serves as a counter-balance to the aggressive high-contrast color palette, providing a sense of ergonomic flow and premium manufacturing.

All buttons, input fields, tags, and container corners (where applicable) should utilize the maximum radius. Smaller elements (like checkboxes) should maintain a minimum of 4px radius to ensure they don't appear "sharp" in an environment of otherwise smooth curves.

## Components

### Buttons
Primary buttons are pill-shaped, solid #645889, with #FFFFFF text. No gradients. The hover state should be a simple shift to a slightly lighter solid violet or a white 1px stroke. Secondary buttons use a solid #121212 fill with a 1px #fff7f7 stroke.

### Input Fields
Inputs feature a #121212 background with a 1px stroke of #fff7f7 at low opacity. Upon focus, the stroke changes instantly to solid #645889. The cursor and text should be #FFFFFF.

### Chips & Tags
Small, pill-shaped elements using #121212 backgrounds and #fff7f7 strokes at low opacity. For active states, use the primary #645889 as the background with bold white text.

### Cards
Cards are flat containers of #121212. They do not use shadows. They are separated from the #000000 background by their color difference and a 1px #fff7f7 border at low opacity to define the edge.

### Lists
List items are separated by solid 1px dividers (#fff7f7 at 10% opacity). Interactive list items should have a solid #645889 vertical "active indicator" on the far left when selected.