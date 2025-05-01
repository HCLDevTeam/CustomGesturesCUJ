package com.example.customgesturesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.example.customgesturesapp.ui.theme.CustomGesturesAppTheme
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CustomGesturesAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CustomGestures(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomGestures(modifier: Modifier = Modifier) {
    ImageWithGestures(modifier)
}

@Preview(showBackground = true)
@Composable
fun CustomGesturesPreview() {
    CustomGestures()
}
@Composable
fun ImageWithGestures(modifier: Modifier = Modifier) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }

    Image(painter = painterResource(id = R.drawable.sample_image_dog),
        contentDescription = stringResource(R.string.sample_content_description),
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .clipToBounds()
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures(onGesture = { centroid, pan, gestureZoom, _ ->
                    val newZoom = (zoom * gestureZoom).coerceAtLeast(1f)
                    offset = offset.calculatePinchDragOffset(centroid, pan, zoom, newZoom, size)
                    zoom = newZoom

                })
            }

            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tapOffset ->

                    val newZoom = if (zoom > 1f) 1f else 2f
                    offset = offset.calculateDoubleTapOffset(newZoom, zoom, size, tapOffset)
                    zoom = newZoom
                })
            }
            .graphicsLayer {
                translationX = -offset.x * zoom
                translationY = -offset.y * zoom
                scaleX = zoom; scaleY = zoom
                transformOrigin = TransformOrigin(0f, 0f)
            })
}

// Calculates double-tap gesture offset
fun Offset.calculateDoubleTapOffset(newZoom: Float,
                                    previousZoom: Float, size: IntSize, tapOffset: Offset
): Offset {
    // Tracks tap and zoom offsets relative to the point of each transformation
    val zoomOffsetChange = (tapOffset / previousZoom) - (tapOffset / newZoom)
    val newOffset = this  + zoomOffsetChange

    // Accumulates current offset with change
    val visibleWidth = size.width / newZoom
    val visibleHeight = size.height / newZoom

    // Calculates maxOffset to keep the transformed image within visible bounds
    val maxOffsetX = max(0f, size.width - visibleWidth)
    val maxOffsetY = max(0f, size.height - visibleHeight)

    return Offset(newOffset.x.coerceIn(0f, maxOffsetX), newOffset.y.coerceIn(0f, maxOffsetY))
}

// Combines the calculation of drag-to-pan and pinch-to-zoom offsets
fun Offset.calculatePinchDragOffset(
    centroid: Offset, pan: Offset, oldZoom: Float, newZoom: Float, size: IntSize
): Offset {
    // Calculates multiple gesture offsets
    val zoomOffset = centroid / oldZoom - centroid / newZoom
    val panOffset = pan / oldZoom

    // Applies zoom and pan changes to new offset
    val newOffset = this + zoomOffset - panOffset

    // Calculates maxOffset to keep the transformed image within visible bounds
    val maxOffsetX = (size.width / oldZoom) * (oldZoom - 1f)
    val maxOffsetY = (size.height / oldZoom) * (oldZoom - 1f)

    return Offset(
        newOffset.x.coerceIn(0f, maxOffsetX), newOffset.y.coerceIn(0f, maxOffsetY)
    )
}
