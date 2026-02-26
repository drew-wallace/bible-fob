# Android App Links

The app declares verified app links (`android:autoVerify="true"`) for:

- `https://www.biblegateway.com/passage`
- `https://biblegateway.com/passage`

Only the `/passage` route is handled in-app. Other incoming deep links are immediately redirected to an external browser.

## Domain verification requirement

For Android App Link verification to succeed, the domain owner must host a valid Digital Asset Links file at:

- `https://www.biblegateway.com/.well-known/assetlinks.json`
- optionally `https://biblegateway.com/.well-known/assetlinks.json`

Without this file (or with invalid contents), Android may still show chooser behavior instead of silently verifying and opening links directly in the app.
