package com.mohgwatch.wear.tile

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TileImageCacheTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun changedRenderedImageGetsANewVersionAndOldRequestsKeepTheirImage() {
        val cache = TileImageCache(folder.newFolder())
        val yesterday = byteArrayOf(1, 2, 3)
        val today = byteArrayOf(4, 5, 6)
        val first = cache.put(yesterday)
        val second = cache.put(today)
        assertNotEquals("A new day or theme changes the image version", first, second)
        assertArrayEquals(yesterday, cache.read(first))
        assertArrayEquals(today, cache.read(second))
        assertEquals(second, cache.put(today))
    }

    @Test fun imageVersionsSurviveServiceRecreation() {
        val directory = folder.newFolder()
        val version = TileImageCache(directory).put(byteArrayOf(7, 8))
        assertArrayEquals(byteArrayOf(7, 8), TileImageCache(directory).read(version))
    }

    @Test fun boundedCacheKeepsTheNewestRequestAndRejectsPathTraversal() {
        val directory = folder.newFolder()
        val cache = TileImageCache(directory, maxVersions = 2)
        var latest = ""
        repeat(5) { latest = cache.put(byteArrayOf(it.toByte())) }
        assertEquals(2, directory.listFiles()?.count { it.extension == "png" })
        assertArrayEquals(byteArrayOf(4), cache.read(latest))
        assertNull(cache.read("../outside"))
        assertNull(cache.read("1"))
        assertNull(cache.read(""))
        assertNull(cache.read("0".repeat(64)))
    }
}
