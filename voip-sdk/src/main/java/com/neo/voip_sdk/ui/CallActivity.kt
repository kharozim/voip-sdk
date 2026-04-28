package com.neo.voip_sdk.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import cc.neo.sdkcall.ui.DialPad
import coil3.compose.AsyncImage
import com.neo.voip_sdk.CallState
import com.neo.voip_sdk.RegistrationState
import com.neo.voip_sdk.SipEngine
import com.neo.voip_sdk.VoipSdk
import com.neo.voip_sdk.icons.SpeakerBluetooth
import com.neo.voip_sdk.icons.SpeakerHeadphone
import com.neo.voip_sdk.service.NeoCallService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CallActivity : ComponentActivity() {

  private var callerName: String = ""
  private var callerAvatar: String = ""
  private var calleeName: String = ""
  private var calleeAvatar: String = ""
  private var callType: String = ""
  private var username: String = ""
  private var password: String = ""
  private var domain: String = ""
  private var destination: String = ""
  private val currentCallDurationSeconds = MutableStateFlow(0L)
  private val lastCallDurationSeconds = MutableStateFlow(0L)
  private var callService: NeoCallService? = null
  private var metaData: HashMap<String, String> = hashMapOf(
    "call_title" to "Free Call",
    "call_busy" to "The customer is busy and cannot be reached",
    "call_calling" to "Calling...",
    "call_connecting" to "Connecting...",
    "call_ringing" to "Ringing...",
    "call_refused" to "Decline",
    "call_end" to "End Call",
    "call_incoming" to "Incoming",
    "call_temporarily_unavailable" to "Currently unreachable",
    "call_lost_connection" to "Connection lost",
    "call_weak_signal" to "Weak Signal",
    "call_btn_message" to "Send Message",
    "call_btn_mute" to "Mute",
    "call_btn_speaker" to "Speaker",
  )
  private var bound = false
  private val callServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
      val serviceBinder = binder as? NeoCallService.LocalBinder ?: return
      callService = serviceBinder.getService()
      bound = true
      currentCallDurationSeconds.value = callService?.getCurrentDurationSeconds() ?: 0L
      lastCallDurationSeconds.value = callService?.getLastCallDurationSeconds() ?: 0L
      callService?.setOnTickerUpdateListener { seconds ->
        currentCallDurationSeconds.value = seconds
      }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
      callService?.setOnTickerUpdateListener(null)
      callService = null
      bound = false
    }
  }

  private fun buildCallServiceIntent(action: String): Intent {
    return Intent(this, NeoCallService::class.java).apply {
      this.action = action
      intent.extras?.let { putExtras(it) }
    }
  }

  private fun stopCallService() {
    val finalDuration = callService?.getLastCallDurationSeconds()
      ?.takeIf { it > 0 }
      ?: currentCallDurationSeconds.value
    lastCallDurationSeconds.value = finalDuration
    callService?.setOnTickerUpdateListener(null)
    stopService(buildCallServiceIntent(NeoCallService.Action.STOP))
    if (bound) {
      unbindService(callServiceConnection)
      bound = false
    }
    callService = null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    callerName = intent.getStringExtra("caller_name") ?: "Unknown"
    callerAvatar = intent.getStringExtra("caller_avatar").orEmpty()
    calleeName = intent.getStringExtra("callee_name") ?: "unknown"
    calleeAvatar = intent.getStringExtra("callee_avatar").orEmpty()
    callType = intent.getStringExtra("call_type") ?: "outgoing"
    username = intent.getStringExtra("username").orEmpty()
    password = intent.getStringExtra("password").orEmpty()
    domain = intent.getStringExtra("domain").orEmpty()
    destination = intent.getStringExtra("destination").orEmpty()

    val extraMeta = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getSerializableExtra("meta_data", HashMap::class.java) as? HashMap<String, String>
    } else {
      @Suppress("UNCHECKED_CAST")
      intent.getSerializableExtra("meta_data") as? HashMap<String, String>
    } ?: emptyMap()

    metaData.putAll(extraMeta)

    if (savedInstanceState == null) {
      VoipSdk.initialize(SipEngine.build(this))
      VoipSdk.register(username = username, password = password, domain = domain)
      Log.e("TAG", "cobacall : Initial Registration")

      lifecycleScope.launch {
        val serviceIntent = buildCallServiceIntent(NeoCallService.Action.OUTGOING).also {
          Log.d("SDK Call", "OUTGOING: start service")
          bindService(it, callServiceConnection, BIND_AUTO_CREATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          startForegroundService(serviceIntent)
        } else {
          startService(serviceIntent)
        }
      }
    } else {
      // Re-bind service if activity recreated
      val serviceIntent = buildCallServiceIntent(NeoCallService.Action.OUTGOING)
      bindService(serviceIntent, callServiceConnection, BIND_AUTO_CREATE)
    }

    enableEdgeToEdge()
    setContent {
      val timeTicker by currentCallDurationSeconds.collectAsState()
      val lastTicker by lastCallDurationSeconds.collectAsState()
      var isMicMuted by remember { mutableStateOf(false) }
      var speakerOutput by remember { mutableIntStateOf(0) }
      var callStatusRaw by remember { mutableStateOf("") }
      var showErrorDialog by remember { mutableStateOf(false) }
      var showSpeakerDialog by remember { mutableStateOf(false) }
      val registrationState by VoipSdk.observeRegistrationState().collectAsState()
      val callState by VoipSdk.observeCallState().collectAsState()
      var errorMessage by remember { mutableStateOf<String?>(null) }
      var loadingMessage by remember { mutableStateOf<String?>(null) }
      var showLoading by remember { mutableStateOf(false) }

      SetSystemBarAppearance(true)

      LaunchedEffect(registrationState) {
        when (val state = registrationState) {
          is RegistrationState.Failed -> {
            // Hanya tampilkan error jika tidak ada panggilan aktif
            if (callState == CallState.Idle) {
              errorMessage = "Registration failed: ${state.message}"
              showErrorDialog = true
            }
            showLoading = false
            Log.e("TAG", "cobacall : Registration Failed")
          }

          RegistrationState.Registered -> {
            if (callState == CallState.Idle) {
              val phoneId: String? = metaData["phone_id"]
              VoipSdk.startCall("sip:$destination@$domain", phoneId)
            }
            showLoading = false
            Log.e("TAG", "cobacall : registered")
          }

          RegistrationState.Progress, RegistrationState.Refreshing -> {
            // Hanya tampilkan loading jika belum dalam panggilan
            if (callState == CallState.Idle) {
              loadingMessage = "Registering..."
              showLoading = true
            } else {
              showLoading = false
            }
          }

          else -> showLoading = false
        }
      }

      LaunchedEffect(callState) {
        when (val state = callState) {
          CallState.Active -> {
            callStatusRaw = "active"
            lastCallDurationSeconds.value = 0L
            startService(buildCallServiceIntent(NeoCallService.Action.ONGOING))
          }

          CallState.Calling -> callStatusRaw = "calling"
          CallState.Connected -> callStatusRaw = "connect"
          CallState.Ringing -> callStatusRaw = "ringging"
          CallState.Hold -> callStatusRaw = "hold"
          CallState.Idle -> callStatusRaw = "idle"

          CallState.Disconnected -> {
            callStatusRaw = "disconnected"
            lastCallDurationSeconds.value =
              callService?.getLastCallDurationSeconds()?.takeIf { it > 0 } ?: timeTicker
            stopCallService()
            Log.e("TAG", "cobacall : Disconnected")
          }

          is CallState.Error -> {
            callStatusRaw = "error"
            lastCallDurationSeconds.value =
              callService?.getLastCallDurationSeconds()?.takeIf { it > 0 } ?: timeTicker
            errorMessage = "Call error ${state.reason}"
            showErrorDialog = true
            stopCallService()
            Log.e("TAG", "cobacall : Error")
          }
        }
      }

      val callTimerText = when {
        callStatusRaw == "active" -> formatElapsedTime(timeTicker)
        callStatusRaw in listOf("disconnected", "error") && lastTicker > 0L ->
          "Call duration ${formatElapsedTime(lastTicker)}"

        else -> (metaData["call_$callStatusRaw"] ?: callStatusRaw)
      }

      MaterialTheme {
        CallScreen(
          callerName = if (callType == "incoming") callerName else calleeName,
          callTimer = callTimerText,
          callStatusRaw = callStatusRaw,
          signalState = "",
          avatarUrl = if (callType == "incoming") callerAvatar else calleeAvatar,
          isMicMuted = isMicMuted,
          speakerOutput = speakerOutput,
          metaData = metaData,
          onMuteClick = {
            isMicMuted = !isMicMuted
            VoipSdk.toggleMute()
          },
          onSpeakerClick = { showSpeakerDialog = true },
          onNumpadClick = { /* TODO */ },
          onAnswerCallClick = { /* TODO */ },
          onEndCallClick = {
            if (callStatusRaw in listOf("disconnected", "error", "idle")) {
              finish()
            } else {
              VoipSdk.endCall()
            }
          })

        if (showLoading) {
          Loading(message = loadingMessage.orEmpty(), onDismissRequest = { showLoading = false })
        }

        if (showSpeakerDialog) {
          val listSpeaker = VoipSdk.getSpeakerOutput().filter { it in listOf(2, 3, 4, 9, 10) }
          Dialog(onDismissRequest = { showSpeakerDialog = false }) {
            Column(
              Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
            ) {
              listSpeaker.forEach {
                Text(
                  text = when (it) {
                    2 -> "Earpiece"
                    3 -> "Speaker"
                    4 -> "Bluetooth"
                    9, 10 -> "Headset"
                    else -> "Output $it"
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      showSpeakerDialog = false
                      VoipSdk.toggleSpeaker(it)
                      speakerOutput = it
                    }
                    .padding(16.dp)
                )
              }
            }
          }
        }

        if (showErrorDialog) {
          ErrorAlertDialog(
            onDismiss = {
              showErrorDialog = false
              VoipSdk.endCall()
              finish()
            },
            message = errorMessage
          )
        }
      }
    }
  }

  override fun onDestroy() {
    if (bound) {
      unbindService(callServiceConnection)
      bound = false
    }
    // Hanya panggil destroy jika activity benar-benar selesai (bukan konfigurasi berubah)
    if (isFinishing) {
      VoipSdk.destroy()
    }
    super.onDestroy()
  }

  @SuppressLint("DefaultLocale")
  private fun formatElapsedTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
  }
}

