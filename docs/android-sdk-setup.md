# Android SDK setup for local Gradle builds

If you see:

`SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file.`

use one of these approaches.

## Preferred: set `sdk.dir` in `local.properties`

Create `<repo>/local.properties` (next to `settings.gradle.kts`) with:

```properties
sdk.dir=/absolute/path/to/your/Android/Sdk
```

Examples:

- macOS: `sdk.dir=/Users/<you>/Library/Android/sdk`
- Linux: `sdk.dir=/home/<you>/Android/Sdk`
- Windows: `sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`

This is the most reliable per-project setup and avoids shell/profile issues.

## Alternative: set environment variables

Set either `ANDROID_HOME` or `ANDROID_SDK_ROOT` to your SDK install path.

### Linux / macOS (bash/zsh)

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin"
```

Persist by adding those lines to your `~/.bashrc`, `~/.zshrc`, or equivalent.

### Windows (PowerShell)

```powershell
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"
setx ANDROID_SDK_ROOT "$env:LOCALAPPDATA\Android\Sdk"
```

Then reopen your terminal/IDE.

## Quick verification

```bash
echo "$ANDROID_HOME"
ls "$ANDROID_HOME/platform-tools"
```

If those work, run:

```bash
./gradlew test
```

## Project auto-detection

This project now attempts to auto-generate `local.properties` during Gradle settings evaluation when:

- `local.properties` is missing, and
- one of these SDK locations exists and contains `platform-tools`:
  - `$ANDROID_HOME`
  - `$ANDROID_SDK_ROOT`
  - `$HOME/Android/Sdk`
  - `/opt/android-sdk`
  - `/usr/local/android-sdk`

If none are found, Gradle prints a clear setup hint and you can still set `sdk.dir` manually as shown above.
