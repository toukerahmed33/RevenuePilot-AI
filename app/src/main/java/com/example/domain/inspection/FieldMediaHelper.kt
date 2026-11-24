package com.example.domain.inspection

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object FieldMediaHelper {

    private const val PHOTOS_DIR_NAME = "inspection_photos"
    private const val MAX_PHOTO_BYTES = 15L * 1024 * 1024 // 15 MB

    /**
     * Copies a picked photo URI into app-private offline storage so it remains readable offline.
     * Cleans up partial output files if reading/copying fails or exceeds size limit.
     */
    fun copyPhotoToInternalStorage(context: Context, sourceUri: Uri): String? {
        val dir = File(context.filesDir, PHOTOS_DIR_NAME).apply { mkdirs() }
        val fileName = "insp_${System.currentTimeMillis()}.jpg"
        val destFile = File(dir, fileName)
        return try {
            val copied = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buf = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        total += read
                        if (total > MAX_PHOTO_BYTES) {
                            throw IllegalArgumentException("Image exceeds maximum allowed size")
                        }
                        output.write(buf, 0, read)
                    }
                    total > 0L
                }
            } ?: false

            if (copied) {
                Uri.fromFile(destFile).toString()
            } else {
                destFile.delete()
                null
            }
        } catch (_: Exception) {
            destFile.delete()
            null
        }
    }

    /**
     * Safely deletes an app-owned inspection photo file after verifying canonical path confinement.
     */
    fun deletePhotoFileIfInternal(context: Context, photoUri: String) {
        if (photoUri.isBlank()) return
        try {
            val parsed = Uri.parse(photoUri)
            if (parsed.scheme != "file") return
            val path = parsed.path ?: return
            val target = File(path)
            val ownedDir = File(context.filesDir, PHOTOS_DIR_NAME).canonicalPath
            if (target.canonicalPath.startsWith("$ownedDir${File.separator}") && target.exists()) {
                target.delete()
            }
        } catch (_: Exception) {
            // Best-effort cleanup
        }
    }

    /**
     * Removes all locally stored inspection photos inside app-private inspection_photos directory.
     */
    fun clearAllInspectionPhotos(context: Context) {
        try {
            val dir = File(context.filesDir, PHOTOS_DIR_NAME)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {
            // Best-effort cleanup
        }
    }

    /**
     * Fetches current foreground location using FusedLocationProviderClient with LocationManager fallback.
     * Caller MUST ensure ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION is granted before invoking.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        val fusedResult = try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            suspendCancellableCoroutine<Location?> { cont ->
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(loc)
                        } else {
                            client.lastLocation
                                .addOnSuccessListener { last -> cont.resume(last) }
                                .addOnFailureListener { cont.resume(null) }
                        }
                    }
                    .addOnFailureListener {
                        client.lastLocation
                            .addOnSuccessListener { last -> cont.resume(last) }
                            .addOnFailureListener { cont.resume(null) }
                    }
            }
        } catch (_: Exception) {
            null
        }

        if (fusedResult != null) return fusedResult

        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            null
        }
    }
}
