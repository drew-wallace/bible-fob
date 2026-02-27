# Bible Fob

Bible Fob is an Android app that intercepts BibleGateway passage URLs, parses the `search` query, and renders results as in-app cards.

## Purpose

- Open BibleGateway `/passage` links directly inside the app.
- Parse the `search` parameter from incoming URLs.
- Split the search string by comma/semicolon chunks and show one results card per parsed chunk.

## Link handling rules

- `/passage` route is handled in-app.
- Any non-`/passage` BibleGateway link is immediately redirected to an external browser.

## Data source assumptions

Bible content is loaded from Android assets (`app/src/main/assets/`) and supports these assumptions:

- Whole-Bible JSON can be provided (default: `bible/whole_bible.json`; versioned: `bible/<VERSION>_bible.json`).
- Per-book JSON can be provided (default: `bible/books/`; versioned: `bible/<VERSION>_books/`).
- SQL is optional:
  - prebuilt DB asset: `database/<VERSION>_bible.db`
  - SQL dump asset: `database/<VERSION>_bible.sql`

## Local setup

### Requirements

- Android Studio (latest stable recommended)
- JDK 17 (project compiles with Java/Kotlin target 17)
- Android SDK configured via one of:
  - `local.properties` with `sdk.dir=...`
  - `ANDROID_HOME` / `ANDROID_SDK_ROOT`

### Place Bible data files

Put Bible data under:

- `app/src/main/assets/bible/`
- `app/src/main/assets/database/` (if using DB/SQL data)

Examples:

- `app/src/main/assets/bible/NET_bible.json`
- `app/src/main/assets/bible/NET_books/John.json`
- `app/src/main/assets/database/NET_bible.sql`

### Build and run

```bash
# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Install debug APK on connected device/emulator
./gradlew installDebug

# Build release APK
./gradlew assembleRelease
```

You can also open the project in Android Studio and run the `app` configuration on an emulator/device.

## Testing guidance (sample URL)

Use this URL:

`https://www.biblegateway.com/passage/?search=John%203%3A16-19&version=NET`

Expected behavior:

1. Android opens Bible Fob for this deep link.
2. App parses `search=John 3:16-19`.
3. A card appears for `John 3:16-19 (NET)` with verses from the configured NET data source.
4. If NET data is unavailable or does not contain that passage, the app shows an empty/no-verses state.

Quick manual checks:

- In emulator/device browser or `adb`, open the sample URL and confirm app-link routing.
- Verify non-`/passage` links (for example `https://www.biblegateway.com/`) open an external browser.

Optional `adb` command:

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "https://www.biblegateway.com/passage/?search=John%203%3A16-19&version=NET"
```

## Release instructions (GitHub Actions)

This repo ships with `.github/workflows/release-apk.yml`.

### Triggering a release

- Push a tag matching `v*` (example: `v1.0.0`), or
- Run **Actions → Release APK → Run workflow** manually.

### Required secrets for signed release

Configure all of these repository secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

If any are missing, the workflow still builds/releases an unsigned APK.

### Typical release flow

```bash
git checkout main
git pull
git tag v1.0.0
git push origin v1.0.0
```

Then verify the created GitHub Release and attached APK artifact.
