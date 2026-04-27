package com.neo.voip_sdk.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
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
import kotlin.collections.get

class CallActivity : ComponentActivity() {

  var callerName: String = ""
  var callerAvatar: String = ""
  var calleeName: String = ""
  var calleeAvatar: String = ""
  var callType: String = ""
  var username: String = ""
  var password: String = ""
  var domain: String = ""
  var destination: String = ""
  private val currentCallDurationSeconds = MutableStateFlow(0L)
  private val lastCallDurationSeconds = MutableStateFlow(0L)
  private var callService: NeoCallService? = null
  var metaData: HashMap<*, *> = hashMapOf(
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
    //"call_name_title" to "Xanh SM Customer",
    "call_btn_message" to "Send Message",
    "call_btn_mute" to "Mute",
    "call_btn_speaker" to "Speaker",
    "call_failed_api" to "Call failed due to system error",
    "call_failed_no_connection" to "No internet connection",
    "call_feedback_bad" to "Bad experience",
    "call_feedback_bad_driver_cannot_hear" to "Driver couldn't hear me",
    "call_feedback_bad_lost_connection" to "Call was disconnected",
    "call_feedback_bad_noisy" to "Too much background noise",
    "call_feedback_bad_unstable_connection" to "Unstable connection",
    "call_feedback_btn_submit" to "Submit Feedback",
    "call_feedback_desc_content" to "Help us improve by sharing your experience",
    "call_feedback_desc_title" to "Tell us about your call experience",
    "call_feedback_good" to "Good experience",
    "call_feedback_good_connection" to "Good connection",
    "call_feedback_good_no_delay" to "No audio delay",
    "call_feedback_good_sound" to "Clear sound quality",
    "call_feedback_okay" to "Okay",
    "call_feedback_okay_delay" to "Audio was delayed",
    "call_feedback_okay_flickering_sound" to "Audio was flickering",
    "call_feedback_okay_small_sound" to "Sound was too low",
    "call_feedback_skip" to "Skip feedback",
    "call_feedback_title" to "Call Feedback",
    "call_option_btn_free_call" to "Free Call",
    "call_option_title" to "Call Options",
    "call_permission_btn_allow" to "Allow",
    "call_permission_btn_deny" to "Deny",
    "call_permission_btn_setting" to "Go to Settings",
    "call_permission_btn_skip" to "Skip",
    "call_permission_microphone_content" to "We need access to your microphone to make calls",
    "call_permission_microphone_demied_content" to "Please enable microphone access in your phone’s Settings",
    "call_permission_microphone_demied_title" to "Microphone access is required to make a call",
    "call_permission_microphone_title" to "Microphone Permission",
    "call_status_call_customer" to "Calling customer",
    "call_status_call_customer_no_answer" to "Customer did not answer",
    "call_status_call_customer_refused" to "Customer refused the call",
    "call_status_call_driver" to "Calling driver",
    "call_status_call_driver_cancelled" to "Driver cancelled the call",
    "call_status_call_driver_no_answer" to "Driver did not answer",
    "call_status_call_driver_refused" to "Driver refused the call",
    "call_status_call_from_customer" to "Incoming call from customer",
    "call_status_call_from_customer_miss" to "Missed call from customer",
    "call_status_call_from_driver" to "Incoming call from driver",
    "call_status_call_from_driver_miss" to "Missed call from driver",
    "call_status_call_guide_again" to "Please try calling again",
    "call_status_call_guide_back" to "Please return to the app to continue the call",
    "call_suggestion_btn_dial" to "Dial",
    "call_suggestion_btn_free_call" to "Call for Free",
    "call_suggestion_btn_message" to "Send a Message",
    "call_suggestion_desc_travelling" to "The user might be traveling",
    "call_suggestion_desc_try_again" to "Try calling again in a moment",
  )
  private var bound = false
  private val callServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
      val binder = binder as? NeoCallService.LocalBinder ?: return
      callService = binder.getService()
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

