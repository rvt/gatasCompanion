package nl.rvt.gatas.companion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.rvt.gatas.PlatformKeepScreenOnEffect
import nl.rvt.gatas.companion.services.BridgeStatus

@Composable
fun ConnectScreen(
    bridgeStatus: BridgeStatus,
    onBack: () -> Unit,
    onStopBridge: () -> Unit,
) {
    val status = bridgeStatus

    val serverActive = status.udpHealthy
    val bleActive = status.bleConnected
    val serverColor = if (serverActive) Color(0xFF4CAF50) else Color(0xFFF44336)
    val bleColor = if (bleActive) Color(0xFF4CAF50) else Color(0xFFF44336)
    val availableStreams = status.availableStreams ?: "No stream detected"

    PlatformKeepScreenOnEffect()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "GATAS Bridge",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatusRow(
                    label = "gatasServer",
                    active = serverActive,
                    activeText = if (serverActive) "Online" else "Offline",
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(
                    label = "BLE",
                    active = bleActive,
                    activeText = if (bleActive) "Online" else "Offline",
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Available streams: $availableStreams",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowNode(
                    label = "gatasServer",
                    active = serverActive,
                    color = serverColor
                )

                FlowLink(
                    label = "gatasServer ↔ Mobile",
                    streamLabel = "NMEA / COBS",
                    arrow = "↓",
                    active = serverActive,
                    color = serverColor,
                    pulseTick = status.serverActivityTick,
                )

                FlowNode(
                    label = "Mobile",
                    active = true,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowLink(
                    label = "Mobile ↔ GATAS",
                    streamLabel = "NMEA / COBS",
                    arrow = "↓",
                    active = bleActive,
                    color = bleColor,
                    pulseTick = status.gatasActivityTick,
                )

                FlowNode(
                    label = "GATAS",
                    active = bleActive,
                    color = bleColor
                )
            }
        }

        if (status.gdl90BridgeEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Gdl90BridgeActivity(status = status)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onStopBridge()
                onBack()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF44336),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop bridge")
        }
    }
}

@Composable
private fun Gdl90BridgeActivity(status: BridgeStatus) {
    val active = status.gdl90FramesBridged > 0
    val textColor = if (active) {
        Color(0xFF2E7D32)
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ActivityPropeller(
            pulseTick = status.gdl90ActivityTick,
            color = textColor,
            active = active,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (active) {
                    "Receiving GDL90 and sending to localhost:4000"
                } else {
                    "Waiting for GDL90 messages to send to localhost:4000"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${status.gdl90FramesBridged} frames, ${status.gdl90BytesBridged} bytes forwarded",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        ActivityPropeller(
            pulseTick = status.gdl90ActivityTick,
            color = textColor,
            active = active,
            reverse = true,
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    active: Boolean,
    activeText: String,
) {
    val color = if (active) Color(0xFF4CAF50) else Color(0xFFF44336)

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = activeText,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FlowNode(
    label: String,
    active: Boolean,
    color: Color,
) {
    val backgroundColor = if (active) color.copy(alpha = 0.14f) else color.copy(alpha = 0.08f)
    val textColor = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier
                .weight(1f)
                .background(backgroundColor, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FlowLink(
    label: String,
    streamLabel: String,
    arrow: String,
    active: Boolean,
    color: Color,
    pulseTick: Long,
) {
    val textColor = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ActivityPropeller(
                pulseTick = pulseTick,
                color = textColor,
                active = active
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = arrow,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                color = textColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(8.dp))
            ActivityPropeller(
                pulseTick = pulseTick,
                color = textColor,
                active = active,
                reverse = true
            )
        }
        Text(
            text = streamLabel,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActivityPropeller(
    pulseTick: Long,
    color: Color,
    active: Boolean,
    reverse: Boolean = false,
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(pulseTick) {
        if (pulseTick == 0L) return@LaunchedEffect
        rotation.snapTo(0f)
        rotation.animateTo(
            targetValue = if (reverse) -360f else 360f,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
        )
    }

    val bladeColor = color.copy(alpha = if (active) 0.95f else 0.45f)
    val hubColor = color.copy(alpha = if (active) 1f else 0.55f)

    Canvas(
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                rotationZ = rotation.value
            }
    ) {
        val bladeWidth = size.width * 0.16f
        val bladeHeight = size.height * 0.34f
        val bladeTop = size.height * 0.08f
        val bladeLeft = (size.width - bladeWidth) / 2f

        repeat(3) { index ->
            rotate(index * 120f) {
                drawRoundRect(
                    color = bladeColor,
                    topLeft = Offset(bladeLeft, bladeTop),
                    size = Size(bladeWidth, bladeHeight),
                    cornerRadius = CornerRadius(bladeWidth, bladeWidth)
                )
            }
        }

        drawCircle(
            color = hubColor,
            radius = size.minDimension * 0.12f
        )
        drawCircle(
            color = hubColor.copy(alpha = 0.24f),
            radius = size.minDimension * 0.2f
        )
    }
}
