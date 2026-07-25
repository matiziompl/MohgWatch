package com.mohgwatch.wear.watchface

import android.service.wallpaper.WallpaperService

/**
 * Bazowa usługa powłoki dla tarcz zadeklarowanych w formacie WFF (Watch Face Format XML).
 * System Wear OS 4/5 odczytuje deklaratywny XML wskazany w meta-data.
 */
abstract class DeclarativeWatchFaceService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return object : Engine() {}
    }
}

class D1ClassicWatchFace : DeclarativeWatchFaceService()
class D2NeonPulseWatchFace : DeclarativeWatchFaceService()
class D3MinimalWhiteWatchFace : DeclarativeWatchFaceService()
class D4DashboardWatchFace : DeclarativeWatchFaceService()
class D5NightModeWatchFace : DeclarativeWatchFaceService()

class A1SwissHeritageWatchFace : DeclarativeWatchFaceService()
class A2DiverProWatchFace : DeclarativeWatchFaceService()
class A3ChronographWatchFace : DeclarativeWatchFaceService()
class A4FieldWatchWatchFace : DeclarativeWatchFaceService()
class A5MoonphaseWatchFace : DeclarativeWatchFaceService()

class H1TechFusionWatchFace : DeclarativeWatchFaceService()
class H2VitalsMonitorWatchFace : DeclarativeWatchFaceService()
class H3RetroDigitalWatchFace : DeclarativeWatchFaceService()
class H4SportTrackerWatchFace : DeclarativeWatchFaceService()
class H5AuroraWatchFace : DeclarativeWatchFaceService()
