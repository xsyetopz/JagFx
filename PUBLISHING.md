# Publishing Guide (host-OS aware)

Use this if you develop on macOS, Windows, or Linux.

## 10-minute quick path

1. Confirm required GitHub secrets exist (section **Secrets**).
2. Bump version in `Directory.Build.props`.
3. Run local preflight:
   ```bash
   dotnet build --nologo
   dotnet test --nologo
   ```
4. Build local installers you can build on your host OS (see matrix below).
5. Tag + push:
   ```bash
   git tag v2.2.2
   git push origin v2.2.2
   ```
6. Watch `Actions -> Release`.
7. Download release artifacts from workflow run.

---

## Safety rule

- Never commit secrets to git.
- Put secrets only in GitHub Actions Secrets.
- Path: `Repo -> Settings -> Secrets and variables -> Actions -> New repository secret`

---

## Host OS matrix (what you can do locally)

| You develop on | Can build app binaries locally                        | Can build installer locally                        | Can sign/notarize locally                                                  | Recommended path                                         |
| -------------- | ----------------------------------------------------- | -------------------------------------------------- | -------------------------------------------------------------------------- | -------------------------------------------------------- |
| macOS          | macOS / Windows / Linux binaries via `dotnet publish` | macOS DMG (`make release-desktop-macos-*`)         | macOS signing + notarization: **yes** (with Apple cert + creds)            | Build/test locally, rely on CI for full multi-OS release |
| Windows        | macOS / Windows / Linux binaries via `dotnet publish` | Windows setup EXE (`make release-desktop-windows`) | Windows signing: **yes** (with code-sign cert), macOS notarization: **no** | Build/test locally, rely on CI for macOS notarization    |
| Linux          | macOS / Windows / Linux binaries via `dotnet publish` | Linux AppImage/tar (`make release-desktop-linux`)  | macOS notarization: **no**, Windows signing: usually **no**                | Build/test locally, rely on CI for signed installers     |

**Key truth:** developing on one OS is fine. Final trusted installers come from CI runners for each target OS.

---

## Quick playbooks by host OS

### If you develop on macOS

1. Build + test:
   ```bash
   dotnet build --nologo
   dotnet test --nologo
   ```
2. Build local macOS installers:
   ```bash
   make release-desktop-macos-arm64
   make release-desktop-macos-x64
   ```
3. Verify DMGs:
   ```bash
   hdiutil verify publish/JagFx-*-macos-arm64.dmg
   hdiutil verify publish/JagFx-*-macos-x64.dmg
   ```
4. Push tag. Let CI build all platforms and run signing steps.

### If you develop on Windows

1. Build + test:
   ```powershell
   dotnet build --nologo
   dotnet test --nologo
   ```
2. Build local Windows installer:
   ```powershell
   make release-desktop-windows
   ```
3. Optional local signing (if cert available). Otherwise CI handles release artifacts.
4. Push tag. CI handles macOS notarization + Linux artifacts.

### If you develop on Linux

1. Build + test:
   ```bash
   dotnet build --nologo
   dotnet test --nologo
   ```
2. Build local Linux artifacts:
   ```bash
   make release-desktop-linux
   ```
3. Push tag. CI handles macOS and Windows installer trust steps.

---

## Secrets

## Required now (macOS signing + notarization in CI)

1. `MACOS_CERT_P12_BASE64`
2. `MACOS_CERT_PASSWORD`
3. `MACOS_SIGN_IDENTITY`
4. `MACOS_NOTARY_APPLE_ID`
5. `MACOS_NOTARY_TEAM_ID`
6. `MACOS_NOTARY_PASSWORD`

### Where these come from

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

Apple ID email used for Developer account.

#### `MACOS_NOTARY_TEAM_ID`

Apple Developer Team ID.

#### `MACOS_NOTARY_PASSWORD`

Apple app-specific password (not your normal login password):

- <https://account.apple.com/account/manage>
- Sign-In and Security -> App-Specific Passwords

### Optional/future (Windows signing)

Not required by current workflow. Use when you add Windows signing step:

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

Option A (normal):

```bash
git tag v2.2.2
git push origin v2.2.2
```

Option B:

- GitHub -> `Actions` -> `Release` -> `Run workflow`

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

Not directly in this setup. Sign on Windows runner (CI) or Windows machine.

### “Can I notarize macOS app from Linux/Windows?”

Not in this workflow. Notarization runs on macOS runner.

### `No space left on device` while building DMG

DMG creator already uses explicit size + retry. Re-run:

```bash
make release-desktop-macos-arm64
```

### `ISCC not found`

Install Inno Setup and ensure `ISCC` in PATH.
