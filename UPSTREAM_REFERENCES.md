# Implementation references

The existing Android/Compose/Room/Firebase versions are deliberately retained for the uploaded
Flamingo project, not upgraded to the newest SDKs. These primary references explain the APIs used:

* Android sharing / temporary URI permission: https://developer.android.com/training/secure-file-sharing/share-file
* FileProvider: https://developer.android.com/reference/androidx/core/content/FileProvider
* Room migrations: https://developer.android.com/training/data-storage/room/migrating-db-versions
* ML Kit text recognition: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
* PDFBox Android (2.0.27.0): https://github.com/TomRoush/PdfBox-Android
* Firebase setup and auth: references in FIREBASE_SETUP.md

Gradle resolves third-party runtime dependencies under their respective licenses. No dependency font
files, signing keys, service-account credentials or APK binaries are redistributed in this source ZIP.
