VERSION := `sed -n 's/^version=//p' gradle.properties`

# Build the desktop installer for the host OS into publish/ (jpackage cannot cross-build).
release-desktop:
    ./gradlew :desktopApp:packageReleaseDistributionForCurrentOS
    mkdir -p publish
    find desktopApp/build/compose/binaries/main-release \( -name '*.dmg' -o -name '*.msi' -o -name '*.deb' \) -exec cp {} publish/ \;

# Build the CLI zip, which runs anywhere with Java 21 or later, into publish/.
release-cli:
    ./gradlew :cli:distZip
    mkdir -p publish
    cp cli/build/distributions/jagfx-{{ VERSION }}.zip publish/jagfx-cli-{{ VERSION }}.zip

# Build the desktop installer and the CLI zip.
release-all: release-desktop release-cli
