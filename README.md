# Compresso

Compresso is an Android app for lossless image and video compression. It re-encodes each file with the most efficient lossless codec available on the platform, verifies the output decodes back to identical data, and only keeps the result if it is smaller than the original.

## How it works

**Images** are decoded to a canonical bitmap and re-encoded as both optimized PNG and lossless WebP. Each candidate is decoded back and compared pixel-for-pixel against the source; only a verified, byte-for-byte-lossless candidate can be kept, and only if it is smaller than the original. Extreme mode pushes the WebP encoder to its highest compression effort. EXIF metadata is copied across when present. Animated images are left untouched rather than risk dropping frames.

**Video** is losslessly repackaged at the container level: every audio and video sample is copied byte-for-byte into the most compatible container (MP4, or WebM when the codecs allow it), then verified by hashing every sample in both files and comparing. No pixels are re-encoded, so the result is guaranteed bit-identical to the source streams.

Everything runs through a foreground service with a live progress notification, so a batch can keep processing even while the app is in the background.

## Tech stack

Kotlin, Jetpack Compose (Material 3), Navigation Compose, DataStore for settings, kotlinx.serialization for stats persistence, and the platform `MediaExtractor`/`MediaMuxer` and `Bitmap` APIs for the actual compression work — no third-party codec libraries.

## Building

```
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`. A GitHub Actions workflow (`Build Debug APK`, triggered manually from the Actions tab) builds the same APK in CI and uploads it as a downloadable artifact.
