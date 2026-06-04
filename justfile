# JagFx build and release targets
#
# Development
#   just run
#   just build
#   just test
#   just format
#   just format-check
#
# Desktop publish (single-file, self-contained)
#   just publish-desktop-macos-arm64
#   just publish-desktop-macos-x64
#   just publish-desktop-windows
#   just publish-desktop-linux
#
# Desktop installers
#   just release-desktop-macos-arm64   # .dmg
#   just release-desktop-macos-x64     # .dmg
#   just release-desktop-windows       # Inno Setup .exe
#   just release-desktop-linux         # .AppImage + .tar.gz
#
# CLI artifacts
#   just release-cli-macos-arm64
#   just release-cli-macos-x64
#   just release-cli-windows
#   just release-cli-linux
#
# Convenience
#   just release-macos
#   just release-cli
#   just release-all

DESKTOP := "src/JagFx.Desktop"
CLI     := "src/JagFx.Cli"
CONF    := "Release"
VERSION := `grep -oE '<Version>[^<]+' Directory.Build.props | head -1 | sed 's/<Version>//'`
CURDIR  := `pwd`

PUBLISH_FLAGS := "--self-contained /p:PublishSingleFile=true /p:IncludeNativeLibrariesForSelfExtract=true /p:IncludeAllContentForSelfExtract=true /p:DebugSymbols=false /p:DebugType=None"

run:
	dotnet run --project {{DESKTOP}}

build:
	dotnet build --nologo

format:
	dotnet tool restore
	dotnet csharpier format .

format-check:
	dotnet tool restore
	dotnet csharpier check .

test:
	dotnet test --nologo

clean:
	rm -rf publish


# -- Desktop publish -----------------------------------------------------------

publish-desktop-macos-arm64:
	dotnet publish {{DESKTOP}} -c {{CONF}} -r osx-arm64 -o publish/desktop/osx-arm64 --nologo {{PUBLISH_FLAGS}}

publish-desktop-macos-x64:
	dotnet publish {{DESKTOP}} -c {{CONF}} -r osx-x64 -o publish/desktop/osx-x64 --nologo {{PUBLISH_FLAGS}}

publish-desktop-windows:
	dotnet publish {{DESKTOP}} -c {{CONF}} -r win-x64 -o publish/desktop/win-x64 --nologo {{PUBLISH_FLAGS}}

publish-desktop-linux:
	dotnet publish {{DESKTOP}} -c {{CONF}} -r linux-x64 -o publish/desktop/linux-x64 --nologo {{PUBLISH_FLAGS}}

# -- Desktop installers --------------------------------------------------------

package-desktop-macos-arm64:
	tools/release/create-dmg.sh publish/desktop/osx-arm64/JagFx.app publish/JagFx-{{VERSION}}-macos-arm64.dmg "JagFx macOS arm64"

package-desktop-macos-x64:
	tools/release/create-dmg.sh publish/desktop/osx-x64/JagFx.app publish/JagFx-{{VERSION}}-macos-x64.dmg "JagFx macOS x64"

release-desktop-macos-arm64: publish-desktop-macos-arm64 package-desktop-macos-arm64

release-desktop-macos-x64: publish-desktop-macos-x64 package-desktop-macos-x64

release-desktop-windows: publish-desktop-windows
	@if command -v ISCC >/dev/null 2>&1; then \
		ISCC /DAppVersion={{VERSION}} /DSourceDir="{{CURDIR}}\publish\desktop\win-x64" /DOutputDir="{{CURDIR}}\publish" /DIconFile="{{CURDIR}}\assets\jagfx-icon.ico" packaging/windows/JagFx.iss; \
	else \
		echo "ISCC not found. Install Inno Setup and ensure ISCC is on PATH."; \
		exit 1; \
	fi

release-desktop-linux: publish-desktop-linux
	@if command -v appimagetool >/dev/null 2>&1; then \
		tools/release/create-appimage.sh publish/desktop/linux-x64/JagFx.Desktop publish/JagFx-{{VERSION}}-linux-x64.AppImage {{VERSION}}; \
	else \
		echo "appimagetool not found; skipping AppImage and producing .tar.gz fallback only."; \
	fi
	tar -czf publish/JagFx-{{VERSION}}-linux-x64.tar.gz -C publish/desktop/linux-x64 JagFx.Desktop

# -- CLI publish ---------------------------------------------------------------

publish-cli-macos-arm64:
	dotnet publish {{CLI}} -c {{CONF}} -r osx-arm64 -o publish/cli/osx-arm64 --nologo {{PUBLISH_FLAGS}}

publish-cli-macos-x64:
	dotnet publish {{CLI}} -c {{CONF}} -r osx-x64 -o publish/cli/osx-x64 --nologo {{PUBLISH_FLAGS}}

publish-cli-windows:
	dotnet publish {{CLI}} -c {{CONF}} -r win-x64 -o publish/cli/win-x64 --nologo {{PUBLISH_FLAGS}}

publish-cli-linux:
	dotnet publish {{CLI}} -c {{CONF}} -r linux-x64 -o publish/cli/linux-x64 --nologo {{PUBLISH_FLAGS}}

# -- CLI artifacts -------------------------------------------------------------

release-cli-macos-arm64: publish-cli-macos-arm64
	tar -czf publish/JagFx.Cli-{{VERSION}}-macos-arm64.tar.gz -C publish/cli/osx-arm64 JagFx.Cli

release-cli-macos-x64: publish-cli-macos-x64
	tar -czf publish/JagFx.Cli-{{VERSION}}-macos-x64.tar.gz -C publish/cli/osx-x64 JagFx.Cli

release-cli-windows: publish-cli-windows
	cd publish/cli/win-x64 && zip -rq ../../JagFx.Cli-{{VERSION}}-win-x64.zip JagFx.Cli.exe

release-cli-linux: publish-cli-linux
	tar -czf publish/JagFx.Cli-{{VERSION}}-linux-x64.tar.gz -C publish/cli/linux-x64 JagFx.Cli

# -- Convenience ---------------------------------------------------------------

release-macos: release-desktop-macos-arm64 release-desktop-macos-x64

release-cli: release-cli-macos-arm64 release-cli-macos-x64 release-cli-windows release-cli-linux

release-all: release-macos release-desktop-windows release-desktop-linux release-cli

# -- Backwards-compatible aliases ---------------------------------------------

publish-macos-arm64: publish-desktop-macos-arm64
publish-macos-x64: publish-desktop-macos-x64
publish-windows: publish-desktop-windows
publish-linux: publish-desktop-linux

release-macos-arm64: release-desktop-macos-arm64
release-macos-x64: release-desktop-macos-x64
release-windows: release-desktop-windows
release-linux: release-desktop-linux
