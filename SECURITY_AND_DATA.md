# Data handling and upgrade notes

* Room schema version is 2. `DatabaseMigrations.MIGRATION_1_2` adds six columns; no table reset,
  destructive migration fallback or automatic uninstall is used. Original local database name is retained.
* Version-1 uploads had no fingerprint/import-source metadata. The new code backfills fingerprints from
  files when checking for duplicates. Matching semantic entries are skipped; check manually if an old
  entry has a materially different title/date, as it cannot be safely assumed to be the same record.
* Local mode is not a password-protected vault. The device owner can use its local workspace. Firebase
  authentication selects separate on-device account workspaces; this is not additional at-rest encryption.
* Account DB filenames and evidence/export folders use SHA-256 of UID (no raw email in filenames).
  Room data is not automatically uploaded just because FirebaseAuth has a current user.
* The explicit local-to-account move is permitted only into an empty account. Destination copy/transaction
  completes before source clearing. A rare failure after destination commit but before clearing the source
  can leave both copies; do not delete either blindly. This operation is not a distributed two-DB transaction.
* Confirmed document saves combine structured entries, selected skills and verified document metadata in
  one Room transaction. Re-extraction only changes the review draft until the user approves it.
* Skills are normalized and de-duplicated; conference topics start unchecked. No proficiency levels are
  inferred. Unsupported existing skills are not silently removed by later document uploads.
* Sharing grants access to a generated export URI, not all app files. FileProvider exposes only `exports/`
  and `cache/camera/`. WhatsApp targets are declared for package visibility. The app does not auto-send to
  contacts or claim delivery from a successful Android intent handoff.
* HTML uses escaped text and HTTP(S)/DOI links. It embeds no private evidence text. PDF includes actual
  link annotations. Resume PDF contains profile contact details the user entered; inspect before sharing.
* Raw scans remain local; private verified text is uploaded only on an explicitly confirmed cloud sync.
  Public portfolio publication has a separate confirmation and optional email checkbox. Phone is excluded
  from public hosting by default. Content the user types in a public description may still itself contain
  private information, so review those descriptions before publishing.
* Android automatic app backup is disabled rather than allowing a broad system backup of local evidence.
  Cloud snapshots do not include PDF/image binaries. Keep original documents backed up independently.
* Firebase client configuration is not a service-account credential. No client JSON was in the uploaded
  project; add your own. Never include admin private keys, passwords or signing credentials in source ZIPs.
