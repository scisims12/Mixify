# Remote changes and user counts

## What is included

GMS and GMS Full **release** builds connect to the existing `mixify-1d661`
project for `com.mixify.app`. Izzy/F-Droid, debug and PR packages do not fetch
configuration or collect analytics. Firebase dependencies are GMS-only.

The supplied Android client configuration is represented by
`app/src/cloudRelease/res/values/firebase_config.xml` (the standard resource
values normally emitted by the Google Services plugin). Both GMS release source
sets include that directory. No service-account key or admin credential is used.
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
