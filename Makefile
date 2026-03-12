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
#
# Release -- macOS signed DMG, skip notarization
#   make release-macos-arm64 SKIP_NOTARIZE=1
#   make release-macos-x64  SKIP_NOTARIZE=1
#
# Release -- Linux / Windows distributable archives
#   make release-linux          .tar.gz
#   make release-windows        .zip

DESKTOP  := src/JagFx.Desktop
SCRIPTS  := scripts
CONF     := Release
VERSION  := $(shell grep -oE '<Version>[^<]+' Directory.Build.props | head -1 | sed 's/<Version>//')

NOTARIZE_FLAGS := $(if $(SKIP_NOTARIZE),--skip-notarize)

.PHONY: run build test \
        publish-macos-arm64 publish-macos-x64 publish-windows publish-linux \
        release-macos-arm64 release-macos-x64 release-macos \
        release-linux release-windows

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
	$(SCRIPTS)/notarize-macos.sh $(NOTARIZE_FLAGS) osx-arm64

release-macos-x64:
	$(SCRIPTS)/notarize-macos.sh $(NOTARIZE_FLAGS) osx-x64

release-macos: release-macos-arm64 release-macos-x64

# -- Linux / Windows distributable archives ------------------------------------

release-linux: publish-linux
	rm -f publish/linux-x64/*.pdb
	tar -czf publish/JagFx-$(VERSION)-linux-x64.tar.gz -C publish/linux-x64 .

release-windows: publish-windows
	rm -f publish/win-x64/*.pdb
	cd publish/win-x64 && zip -rq ../JagFx-$(VERSION)-win-x64.zip .