@Composable
fun CallScreen(
  callerName: String,
  callTimer: String,
  callStatusRaw: String,
  signalState: String,
  avatarUrl: String,
  isMicMuted: Boolean,
  speakerOutput: Int?,
  metaData: Map<String, String>,
  onMuteClick: () -> Unit,
  onSpeakerClick: () -> Unit,
  onNumpadClick: (it: String) -> Unit,
  onAnswerCallClick: () -> Unit,
  onEndCallClick: () -> Unit,
) {
  var showDialPad by rememberSaveable { mutableStateOf(false) }

  BackHandler(showDialPad) { showDialPad = false }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
  ) { padding ->
    Box {
      MultiLayerGradientBackground()
      Column(
        modifier = Modifier
          .padding(padding)
          .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Spacer(modifier = Modifier.height(20.dp))
          Text(
            text = metaData["call_title"] ?: "Free Call",
            style = MaterialTheme.typography.headlineSmall
          )
          Spacer(modifier = Modifier.height(60.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = callTimer, style = MaterialTheme.typography.bodyLarge)
          Spacer(modifier = Modifier.height(55.dp))
          CallAvatar(avatarUrl)
          Spacer(modifier = Modifier.height(35.dp))
          Text(text = callerName, style = MaterialTheme.typography.headlineSmall)
          if (signalState.isNotEmpty()) {
            Text(text = signalState, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
          }
        }

        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 15.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RoundIconButton(
            icon = when (speakerOutput) {
              4 -> SpeakerBluetooth
              9, 10 -> SpeakerHeadphone
              else -> Icons.AutoMirrored.Outlined.VolumeUp
            },
            label = metaData["call_btn_speaker"] ?: "Speaker",
            onClick = onSpeakerClick,
            backgroundColor = if (speakerOutput == 3) Color(0xFF00BABD) else Color(0xFFE9F8F9),
            iconTint = if (speakerOutput == 3) Color.White else Color(0xFF17666A)
          )

          RoundIconButton(
            icon = Icons.Filled.Dialpad,
            label = metaData["call_numpad"] ?: "Numpad",
            onClick = { showDialPad = true },
//            enabled = callStatusRaw == "active" || callStatusRaw == "connect"
            enabled = false
          )

          RoundIconButton(
            icon = Icons.Default.MicOff,
            label = metaData["call_btn_mute"] ?: "Mute",
            onClick = onMuteClick,
            backgroundColor = if (isMicMuted) Color(0xFF00BABD) else Color(0xFFE9F8F9),
            iconTint = if (isMicMuted) Color.White else Color(0xFF17666A),
            enabled = callStatusRaw == "active" || callStatusRaw == "connect"
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 55.dp),
          horizontalArrangement = Arrangement.Center
        ) {
          RoundIconButton(
            icon = Icons.Filled.Close,
            label = "",
            onClick = onEndCallClick,
            backgroundColor = Color.Red,
            iconTint = Color.White
          )
          if (callStatusRaw == "incoming") {
            Spacer(modifier = Modifier.width(60.dp))
            RoundIconButton(
              icon = Icons.Default.Phone,
              label = "",
              onClick = onAnswerCallClick,
              backgroundColor = Color.Green,
              iconTint = Color.White
            )
          }
        }
      }

      AnimatedVisibility(
        visible = showDialPad,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier
          .fillMaxWidth()
          .zIndex(2f)
          .align(Alignment.BottomCenter)
      ) {
        Surface(tonalElevation = 8.dp) {
          DialPad(onKeyPress = { if (it == "close") showDialPad = false else onNumpadClick(it) })
        }
      }
    }
  }
}

