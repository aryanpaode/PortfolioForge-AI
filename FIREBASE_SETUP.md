# Firebase email/password setup

## Required for registration and sign-in (not for local mode)

1. In the Firebase console, open the project you intend to use. Under Project settings → Your apps,
   register/select an **Android** application with the exact package name:
   `com.aryanpaode.portfolioforgeai`.
2. Download that Android app's **google-services.json** and put it at:
   `PortfolioForgeAI_Final/app/google-services.json` (not the project root). The file was not supplied
   in the user's uploaded ZIP, so no invented configuration is bundled. Do not paste a service-account
   private key, Firebase administrator password or personal email password into the app.
3. Under **Authentication → Sign-in method / Sign-in providers**, enable **Email/Password**.
   Do not choose email-link/passwordless sign-in as a substitute. Google sign-in is not used here;
   SHA-1 / Google OAuth setup is not necessary for this email/password flow.
4. Sync Gradle and run again. The existing conditional Google Services plugin now processes the file.
   Create an account, open the verification email, return to **Account & cloud**, and press
   **I have verified my email**. Resend has a one-minute local cooldown; backend quotas still apply.

`python tools/check_firebase_config.py` optionally verifies that the JSON contains the correct
Android package and required fields without printing the API key.

## Behavior in this implementation

* Password confirmation is checked before registration. At least six characters are required locally;
  a stronger password policy configured in your Firebase project can reject weaker input with an error.
* AuthStateListener + IdTokenListener drive Compose. Login/logout switches workspaces and clears the
  old account's navigation/form/export state. Firebase's own SDK handles session persistence.
* Passwords are held only in transient input state, not in Room, preferences or saved-state bundles.
* Signed-in but unverified users can do local work. Private cloud operations and public publishing
  require verification, user reload and a fresh token. Firestore rules enforce verified email too.
* Reset sends a generic response for an unknown email instead of deliberately exposing account existence.
* Explicit Logout hides account data and returns to the separate local workspace. It does not erase
  the signed-in user's saved Room records or pretend that Firebase auth is cloud profile backup.
* No Firebase project or mail provider has been live-tested here. Delivery can depend on spam filtering,
  enabled provider, project quota, email templates and your network. Test with an address you control.

## Optional: private cloud backup and restore

Authentication alone does not create a Firestore database. To use Account & cloud sync, create Cloud
Firestore in your Firebase project. Publish the included **firestore.rules** in the Firestore Rules tab
(or deploy via CLI below). Never leave private user collections in public/test mode for real data.

Private snapshot path: `users/{uid}/private/snapshot`. A single atomic document stores the profile,
career entries, approved skills and verified document text/metadata. No PDF/image binaries or local
filesystem paths are uploaded. The payload is limited conservatively below Firestore's document
limit; oversized payloads are rejected visibly, with the local workspace unchanged. Restore is a
non-destructive merge, not a deletion/replacement of the current local workspace. Metadata-only
restored documents can be edited using saved text or reattached by uploading the same original file.

Cloud sync is manual and explicitly confirmed. Edits are not automatically backed up on every keystroke.
Verify the signed-in account before overwriting its saved snapshot. Old per-collection structured
backups are readable through the legacy fallback in CloudService.

### Rules checks to perform before using real data

Use the Firestore Rules playground or emulator. Expected outcomes:

| Request | Expected |
|---|---|
| No auth → read/write `users/alice/private/snapshot` | Deny |
| Alice unverified → own private read/write | Deny |
| Alice verified → own private read/write | Allow |
| Bob verified → Alice's private data | Deny |
| Alice verified → create `publicPortfolios/portfolio-alice` with matching owner and expected fields | Allow |
| Alice unverified → create/update public portfolio | Deny |
| Bob verified → change/delete Alice's portfolio | Deny |
| No auth → get a specific shared public portfolio | Allow |
| No auth → list `publicPortfolios` | Deny |

These rules checks are a required deployment test, not claimed as executed in this package's preparation.

## Optional: clickable public website link

Sending a PDF through WhatsApp needs none of this. Do this only for a publicly hosted portfolio page.
The included website reads `publicPortfolios/portfolio-{uid}` and includes conferences and publications.
The app presents a publish confirmation; profile email is an unchecked opt-in and phone is excluded.

With the Firebase CLI installed and signed into your own project:

```text
firebase login
firebase deploy --project YOUR_PROJECT_ID --only firestore:rules,hosting
```

Run that from the directory containing `firebase.json`. Do not literally use `YOUR_PROJECT_ID`.
The example `.firebaserc.example` is only an example; the explicit `--project` flag avoids accidental
cross-project deployments. Do not reinitialize and overwrite the included rules/site unnecessarily.

Publish from the Android app, then test the displayed `https://<project-id>.web.app/?u=portfolio-<uid>`
link in a signed-out browser. The Hosting reserved `/__/firebase/init.js` configures the included
web client for the deployed project. Confirm both device app and Hosting use the **same** project.

The link is not a secret: explicitly published fields are readable without signing in. Disabling
collection listing does not turn a public portfolio into confidential storage. **Unpublish** removes
the current public Firestore record, not recipients' downloaded PDFs, screenshots or previously
created old-version slugs. Remove any old public slugs you no longer want directly in your project.

## Official references

* https://firebase.google.com/docs/android/setup
* https://firebase.google.com/docs/auth/android/password-auth
* https://firebase.google.com/docs/auth/android/manage-users
* https://firebase.google.com/docs/firestore/security/rules-conditions
* https://firebase.google.com/docs/hosting/quickstart
