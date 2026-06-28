# Publishing

Use this guide when cutting a JagFx release or testing release artifacts locally.

## Release checklist

1. Confirm required GitHub Actions secrets are present. See [Secrets](#secrets).
2. Update version fields in `Directory.Build.props`.
3. Run local checks:
   ```bash
   dotnet build --nologo
   dotnet test --nologo
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

| Host OS | Local app binaries | Local installer | Local signing/notarization | Release path |
| --- | --- | --- | --- | --- |
| macOS | macOS, Windows, and Linux binaries via `dotnet publish`. | macOS DMG with `just release-desktop-macos-*`. | macOS signing and notarization when Apple credentials are available. | Build and test locally; use CI for all release artifacts. |
| Windows | macOS, Windows, and Linux binaries via `dotnet publish`. | Windows setup EXE with `just release-desktop-windows`. | Windows signing when a certificate is available. macOS notarization is not available. | Build and test locally; use CI for macOS and Linux artifacts. |
| Linux | macOS, Windows, and Linux binaries via `dotnet publish`. | Linux AppImage or tarball with `just release-desktop-linux`. | macOS notarization is not available. Windows signing usually runs in CI. | Build and test locally; use CI for macOS and Windows artifacts. |

## Host commands

### macOS

```bash
dotnet build --nologo
dotnet test --nologo
just release-desktop-macos-arm64
just release-desktop-macos-x64
hdiutil verify publish/JagFx-*-macos-arm64.dmg
hdiutil verify publish/JagFx-*-macos-x64.dmg
```

Push the release tag after local verification. CI builds Windows, Linux, and signed macOS release artifacts.

### Windows

```powershell
dotnet build --nologo
dotnet test --nologo
just release-desktop-windows
```

Sign locally only when the Windows certificate is available. CI handles macOS notarization and Linux artifacts.

### Linux

```bash
dotnet build --nologo
dotnet test --nologo
just release-desktop-linux
```

CI handles macOS notarization and Windows installer trust steps.

## Secrets

### macOS signing and notarization

The release workflow uses these secrets for macOS DMG signing and notarization:

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

### Optional Windows signing

Windows signing uses these secrets only when Windows signing is enabled:

- `WIN_CERT_PFX_BASE64`
- `WIN_CERT_PASSWORD`

## Version bump

Edit these fields in `Directory.Build.props`:

- `Version`
- `AssemblyVersion`
- `FileVersion`
- `InformationalVersion`

Patch bump example:

```text
2.4.1   -> 2.4.2
2.4.1.0 -> 2.4.2.0
```

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

- macOS arm64 DMG
- macOS x64 DMG
- Windows x64 setup EXE
- Linux x64 AppImage or tarball fallback
- CLI archives for supported platforms

## Troubleshooting

### Can Windows apps be signed from macOS?

Not with the current local setup. Sign on a Windows machine or in CI on a Windows runner.

### Can macOS apps be notarized from Linux or Windows?

No. Notarization runs on a macOS runner.

### `No space left on device` while building a DMG

The DMG recipe sets an explicit size and retries. Re-run the target:

```bash
just release-desktop-macos-arm64
```

### Apple notarization returns HTTP 401

The release workflow checks notarization credentials before the macOS build. If `notarytool` reports HTTP 401, the workflow warns and skips DMG notarization so other artifacts can publish.

Fix the secrets before cutting a notarized macOS release:

1. Refresh `MACOS_NOTARY_PASSWORD` with a current Apple app-specific password.
2. Confirm `MACOS_NOTARY_APPLE_ID` belongs to the Developer account.
3. Confirm `MACOS_NOTARY_TEAM_ID` matches that account.

### `ISCC not found`

Install Inno Setup and put `ISCC` on `PATH`.
