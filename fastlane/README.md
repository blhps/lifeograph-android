# fastlane metadata

This folder holds the F-Droid / Google Play style store-listing metadata
(app title, descriptions, changelog, icon, screenshots) in the standard
"Fastlane" layout:

https://gitlab.com/-/snippets/1895688

No Fastlane tooling is required to use it — F-Droid's build server and
IzzyOnDroid read these files directly from the repo on each release.

Layout:

```
fastlane/metadata/android/en-US/
├── title.txt
├── short_description.txt   (< 80 chars, no trailing period)
├── full_description.txt
├── changelogs/
│   └── <versionCode>.txt   (one file per release, matches versionCode in app/build.gradle)
└── images/
    └── icon.png
```

When cutting a new release, add a `changelogs/<versionCode>.txt` with a short
plain-text summary of what changed in that version.
