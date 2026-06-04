# Publishing Guide

Release steps for macOS, Windows, and Linux hosts.

## Quick path

1. Confirm required GitHub secrets exist (section **Secrets**).
2. Bump version in `Directory.Build.props`.
3. Run local preflight:
   ```bash
   dotnet build --nologo
   dotnet test --nologo
   ```
4. Build the installers supported by your host OS (see matrix below).
5. Tag + push:
   ```bash
   git tag v2.2.2
   git push origin v2.2.2
   ```
6. Watch `Actions -> Release`.
7. Download artifacts from the workflow run.

---

## Secret handling

- Never commit secrets to git.
- Store secrets only in GitHub Actions Secrets.
- GitHub path: `Repo -> Settings -> Secrets and variables -> Actions -> New repository secret`

---

## Host OS matrix (what you can do locally)

| Host OS | Local app binaries                                    | Local installer                                    | Local signing/notarization                                                 | Release path                                             |
| ------- | ----------------------------------------------------- | -------------------------------------------------- | -------------------------------------------------------------------------- | -------------------------------------------------------- |
| macOS   | macOS / Windows / Linux binaries via `dotnet publish` | macOS DMG (`just release-desktop-macos-*`)         | macOS signing + notarization: **yes** (with Apple cert + credentials)      | Build/test locally, use CI for the full multi-OS release |
| Windows | macOS / Windows / Linux binaries via `dotnet publish` | Windows setup EXE (`just release-desktop-windows`) | Windows signing: **yes** (with code-sign cert), macOS notarization: **no** | Build/test locally, use CI for macOS notarization        |
| Linux   | macOS / Windows / Linux binaries via `dotnet publish` | Linux AppImage/tar (`just release-desktop-linux`)  | macOS notarization: **no**, Windows signing: usually **no**                | Build/test locally, use CI for signed installers         |

Develop on any supported OS. CI produces the trusted release installers on target OS runners.

---

## Host OS playbooks

### If you develop on macOS

1. Build + test:
   ```bash
   dotnet build --nologo
   dotnet test --nologo
   ```
2. Build local macOS installers:
   ```bash
   just release-desktop-macos-arm64
   just release-desktop-macos-x64
   ```
3. Verify DMGs:
   ```bash
   hdiutil verify publish/JagFx-*-macos-arm64.dmg
   hdiutil verify publish/JagFx-*-macos-x64.dmg
   ```
4. Push the tag. CI builds all platforms and runs signing steps.

### If you develop on Windows

1. Build + test:
   ```powershell
   dotnet build --nologo
   dotnet test --nologo
   ```
2. Build local Windows installer:
   ```powershell
   just release-desktop-windows
   ```
3. Sign locally only if the certificate is available. Otherwise, CI signs release artifacts.
4. Push the tag. CI handles macOS notarization and Linux artifacts.

### If you develop on Linux

1. Build + test:
   ```bash
   dotnet build --nologo
   dotnet test --nologo
   ```
2. Build local Linux artifacts:
   ```bash
   just release-desktop-linux
   ```
3. Push the tag. CI handles macOS and Windows installer trust steps.

---

## Secrets

### Required for macOS signing and notarization in CI

1. `MACOS_CERT_P12_BASE64`
2. `MACOS_CERT_PASSWORD`
3. `MACOS_SIGN_IDENTITY`
4. `MACOS_NOTARY_APPLE_ID`
5. `MACOS_NOTARY_TEAM_ID`
6. `MACOS_NOTARY_PASSWORD`

### Secret sources

#### `MACOS_CERT_P12_BASE64`

Export **Developer ID Application** cert + private key to `cert.p12` in Keychain Access, then:

```bash
base64 -i cert.p12 | pbcopy
```

#### `MACOS_CERT_PASSWORD`

Password you used while exporting `cert.p12`.

#### `MACOS_SIGN_IDENTITY`

```bash
security find-identity -v -p codesigning
```

Use exact value like:

`Developer ID Application: Your Name (TEAMID)`

#### `MACOS_NOTARY_APPLE_ID`

Apple ID email for the Developer account.

#### `MACOS_NOTARY_TEAM_ID`

Apple Developer Team ID.

#### `MACOS_NOTARY_PASSWORD`

Apple app-specific password, not the normal login password:

- <https://account.apple.com/account/manage>
- Sign-In and Security -> App-Specific Passwords

### Optional Windows signing

The workflow does not require these unless Windows signing is enabled:

- `WIN_CERT_PFX_BASE64`
- `WIN_CERT_PASSWORD`

---

## Bump version

Edit `Directory.Build.props`:

- `Version`
- `AssemblyVersion`
- `FileVersion`
- `InformationalVersion`

Example patch bump:

- `2.2.2` -> `2.2.3`
- `2.2.2.0` -> `2.2.3.0`

---

## Trigger release workflow

Workflow file: `.github/workflows/release.yml`

Tag push:

```bash
git tag v2.2.2
git push origin v2.2.2
```

Manual run:

- GitHub -> `Actions` -> `Release` -> `Run workflow`.

---

## Expected artifacts

- macOS arm64 DMG
- macOS x64 DMG
- Windows x64 setup EXE
- Linux x64 AppImage (or tar fallback)
- CLI archives per platform

---

## Troubleshooting

### “Can I sign Windows apps from macOS?”

Not with the current local setup. Sign on a Windows runner in CI or on a Windows machine.

### “Can I notarize macOS app from Linux/Windows?”

No. This workflow runs notarization on a macOS runner.

### `No space left on device` while building DMG

The DMG recipe already sets an explicit size and retries. Re-run:

```bash
just release-desktop-macos-arm64
```

### `ISCC not found`

Install Inno Setup and put `ISCC` on PATH.
