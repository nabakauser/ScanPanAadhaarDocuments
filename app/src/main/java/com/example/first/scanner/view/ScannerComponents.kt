package com.example.first.scanner.view

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(
    modifier: Modifier,
    previewView: PreviewView
) {
    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}

@Composable
fun DimmedOverlayAndOutline(
    borderColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val rectWidth = canvasWidth * 0.7f
        val rectHeight = canvasHeight * 0.3f

        val left = (canvasWidth - rectWidth) / 2
        val top = (canvasHeight - rectHeight) / 2

        drawRect(
            color = Color.Black.copy(alpha = 0.7f),
            size = androidx.compose.ui.geometry.Size(canvasWidth, canvasHeight)
        )

        drawRoundRect(
            color = Color.Transparent,
            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        drawRoundRect(
            color = borderColor,
            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            style = Stroke(width = 2.dp.toPx()),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )
    }
}

@Composable
fun ScannerBottomBar(
    isTakePictureVisible: Boolean,
    onStartCameraClicked: () -> Unit,
    onTakePictureClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Absolute.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            shape = CircleShape,
            onClick = {
                if(!isTakePictureVisible) onStartCameraClicked()
                else onTakePictureClicked()
            }
        ) {
            Text(
                if(!isTakePictureVisible) "Start Camera"
                else "Take Picture Manually"
            )
        }
        /*if(isTakePictureVisible) {
            Button(
                shape = CircleShape,
                onClick = {
                    onTakePictureClicked("Manual")
                }
            ) {
                Text("Take Picture Manually")
            }
        }*/
    }
}