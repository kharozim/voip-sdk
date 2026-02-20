package com.neo.sipapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.neo.sipapp.ui.theme.SIPAppTheme
import kotlinx.serialization.Serializable

sealed interface Screen {
  @Serializable
  data object Home : Screen

  @Serializable
  data object Call : Screen
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SIPAppTheme {
        val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
        NavDisplay(
          backStack = backStack,
          onBack = { backStack.removeLastOrNull() },
          entryProvider = { key ->
            when (key) {
              Screen.Home -> NavEntry(key) {
                HomeScreen(
                  navToCall = { backStack.add(Screen.Call) },
                  modifier = Modifier,
                )
              }

              Screen.Call -> NavEntry(key) { CallScreen() }
            }
          }
        )
      }
    }
  }
}


data class LoginModel(
  val username: String,
  val pass: String,
  val domain: String,
)

@Composable
fun HomeScreen(
  modifier: Modifier = Modifier,
  navToCall: () -> Unit,
) {

  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Column(
      Modifier
        .padding(innerPadding)
        .padding(24.dp)
        .fillMaxSize(),
      verticalArrangement = Arrangement.Center
    ) {

      Spacer(Modifier.size(12.dp))
      val context = androidx.compose.ui.platform.LocalContext.current
      Button(
        onClick = {
          val intent = Intent(context, TestCallActivity::class.java)
          context.startActivity(intent)
        },
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Test Call Sample")
      }
      Spacer(Modifier.size(12.dp))
      Button(onClick = { navToCall.invoke() }, Modifier.fillMaxWidth()) { Text("Call Screen") }
    }
  }
}

@Composable
fun CallScreen(modifier: Modifier = Modifier) {
  var fieldPhone by remember { mutableStateOf(TextFieldValue("")) }
  Scaffold { innerPadding ->
    Column(
      Modifier
        .padding(innerPadding)
        .padding(24.dp)
        .fillMaxSize()
    ) {
      Text("Phone : ${fieldPhone.text}")
      OutlinedTextField(
        fieldPhone,
        onValueChange = {
          if (it.text.isDigitsOnly() || it.text.isEmpty()) {
            fieldPhone = it
          }
        },
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Phone
        )
      )

      Button(onClick = {
//        val callee = Factory.instance().createAddress("sip:janedoe@sip.example.org")
//        val params = core.createCallParams(null)
//        val call = core.inviteAddressWithParams(callee, params)
//        call?.addListener(object: CallListenerStub() {
//          override fun onStateChanged(call: Call, state: Call.State, message: String) {
//            when (state) {
//              Call.State.Connected -> Log.i("Call is connected")
//              else -> Log.i("Call is $state")
//            }
//          }
//        })
      }) {
        Text("Call")
      }

      Button(onClick = {}) {
        Text("Stop")
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = "Hello $name!",
    modifier = modifier
  )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  SIPAppTheme {
    Greeting("Android")
  }
}