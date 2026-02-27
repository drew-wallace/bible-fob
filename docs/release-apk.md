# APK Release Workflow

This repository includes a GitHub Actions workflow at `.github/workflows/release-apk.yml`.

## Triggering releases

The workflow runs when:

- A tag matching `v*` is pushed (for example: `v1.0.0`).
- It is manually started from **Actions → Release APK → Run workflow** (`workflow_dispatch`).

## What the workflow does

1. Checks out the repository.
2. Sets up JDK 17.
3. Sets up Android SDK.
4. Runs `./gradlew assembleRelease`.
5. Optionally signs the APK when all signing secrets are configured.
6. Uploads the resulting APK as a workflow artifact.
7. Creates or updates a GitHub Release for the tag and attaches the APK.

## Required secrets for APK signing

To enable APK signing, configure **all** of the following repository secrets:

- `KEYSTORE_BASE64`: Base64-encoded keystore file contents.
- `KEYSTORE_PASSWORD`: Keystore password.
- `KEY_ALIAS`: Key alias inside the keystore.
- `KEY_PASSWORD`: Key password.

If any of these are missing, the workflow still builds and releases an unsigned APK.

## Tag-based release flow

Typical release steps:

1. Update app versioning as needed.
2. Commit and push changes to the default branch.
3. Create and push a release tag:

   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

4. Open the GitHub Release created by the workflow and verify the attached APK.
5. (Optional) Re-run workflow manually from `workflow_dispatch` for the same tag if needed.
