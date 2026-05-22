# AndroidUI Migration Plan
> Core: UI must match main branch pixel-by-pixel. Lighting = real-time Canvas/Shader.

## Phase 1 - Design Tokens
- colors.xml (8 theme combos + HDR colors)
- dimens.xml (spacing/radius/font/shadow)
- styles.xml (base theme)

## Phase 2 - Theme System
- 4 themes + transition animation (0.6s)

## Phase 3 - Base Components
- Header / Nav / Bubble / Page transitions

## Phase 4 - HDR Lighting (real-time)
- Canvas/Shader glow, shadow, border highlights
- P3 color gamut

## Phase 5 - Glass Effect
- RenderEffect blur (API 31+)

## Phase 6 - Polish
- Page transitions, message animations, haptics
