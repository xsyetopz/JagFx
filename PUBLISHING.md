# Publishing

Use this guide when cutting a JagFx release or testing release artifacts locally.

## Release checklist

1. Confirm required GitHub Actions secrets are present. See [Secrets](#secrets).
2. Add the `CHANGELOG.md` entry, then bump the version: `scripts/bump-version.sh <version>`. See [Version bump](#version-bump).
3. Run local checks:
   ```bash
   ./gradlew check
   ```
4. Build local artifacts supported by the host OS. See [Host support](#host-support).
5. Create and push a version tag:
   ```bash
   git tag v<version>
   git push origin v<version>
   ```
6. Watch GitHub Actions -> `Release`.
7. Download artifacts from the workflow run.

## Secret handling

- Do not commit secrets.
- Store release credentials only in GitHub Actions secrets.
- Add or edit secrets at `Settings -> Secrets and variables -> Actions`.

## Host support

| Host OS | Local installer | Local signing/notarization | Release path |
| --- | --- | --- | --- |
| macOS | DMG for the host architecture with `just release-desktop`. | Signing and notarization with `./gradlew :desktopApp:notarizeReleaseDmg` and the `compose.desktop.mac.*` properties when Apple credentials are available. | Build and test locally; use CI for all release artifacts. |
| Windows | MSI with `just release-desktop`. | macOS notarization is not available. | Build and test locally; use CI for macOS and Linux artifacts. |
| Linux | DEB with `just release-desktop`. | macOS notarization is not available. | Build and test locally; use CI for macOS and Windows artifacts. |

jpackage builds only for the host OS and architecture, so CI builds macOS arm64 on `macos-latest` and macOS x64 on `macos-15-intel`. `just release-cli` builds the platform-independent CLI zip on any host.

## Host commands

### macOS

```bash
./gradlew check
just release-desktop
hdiutil verify publish/JagFx-*.dmg
```

Push the release tag after local verification. CI builds Windows, Linux, and signed macOS release artifacts.

### Windows

```powershell
./gradlew check
just release-desktop
```

CI handles macOS notarization and Linux artifacts.

### Linux

```bash
./gradlew check
just release-desktop release-cli
```

CI handles macOS notarization and the Windows installer.

## Secrets

### macOS signing and notarization

The release workflow uses these secrets for macOS DMG signing and notarization. It passes them to `./gradlew :desktopApp:notarizeReleaseDmg` as `-Pcompose.desktop.mac.sign`, `.signing.identity`, `.signing.keychain`, `.notarization.appleID`, `.notarization.teamID`, and `.notarization.password`. The app is signed with `desktopApp/macos/entitlements.plist`. When a secret is missing or `notarytool` rejects the credentials, the workflow builds an unsigned DMG instead.

1. `MACOS_CERT_P12_BASE64`
2. `MACOS_CERT_PASSWORD`
3. `MACOS_SIGN_IDENTITY`
4. `MACOS_NOTARY_APPLE_ID`
5. `MACOS_NOTARY_TEAM_ID`
6. `MACOS_NOTARY_PASSWORD`

#### `MACOS_CERT_P12_BASE64`

Export a Developer ID Application certificate and private key to `cert.p12` in Keychain Access, then copy the base64 payload:

```bash
base64 -i cert.p12 | pbcopy
```

#### `MACOS_CERT_PASSWORD`

Use the password set when exporting `cert.p12`.

#### `MACOS_SIGN_IDENTITY`

List signing identities:

```bash
security find-identity -v -p codesigning
```

Use the exact identity string, for example:

```text
Developer ID Application: Your Name (TEAMID)
```

#### `MACOS_NOTARY_APPLE_ID`

Use the Apple ID email for the Developer account.

#### `MACOS_NOTARY_TEAM_ID`

Use the Apple Developer Team ID tied to `MACOS_NOTARY_APPLE_ID`.

#### `MACOS_NOTARY_PASSWORD`

Use an Apple app-specific password, not the normal Apple ID password.

Create one at <https://account.apple.com/account/manage> under `Sign-In and Security -> App-Specific Passwords`.

## Version bump

Add the new version's entry at the top of `CHANGELOG.md` first, then run:

```bash
scripts/bump-version.sh 3.0.1
```

The script updates `version` in `gradle.properties` and `version` in `packages/*/package.json`.

## Triggering the release workflow

Workflow file: `.github/workflows/release.yml`.

Tag push:

```bash
git tag v<version>
git push origin v<version>
```

Manual run:

```text
GitHub -> Actions -> Release -> Run workflow
```

## Artifacts

The release workflow publishes:

- `JagFx-<version>-macos-arm64.dmg`
- `JagFx-<version>-macos-x64.dmg`
- `JagFx-<version>-windows-x64.msi`
- `jagfx_<version>_amd64.deb`
- `jagfx-cli-<version>.zip` (needs Java 21 or later)

The desktop installers bundle a Temurin JRE and use a ProGuard-shrunk release build.

## Troubleshooting

### Can macOS apps be notarized from Linux or Windows?

No. Notarization runs on a macOS runner.

### Apple notarization returns HTTP 401

The release workflow checks notarization credentials before the macOS build. If `notarytool` reports HTTP 401, the workflow warns and builds an unsigned DMG so other artifacts can publish.

Fix the secrets before cutting a notarized macOS release:

1. Refresh `MACOS_NOTARY_PASSWORD` with a current Apple app-specific password.
2. Confirm `MACOS_NOTARY_APPLE_ID` belongs to the Developer account.
3. Confirm `MACOS_NOTARY_TEAM_ID` matches that account.
