@file:OptIn(ExperimentalFoundationApi::class)

package nl.rvt.gatas.companion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import nl.rvt.gatas.PlatformKeepScreenOnEffect
import nl.rvt.gatas.companion.background.BridgeBackgroundComponent
import nl.rvt.gatas.companion.services.BridgeStatus

@Composable
fun ConnectScreen(
    bridgeStatus: BridgeStatus,
    onBack: () -> Unit,
    onStopBridge: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var showAircraftDialog by remember { mutableStateOf(false) }
    val ownshipConfiguration = bridgeStatus.ownshipConfiguration
    var aircraftPickerEntries by remember(ownshipConfiguration?.icaoAddressList) {
        mutableStateOf<List<AircraftPickerEntry>>(emptyList())
    }
    var aircraftPickerLoading by remember(ownshipConfiguration?.icaoAddressList) {
        mutableStateOf(false)
    }
    var aircraftPickerError by remember(ownshipConfiguration?.icaoAddressList) {
        mutableStateOf<String?>(null)
    }

    PlatformKeepScreenOnEffect()

    LaunchedEffect(showAircraftDialog, ownshipConfiguration?.icaoAddressList) {
        if (!showAircraftDialog || ownshipConfiguration == null) {
            return@LaunchedEffect
        }

        aircraftPickerLoading = true
        aircraftPickerError = null
        aircraftPickerEntries = runCatching {
            AircraftLookupService.loadAircraftPickerEntries(ownshipConfiguration.icaoAddressList)
        }.getOrElse { error ->
            aircraftPickerError = error.message ?: "Unable to load aircraft list"
            emptyList()
        }
        aircraftPickerLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ConnectionInfoCard(
            gatasHexCode = bridgeStatus.ownshipConfiguration?.gatasId?.toHexId() ?: "--------",
            selectedAircraftCallsign = currentSelectedAircraftCallsign(
                ownshipConfiguration = bridgeStatus.ownshipConfiguration,
                aircraftPickerEntries = aircraftPickerEntries,
            ),
            onChangeAircraft = { showAircraftDialog = true },
            changeAircraftEnabled = ownshipConfiguration != null,
        )

        LinkStatusCard(
            leftLabel = "GATAS Server",
            rightLabel = "Phone",
            connected = bridgeStatus.udpHealthy,
            statusText = if (bridgeStatus.udpHealthy) {
                "Connected via UDP"
            } else {
                "Waiting for UDP"
            },
            nmeaPackets = bridgeStatus.udpNmeaPackets,
            cobsPackets = bridgeStatus.udpCobsPackets,
            nmeaTick = bridgeStatus.udpNmeaActivityTick,
            cobsTick = bridgeStatus.udpCobsActivityTick,
            totalPackets = bridgeStatus.udpPacketCount,
        )

        LinkStatusCard(
            leftLabel = "Phone",
            rightLabel = "GATAS",
            connected = bridgeStatus.bleConnected,
            statusText = when {
                bridgeStatus.bleConnected -> "Connected via Bluetooth"
                bridgeStatus.connecting -> "Connecting via Bluetooth"
                else -> "Waiting for Bluetooth"
            },
            nmeaPackets = bridgeStatus.bleNmeaPackets,
            cobsPackets = bridgeStatus.bleCobsPackets,
            nmeaTick = bridgeStatus.bleNmeaActivityTick,
            cobsTick = bridgeStatus.bleCobsActivityTick,
            totalPackets = bridgeStatus.blePacketCount,
        )

        if (bridgeStatus.gdl90BridgeEnabled) {
            Gdl90BridgeCard(status = bridgeStatus)
        }

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

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showAircraftDialog) {
        AircraftSelectionDialog(
            entries = aircraftPickerEntries,
            loading = aircraftPickerLoading,
            loadError = aircraftPickerError,
            selectedIcaoAddress = ownshipConfiguration?.icaoAddress,
            pendingIcaoAddress = bridgeStatus.aircraftChangeTargetIcaoAddress,
            onSelectAircraft = BridgeBackgroundComponent::requestAircraftChange,
            onDismiss = { showAircraftDialog = false },
        )
    }
}

private fun UInt.toHexId(): String = toString(16).uppercase().padStart(8, '0')
private fun Long.toIcaoHex(): String = toString(16).uppercase().padStart(6, '0')

private fun currentSelectedAircraftCallsign(
    ownshipConfiguration: nl.rvantwisk.gatas.lib.models.OwnshipAircraftConfiguration?,
    aircraftPickerEntries: List<AircraftPickerEntry>,
): String {
    val selectedIcaoAddress = ownshipConfiguration?.icaoAddress ?: return "------"
    return aircraftPickerEntries
        .firstOrNull { it.icaoAddress == selectedIcaoAddress }
        ?.registration
        ?.takeUnless { it.isBlank() || it == "-" }
        ?: selectedIcaoAddress.toIcaoHex()
}

