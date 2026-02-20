# SIPApp - Android Headless VoIP SDK Sample

## Overview

SIPApp adalah contoh aplikasi Android yang menggunakan voip-sdk untuk melakukan koneksi SIP ke server (Asterisk) secara headless (tanpa UI khusus). SDK ini dapat digunakan untuk membangun aplikasi VoIP Android yang ringan, modular, dan dapat diintegrasikan ke berbagai aplikasi lain.

## Fitur voip-sdk
- SIP Registration (daftar ke server SIP)
- Outgoing Call (panggilan keluar)
- Incoming Call (panggilan masuk)
- Call State Observer (pantau status panggilan)
- Audio Management (speaker, mute, dsb)
- Foreground Service
- Auto Reconnect Network

## Struktur Modul
- `voip-sdk-core`: API publik, state machine, repository, error mapping
- `voip-sdk-linphone`: Core initialization, listener, account setup, call state mapping
- `voip-sdk-service`: Foreground service, notification, audio focus, network monitoring

## Cara Penggunaan voip-sdk

1. **Inisialisasi SDK**

```kotlin
val voipSdk = VoipSdk
val sipEngine = SipEngine.build(context)
voipSdk.initialize(engine = sipEngine)
sipEngine.setListener(listener)
```

2. **Register SIP**

```kotlin
voipSdk.login(username, password, domain)
```

3. **Mulai Panggilan**

```kotlin
voipSdk.startCall("sip:tujuan@domain")
```

4. **Akhiri Panggilan**

```kotlin
voipSdk.endCall()
```

5. **Toggle Speaker & Mute**

```kotlin
voipSdk.toggleSpeaker()
voipSdk.toggleMute()
```

6. **Pantau Status**

```kotlin
voipSdk.observeCallState() // StateFlow<CallState>
voipSdk.observeRegistrationState() // StateFlow<RegistrationState>
voipSdk.observeIncomingCall() // Flow<IncomingCallEvent>
```

## Contoh Integrasi di TestCallActivity
Lihat file `app/src/main/java/com/neo/sipapp/TestCallActivity.kt` untuk contoh implementasi Compose.

## CI/CD

Project ini sudah dilengkapi workflow GitHub Actions untuk build dan test otomatis setiap push/pull request ke branch `main`. Artifact APK debug akan di-upload ke GitHub.

## License

MIT
