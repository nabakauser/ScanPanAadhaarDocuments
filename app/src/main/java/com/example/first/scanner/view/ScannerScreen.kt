package com.example.first.scanner.view

import android.Manifest
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.first.scannerUtils.CameraManager

@Composable
fun ScannerScreen() {
    val localContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(localContext) }
    val isCameraInitialised = remember { mutableStateOf(false) }
    val isPermissionGranted = remember { mutableStateOf(ContextCompat.checkSelfPermission(localContext, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val croppedImage = remember { mutableStateOf<Bitmap?>(null) }
    val borderColor = remember { mutableStateOf(Color.White) }

    val cameraManager = remember { CameraManager() }

    LaunchedEffect(localContext, lifecycleOwner) {
        if (!isCameraInitialised.value) {
            cameraManager.initialiseCameraComponents(localContext, lifecycleOwner, previewView)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraManager.startCamera(isCameraInitialisedState = isCameraInitialised)
                isPermissionGranted.value = true
            }
            else Toast.makeText(localContext, "Camera Permission is needed", Toast.LENGTH_SHORT).show()
        }
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, 48.dp),
        content = {
            Column(
                modifier = Modifier.padding(it)
            ) {
                if (isPermissionGranted.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                    ) {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            previewView = previewView
                        )
                        if(isCameraInitialised.value) {
                            DimmedOverlayAndOutline(
                                borderColor = borderColor.value
                            )
                        }
                    }
                }
                croppedImage.value?.let { image ->
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "Cropped Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .size(150.dp)
                    )
                }
            }
        },
        bottomBar = {
            ScannerBottomBar(
                isTakePictureVisible = isCameraInitialised.value,
                onStartCameraClicked = {
                    if (!isPermissionGranted.value) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    else cameraManager.startCamera(isCameraInitialisedState = isCameraInitialised)
                },
                onTakePictureClicked = {
                    cameraManager.captureImage(
                        croppedImage = croppedImage,
                        borderColor = borderColor
                    )
                }
            )
        }
    )
}