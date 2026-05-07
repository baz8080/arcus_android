# Arcus Dictionary

An offline English dictionary for Android. No internet connection required — all dictionary data is bundled with the app.

Originally published around 2014, when offline apps were particularly valuable on low-end or low-connectivity devices. This repository is preserved as a code example.

---

## Features

- **Instant search** — results appear as you type, with exact matches promoted to the top
- **Definitions** — each result shows the word, part of speech, definition, and synonyms where available
- **Text-to-speech** — tap the speaker icon to hear a word pronounced
- **Favourites** — save words for later reference, with sort and export options
- **Share** — share a word and its definition via any app on your device
- **UK / US English** — toggle between British and American spelling in settings
- **Sort order** — choose between relevance-ranked results or pure alphabetical order

---

## Requirements

- Android 7.0 (API 24) or higher

---

## Building

The project uses Gradle with a version catalog (`gradle/libs.versions.toml`).

```bash
# Debug build
./gradlew assembleDebug

# Release bundle (for Play Store upload)
./gradlew bundleRelease

# Run unit tests
./gradlew test

# Run lint
./gradlew lint
```

A release keystore is required for signed builds. The signing config in `app/build.gradle` expects a `release.keystore` file in the `app/` directory.

---

## Architecture & Technical notes

### Dictionary data

The dictionary is shipped as two binary files bundled as raw resources (`res/raw/`):

- `index.dat` — a sorted, tab-delimited index mapping each word to a byte offset in the definitions file
- `wdefs_all.dat` — the definitions file, seeked into directly using those offsets

On first launch, `DataFileManager` copies these files to internal storage. Subsequent launches skip extraction unless the bundled `DATA_VERSION` constant has been bumped, which is the mechanism for shipping updated dictionary data.

### Lookup

`ArcusDictionary` performs a **binary search** over the memory-mapped index file (`MappedFile`) to locate the insertion point for a query, then reads forward collecting prefix matches. This avoids loading the full dictionary into memory and keeps search fast on low-resource devices. Results are capped at 40 entries and relevance-sorted, with exact matches pinned to the top.

All file I/O runs on a single-threaded coroutine dispatcher to avoid concurrent access to the mutable position state of `MappedByteBuffer`.

### Storage

Favourites are persisted in a local **SQLite** database via `FavouritesDbHelper`.

User preferences (sort order, language variant) are stored in `SharedPreferences` and accessed through `ArcusPreferences`.

### UI

- Single-Activity search screen (`ArcusSearchActivity`) with a debounced text watcher triggering coroutine-based searches
- `FavouritesActivity` backed by `FavouritesViewModel` (Jetpack ViewModel + coroutines)
- **View Binding** throughout — no `findViewById`
- **Material You** dynamic colour theming with a Material 3 fallback palette

### Key dependencies

| Library | Purpose |
|---|---|
| Okio | Buffered I/O for dictionary file extraction |
| Timber | Logging |
| AndroidX Lifecycle / ViewModel | Favourites screen state management |
| Material Components | UI theming |

### Testing

Unit tests cover `WordModel`, `PartOfSpeech`, `WordTextUtils`, and `FavouritesDbHelper`, using JUnit 4, Mockito-Kotlin, and Robolectric.

```bash
./gradlew test
```

Lint is configured to fail the build on any error-level issue (`abortOnError true`).
