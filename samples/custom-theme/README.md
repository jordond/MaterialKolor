# Custom theme sample

The [Tasks sample](../README.md) built on Compose Foundation with hand-rolled components. No design system library
is involved. `material-kolor-core` builds the tonal ramps, and the sample maps them into its own `AppColors`.

The app is styled as a risograph zine. A riso prints one flat spot ink per pass, and the passes never line up
exactly, so the theme is a paper stock plus a short list of inks instead of surfaces and containers.

- The paper comes off a cream ramp and the key ink for text and rules off the scheme's primary ramp.
- Fluorescent Pink, Blue and Yellow start from the published Riso ink colors. `rememberTonalPalette` pulls each one
  towards the seed, so a new seed shifts the whole ink set with it.
- The primary and error inks and the scrim are Material roles, used as they are.
- The text printed on a spot ink comes from `onTone`, so it stays readable whatever the seed.

```kotlin
val pink = rememberTonalPalette(seed = seeds.pink, harmonizeWith = seed)

val ink = pink.toneColor(64)
val onInk = pink.onTone(64)
```

Inks overprint wherever they overlap. On the light stock they multiply like real ink. The dark stock is black paper,
where the inks are lifted to lighter tones and add up instead, so overlaps glow. The masthead is printed in two inks
off register, controls knock their second pass further out on hover and press it back into line on click, and every
solid block has paper grain showing through.

The Palette tab shows each ink's ramp with the tone the theme prints at, the halftone screens that ink can make, and
how the inks overprint.

Run it with `./gradlew :samples:custom-theme:run`.

![custom-theme sample](../screenshots/images/custom-theme.png)
