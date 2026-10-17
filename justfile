# JagFx build and release targets. Every target wraps Gradle, which needs JDK 21.
#
# Development
#   just run             # desktop editor
#   just build
#   just test
#   just format
#   just format-check
#   just js              # Kotlin/JS + Kotlin/Wasm npm packages
#
# Release artifacts (jpackage builds only for the host OS and architecture)
#   just release-desktop # .dmg on macOS, .msi on Windows, .deb on Linux
#   just release-cli     # jagfx-cli-<version>.zip, runs anywhere with Java 21+
#   just release-all

VERSION := `sed -n 's/^version=//p' gradle.properties`

run:
	./gradlew :desktopApp:run

build:
	./gradlew assemble

test:
	./gradlew check

format:
	./gradlew spotlessApply

format-check:
	./gradlew spotlessCheck detekt

js:
	./gradlew :bindings:syncNpmPackages
	bun run build:js
	bun run test:js

clean:
	./gradlew clean
	rm -rf publish

release-desktop:
	./gradlew :desktopApp:packageReleaseDistributionForCurrentOS
	mkdir -p publish
	find desktopApp/build/compose/binaries/main-release \( -name '*.dmg' -o -name '*.msi' -o -name '*.deb' \) -exec cp {} publish/ \;

release-cli:
	./gradlew :cli:distZip
	mkdir -p publish
	cp cli/build/distributions/jagfx-{{VERSION}}.zip publish/jagfx-cli-{{VERSION}}.zip

release-all: release-desktop release-cli