    metaData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val extra = intent.getSerializableExtra("meta_data", HashMap::class.java)?.mapNotNull {
        val key = it.key as? String
        val value = it.value as? String
        if (key != null && value != null) key to value else null
      }?.toMap() ?: emptyMap()
      HashMap(metaData + extra)
    } else {
      val extra = (intent.getSerializableExtra("meta_data") as? HashMap<*, *>)?.mapNotNull {
        val key = it.key as? String
        val value = it.value as? String
        if (key != null && value != null) key to value else null
      }?.toMap() ?: emptyMap()
      HashMap(metaData + extra)
    }


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

    VoipSdk.initialize(SipEngine.build(this))

    enableEdgeToEdge()
    setContent {
      val timeTicker by currentCallDurationSeconds.collectAsState()
      val lastTicker by lastCallDurationSeconds.collectAsState()
      var isMicMuted by remember { mutableStateOf(false) }
      var speakerOutput by remember { mutableIntStateOf(0) }
      var callStatusRaw by remember { mutableStateOf("") }
      val connectionState by remember { mutableStateOf("") }
      var showErrorDialog by remember { mutableStateOf(false) }
      var showSpeakerDialog by remember { mutableStateOf(false) }
      val registrationState by VoipSdk.observeRegistrationState().collectAsState()
      val callState by VoipSdk.observeCallState().collectAsState()
      var errorMessage by remember { mutableStateOf<String?>(null) }
      var loadingMessage by remember { mutableStateOf<String?>(null) }
      var showLoading by remember { mutableStateOf(false) }

      SetSystemBarAppearance(isLight = true)
      LaunchedEffect(registrationState) {
        when (val state = registrationState) {
          is RegistrationState.Failed -> {
            errorMessage = "registration failed : ${state.message}"
            showErrorDialog = true
            showLoading = false
          }

          RegistrationState.None -> {
          }

          RegistrationState.Registered -> {
//            callStatusRaw = "registered"
            VoipSdk.startCall("sip:$destination@$domain")
            showLoading = false
          }

          RegistrationState.Registering -> {
//            callStatusRaw = "registering"
            loadingMessage = "Registering..."
            showLoading = true
          }
        }
      }

      LaunchedEffect(callState) {
        when (val state = callState) {
          CallState.Active -> {
            callStatusRaw = "active"
            lastCallDurationSeconds.value = 0L
            startService(buildCallServiceIntent(NeoCallService.Action.ONGOING))
          }

          CallState.Calling -> {
            callStatusRaw = "calling"
          }

          CallState.Connected -> {
            callStatusRaw = "connect"
          }

          CallState.Disconnected -> {
            callStatusRaw = "disconnected"
            lastCallDurationSeconds.value =
              callService?.getLastCallDurationSeconds()?.takeIf { it > 0 } ?: timeTicker
            stopCallService()
          }

          is CallState.Error -> {
            callStatusRaw = "error"
            lastCallDurationSeconds.value =
              callService?.getLastCallDurationSeconds()?.takeIf { it > 0 } ?: timeTicker
            stopCallService()
            errorMessage = "call error ${state.reason}"
            showErrorDialog = true
          }

          CallState.Hold -> {
            callStatusRaw = "hold"
          }

          CallState.Idle -> {
            callStatusRaw = "idle"
          }

          CallState.Ringing -> {
            callStatusRaw = "ringging"
          }
        }
      }

      DisposableEffect(Unit) {
        VoipSdk.register(username = username, password = password, domain = domain)
        Log.e("TAG", "cobacall : DisposableEffect init & register")

        onDispose {
          callService?.setOnTickerUpdateListener(null)
          stopCallService()
          VoipSdk.destroy()
          Log.e("TAG", "cobacall : onDispose destroy")

        }
      }

      val callTimerText = when {
        callStatusRaw == "active" -> formatElapsedTime(timeTicker)
        callStatusRaw in listOf("disconnected", "error") && lastTicker > 0L ->
          "Call duration ${formatElapsedTime(lastTicker)}"

        else -> (metaData["call_$callStatusRaw"] ?: callStatusRaw).toString()
      }

      MaterialTheme {
        CallScreen(
          callerName = if (callType == "incoming") callerName else calleeName,
          callTimer = callTimerText,
          callStatusRaw = callStatusRaw,
          signalState = if (connectionState == "active") "" else connectionState,
          avatarUrl = if (callType == "incoming") callerAvatar else calleeAvatar,
          isMicMuted = isMicMuted,
          speakerOutput = speakerOutput,
          metaData = metaData.mapKeys { it.key.toString() }.mapValues { it.value.toString() },
          onMuteClick = {
            isMicMuted = !isMicMuted
            VoipSdk.toggleMute()
          },
          onSpeakerClick = {
            showSpeakerDialog = true
          },
          onNumpadClick = {
//            callService?.sendDTMF(it)
            // TODO: set click numpad
          },
          onAnswerCallClick = {
//            answer()
            // TODO: answerclick
          },
          onEndCallClick = {
            if (callStatusRaw in listOf("disconnected", "error", "idle")) {
              finish()
            } else {
              VoipSdk.endCall()
            }
          })

        if (showLoading) {
          Loading(message = loadingMessage.orEmpty(), onDismissRequest = {})
        }

        if (showSpeakerDialog) {
          val listSpeaker = VoipSdk.getSpeakerOutput().filter { it in listOf(2, 3, 4, 9, 10) }
          Dialog(onDismissRequest = {
            showSpeakerDialog = false
          }, content = {
            Column(
              Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
            ) {
              listSpeaker.forEach {
                Text(
                  when (it) {
                    0 -> "Unknown"
                    1 -> "Microphone"
                    2 -> "Earpiece"
                    3 -> "Speaker"
                    4 -> "Bluetooth"
                    9, 10 -> "HeadSet"
                    else -> "$it Tidak terdaftar"
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      showSpeakerDialog = false
                      VoipSdk.toggleSpeaker(it)
                      speakerOutput = it
                    }
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                )
              }
            }
          })
        }

        if (showErrorDialog) {
          ErrorAlertDialog(
            onDismiss = {
              showErrorDialog = false
              VoipSdk.endCall()
              VoipSdk.logout()
              stopCallService()
              VoipSdk.destroy()
              finish()
            },
            message = errorMessage
          )
        }
      }
    }
  }

  private fun showToast(string: String) {
    Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
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
  callTimer: Any,
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

  BackHandler {
    if (showDialPad) {
      showDialPad = false
    } else {

    }
  }

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

        // Avatar
        Row(
          horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {

            Text(text = callTimer as String, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(55.dp))
            CallAvatar(avatarUrl)
            Spacer(modifier = Modifier.height(35.dp))

            Text(
              text = if (metaData["call_name_title"]?.isBlank() == true) callerName else metaData["call_name_title"]
                ?: callerName, style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(15.dp))
            Text(
              text = metaData[signalState] ?: signalState, // ->status network
              style = MaterialTheme.typography.bodyMedium, color = Color.Red
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))

        //
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 15.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RoundIconButton(
            icon = when (speakerOutput) {
              0, 2, 3 -> Icons.AutoMirrored.Outlined.VolumeUp
              4 -> SpeakerBluetooth
              9, 10 -> SpeakerHeadphone
              else -> Icons.AutoMirrored.Outlined.VolumeUp
            },

            label = metaData["call_btn_speaker"] ?: "Speaker",
            onClick = onSpeakerClick,
            backgroundColor = if (speakerOutput == 3) Color(0xFF00BABD) else Color(0xFFE9F8F9),
            iconTint = if (speakerOutput == 3) Color.White else Color(0xFF17666A),
            enabled = callStatusRaw.lowercase() != "ended"
          )

          RoundIconButton(
            icon = Icons.Filled.Dialpad,
            label = metaData["call_numpad"] ?: "Numpad",
            onClick = { showDialPad = true },
            backgroundColor = Color(0xFFE9F8F9),
            iconTint = Color(0xFF17666A),
            enabled = callStatusRaw.lowercase() == "connected"
          )

          RoundIconButton(
            icon = Icons.Default.MicOff,
            label = metaData["call_btn_mute"] ?: "Mute",
            onClick = onMuteClick,
            backgroundColor = if (isMicMuted) Color(0xFF00BABD) else Color(0xFFE9F8F9),
            iconTint = if (isMicMuted) Color.White else Color(0xFF17666A),
            enabled = callStatusRaw.lowercase() == "connected"
          )

          //                if (callStatusRaw.lowercase() == "incoming") {
          //                    RoundIconButton(
          //                        icon = Icons.AutoMirrored.Outlined.Chat,
          //                        label = metaData["call_btn_message"] ?: "Message",
          //                        onClick = onMessageClick,
          //                        backgroundColor = Color(0xFFE9F8F9),
          //                        iconTint = Color(0xFF17666A),
          //                    )
          //                }
        }
        Row(
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 55.dp)
        ) {

          RoundIconButton(
            icon = Icons.Filled.Close,
            label = "",
            onClick = onEndCallClick,
            backgroundColor = Color.Red,
            iconTint = Color.White,
            enabled = callStatusRaw.lowercase() != "ended"
          )
          if (callStatusRaw.lowercase() == "incoming") {
            Spacer(modifier = Modifier.width(160.dp))
            RoundIconButton(
              icon = Icons.Default.Phone,
              label = "",
              onClick = onAnswerCallClick,
              backgroundColor = Color.Green,
              iconTint = Color.White,
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
          .align(Alignment.BottomCenter)) {

        Surface(tonalElevation = 8.dp) {
          DialPad(
            onKeyPress = { digit ->
              if (digit == "close") {
                showDialPad = false
                return@DialPad
              }
              onNumpadClick(digit)
            })
        }
      }
    }
  }
}

