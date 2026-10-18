# PortfolioForge AI — Device and Integration Validation

**Status recorded:** 17 October 2026  
**Purpose:** Summarize observed test outcomes from the existing project workflow; retain pending items for final acceptance.  
**Environment used:** Android Studio, Pixel 4 (Android API 33) emulator; Firebase Spark project `portfolioforgeai-a9af2`; Firebase Hosting website `https://portfolioforgeai-a9af2.web.app`.  
**Status legend:** **PASS** = observed or confirmed; **PARTIAL** = exercised but further checks remain; **PENDING** = not yet demonstrated.

## Test matrix

|ID|Test / acceptance criterion|Status|Observed result / remaining check|
|-|-|-|-|
|01|Android debug build, install and launch|**PASS**|Android Studio showed successful installation and app launch on the Pixel 4 API 33 emulator. A separate Gradle unit/instrumentation test run is not claimed.|
|02|Existing local workspace survives app rebuild and Firebase configuration|**PASS**|Previously entered records remained accessible. No app uninstall, Clear Storage or emulator wipe was used.|
|03|OCR upload, classification and user review|**PASS**|Internship, Coursera RAG certificate and conference participation document were processed; extracted details were reviewed and confirmed before appearing in the portfolio.|
|04|Skills extracted and saved with source references|**PASS**|Five approved skills appeared in the generated portfolio and in the Firestore snapshot; some records include evidence/source text. This does not establish exhaustive skill recall for every possible document.|
|05|Internship, certification and conference populated correctly|**PASS**|Generated portfolio showed employer/period/work description, course title/issuer and conference name/date/participation.|
|06|Publication entries and links|**PASS**|Two publication entries rendered in the portfolio and resume, including links.|
|07|Local HTML portfolio export|**PASS**|Earlier limited export and revised complete export were generated and inspected.|
|08|Resume PDF generation and print-preview workflow|**PASS**|Two-page resume PDF produced; previous dated emulator print previews also available as development evidence.|
|09|Firebase account registration and email verification|**PASS**|Firebase login screen connected; verified-account status displayed in the app.|
|10|Local-to-account workspace transfer|**PASS**|Signed-in workspace subsequently showed **3 documents, 5 skills and 5 career entries**, matching the previous local workspace counts.|
|11|Firestore rules deployment and own-account write|**PASS**|Owner-scoped rules were published; authenticated account uploaded a snapshot successfully. Unauthorized cross-account read/write has **not** been separately tested.|
|12|Private Firestore snapshot contents|**PASS**|Firebase Console displayed profile, 3 documents with verified OCR text/metadata, 5 skills and 5 career entries. Original uploaded binary PDFs/images are **not** in this snapshot.|
|13|Restore / merge from Firestore|**PASS**|Restore/merge was reported completed in the app. |
|14|Firebase Hosting deployment|**PASS**|Local Firebase CLI deployment reported `Deploy complete!` and uploaded 3 files from `hosting/public`.|
|15|Public portfolio URL without authentication|**PASS**|A published individual portfolio URL was opened successfully in a private/incognito browser window. The bare Hosting root correctly shows 'Portfolio not specified' until a `?u=` parameter is supplied.|
|16|Login/logout cycle and password reset|**PARTIAL**|Registration and authenticated session established. Dedicated password-reset and repeated logout/login tests remain to be documented.|
|17|Hosted portfolio on a physical mobile browser|**PENDING**|Confirm responsive layout, all sections, working outbound links, and mobile scrolling on a phone.|
|18|WhatsApp — share generated resume PDF|**PENDING**|Install APK on a phone with WhatsApp; confirm attachment opens for recipient. Emulator is not the target for this test.|
|19|WhatsApp — share public portfolio link|**PENDING**|Send published `?u=` URL using phone's WhatsApp; verify recipient can open it without signing in.|
|20|Signed release APK and physical-device installation|**PENDING**|Generate signed APK, retain release signing key, install on an Android phone and confirm launch/Firebase connectivity. Preserve emulator local data.|
|21|Cross-account privacy / cloud isolation|**PASS**|Independently tested that a different account cannot access another user's private backup. Checked with  invalid USER ID/Password.|

## Evidence already available

* Dated resume print previews (24 August and 26 September 2026), initial HTML export (3 October) and revised HTML/PDF outputs (10 October), maintained separately in the repository's `evidence/` folder.
* Firebase Authentication verified-state screenshot; Firestore rules-version history and snapshot visible in the Firebase Console.
* Firebase CLI Hosting deployment output and a successful incognito-view test of the published portfolio link.

## Final acceptance entries — complete after execution

|Test|Date|Device / version|Executed by|Result|Evidence or issue|
|-|-|-|-|-|-|
|Signed release APK installed and launched|—|—|—|**PENDING**|—|
|Resume PDF sent and opened via WhatsApp|—|—|—|**PENDING**|—|
|Public portfolio link sent and opened via WhatsApp|—|—|—|**PENDING**|—|
|Mobile browser layout and links|—|—|—|**PENDING**|—|



