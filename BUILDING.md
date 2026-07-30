# Building Lifeograph for Android

## Prerequisites

* **JDK 17 must already be installed and discoverable by Gradle** (e.g. via
  `JAVA_HOME`, your `PATH`, Android Studio's bundled JDK, SDKMAN, etc.).
  `app/build.gradle` pins a Java 17 toolchain, and the project does **not**
  use the `foojay-resolver-convention` plugin to auto-download a JDK if one
  isn't found — that plugin was intentionally removed so the build doesn't
  reach out to the network for a toolchain, which F-Droid's build
  environment disallows. If no compatible JDK is available locally, the
  build will fail with a "no matching toolchain" error rather than fetching
  one automatically.
* **Android SDK**, platform 37 (`compileSdk 37` / `targetSdk 37`)
* **Android NDK 28.0.12433566** — the version is pinned in
  `app/build.gradle` (`ndkVersion`). Other NDK versions may not work with
  the native build below.
* **CMake >= 3.22.1** (via the SDK manager, or system-installed)
* A working host **autotools toolchain** (`autoconf`, `automake`,
  `libtool`, `make`, `gcc`) — needed because `libgpg-error` and
  `libgcrypt` are cross-compiled from source as part of the native build,
  not fetched as prebuilt binaries.
* **Network access during the first CMake configure**: `abseil-cpp` and
  `re2` are pulled automatically via CMake `FetchContent` from GitHub.

All of the above are satisfied by a normal Android Studio installation
plus the Android NDK component, except for the host autotools toolchain,
which you'll need to install separately (e.g. `apt install autoconf
automake libtool build-essential` on Debian/Ubuntu).

## 1. Clone the repository

```sh
git clone https://github.com/blhps/lifeograph-android.git
cd lifeograph-android
```

## 2. Fetch libgpg-error and libgcrypt sources

The native build compiles `libgpg-error` and `libgcrypt` from source for
each target ABI (the configure/build logic is in `external/Makefile` and
`app/src/main/jni/CMakeLists.txt`, adapted from [Guardian Project's
gnupg-for-android](https://github.com/guardianproject/gnupg-for-android)).
These sources are not checked into the repo (see `.gitignore`) and must be
placed manually before building:

```sh
external/fetch-deps.sh
```

This downloads and extracts the pinned versions (currently `libgpg-error
1.61` and `libgcrypt 1.12.2`) from https://gnupg.org/ftp/gcrypt/ into
`external/libgpg-error` and `external/libgcrypt`. It's idempotent — safe to
re-run, and it skips any library that's already present. The exact pinned
versions live in `external/fetch-deps.sh` itself; update them there if you
bump either library.

The CMake script automatically copies `external/libgpg-error` and
`external/libgcrypt` into per-ABI directories (e.g.
`external/libgpg-error-arm64`) and cross-compiles each one with the NDK
toolchain the first time you build.

## 3. Build

```sh
./gradlew assembleDebug
# or, for a release build:
./gradlew assembleRelease
```

The first build will take a while: it configures and compiles
`libgpg-error` and `libgcrypt` via autotools for every enabled ABI
(`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`), and fetches/builds Abseil
and RE2 via CMake. Subsequent builds reuse the `.cxx`/`externalNativeBuild`
cache and are much faster.

Resulting APKs are under `app/build/outputs/apk/`.

## Notes for reproducible / F-Droid builds

* No proprietary SDKs, Google Play services, or Firebase are used.
* All native dependencies (RE2, Abseil, libgpg-error, libgcrypt) are built
  from their own upstream FOSS source, either via CMake `FetchContent` or
  the autotools step above.
* If you'd rather avoid the `FetchContent` network fetch during CI builds,
  vendor `abseil-cpp` and `re2` locally and point the `FetchContent_Declare`
  calls in `app/src/main/jni/CMakeLists.txt` at the local paths instead of
  the GitHub URLs.