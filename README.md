# Row placement

A small Kotlin Multiplatform engine for placing variable-width items into rows.
It finds the legal position nearest the requested left edge, with configurable
spacing and padding, and falls back to another row when needed. There is no
fixed item limit and no sampled position grid.

## Scope and origin

Adapted from the placement problem in MyGameShelf's `DecorRunGeometry` and
`ShelfStore.visibleDecorPlacement`. This is a new, isolated implementation:
app-specific game counts, dimensions, themes, models and Compose dependencies
are removed. The app's sampled search is replaced with sorted occupied bounds
and direct gap examination. **The store builds do not use this module yet.**
No app code, backend, artwork, credentials, purchase logic or sync data is included.
MIT license applies to this component only, not to MyGameShelf or its assets.
The generated Gradle wrapper retains Gradle's Apache-2.0 licensing notices.

## Usage

```kotlin
val result = RowPlacement.find(
    rows = listOf(
        Row("top", 300.0, listOf(Occupied(8.0, 284.0))),
        Row("bottom", 300.0, listOf(Occupied(8.0, 40.0))),
    ),
    itemWidth = 60.0,
    preferredRowId = "top",
    preferredLeft = 24.0,
    gap = 8.0,
    padding = 8.0,
)
// Placement(rowId=bottom, left=56.0, width=60.0)
```

Import `com.geometry.rowplacement.Row`, `Occupied` and `RowPlacement`.
The runnable consumer is in `example/` and depends on the library project.

## Coordinate and state contract

- Supply final rendered widths after scaling. Use one coordinate unit consistently
  (for example Compose dp values, pixels, or world units).
- `preferredLeft` is a left edge, not a center or percentage. Out-of-range finite
  preferences are clamped to a legal gap. Positions are row-content coordinates,
  not viewport coordinates; the UI handles scrolling and pointer conversion.
- Preferred row is tried first; remaining rows retain caller order. Closest legal
  left edge wins within a row; exact ties go left. A missing preferred row or
  duplicate row IDs is an input error.
- Existing overlaps are treated as occupied space. Items must otherwise have
  finite, positive widths and fit inside their rows.
- `null` means no space. Invalid inputs throw `IllegalArgumentException`.
- Save the result in your own state before the next placement. For moving an
  existing item, exclude its old bounds. After a resize, recompute rendered bounds
  and request placement again; this engine does not automatically reflow items.
- This is one-dimensional layout, not 2D packing, drag handling, persistence or
  cross-device synchronization. IEEE Double arithmetic applies; use practical UI
  dimensions, not astronomical values. A marginal floating-point fit can be
  rejected to avoid returning an overlapping placement.
- Per call: O(sum(n log n)) time, O(max(n)) temporary storage over row occupancies.

## Run the tests and example

One command on an Apple Silicon Mac with JDK 21 and Xcode:

```sh
bash verify.sh
```

This standalone copy includes the Gradle 8.11.1 wrapper. The script uses it
first. If needed, set `JAVA_HOME=/opt/homebrew/opt/openjdk@21`.
With a standalone Gradle 8.11.1 installation, the equivalent command is:

```sh
gradle jvmTest wasmJsNodeTest macosArm64Test compileKotlinIosArm64 compileKotlinIosSimulatorArm64 :example:run
```

Using this repository's wrapper directly:

```sh
./gradlew jvmTest wasmJsNodeTest macosArm64Test compileKotlinIosArm64 compileKotlinIosSimulatorArm64 :example:run
```

The native tasks require an Apple Silicon Mac with Xcode. JVM tests can run
separately on other hosts. Runtime tests cover JVM, Wasm/Node and macOS Native;
iOS tasks compile the common code but do not establish physical-device behavior.
The JVM artifact is usable by Android/JVM consumers; no Android-specific API or
Android AAR is needed. No Maven publication or hosted CI is configured.

Tests cover narrow gaps, exact fits, overflow, padding, nearest-position ties,
overlapping inputs, fallback rows, scaled widths, more than three decorations,
invalid inputs and 1,000 seeded layouts compared with an exhaustive oracle.

### Verification, September 14, 2026

- JVM: 20 tests passed, 0 skipped/failures/errors.
- Wasm/Node: the same 20 tests passed, 0 skipped/failures/errors.
- macOS ARM64 Native: the same 20 tests passed, 0 skipped/failures/errors.
- Each runtime executed the 1,000-case seeded exhaustive-oracle comparison.
- iOS ARM64 and iOS simulator ARM64: common code compiled successfully.
- Runnable JVM consumer: printed `Placement(rowId=bottom, left=56.0, width=60.0)`
  and its assertion passed.
- No Android device, browser UI or iPhone session was used. There is no UI here.
- The parent app settings/build files do not reference this independent Gradle
  project. Static scope mapping inherits app platform labels from the directory;
  actual affected consumers are this library and its example only. No unmapped
  paths or coverage gaps were reported. App-wide builds were not rerun.
