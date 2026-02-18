package com.neo.sipapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.neo.sipapp.ui.theme.SIPAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import org.linphone.core.Account
import org.linphone.core.AccountListenerStub
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.LogLevel
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType

sealed interface Screen {
  @Serializable
  data object Home : Screen

  @Serializable
  data object Call : Screen
}

class MainActivity : ComponentActivity() {
  private lateinit var core: Core
  private val registStatus = MutableStateFlow("")
  private val registState = MutableStateFlow<RegistrationState?>(null)

  // Create a Core listener to listen for the callback we need
  // In this case, we want to know about the account registration status
  private val coreListener = object : CoreListenerStub() {
    override fun onAccountRegistrationStateChanged(
      core: Core,
      account: Account,
      state: RegistrationState,
      message: String,
    ) {
      // If account has been configured correctly, we will go through Progress and Ok states
      // Otherwise, we will be Failed.
      registStatus.value = message
      registState.value = state
//      if (state == RegistrationState.Failed || state == RegistrationState.Cleared) {
//        findViewById<Button>(R.id.connect).isEnabled = true
//      } else if (state == RegistrationState.Ok) {
//        findViewById<Button>(R.id.disconnect).isEnabled = true
//      }
    }
  }

  private val accountListener = object : AccountListenerStub() {
    override fun onRegistrationStateChanged(
      account: Account,
      state: RegistrationState?,
      message: String,
    ) {
      // There is a Log helper in org.linphone.core.tools package
      Log.i("TAG", "[Account] Registration state changed: $state, $message")
    }
  }


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Some configuration can be done before the Core is created, for example enable debug logs.
    val factory = Factory.instance()
    Factory.instance().setLoggerDomain("mydebug")
    Factory.instance().loggingService.setLogLevel(LogLevel.Message)
    Factory.instance().enableLogcatLogs(true)

    // Your Core can use up to 2 configuration files, but that isn't mandatory.
    // On Android the Core needs to have the application context to work.
    // If you don't, the following method call will crash.
    core = factory.createCore(null, null, this)


    setContent {
      SIPAppTheme {
        val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
        val status = registStatus.asStateFlow()
        val registState = registState.asStateFlow()
        NavDisplay(
          backStack = backStack,
          onBack = { backStack.removeLastOrNull() },
          entryProvider = { key ->
            when (key) {
              Screen.Home -> NavEntry(key) {
                HomeScreen(
                  navToCall = { backStack.add(Screen.Call) },
                  coreVersion = core.version,
                  modifier = Modifier,
                  status = status.value,
                  registState = registState.value,
                  loginClick = { model ->

                    // To configure a SIP account, we need an Account object and an AuthInfo object
                    // The first one is how to connect to the proxy server, the second one stores the credentials

                    // The auth info can be created from the Factory as it's only a data class
                    // userID is set to null as it's the same as the username in our case
                    // ha1 is set to null as we are using the clear text password. Upon first register, the hash will be computed automatically.
                    // The realm will be determined automatically from the first register, as well as the algorithm
                    val authInfo =
                      Factory.instance()
                        .createAuthInfo(model.username, null, model.pass, null, null, model.domain, null)

                    // Account object replaces deprecated ProxyConfig object
                    // Account object is configured through an AccountParams object that we can obtain from the Core
                    val accountParams = core.createAccountParams()

                    // A SIP account is identified by an identity address that we can construct from the username and domain
                    val identity = Factory.instance().createAddress("sip:${model.username}@${model.domain}")
                    accountParams.identityAddress = identity


                    // We also need to configure where the proxy server is located
                    val address = Factory.instance().createAddress("sip:${model.domain}")
                    // We use the Address object to easily set the transport protocol
                    address?.transport = model.transportType
                    accountParams.serverAddress = address
                    // And we ensure the account will start the registration process
                    accountParams.isRegisterEnabled = true

                    // Now that our AccountParams is configured, we can create the Account object
                    val account = core.createAccount(accountParams)

                    // Now let's add our objects to the Core
                    core.addAuthInfo(authInfo)
                    core.addAccount(account)

                    // Also set the newly added account as default
                    core.defaultAccount = account

                    // Allow account to be removed
//                    findViewById<Button>(R.id.delete).isEnabled = true

                    // To be notified of the connection status of our account, we need to add the listener to the Core
                    core.addListener(coreListener)

                    // We can also register a callback on the Account object

                    account.addListener(accountListener)

                    // Finally we need the Core to be started for the registration to happen (it could have been started before)
                    core.start()

                  }
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
  val transportType: TransportType,
)

@Composable
fun HomeScreen(
  modifier: Modifier = Modifier,
  navToCall: () -> Unit,
  coreVersion: String,
  status: String,
  registState: RegistrationState? = null,
  loginClick: (model: LoginModel) -> Unit,
) {
  val username = "kharozim"
  val password = "pass123"
  val domain = "domain123"
  var transport by remember { mutableStateOf(TransportType.Tls) }

  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Column(
      Modifier
        .padding(innerPadding)
        .padding(24.dp)
        .fillMaxSize(),
      verticalArrangement = Arrangement.Center
    ) {
      Text("core : $coreVersion")
      Text("status : $status")
      Text("state : ${registState?.name}")
      Text("username : $username")
      Text("password : $password")
      Text("domain : $domain")
      Text("transport : ${transport.name} ")

      Spacer(Modifier.size(12.dp))
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
          transport == TransportType.Tls,
          onClick = { transport = TransportType.Tls },
        )
        Text("Tcp")
        RadioButton(
          transport == TransportType.Tcp,
          onClick = { transport = TransportType.Tcp },
        )
        Text("Udp")
        RadioButton(
          transport == TransportType.Udp,
          onClick = { transport = TransportType.Udp },
        )
      }

      Spacer(Modifier.size(12.dp))
      Button(onClick = { navToCall.invoke() }, Modifier.fillMaxWidth()) { Text("Call Screen") }
      Button(onClick = {
        loginClick.invoke(
          LoginModel(
            username = username,
            pass = password,
            domain = domain,
            transportType = transport,
          )
        )
      }, Modifier.fillMaxWidth()) { Text("Login") }
      Button(onClick = { }, Modifier.fillMaxWidth()) { Text("Connect") }
      Button(onClick = { }, Modifier.fillMaxWidth()) { Text("Disconnect") }
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