@Composable
fun MultiLayerGradientBackground(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize()
  ) {
    // Layer 3: Base horizontal gradient (270deg)
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(
          brush = Brush.horizontalGradient(
            colorStops = arrayOf(
              0.0f to Color(0xFFFFF4DF), // Left becomes #FFF4DF
              0.5f to Color(0xFFFFFFFF), 1.0f to Color(0xFFDAFFFF)  // Right becomes #DAFFFF
            )
          )
        )
    )

    // Layer 2: Vertical fade (180deg)
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(
          brush = Brush.verticalGradient(
            colorStops = arrayOf(
              0.3167f to Color(0x00F6F6F6), // transparent
              1.0f to Color(0xFFF6F6F6)
            )
          )
        )
    )

    // Layer 1: Diagonal fade (224.7deg ≈ ~45° flip)
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(
          brush = Brush.linearGradient(
            colorStops = arrayOf(
              0.3943f to Color(0x00DFEFFF), // transparent
              1.0f to Color(0xFFEBFFFF)
            ), start = Offset.Infinite, end = Offset.Zero
          )
        )
    )
  }
}

@Composable
fun CallAvatar(imageUrl: String?) {
  if (imageUrl.isNullOrBlank()) {
    // Tampilkan icon orang jika URL kosong
    Box(
      modifier = Modifier
        .size(160.dp)
        .clip(CircleShape)
        .background(Color.LightGray),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "Default Avatar",
        tint = Color.White,
        modifier = Modifier.size(80.dp)
      )
    }
  } else {
    // Tampilkan gambar dari URL
    AsyncImage(
      model = imageUrl,
      contentDescription = "Caller Avatar",
      modifier = Modifier
        .size(160.dp)
        .clip(CircleShape)
        .border(2.dp, Color.Gray, CircleShape)
    )
  }
}