@Composable
private fun ConnectionInfoCard(
    gatasHexCode: String,
    selectedAircraftCallsign: String,
    onChangeAircraft: () -> Unit,
    changeAircraftEnabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GATAS ID",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = gatasHexCode,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "callsign",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onChangeAircraft,
                    enabled = changeAircraftEnabled,
                ) {
                    Text(
                        text = selectedAircraftCallsign,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AircraftSelectionDialog(
    entries: List<AircraftPickerEntry>,
    loading: Boolean,
    loadError: String?,
    selectedIcaoAddress: Long?,
    pendingIcaoAddress: Long?,
    onSelectAircraft: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Change Aircraft",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    loadError != null && entries.isEmpty() -> {
                        Text(
                            text = loadError,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    entries.isEmpty() -> {
                        Text(
                            text = "No aircraft available.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 220.dp),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(entries) { entry ->
                                AircraftPickerCard(
                                    entry = entry,
                                    selected = selectedIcaoAddress == entry.icaoAddress,
                                    pending = pendingIcaoAddress == entry.icaoAddress,
                                    onClick = { onSelectAircraft(entry.icaoAddress) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AircraftPickerCard(
    entry: AircraftPickerEntry,
    selected: Boolean,
    pending: Boolean,
    onClick: () -> Unit,
) {
    val imageBitmap = rememberAircraftThumbnail(entry)
    val backgroundTint = if (selected) Color(0xFFE9FADF) else MaterialTheme.colorScheme.surface
    val borderColor = when {
        pending -> MaterialTheme.colorScheme.primary
        selected -> Color(0xFF0F9D2A)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundTint),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = entry.registration,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = 0.22f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundTint.copy(alpha = if (imageBitmap != null) 0.78f else 1f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (pending) animatedSelectionBarColor() else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = entry.registration,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.aircraftType,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.icaoHexCode,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (selected) {
                Text(
                    text = "✓",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun rememberAircraftThumbnail(entry: AircraftPickerEntry): ImageBitmap? {
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = entry.icaoHexCode, key2 = entry.imageUrl) {
        value = if (entry.imageUrl.isNullOrBlank()) {
            null
        } else {
            AircraftLookupService.loadThumbnail(entry.icaoHexCode, entry.imageUrl)
        }
    }
    return imageBitmap
}

@Composable
private fun animatedSelectionBarColor(): Color {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing)
        )
    )
    return MaterialTheme.colorScheme.primary.copy(alpha = alpha)
}

@Composable
private fun LinkStatusCard(
    leftLabel: String,
    rightLabel: String,
    connected: Boolean,
    statusText: String,
    nmeaPackets: Long,
    cobsPackets: Long,
    nmeaTick: Long,
    cobsTick: Long,
    totalPackets: Long,
) {
    val activeColor = if (connected) Color(0xFF57D48B) else Color(0xFFF44336)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ConnectionRoute(
                leftLabel = leftLabel,
                rightLabel = rightLabel,
                connected = connected,
                statusColor = activeColor,
                packetCount = totalPackets,
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = activeColor,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProtocolActivityIndicator(
                    label = "NMEA",
                    pulseTick = nmeaTick,
                    active = connected,
                    color = activeColor,
                )
                Spacer(modifier = Modifier.width(28.dp))
                ProtocolActivityIndicator(
                    label = "COBS",
                    pulseTick = cobsTick,
                    active = connected,
                    color = activeColor,
                    reverse = true,
                )
            }
        }
    }
}

@Composable
private fun Gdl90BridgeCard(status: BridgeStatus) {
    val active = status.gdl90FramesBridged > 0
    val textColor = if (active) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ConnectionRoute(
                leftLabel = "Phone",
                rightLabel = "localhost",
                connected = active,
                statusColor = textColor,
                packetCount = status.gdl90FramesBridged,
            )
            Text(
                text = if (active) "Connected via GDL90" else "Waiting for GDL90",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProtocolActivityIndicator(
                    label = "GDL90",
                    pulseTick = status.gdl90ActivityTick,
                    active = active,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun ConnectionRoute(
    leftLabel: String,
    rightLabel: String,
    connected: Boolean,
    statusColor: Color,
    packetCount: Long,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EndpointMarker(
                accentColor = statusColor,
                active = connected,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(statusColor, RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(26.dp)
                    .background(statusColor.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (connected) "✓" else "!",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(statusColor, RoundedCornerShape(999.dp))
            )
            EndpointMarker(
                accentColor = statusColor,
                active = connected,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = leftLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = packetCountLabel(packetCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = rightLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EndpointMarker(
    accentColor: Color,
    active: Boolean,
) {
    val bubbleColor = if (active) accentColor.copy(alpha = 0.18f) else accentColor.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .size(38.dp)
            .background(bubbleColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(accentColor, CircleShape)
        )
    }
}

@Composable
private fun ProtocolActivityIndicator(
    label: String,
    pulseTick: Long,
    active: Boolean,
    color: Color,
    reverse: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ActivityPropeller(
            pulseTick = pulseTick,
            color = color,
            active = active,
            reverse = reverse,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ProtocolActivityBadge(
    text: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}

@Composable
private fun ActivityPropeller(
    pulseTick: Long,
    color: Color,
    active: Boolean,
    reverse: Boolean = false,
    modifier: Modifier = Modifier,
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

    val bladeColor = color.copy(alpha = if (active) 0.35f else 0.16f)
    val hubColor = color.copy(alpha = if (active) 0.65f else 0.32f)

    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = rotation.value
        }
    ) {
        val bladeWidth = size.width * 0.18f
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
            color = hubColor.copy(alpha = 0.22f),
            radius = size.minDimension * 0.2f
        )
    }
}

private fun packetCountLabel(count: Long): String {
    return "$count ${if (count == 1L) "packet" else "packets"}"
}
