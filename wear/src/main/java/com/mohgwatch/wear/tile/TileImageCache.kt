package com.mohgwatch.wear.tile

import java.io.File
import java.security.MessageDigest

/** Immutable image versions let in-flight Wear resource requests receive their matching bitmap. */
internal class TileImageCache(private val directory: File, private val maxVersions: Int = 4) {
    @Synchronized
    fun put(bytes: ByteArray): String {
        val version = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        directory.mkdirs()
        val destination = File(directory, "$version.png")
        if (!destination.exists()) {
            val temporary = File.createTempFile("tile", ".tmp", directory)
            try {
                temporary.writeBytes(bytes)
                check(temporary.renameTo(destination))
            } finally {
                temporary.delete()
            }
        }
        destination.setLastModified(System.currentTimeMillis())
        directory.listFiles()?.filter { it.extension == "png" && it != destination }
            ?.sortedByDescending { it.lastModified() }?.drop((maxVersions - 1).coerceAtLeast(0))
            ?.forEach { it.delete() }
        return version
    }

    @Synchronized
    fun read(version: String): ByteArray? {
        if (!version.matches(Regex("[0-9a-f]{64}"))) return null
        return File(directory, "$version.png").takeIf { it.isFile }?.readBytes()
    }
}