@Composable
fun RoundIconButton(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  backgroundColor: Color = Color.LightGray,
  iconTint: Color = Color.Black,
  enabled: Boolean = true,
) {
  val actualBackground = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.4f)
  val actualTint = if (enabled) iconTint else iconTint.copy(alpha = 0.6f)
  val textColor = Color(0XFF7F7F7F)
  val actualText = if (enabled) textColor else textColor.copy(alpha = 0.6f)

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier.size(64.dp).clip(CircleShape).background(actualBackground).let {
        if (enabled) it.clickable(onClick = onClick) else it
      }, contentAlignment = Alignment.Center
    ) {
      Icon(imageVector = icon, contentDescription = label, tint = actualTint)
    }

    if (label.isNotBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label, style = MaterialTheme.typography.bodySmall, color = actualText
      )
    }
  }
}

@Composable
private fun Loading(message: String, onDismissRequest: () -> Unit) {
  MaterialTheme() {
    Dialog(
      onDismissRequest = onDismissRequest
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
          .background(
            Color.White,
            RoundedCornerShape(12.dp)
          )
          .padding(vertical = 12.dp, horizontal = 24.dp)
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(32.dp),
        )
        Text(message, Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
      }

    }
  }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
  Loading(message = "Loading...") { }
}

//@Composable
//@Preview
//private fun DefaultPreview() {
//  val metaData: HashMap<*, *> = hashMapOf(
//    "initializing" to "Initializing",
//    "call_title" to "Telpone gratis",
//    "ringing" to "Ringing",
//    "connected" to "Connected",
//    "ended" to "Ended",
//    "answer" to "Answer",
//    "decline" to "Decline",
//    "mute" to "Mute",
//    "unmute" to "Unmute",
//    "speaker" to "Speaker",
//    "phone_speaker" to "Phone Speaker",
//  )
//  MaterialTheme {
//    CallScreen(
//      callerName = "Driver Andhi",
//      callTimer = "",
//      callStatusRaw = "connected",
//      signalState = "call_lost_connection",
//      avatarUrl = "",
//      isMicMuted = false,
//      speakerOutput = 4,
//      metaData = metaData.mapKeys { it.key.toString() }.mapValues { it.value.toString() },
//      onMuteClick = {},
//      onEndCallClick = {},
//      onAnswerCallClick = {},
//      onSpeakerClick = {},
//      onNumpadClick = {})
//    ErrorAlertDialog(
//      onDismiss = {},
//      message = "networkErrorText"
//    )
//  }
//}
