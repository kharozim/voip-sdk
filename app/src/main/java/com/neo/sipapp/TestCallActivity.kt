package com.neo.sipapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.neo.sipapp.ui.theme.SIPAppTheme
import com.neo.voip_sdk.CallState
import com.neo.voip_sdk.RegistrationState
import com.neo.voip_sdk.SipEngine
import com.neo.voip_sdk.SipEngineListener
import com.neo.voip_sdk.VoipSdk

// Import VoipSdk sesuai package yang benar
// import com.yourpackage.voipsdk.VoipSdk

class TestCallActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      SIPAppTheme {
        TestCallScreen()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestCallScreen() {
  val context = LocalContext.current
  var username by remember { mutableStateOf("1012") }
  var password by remember { mutableStateOf("5678") }
  var domain by remember { mutableStateOf("147.139.193.218:5061") }
  var destination by remember { mutableStateOf("082386092523") }
  val callState = remember { mutableStateListOf("Idle") }
  var registrationStatus by remember { mutableStateOf<RegistrationState>(RegistrationState.None) }
  var isLoggedIn by remember { mutableStateOf(false) }
  var isCalling by remember { mutableStateOf(false) }
  val voipSdk = remember { VoipSdk } // Uncomment and adjust as needed

  // Contoh implementasi sederhana SipEngine (hanya dibuat sekali)
  val sipEngine = remember { SipEngine.build(context) }
  val listener: SipEngineListener = remember {
    object : SipEngineListener {
      override fun onRegistration(state: RegistrationState) {
        registrationStatus = state
        callState.add(
          when (state) {
            RegistrationState.Failed -> "register failed"
            RegistrationState.None -> "register None"
            RegistrationState.Registered -> {
              isLoggedIn = true
              "registered"
            }
            RegistrationState.Registering -> "registering"
          }
        )
      }

      override fun onCallState(state: CallState) {
        when (state) {
          CallState.Active, CallState.Calling, CallState.Connected -> isCalling = true
          CallState.Disconnected, CallState.Idle -> isCalling = false
          is CallState.Error -> isCalling = false
          else -> {}
        }
        callState.add(
          when (state) {
            CallState.Active -> "call active"
            CallState.Calling -> "call calling"
            CallState.Connected -> "call connected"
            CallState.Disconnected -> "call disconnect"
            is CallState.Error -> {
              val message = state.reason
              "call error : $message"
            }
            CallState.Hold -> "call hold"
            CallState.Idle -> "call idle"
            CallState.Ringing -> "call ringing"
          }
        )
      }

      override fun onIncomingCall(from: String) {
        callState.add("incoming call : $from")
        android.util.Log.e("TAG", "cobacall onIncomingCall: $from")
      }
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      Toast.makeText(context, "Microphone permission granted", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
    }
  }

  DisposableEffect(Unit){

    voipSdk.initialize(engine = sipEngine)
    sipEngine.setListener(listener)
    Log.e("TAG", "cobacall : DisposableEffect")

    onDispose {
      sipEngine.destroy()
      Log.e("TAG", "cobacall : ondispose")
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(title = { Text("Test Call Sample") })
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .padding(padding)
        .padding(16.dp)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("SIP Username") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("SIP Password") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = domain,
        onValueChange = { domain = it },
        label = { Text("SIP Domain") },
        modifier = Modifier.fillMaxWidth()
      )
      OutlinedTextField(
        value = destination,
        onValueChange = { destination = it },
        label = { Text("Destination Number") },
        modifier = Modifier.fillMaxWidth()
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Jika belum register, tampilkan tombol Register
        if (registrationStatus != RegistrationState.Registered && !isLoggedIn) {
          Button(onClick = {
            val packageManager = context.packageManager
            if (packageManager.checkPermission(
                Manifest.permission.RECORD_AUDIO,
                context.packageName
              ) != PackageManager.PERMISSION_GRANTED
            ) {
              permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              return@Button
            }
            voipSdk.login(username, password, domain)
          }) {
            Text("Login")
          }
        }

        // Jika sudah login, tampilkan tombol Call dan Logout
        if (isLoggedIn && !isCalling) {
          Button(onClick = {
            voipSdk.startCall("sip:$destination@$domain")
          }) {
            Text("Call")
          }
          Button(onClick = {
            VoipSdk.logout()
            isLoggedIn = false
            registrationStatus = RegistrationState.None
          }) {
            Text("Logout")
          }
        }
        // Jika sedang call, tampilkan tombol Load Speaker, End Call, dan Mute
        if (isCalling) {
          Button(onClick = {
            VoipSdk.toggleSpeaker()
          }) {
            Text("Load Speaker")
          }
          Button(onClick = {
            voipSdk.endCall()
            isCalling = false
          }) {
            Text("End Call")
          }
          Button(onClick = {
            VoipSdk.toggleMute()
          }) {
            Text("Mute")
          }
        }
      }
      LazyColumn(Modifier.fillMaxWidth()) {
        items(callState.size) {
          Text("Call State: ${callState[it]}")
        }
      }
    }
  }
}
