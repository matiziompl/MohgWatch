# Fix errors in settings.gradle.kts

The `settings.gradle.kts` file has several compilation errors because it uses the wrong block name for dependency resolution management.

## Proposed Changes

### [Component Name]

#### [MODIFY] [settings.gradle.kts](file:///C:/Users/polas/OneDrive/Dokumenty/Antigravity%20projects/MohgWatch/settings.gradle.kts)
- Rename `dependencyResolution` block to `dependencyResolutionManagement`.
- Add `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` to ensure repositories are defined centrally.

## Verification Plan

### Automated Tests
- Run `gradle_sync` to verify that the compilation errors are resolved.
