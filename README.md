# Lifeograph for Android

Lifeograph is a personal diary and note-taking app with rich-text editing,
tagging, filtering, and strong encryption. This is the Android port; it
shares its core C++ engine with [Lifeograph for
desktop](https://gitlab.com/bilheps/lifeograph) (Linux/Windows/macOS), so
diary files are fully interchangeable between the two.

* Project website: https://lifeograph.sourceforge.net
* Desktop source: https://gitlab.com/bilheps/lifeograph
* License: GNU GPLv3 (see [COPYING](COPYING))

## Features

* Rich-text entries with formatting, headings, and foldable paragraphs
* To-do items with completion tracking
* Tagging, including parametric tags
* Full-text search and filtering
* AES-256 diary encryption with auto-lock on inactivity
* Integrated world map for travel entries
* Statistical charts from tagged entries
* Python-based import plugins (e.g. Markdown or KML)

The Android app is still catching up to some desktop-only features
(printing, scripts, some chart and table support). See the [feature
comparison](https://lifeograph.sourceforge.net/doku.php?id=features) on the
project website for the current state.

## Building

See [BUILDING.md](BUILDING.md) for prerequisites and step-by-step build
instructions.

## Getting Lifeograph

* [Google Play](https://play.google.com/store/apps/details?id=net.sourceforge.lifeograph)
* F-Droid: pending inclusion

## Contributing

Issues and pull requests are welcome. If you're adding a new import plugin
or touching the native (JNI) layer, please keep patches small and follow
the existing code style.

## License

Lifeograph for Android is free software, licensed under the GNU General
Public License v3.0. See [COPYING](COPYING) for the full text.
