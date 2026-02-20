
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


## Cara Import & Penggunaan voip-sdk

### 1. Tambahkan JitPack ke repositories

Tambahkan di root build.gradle atau settings.gradle:

```gradle
allprojects {
	repositories {
		maven { url 'https://jitpack.io' }
	}
}
```

### 2. Tambahkan dependency voip-sdk

Ganti `username` dan `repo` sesuai GitHub Anda, dan `tag` sesuai versi release/tag:

```gradle
dependencies {
	implementation 'com.github.username:repo:tag'
}
```

Contoh:
```gradle
implementation 'com.github.kharozim:SIPApp:1.0.5'
```

### 3. Cara Penggunaan voip-sdk

**Inisialisasi SDK**

```kotlin
val voipSdk = VoipSdk
val sipEngine = SipEngine.build(context)
voipSdk.initialize(engine = sipEngine)
sipEngine.setListener(listener)
```

**Register SIP**

```kotlin
voipSdk.login(username, password, domain)
```

**Mulai Panggilan**

```kotlin
voipSdk.startCall("sip:tujuan@domain")
```

**Akhiri Panggilan**

```kotlin
voipSdk.endCall()
```

**Toggle Speaker & Mute**

```kotlin
voipSdk.toggleSpeaker()
voipSdk.toggleMute()
```

**Pantau Status**

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
