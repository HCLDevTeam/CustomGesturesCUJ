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
import androidx.compose.material3.Text
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
fun GreetingPreview() {
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
//                    offset /= zoom
                })
            }
            .graphicsLayer {
                translationX = -offset.x * zoom
                translationY = -offset.y * zoom
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = zoom; scaleY = zoom
            })
}
fun Offset.calculateDoubleTapOffset(newZoom: Float,
                                    previousZoom: Float, size: IntSize, tapOffset: Offset
): Offset {

    // 1. Calculate the change in offset due to the zoom.
    //    - tapOffset / previousZoom: The tap position relative to the content at the previous zoom.
    //    - tapOffset / newZoom: The tap position relative to the content at the new zoom.
    //    - The difference between these two is the change in offset needed to keep the tap position
    //      under the same point in the content.
    val zoomOffsetChange = (tapOffset / previousZoom) - (tapOffset / newZoom)

    // 2. Calculate the new offset by adding the change to the current offset.
    val newOffset = this  + zoomOffsetChange

    // 3. Calculate the visible area of the content at the new zoom level.
    val visibleWidth = size.width / newZoom
    val visibleHeight = size.height / newZoom

    // 4. Calculate the maximum allowed offset in each direction.
    //    - If the visible area is larger than the content, the max offset is 0 (no scrolling needed).
    //    - Otherwise, it's the difference between the content size and the visible area.
    val maxOffsetX = max(0f, size.width - visibleWidth)
    val maxOffsetY = max(0f, size.height - visibleHeight)

    // 5. Ensure the new offset stays within the allowed bounds.
    return Offset(newOffset.x.coerceIn(0f, maxOffsetX), newOffset.y.coerceIn(0f, maxOffsetY))
}

fun Offset.calculatePinchDragOffset(
    centroid: Offset, pan: Offset, oldZoom: Float, newZoom: Float, size: IntSize
): Offset {
    //calculate zoom change
    // The centroid is the point of focus for the zoom. We use the difference between the centroid's
    // position at the old zoom and the new zoom to calculate how the offset should change.
    val zoomOffset = centroid / oldZoom - centroid / newZoom

    // Calculate the pan change relative to the old zoom.
    // pan is the change in the gesture's position, so we need to adjust it by the old zoom to
    // figure out how much it moved relative to the content.
    val panOffset = pan / oldZoom

    // Calculate the new offset by applying the zoom and pan changes to the current offset.
    // This is the new position of the top-left corner of the visible content.
    val newOffset = this + zoomOffset - panOffset

    // Calculate the maximum allowed offset in each direction.
    // It's the difference between the content size and the visible area.
    val maxOffsetX = (size.width / oldZoom) * (oldZoom - 1f)
    val maxOffsetY = (size.height / oldZoom) * (oldZoom - 1f)
    return Offset(
        newOffset.x.coerceIn(0f, maxOffsetX), newOffset.y.coerceIn(0f, maxOffsetY)
    )
}