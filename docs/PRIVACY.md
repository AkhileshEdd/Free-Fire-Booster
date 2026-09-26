# Ember privacy policy — draft for publication

Effective date: set to the public release date. Publisher: Akhilesh Mahto. Contact: confirm a public support email before publication.

Ember is an independent gaming preparation and diagnostics app. It has no developer-operated backend, advertising, analytics or account system.

## Information on your device

The app stores your selected game and setup profile, setup notes, checklist choices, session start/end times, start/end battery level and battery temperature, and your ratings and notes. It retains the latest 200 completed sessions. Device diagnostics also read memory, storage, display refresh, battery state and network connection type. Ember checks only the two supported game packages to provide launch actions; it does not enumerate all installed applications.

Profile and session information remains in the app's private local storage. Network-test results remain in memory. Android cloud backup is disabled for the app; device manufacturer migration behavior may differ.

## Optional external connections

Only when you explicitly confirm a test, Ember makes 12 TCP connections to port 443 at your chosen public endpoint: Cloudflare (1.1.1.1) or Google (8.8.8.8). No application payload, session history or profile notes are transmitted. The endpoint provider can see your public IP address and connection metadata and handles these under its policies. Ember does not operate those endpoints or control their logs.

- Cloudflare privacy: https://www.cloudflare.com/privacypolicy/
- Google privacy: https://policies.google.com/privacy

Opening Google Play or a supported game hands control to those services and their privacy policies. Paid download processing is handled by Google Play; Ember does not receive or store payment card information.

## Export and deletion

You may export your journal to a CSV document in a location you choose using Android's file picker. That destination may be a cloud provider. Exported copies are outside Ember's private storage and must be deleted separately. Use More → Erase all local data, or uninstall the app, to delete local profiles, sessions and preferences. No server-side account deletion is needed because Ember has no accounts.

## Permissions

INTERNET supports user-requested connection tests. ACCESS_NETWORK_STATE reads the current connection status. There is no location, contacts, microphone, storage-wide, usage-access, notification-policy, VPN, accessibility or overlay permission.

## Before publishing this policy

Replace the effective-date and contact placeholders, verify this policy against the final signed build and any later SDK changes, and publish at a publicly accessible stable URL. Complete Google Play Data safety declarations after reviewing its current definitions; do not assume third-party endpoint traffic is automatically exempt from disclosure.
