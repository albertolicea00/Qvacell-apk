GRADLE_FILE := app/build.gradle.kts

VERSION_NAME := $(shell grep 'versionName' $(GRADLE_FILE) | head -1 | sed 's/.*"\(.*\)".*/\1/')
VERSION_CODE := $(shell grep 'versionCode' $(GRADLE_FILE) | head -1 | sed 's/[^0-9]//g')

MAJOR := $(word 1,$(subst ., ,$(VERSION_NAME)))
MINOR := $(word 2,$(subst ., ,$(VERSION_NAME)))
PATCH := $(word 3,$(subst ., ,$(VERSION_NAME)))

.PHONY: help version bump-patch bump-minor bump-major release

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

release: ## Create GitHub release from current version
	$(eval CURRENT_VERSION := $(shell grep 'versionName' $(GRADLE_FILE) | head -1 | sed 's/.*"\(.*\)".*/\1/'))
	@if [ -n "$$(git status --porcelain)" ]; then \
		echo "Error: working tree dirty. Commit changes first."; \
		exit 1; \
	fi
	git tag -a "v$(CURRENT_VERSION)" -m "Release v$(CURRENT_VERSION)"
	git push origin "v$(CURRENT_VERSION)"
	gh release create "v$(CURRENT_VERSION)" \
		--title "v$(CURRENT_VERSION)" \
		--generate-notes
	@echo "Released v$(CURRENT_VERSION)"