@Composable
fun MultiLayerGradientBackground() {
  Box(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(
          Brush.horizontalGradient(
            listOf(
              Color(0xFFFFF4DF),
              Color(0xFFFFFFFF),
              Color(0xFFDAFFFF)
            )
          )
        )
    )
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(Brush.verticalGradient(listOf(Color(0x00F6F6F6), Color(0xFFF6F6F6))))
    )
  }
}

@Composable
fun CallAvatar(imageUrl: String?) {
  Box(
    modifier = Modifier
      .size(160.dp)
      .clip(CircleShape)
      .background(Color.LightGray),
    contentAlignment = Alignment.Center
  ) {
    if (imageUrl.isNullOrBlank()) {
      Icon(
        Icons.Default.Person,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(80.dp)
      )
    } else {
      AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape)
          .border(2.dp, Color.Gray, CircleShape)
      )
    }
  }
}

@Composable
fun RoundIconButton(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  backgroundColor: Color = Color(0xFFE9F8F9),
  iconTint: Color = Color(0xFF17666A),
  enabled: Boolean = true,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .background(if (enabled) backgroundColor else backgroundColor.copy(0.4f))
        .clickable(enabled = enabled, onClick = onClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = label, tint = if (enabled) iconTint else iconTint.copy(0.4f))
    }
    if (label.isNotBlank()) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = if (enabled) Color.Gray else Color.Gray.copy(0.4f)
      )
    }
  }
}

@Composable
private fun Loading(message: String, onDismissRequest: () -> Unit) {
  Dialog(onDismissRequest = onDismissRequest) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .background(Color.White, RoundedCornerShape(12.dp))
        .padding(24.dp)
    ) {
      CircularProgressIndicator(modifier = Modifier.size(32.dp))
      Text(message, Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
  Loading(message = "Loading...") { }
}
