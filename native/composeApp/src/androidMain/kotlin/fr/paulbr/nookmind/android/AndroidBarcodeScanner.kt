package fr.paulbr.nookmind.android

import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import fr.paulbr.nookmind.core.platform.BarcodeScanner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google's code scanner: Play services shows the camera screen itself, so the app needs no camera
 * permission and ships no scanning model. Only book barcodes (EAN-13, which ISBNs are) are read.
 */
class AndroidBarcodeScanner : BarcodeScanner {
    override val isAvailable: Boolean = true

    override suspend fun scan(): String? {
        val activity = ActivityTracker.current ?: return null
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_UPC_A)
            .enableAutoZoom()
            .build()
        val scanner = GmsBarcodeScanning.getClient(activity, options)
        return suspendCancellableCoroutine { continuation ->
            scanner.startScan()
                .addOnSuccessListener { barcode -> if (continuation.isActive) continuation.resume(barcode.rawValue) }
                .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
                .addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
        }
    }
}
