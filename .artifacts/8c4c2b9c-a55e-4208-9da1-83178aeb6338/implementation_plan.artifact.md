# Implementation Plan - Fix Missing Launcher Icon

The build is failing because the `mipmap/ic_launcher` resource is missing but referenced in `AndroidManifest.xml` of both `:app` and `:wear` modules. A summary of these errors has been written to [errors.md](file:///C:/Users/polas/OneDrive/Dokumenty/Antigravity projects/MohgWatch/errors.md).

## User Review Required
> [!IMPORTANT]
> This will add placeholder vector-based launcher icons. You should replace these with your actual branding assets later.

## Proposed Changes

### [app module]

#### [NEW] [ic_launcher_background.xml](file:///C:/Users/polas/OneDrive/Dokumenty/Antigravity projects/MohgWatch/app/src/main/res/drawable/ic_launcher_background.xml)
A simple solid color background for the adaptive icon.

#### [NEW] [ic_launcher_foreground.xml](file:///C:/Users/polas/OneDrive/Dokumenty/Antigravity projects/MohgWatch/app/src/main/res/drawable/ic_launcher_foreground.xml)
A simple vector foreground (blood drop shape placeholder).

#### [NEW] [ic_launcher.xml](file:///C:/Users/polas/OneDrive/Dokumenty/Antigravity projects/MohgWatch/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml)
The adaptive icon definition for API 26+.

#### [NEW] [ic_launcher.xml](file:///C:/Users/polas/OneDrive/Dokumenty/Antigravity projects/MohgWatch/app/src/main/res/mipmap/ic_launcher.xml)
A legacy vector-based launcher icon for API < 26.

### [wear module]

#### [NEW] [ic_launcher_background.xml](file:///C:/Users/polas/OneDrive/Dokumenty/Antigravity projects/MohgWatch/wear/src/main/res/drawable/ic_launcher_background.xml)
#### [NEW] [ic_launcher_foreground.xml](file:///C:/Users/polas/OneDrive/Dokumenty/Antigravity projects/MohgWatch/wear/src/main/res/drawable/ic_launcher_foreground.xml)
#### [NEW] [ic_launcher.xml](file:///C:/Users/polas/OneDrive/Dokumenty/Antigravity projects/MohgWatch/wear/src/main/res/mipmap-anydpi-v26/ic_launcher.xml)
#### [NEW] [ic_launcher.xml](file:///C:/Users/polas/OneDrive/Dokumenty/Antigravity projects/MohgWatch/wear/src/main/res/mipmap/ic_launcher.xml)

## Verification Plan

### Automated Tests
- Run `./gradlew :app:processDebugResources` to verify the error is gone.
- Run `./gradlew :wear:processDebugResources` to verify the wear module is also fixed.
