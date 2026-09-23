# Android APK build

This is the Android wrapper for the PWA assets in `app/src/main/assets/web/`.

## Cloud build

Push this folder as the root of a private GitHub repository. The included GitHub Actions workflow builds a debug APK and uploads it as an Actions artifact. No signing secrets are needed for a debug build.

For a locally signed release, configure a keystore and signing secrets before enabling release builds. Never commit signing keys or API keys.
