# Remote changes and user counts

## What is included

GMS and GMS Full **release** builds connect to the existing `mixify-1d661`
project for `com.mixify.app`. Izzy/F-Droid, debug and PR packages do not fetch
configuration or collect analytics. Firebase dependencies are GMS-only.

The supplied Android client configuration is represented by
`app/src/cloudRelease/res/values/firebase_config.xml` (the standard resource
values normally emitted by the Google Services plugin). Both GMS release source
sets include that directory. The API key is generated as a release-only resource
from `FIREBASE_API_KEY`, not stored in this XML file. No service-account key or admin credential is used.
Do not add a second Google Services plugin/config generator without removing
these resources, or it will generate duplicate resources. For a new application
ID, register a separate Firebase Android app and replace the release resources;
the runtime guard in AppCloud must also be updated deliberately.

One app release containing this integration must be installed by users first.
Previously installed versions cannot start reporting retroactively.

## Finish setup in Firebase Console

1. Finish the Android registration wizard (Next, then Continue to console).
   The SDK integration is in this branch; do not paste the wizard's Gradle code
   over this implementation.
2. In Project settings → Integrations, enable/link Google Analytics if not already
   enabled. Then open Analytics → Dashboard or Google Analytics Reports.
3. Find Remote Config using the console's product search. Add and publish:

| Parameter | Type | Default | Effect |
| --- | --- | --- | --- |
| `home_title` | String | empty string | Overrides the home greeting; empty restores the normal greeting |
| `announcement_enabled` | Boolean | `false` | Shows/hides the home announcement |
| `announcement_text` | String | empty string | Announcement body, up to 500 characters |

Example: set `announcement_enabled` to `true` and `announcement_text` to
`Welcome to Mixify! New playlists are available.` Then **Publish changes**.
The app fetches at startup with a one-hour minimum fetch interval; the real-time
listener fetches published changes while connected in the foreground. Values
activate on receipt and the Compose UI updates without restarting. Delivery is
not guaranteed instantly to every device. Offline users retain activated cached
values; first-run offline users see normal UI. Empty/disabled announcements stay
hidden. Home titles are trimmed and capped at 120 characters. Only these wired
parameters change remotely; Kotlin logic, new screens and native permissions
still require a new APK. Never store secrets in Remote Config.

## Read the user count correctly

Use Analytics **Total users** for the selected date range, and **Active users**
for engagement; inspect new-user and first-open reports separately. These are
analytics estimates of measured app instances/users, **not** an exact count of
all people, all historical downloads, or registered accounts. Mixify does not
have a central app account registry in this integration. Reinstallation,
multiple devices, opt-outs, offline use and users remaining on old APKs affect
counts. Reports may take time to populate (allow 24–48 hours for standard
reports). No historical user count can be recovered by adding this SDK.

Basic automatic Firebase Analytics events are enabled by default for supported
release builds. Users can stop collection and reset the local analytics ID in
Settings → Privacy → Usage analytics. This does not delete previously uploaded
server data or disable Remote Config. No song names, search queries, playlists,
account credentials, or custom user IDs are logged by this integration.
Advertising ID permission/collection and ad personalization signals are disabled.
Both the in-app and HTML privacy disclosures describe the new Google/Firebase
processing. Update store data disclosures as appropriate before distributing.

## Validation before release

- Compile GMS release, GMS Full release, and Izzy debug with JDK 21 and the Android
  SDK required by the repository. `.github/workflows/cloud-checks.yml` performs
  these compilation checks for the PR, including release-only resources.
- Install a signed **GMS release** with application ID `com.mixify.app` on a test
  device. The existing PR APK uses a different ID and intentionally does not
  connect to production Firebase.
- Enable Firebase Analytics debug mode on that device:
  `adb shell setprop debug.firebase.analytics.app com.mixify.app`, then open the
  app and inspect Analytics DebugView. Afterwards turn debug mode off:
  `adb shell setprop debug.firebase.analytics.app .none`.
- Publish the sample announcement; verify it appears and changes while the app
  is open. Disable it and clear `home_title` to restore the default UI.
- Reopen offline and confirm cached UI, then test a fresh offline install.
- Toggle analytics off, restart, and verify collection stays off; Remote Config
  should continue to function. Izzy must build without any Firebase dependencies.

Local compilation in the authoring environment was blocked before compilation
by restricted network access to the Gradle distribution. Device/console checks
must still be completed; no live analytics data or successful APK build is claimed.

## API-key setup and secret-scanning alert

### Android Studio

1. Add `FIREBASE_API_KEY=YOUR_FIREBASE_CLIENT_API_KEY` to the root
   `local.properties` (already ignored by Git). Keep the existing `sdk.dir`.
2. Sync Gradle and build a GMS release. Do not paste the value into Kotlin,
   XML, Gradle source, issues, or chat. Do not commit `local.properties`.
3. Test Remote Config and Analytics on the registered production package.
   Debug, PR and Izzy builds still do not initialize production Firebase.

### GitHub Actions

In repository Settings → Secrets and variables → Actions, add the repository
secret `FIREBASE_API_KEY` using the same Firebase client key. The release,
nightly and quick-build workflows pass it to Gradle. Compilation checks and
fork PRs can run without it; missing configuration disables cloud services.
Before publishing a production APK, verify the secret is configured and test
Analytics and Remote Config. A missing key must not be mistaken for a successful
Firebase integration.

### Review the exposed key before resolving the alert

Moving a key out of source does not remove it from old commits or existing APKs.
Firebase client keys remain extractable from built apps. They identify a project;
they do not authorize database access. Review the key in Google Cloud → APIs &
Services → Credentials and restrict it to the required Firebase APIs. Never
allow Generative Language API or unrelated billable APIs on this client key.
Test restrictions before changing a production key. Use separate keys for
non-Firebase APIs, and enforce Security Rules/App Check for applicable products.

If the exposed key has inappropriate access or suspected misuse, replace it,
update local.properties and the Actions secret, test and distribute the new app,
then revoke the old key with an explicit migration plan for installed versions.
Do not revoke a production key blindly: older APKs still contain it.

The old GitHub alert does not disappear when this patch is merged. After
verifying Firebase-only restrictions, resolve it as **Used in tests** only for an
actual test key; for an intentionally public, correctly restricted Firebase
client key choose **Won't fix** and explain that decision. Choose **Revoked**
only after actual revocation. Do not label a real key as a false positive merely
because it was moved. No history rewrite is required by this patch.

Reference: https://firebase.google.com/docs/projects/api-keys
