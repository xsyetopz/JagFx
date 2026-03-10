# JagFx build targets
#
# Development
#   make run            run the desktop GUI
#   make build          build (debug)
#   make test           run all tests
#
# Release -- unsigned, no notarization
#   make publish-macos-arm64
#   make publish-macos-x64
#   make publish-windows
#   make publish-linux
#
# Release -- macOS signed + notarized DMG (requires .env)
#   make release-macos-arm64
#   make release-macos-x64
#   make release-macos          builds both arches

DESKTOP  := src/JagFx.Desktop
SCRIPTS  := scripts
CONF     := Release

.PHONY: run build test \
        publish-macos-arm64 publish-macos-x64 publish-windows publish-linux \
        release-macos-arm64 release-macos-x64 release-macos

# -- Development --------------------------------------------------------------

run:
	dotnet run --project $(DESKTOP)

build:
	dotnet build --nologo

test:
	dotnet test --nologo

# -- Unsigned publish ---------------------------------------------------------

publish-macos-arm64:
	dotnet publish $(DESKTOP) -c $(CONF) -r osx-arm64 -o publish/osx-arm64 --nologo

publish-macos-x64:
	dotnet publish $(DESKTOP) -c $(CONF) -r osx-x64 -o publish/osx-x64 --nologo

publish-windows:
	dotnet publish $(DESKTOP) -c $(CONF) -r win-x64 --self-contained -o publish/win-x64 --nologo

publish-linux:
	dotnet publish $(DESKTOP) -c $(CONF) -r linux-x64 --self-contained -o publish/linux-x64 --nologo

# -- Signed + notarized macOS DMG ---------------------------------------------

release-macos-arm64:
	$(SCRIPTS)/notarize-macos.sh osx-arm64

release-macos-x64:
	$(SCRIPTS)/notarize-macos.sh osx-x64

release-macos: release-macos-arm64 release-macos-x64
