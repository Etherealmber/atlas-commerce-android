package ma.etherealmber.atlascommerce.ui.screens.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import java.util.concurrent.Executors

/**
 * Barcode scanner for field operations.
 *
 * CameraX + ML Kit (bundled model, works offline). A manual code field is always
 * available: emulators, damaged labels and camera-less tablets still work.
 */
@Composable
fun ScanScreen(
    onOpenSale: () -> Unit,
    onOpenProducts: () -> Unit,
    onToast: (String) -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        granted = allowed
        askedOnce = true
        if (!allowed) onToast("Caméra refusée — utilisez la saisie manuelle.")
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> onToast(message) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
        ) {
            if (granted) {
                CameraPreview(
                    onBarcode = viewModel::onBarcode,
                    modifier = Modifier.fillMaxSize(),
                )
                ScannerFrame()
            } else {
                CameraPermissionPane(
                    askedOnce = askedOnce,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        context.startActivity(intent)
                    },
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Atlas.density.screenPadding, vertical = 12.dp),
            ) {
                ScanResultPanel(
                    state = state,
                    onAdd = { viewModel.addToCart() },
                    onOpenSale = onOpenSale,
                    onOpenProducts = onOpenProducts,
                    onRetry = viewModel::retry,
                )
                Spacer(Modifier.height(10.dp))
                ManualCodeField(onSubmit = viewModel::onBarcode)
            }
        }
    }
}

@Composable
private fun ScanResultPanel(
    state: ScanViewModel.UiState,
    onAdd: () -> Unit,
    onOpenSale: () -> Unit,
    onOpenProducts: () -> Unit,
    onRetry: () -> Unit,
) {
    val product = state.product
    when {
        state.lookingUp -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Recherche de ${state.lastCode}…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        product != null -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${AtlasFormat.money(product.price)} · Stock ${AtlasFormat.quantity(product.stock)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.added) {
                    AtlasChip(text = "Ajouté", tone = AtlasTone.POSITIVE, icon = Icons.Outlined.CheckCircle)
                } else {
                    AtlasChip(
                        text = if (product.isLowStock) "Stock faible" else "En stock",
                        tone = if (product.isLowStock) AtlasTone.WARNING else AtlasTone.POSITIVE,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtlasButton(
                    text = if (state.added) "Ajouter encore" else "Ajouter au panier",
                    onClick = onAdd,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.ShoppingCart,
                )
                TextButton(onClick = onOpenSale) { Text("Voir le panier") }
            }
        }

        state.notFound -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = Atlas.palette.warning,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Code ${state.lastCode} inconnu",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Aucun produit ne correspond. Créez-le depuis le web ou scannez un autre article.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRetry) { Text("Réessayer") }
                TextButton(onClick = onOpenProducts) { Text("Voir le catalogue") }
            }
        }

        state.error != null -> AtlasErrorCard(
            title = "Recherche impossible",
            message = state.error.orEmpty(),
            onRetry = onRetry,
        )

        else -> {
            Text(
                text = "Scannez un code-barres",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Le produit est ajouté au panier de vente rapide. La recherche d'un code " +
                    "nécessite le serveur ; sans réseau, saisissez le code depuis l'onglet Produits.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ManualCodeField(onSubmit: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AtlasTextField(
            value = code,
            onValueChange = { code = it },
            label = "Saisie manuelle",
            placeholder = "Code-barres / SKU",
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search,
            onDone = {
                if (code.isNotBlank()) onSubmit(code)
            },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        TextButton(
            onClick = { if (code.isNotBlank()) onSubmit(code) },
            enabled = code.isNotBlank(),
        ) {
            Text("Chercher")
        }
    }
}

@Composable
private fun ScannerFrame() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameWidth = size.minDimension * 0.66f
        val frameHeight = frameWidth * 0.62f
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun CameraPermissionPane(
    askedOnce: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Accès caméra requis",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "La caméra sert uniquement à lire les codes-barres. Aucune image n'est envoyée " +
                "ni conservée : la lecture se fait sur l'appareil.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(14.dp))
        AtlasButton(text = "Autoriser la caméra", onClick = onRequest)
        if (askedOnce) {
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onOpenSettings) {
                Text("Ouvrir les réglages", color = Color.White)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Vous pouvez aussi saisir le code manuellement ci-dessous.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(
    onBarcode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var analysis: ImageAnalysis? = null
        var disposed = false

        providerFuture.addListener(
            {
                // The user may have left the tab before the provider resolved;
                // binding then would keep the camera open forever.
                if (disposed) return@addListener
                runCatching {
                    val cameraProvider = providerFuture.get()
                    provider = cameraProvider
                    val preview = Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analyzer ->
                            analyzer.setAnalyzer(cameraExecutor) { proxy ->
                                analyzeFrame(proxy, scanner, onBarcode)
                            }
                        }
                    analysis = imageAnalysis
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            disposed = true
            runCatching { provider?.unbindAll() }
            runCatching { analysis?.clearAnalyzer() }
            runCatching { scanner.close() }
            cameraExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeFrame(
    proxy: ImageProxy,
    scanner: BarcodeScanner,
    onBarcode: (String) -> Unit,
) {
    val mediaImage = proxy.image
    if (mediaImage == null) {
        proxy.close()
        return
    }
    val image = runCatching {
        InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
    }.getOrNull()
    if (image == null) {
        proxy.close()
        return
    }
    // The frame must always be released, even if ML Kit refuses the image.
    runCatching {
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.let(onBarcode)
            }
            .addOnCompleteListener { proxy.close() }
    }.onFailure { proxy.close() }
}
