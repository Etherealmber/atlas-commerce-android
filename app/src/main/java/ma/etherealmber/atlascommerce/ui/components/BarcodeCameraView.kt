package ma.etherealmber.atlascommerce.ui.components

import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

enum class CameraVisualStyle {
    /** Wide rectangular warehouse 1D/EAN/Code-128 reticle for product & stock operations. */
    STOCK_BARCODE,

    /** High-contrast indigo/cyan 1:1 security QR reticle strictly for pairing & authenticating PCs. */
    QR_DEVICE_SECURITY,
}

/**
 * Lifecycle- and tab-strict CameraX barcode scanner.
 *
 * Guarantee: whenever [active] becomes `false` (switching away from the scanner tab, opening
 * any overlay/dialog, disabling camera preview, or backgrounding the app), this composable
 * **immediately** removes `PreviewView`, detaches the surface provider, clears the analyzer,
 * calls `cameraProvider.unbindAll()`, closes ML Kit's native `BarcodeScanner`, and shuts down
 * the analyzer executor so the Android green camera privacy indicator extinguishes at once.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun BarcodeCameraView(
    active: Boolean,
    style: CameraVisualStyle,
    onDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isLifecycleResumed = when (event) {
                Lifecycle.Event.ON_RESUME -> true
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> false
                else -> lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldOpenHardware = active && isLifecycleResumed
    val latestCallback by rememberUpdatedState(onDetected)

    val containerShape = if (style == CameraVisualStyle.QR_DEVICE_SECURITY) {
        RoundedCornerShape(24.dp)
    } else {
        RoundedCornerShape(16.dp)
    }

    if (!shouldOpenHardware) {
        Box(
            modifier = modifier
                .clip(containerShape)
                .background(
                    if (style == CameraVisualStyle.QR_DEVICE_SECURITY) {
                        Color(0xFF0B1026)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = atlasText("Caméra en veille"),
                style = MaterialTheme.typography.bodySmall,
                color = if (style == CameraVisualStyle.QR_DEVICE_SECURITY) {
                    Color(0xFF94A3B8)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        return
    }

    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(style, lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val options = when (style) {
            CameraVisualStyle.QR_DEVICE_SECURITY -> BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()

            CameraVisualStyle.STOCK_BARCODE -> BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_CODABAR,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_QR_CODE,
                )
                .build()
        }
        val scanner = BarcodeScanning.getClient(options)
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        var disposed = false
        var boundProvider: ProcessCameraProvider? = null
        var lastEmitValue = ""
        var lastEmitAt = 0L

        analysis.setAnalyzer(executor) { proxy: ImageProxy ->
            if (disposed) {
                proxy.close()
                return@setAnalyzer
            }
            val media = proxy.image
            if (media == null) {
                proxy.close()
                return@setAnalyzer
            }
            val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
            scanner.process(img)
                .addOnSuccessListener { codes ->
                    if (disposed) return@addOnSuccessListener
                    val value = codes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                    if (!value.isNullOrBlank()) {
                        val now = System.currentTimeMillis()
                        if (value != lastEmitValue || now - lastEmitAt > 2_000L) {
                            lastEmitValue = value
                            lastEmitAt = now
                            latestCallback(value)
                        }
                    }
                }
                .addOnCompleteListener { proxy.close() }
        }

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                runCatching {
                    val provider = future.get()
                    boundProvider = provider
                    provider.unbindAll()
                    if (!disposed) {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            disposed = true
            runCatching { preview.surfaceProvider = null }
            runCatching { analysis.clearAnalyzer() }
            runCatching {
                val provider = boundProvider ?: if (future.isDone) future.get() else null
                provider?.unbindAll()
            }
            runCatching { scanner.close() }
            runCatching { executor.shutdownNow() }
        }
    }

    Box(
        modifier = modifier
            .clip(containerShape)
            .border(
                width = if (style == CameraVisualStyle.QR_DEVICE_SECURITY) 2.dp else 1.dp,
                brush = if (style == CameraVisualStyle.QR_DEVICE_SECURITY) {
                    Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFF59E0B)))
                } else {
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                },
                shape = containerShape,
            ),
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        if (style == CameraVisualStyle.QR_DEVICE_SECURITY) {
            QrSecurityReticleOverlay(Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xCC0F172A))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = atlasText("QR SÉCURITÉ POSTE PC UNIQUEMENT"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                )
            }
        } else {
            StockBarcodeReticleOverlay(Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xAA0F172A))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = atlasText("Alignez le code-barres produit dans le cadre"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun StockBarcodeReticleOverlay(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val boxW = size.width * 0.78f
        val boxH = size.height * 0.46f
        val left = (size.width - boxW) / 2f
        val top = (size.height - boxH) / 2f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.72f),
            topLeft = Offset(left, top),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = 3.5f),
        )
        // Horizontal laser guide characteristic of 1D warehouse barcode scanners
        drawLine(
            color = accent.copy(alpha = 0.85f),
            start = Offset(left + 20f, size.height / 2f),
            end = Offset(left + boxW - 20f, size.height / 2f),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun QrSecurityReticleOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val side = minOf(size.width, size.height) * 0.64f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val corner = side * 0.18f
        val stroke = 7f
        val cyan = Color(0xFF38BDF8)
        val saffron = Color(0xFFF59E0B)

        // Subtle square frame
        drawRoundRect(
            color = Color(0xFF818CF8).copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(28f, 28f),
            style = Stroke(width = 2f),
        )

        // 4 glowing L-brackets unmistakably distinct from horizontal 1D barcode laser
        // Top-left
        drawLine(cyan, Offset(left, top + corner), Offset(left, top), stroke, StrokeCap.Round)
        drawLine(cyan, Offset(left, top), Offset(left + corner, top), stroke, StrokeCap.Round)
        // Top-right
        drawLine(cyan, Offset(left + side - corner, top), Offset(left + side, top), stroke, StrokeCap.Round)
        drawLine(cyan, Offset(left + side, top), Offset(left + side, top + corner), stroke, StrokeCap.Round)
        // Bottom-left
        drawLine(saffron, Offset(left, top + side - corner), Offset(left, top + side), stroke, StrokeCap.Round)
        drawLine(saffron, Offset(left, top + side), Offset(left + corner, top + side), stroke, StrokeCap.Round)
        // Bottom-right
        drawLine(saffron, Offset(left + side - corner, top + side), Offset(left + side, top + side), stroke, StrokeCap.Round)
        drawLine(saffron, Offset(left + side, top + side - corner), Offset(left + side, top + side), stroke, StrokeCap.Round)
    }
}
