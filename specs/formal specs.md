# Formal Specifications: SecureNotes Vault

## 1. Core Note & Vault Management
* 1.1 Note Organization & Content Categorization
  * 1.1.1 The system shall provide standard personal note authoring and lifecycle management (title, category, body content, timestamps).
  * 1.1.2 Every note shall support a security flag designating it as either Standard or Secured (Vault).
  * 1.1.3 Standard notes shall be accessible immediately upon application launch and authentication.
  * 1.1.4 Vault notes shall display masked preview content (`••••••••`) and lock indicators on the main dashboard.
* 1.2 Note CRUD & Validation
  * 1.2.1 The system shall enforce non-empty title and content constraints upon note creation and modification.
  * 1.2.2 Deletion of any secured note shall require explicit user confirmation and biometric re-verification.

## 2. Authentication & Access Control Subsystem
* 2.1 Multi-Tier Authentication
  * 2.1.1 Primary app launch shall authenticate via native platform biometrics (Fingerprint / Touch ID, Facial Recognition / Face ID) with PIN/Password fallback.
  * 2.1.2 Accessing, editing, or revealing any Vault note shall enforce mandatory per-access biometric re-verification.
* 2.2 Operational Access Modes
  * 2.2.1 Single-Owner Mode: Authentication directly delegates to the mobile device's enrolled credentials and Keystore.
  * 2.2.2 Multi-User Mode: Provides isolated user workspaces with credentials stored securely; device owner can delegate biometrics to their profile.

## 3. Hardware Panic Lock & Sensor Subsystem
* 3.1 Proximity Gesture Detection
  * 3.1.1 The system shall monitor the onboard proximity sensor during active note display.
  * 3.1.2 When an obstruction or face-down flat contact is detected ($d = 0$), the app shall immediately trigger the panic lock.
* 3.2 Inertial & Orientation Detection
  * 3.2.1 The system shall process 3-axis accelerometer/gravity vectors to detect sudden vigorous agitation ($a > a_{\text{threshold}}$).
  * 3.2.2 Flip-down detection shall trigger when the gravitational $Z$-component turns negative ($g_z < -9.0\text{ m/s}^2$).
* 3.3 Panic Execution & Haptic Feedback
  * 3.3.1 Triggering a panic gesture shall auto-save pending edits, clear sensitive memory buffers, and route immediately to the secure lock screen.
  * 3.3.2 The vibration motor shall deliver a single short haptic pulse upon successful emergency lock, and double pulses upon auth error.

## 4. Cryptographic Storage & Security Subsystem
* 4.1 On-Device Database Encryption
  * 4.1.1 Local persistent data shall be stored in an encrypted SQLite database (SQLCipher / AES-256-GCM).
  * 4.1.2 Encryption keys shall be managed via platform hardware security (Android KeyStore / iOS Keychain Secure Enclave).
* 4.2 Sensitive Buffer Hygiene
  * 4.2.1 Decrypted note contents shall reside in volatile memory only while actively viewed and immediately zeroed out upon lock.

## 5. Location-Based Access Fencing (Geofencing)
* 5.1 Geographic Access Constraints
  * 5.1.1 The system shall support associating specific notes with designated geographic safe zones (GPS/Network coordinates and radius).
  * 5.1.2 Access to location-fenced notes shall be restricted or hidden when the device is outside the authorized perimeter.

## 6. Mobile Platform & UI Architecture
* 6.1 View Layer Hierarchy
  * 6.1.1 The interface shall consist of: Auth/Lock Screen, Main Dashboard, Note Editor/Viewer, and Settings/Sensor Tuning Screen.
  * 6.1.2 The settings screen shall provide individual toggles for panic gestures and sensitivity calibration sliders.
* 6.2 Platform Interoperability & Privacy
  * 6.2.1 The system shall operate on modern Android and iOS mobile operating systems.
  * 6.2.2 Biometric verification shall occur strictly inside OS secure enclaves without app-level access to raw biometric data.
