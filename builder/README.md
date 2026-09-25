# MaterialKolor Builder

The Compose Multiplatform app behind [materialkolor.com](https://materialkolor.com). Pick a seed
color, tune the scheme, preview it in Material 3, Compose Unstyled or Compose Fluent, and export the
code.

## Modules

| Module                  | What it holds                                                                         |
|-------------------------|---------------------------------------------------------------------------------------|
| `:builder:domain`       | The theme document, edits and undo history, the share code. Plain Kotlin, no Compose. |
| `:builder:codegen`      | Turns a theme into exported code for each target, checked against goldens.            |
| `:builder:engine`       | Generates the color schemes with `material-kolor-core`, and picks seeds from images.  |
| `:builder:kit`          | The design system: components, icons and the skins for each UI library.               |
| `:builder:preview`      | The sample screens a theme is previewed on.                                           |
| `:builder:app`          | Features, state and the platform interfaces, plus the desktop entry point.            |
| `:builder:web`          | The wasm and JS entry point, browser interop and the site assembly.                   |
| `builder/worker`        | The Cloudflare Worker that serves the site and draws link previews for `/t/` links.   |
| `builder/e2e`           | Playwright tests and the perf run, against the assembled site.                        |
| `builder/codegen-check` | A standalone Gradle build that compiles every exported golden.                        |

## Running

```bash
# Desktop
./gradlew :builder:app:run

# Desktop with Compose Hot Reload
./gradlew :builder:app:hotRunJvm

# Web, with the webpack dev server
./gradlew :builder:web:wasmJsBrowserDevelopmentRun

# Web, the optimized build on the webpack dev server
./gradlew :builder:web:wasmJsBrowserProductionRun

# Web on the JS engine, then open the page with ?engine=js
./gradlew :builder:web:jsBrowserDevelopmentRun
```

## Web engines

The site ships two builds of the same app and `boot.js` picks one per visit. Browsers with WasmGC
run the Kotlin/Wasm build. Browsers with WebAssembly and WebGL 2 but no WasmGC run the Kotlin/JS
build, which draws with the same skiko wasm. That covers Safari 15.2 to 18.1 and Chrome, Edge and
Firefox from before WasmGC, back to Chrome 95 and Firefox 100. A browser without WebAssembly and
its legacy exception handling, which skiko needs, or without WebGL 2, WebAssembly turned off
included, gets the unsupported page. There is no notice on the JS engine, and the error details
name the engine that ran.

Add `?engine=js` to any address to run the JS build where wasm would run. It lasts for that visit
only. The JS dev server serves only the JS glue, so the page it opens needs `?engine=js` to boot.

## MaterialKolor source

By default, the builder depends on the MaterialKolor modules in this repo, so a library change shows
up right away. Pass `-Pmaterialkolor.useLocal=false` to build against the published modules at
`materialKolorExport` instead, the version every export pins. Production is always built that way,
so the builder draws each theme with the same library the exported code compiles against.

## Running the site

To run the site the way it ships, assemble it and serve it through the Worker:

```bash
./gradlew :builder:web:assembleSite
cd builder/worker && npm ci && npm run dev
```

## Testing

```bash
# Unit tests
./gradlew :builder:domain:jvmTest :builder:codegen:jvmTest :builder:engine:jvmTest

# Browser tests, after assembleSite
cd builder/e2e && npm ci && npx playwright install chromium && npm test

# Worker tests, after assembleSite
cd builder/worker && npm ci && npm test
```

The codegen goldens live in `builder/codegen/src/jvmTest/resources/golden`. Rewrite them with
`./gradlew :builder:codegen:jvmTest -Pgolden.update=true`, then review the diff. To check that every
golden still compiles, run `./gradlew :builder:codegen:writeCompileFixtures` and then
`./gradlew -p builder/codegen-check compileKotlinJvm`.

Every pull request's preview comment shows a screenshot of the builder at that commit, taken by
`npm run pr-screenshot` in `builder/e2e`.

## Deploys

Everything deploys from the Builder workflow (`.github/workflows/builder.yml`) to Cloudflare
Workers.

- **Pull requests** from this repo upload a preview version of the staging Worker and comment its
  URL on the pull request. The comment updates on every push.
- **Staging** at [staging.materialkolor.com](https://staging.materialkolor.com) deploys on every
  push to `next`. It is built with `-Psite.env=staging`, so it is never indexed and its share links
  stay on staging. It uses the MaterialKolor modules in this repo, as pull request previews do.
- **Production** at [materialkolor.com](https://materialkolor.com) deploys from a
  `builder/<version>` tag. The tag must match `builder-version` in `gradle/libs.versions.toml`, and
  the MaterialKolor version the exports pin (`materialKolorExport`) must already be on Maven
  Central. Everything on a tag, tests included, builds against that published version.
