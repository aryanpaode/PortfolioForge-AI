# Start here — use the complete project

## 1. Preserve your current working copy and installed data

Keep the original project ZIP/folder unchanged as your backup. Extract this revised project into a
**new folder**, then choose **Open** in Android Studio and select the folder containing `settings.gradle.kts`.
Do not open `app/` by itself and do not copy the old `app/build`, `.gradle`, `.idea` or `local.properties`.
Android Studio creates a local SDK path for your machine.

**Do not uninstall the existing app or clear its storage just to install this update.** Room v1→v2 is
an additive migration and retains the original `portfolioforge.db` local workspace. With the same
application ID and signing key, Run should install an update. A different signing certificate can
cause `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; stop rather than uninstalling and losing data. Use the
same machine/debug key that built your existing installation. No signing key is included here.

## 2. Sync with the existing Flamingo toolchain

Use Gradle JDK **17** under Android Studio Settings → Build, Execution, Deployment → Build Tools → Gradle.
Keep AGP 8.0.2, Kotlin 1.8.21 and compile SDK 33. Install Android SDK Platform 33 if your SDK lacks it.
Let Gradle sync online once (the new PDFBox Android dependency must be downloaded). Run on the
existing emulator/device. No Java/XML UI rewrite or KSP conversion is required.

Command line (JDK 17 / Android SDK installed):

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The standard debug APK will then be under `app/build/outputs/apk/debug/`. There is no prebuilt APK
in this package because a full Android build was not available in the preparation environment.

## 3. First verify document-to-section flow in local mode

Choose **Continue in local workspace**. Your existing saved profile/documents should still be there
when you update the existing installation. For each older certificate, open **Docs → Review / reprocess**.
Inspect the proposed type, title, issuer/company, date and other details. Use **Re-extract fields from text**
when you want new suggestions; use **View / correct source text** if text was missed. **Read original
file again using OCR** retries the actual PDF/image, not just parsing old text.

Select the relevant entry and supported skills, then **Confirm & save selected information**.
Check **Build → Career details / Publications / Conferences & seminars** and regenerate the outputs.
A section saves even with no skills selected. Reprocessing uses the same document-linked entry IDs;
matching already approved/manual entries are retained rather than blindly overwritten.

An old unsupported `PaddleOCR` skill is not automatically erased: it might have come from another
legitimate document. Review it in **Skills**, and remove it if it is not yours. Unchecking a skill in a
new document review prevents adding it; it does not silently delete an existing profile skill.

## 4. Enable your Firebase project

See **FIREBASE_SETUP.md**. This upload did not contain `google-services.json`. Without the correct
file, local mode works and the authentication UI explains the setup instead of pretending to log in.
Email/password authentication does not require linking Gmail or Google Drive.

When you first sign in, the **account workspace is separate** from the old local workspace. It may
therefore be empty. To adopt the old data, before adding anything to the new account choose:
**Account & cloud → Move local data to this account**, read the confirmation and proceed only if that
data is yours. The destination must be empty. Files/data are copied before clearing the local source.
This explicit move prevents every newly registered account from inheriting the previous person's data.

## 5. WhatsApp without Gmail/Drive

Generate a PDF in **Build → Resume PDF** or **Portfolio & sharing → Easiest WhatsApp option**.
Press **Share to WhatsApp**. On the same Android device, choose a recipient and press Send in WhatsApp.
The app also recognizes WhatsApp Business. Installing WhatsApp only on your PC or a different phone
does not add it to the emulator. Use a real Android phone with WhatsApp for the simplest test.

No Firebase Hosting, Gmail OAuth, Drive connection or cloud account is required for sending the PDF.
When WhatsApp is absent the app says so. **Save to device** uses Android's location picker; **More apps**
uses the normal share sheet. Successful handoff is not reported as confirmed recipient delivery.

HTML stays available as a downloadable file, but PDF is the primary WhatsApp attachment. A true
HTTPS portfolio link additionally requires the optional Firebase Hosting deployment; generating an
HTML file does not automatically publish a website.

## 6. Keep the genuine team contribution history

The ZIP intentionally has no `.git` directory. Do not replace/delete the shared repository history.
After testing, copy the changed/new files into the existing repository and make normal commits.
Each student should personally review, implement and commit their actual contributions under their
own GitHub identity. Do not fabricate author names, backdate commits, or recreate the repository.
The full file list and obsolete documentation list are in **CHANGED_FILES.md**.
