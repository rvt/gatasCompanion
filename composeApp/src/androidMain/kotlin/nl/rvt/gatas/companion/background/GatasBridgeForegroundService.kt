package nl.rvt.gatas.companion.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import nl.rvt.gatas.MainActivity
import nl.rvt.gatas.companion.services.BlueToothBleService
import nl.rvt.gatas.companion.services.BridgeStatus
import nl.rvt.gatas.companion.services.GatasUdpRelayService

class GatasBridgeForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var statusJob: Job? = null
    private var bridgeService: BlueToothBleService? = null
    private var currentIdentifier: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val startingStatus = BridgeStatus(
            running = true,
            connecting = true,
            lastEvent = "Bridge service starting..."
        )
        _status.value = startingStatus
        promoteToForeground(startingStatus)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val identifier = intent.getStringExtra(EXTRA_IDENTIFIER)
                if (identifier.isNullOrBlank()) {
                    Logger.w { "Foreground bridge start requested without an identifier" }
                    stopSelf()
                } else {
                    startBridge(identifier)
                }
            }

            ACTION_STOP -> stopSelf()
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        stopBridge()
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startBridge(identifier: String) {
        if (currentIdentifier == identifier && bridgeService != null) {
            return
        }

        stopBridge(updateNotification = false)
        currentIdentifier = identifier

        val relayService = GatasUdpRelayService()
        val service = BlueToothBleService(identifier, relayService)
        bridgeService = service

        statusJob = serviceScope.launch {
            service.status.collect { bridgeStatus ->
                _status.value = bridgeStatus
                updateNotification(bridgeStatus)
            }
        }

        promoteToForeground(
            BridgeStatus(
                running = true,
                connecting = true,
                lastEvent = "Connecting to GATAS..."
            )
        )

        service.start()
    }

    private fun stopBridge(updateNotification: Boolean = true) {
        statusJob?.cancel()
        statusJob = null

        bridgeService?.stop()
        bridgeService = null
        currentIdentifier = null
        _status.value = BridgeStatus()
        if (updateNotification) {
            updateNotification(
                BridgeStatus(
                    running = false,
                    lastEvent = "Bridge stopped"
                )
            )
        }
    }

    private fun promoteToForeground(status: BridgeStatus) {
        val notification = buildNotification(status)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(status: BridgeStatus) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun buildNotification(status: BridgeStatus): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentMutableFlag()
        )

        val text = status.lastEvent.ifBlank { "Bridge running" }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("GATAS Bridge")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(status.running || status.connecting)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun pendingIntentMutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    companion object {
        private const val ACTION_START = "nl.rvt.gatas.companion.background.action.START"
        private const val ACTION_STOP = "nl.rvt.gatas.companion.background.action.STOP"
        private const val EXTRA_IDENTIFIER = "extra_identifier"
        private const val CHANNEL_ID = "gatas_bridge"
        private const val CHANNEL_NAME = "GATAS Bridge"
        private const val NOTIFICATION_ID = 4242

        private val _status = MutableStateFlow(BridgeStatus())
        val status: StateFlow<BridgeStatus> = _status.asStateFlow()

        fun start(context: Context, identifier: String) {
            val intent = Intent(context, GatasBridgeForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_IDENTIFIER, identifier)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GatasBridgeForegroundService::class.java))
        }
    }
}
