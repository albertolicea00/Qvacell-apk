# ==============================================================================
# Qvacell :: Version Management & Release Guide
# ==============================================================================
#
# Manual Release Workflow:
# ------------------------
# 1. Update version:
#      make bump-patch   # 1.0.0 -> 1.0.1
#      make bump-minor   # 1.0.0 -> 1.1.0
#      make bump-major   # 1.0.0 -> 2.0.0
#
# 2. Commit the version bump in git:
#      git commit -am "chore: bump version to v$(VERSION_NAME)"
#      git push origin main
#
# 3. Create and push the Git tag:
#      git tag -a "v$(VERSION_NAME)" -m "Release v$(VERSION_NAME)"
#      git push origin "v$(VERSION_NAME)"
#
# 4. Build release APKs (with keys/release.properties):
#      ./gradlew :app:assembleUnlockedRelease :app:assembleStoreRelease
#
#    Generated files are copied to:
#      app/build/outputs/apk/release/qvacell-v<VERSION>-release-unlocked.apk
#      app/build/outputs/apk/release/qvacell-v<VERSION>-release-store.apk
#
# 5. Publish Release on GitHub:
#    - Web UI:
#        Go to https://github.com/albertolicea00/Qvacell-apk/releases/new
#        Select the tag "v<VERSION>", add title and release notes,
#        drag & drop the 2 APKs from app/build/outputs/apk/release/ into the
#        assets box, then click "Publish release".
#
#    - CLI (alternative):
#        gh release create "v<VERSION>" \
#            app/build/outputs/apk/release/*.apk \
#            --title "v<VERSION> — Initial Release" \
#            --notes "..."
# ==============================================================================

GRADLE_FILE := app/build.gradle.kts

VERSION_NAME := $(shell grep 'versionName' $(GRADLE_FILE) | head -1 | sed 's/.*"\(.*\)".*/\1/')
VERSION_CODE := $(shell grep 'versionCode' $(GRADLE_FILE) | head -1 | sed 's/[^0-9]//g')

MAJOR := $(word 1,$(subst ., ,$(VERSION_NAME)))
MINOR := $(word 2,$(subst ., ,$(VERSION_NAME)))
PATCH := $(word 3,$(subst ., ,$(VERSION_NAME)))

.PHONY: help version bump-patch bump-minor bump-major

help: ## Show available commands
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

version: ## Show current version
	@echo "v$(VERSION_NAME) (code $(VERSION_CODE))"

bump-patch: ## Bump patch version (1.0.0 → 1.0.1)
	$(eval NEW_VERSION := $(MAJOR).$(MINOR).$(shell echo $$(($(PATCH)+1))))
	$(eval NEW_CODE := $(shell echo $$(($(VERSION_CODE)+1))))
	@sed -i '' 's/versionCode = $(VERSION_CODE)/versionCode = $(NEW_CODE)/' $(GRADLE_FILE)
	@sed -i '' 's/versionName = "$(VERSION_NAME)"/versionName = "$(NEW_VERSION)"/' $(GRADLE_FILE)
	@echo "$(VERSION_NAME) → $(NEW_VERSION) (code $(NEW_CODE))"

bump-minor: ## Bump minor version (1.0.x → 1.1.0)
	$(eval NEW_VERSION := $(MAJOR).$(shell echo $$(($(MINOR)+1))).0)
	$(eval NEW_CODE := $(shell echo $$(($(VERSION_CODE)+1))))
	@sed -i '' 's/versionCode = $(VERSION_CODE)/versionCode = $(NEW_CODE)/' $(GRADLE_FILE)
	@sed -i '' 's/versionName = "$(VERSION_NAME)"/versionName = "$(NEW_VERSION)"/' $(GRADLE_FILE)
	@echo "$(VERSION_NAME) → $(NEW_VERSION) (code $(NEW_CODE))"

bump-major: ## Bump major version (1.x.x → 2.0.0)
	$(eval NEW_VERSION := $(shell echo $$(($(MAJOR)+1))).0.0)
	$(eval NEW_CODE := $(shell echo $$(($(VERSION_CODE)+1))))
	@sed -i '' 's/versionCode = $(VERSION_CODE)/versionCode = $(NEW_CODE)/' $(GRADLE_FILE)
	@sed -i '' 's/versionName = "$(VERSION_NAME)"/versionName = "$(NEW_VERSION)"/' $(GRADLE_FILE)
	@echo "$(VERSION_NAME) → $(NEW_VERSION) (code $(NEW_CODE))"
