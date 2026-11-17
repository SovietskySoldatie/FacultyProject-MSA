# System Requirements and UML Architecture Diagrams

This document defines the formal software-hardware codesign architecture, system requirements, and UML diagrams for the SecureNotes Vault.

---

## 1. Components View and Component Interactions

The following high-level component diagram illustrates the primary functional blocks of the Mobile Systems and Apps (MSA) architecture and the consolidated relationships between them.

```mermaid
flowchart TD
    subgraph SoftwareDomain["Software Architecture"]
        UIView["UI / View Layer<br/>(Auth Screen, Notes Dashboard, Note Editor, Sensor Settings)"]
        CoreApp["Core Application Logic<br/>(Note Models, Vault Manager, Geofence Controller, Session & User Mgmt)"]
        SecurityService["Security & Cryptography Engine<br/>(Biometric Authenticator, AES-256 Vault Crypto, Keyring Bridge)"]
        SensorCoordinator["Sensor & Panic Coordinator<br/>(Proximity Detector, Motion / Shake Evaluator, Haptic Feedback Driver)"]
    end

    subgraph PlatformDomain["Platform & Hardware Abstraction"]
        HAL["Platform Services & HAL<br/>(BiometricPrompt / LocalAuth, SensorManager, LocationProvider, Secure Enclave / KeyStore, SQLite)"]
        Hardware["Physical Device Hardware<br/>(Touchscreen, Fingerprint / Face ID, Proximity Sensor, Accelerometer / IMU, Haptic Motor, TEE)"]
    end

    %% Consolidated Interactions
    UIView <-->|"User Inputs, Rendered Notes & View States"| CoreApp
    UIView -->|"Panic Triggers & Masking Commands"| SensorCoordinator
    CoreApp <-->|"Encrypted Payloads & Auth Requests"| SecurityService
    CoreApp <-->|"Panic Lock Signals & Event Dispatch"| SensorCoordinator

    SecurityService <-->|"Biometric Prompts & Hardware Key Management"| HAL
    SensorCoordinator <-->|"Sensor Streams & Haptic Vibrations"| HAL
    HAL <-->|"Low-Level OS Drivers & Hardware Buses"| Hardware
```

### High-Level Component Interactions
* **UI / View Layer & Core Application**: Handles user navigation, note authoring, category filtering, and real-time masked/unmasked note presentation.
* **UI / View Layer & Sensor & Panic Coordinator**: Routes direct emergency gestures and receives screen masking/window dismissal events.
* **Core Application & Security & Cryptography Engine**: Manages encryption/decryption of vault notes, user session validation, and biometric challenges.
* **Core Application & Sensor & Panic Coordinator**: Dispatches background panic signals, coordinates automatic draft saves, and triggers immediate state lock.
* **Security Engine & Platform HAL**: Delegates biometric validation to native OS prompts (`BiometricPrompt` / `LocalAuthentication`) and derives encryption keys from the secure hardware keystore.
* **Sensor Coordinator & Platform HAL**: Registers listeners with `SensorManager` (proximity, accelerometer) and dispatches haptic pulses via the device vibrator service.
* **Platform HAL & Physical Device Hardware**: Bridges mobile OS subsystem calls to underlying silicon (IMU, biometric reader, TEE, and haptic actuator).

---

## 2. Main State Machine

The state machine defines the high-level operational lifecycle of the SecureNotes Vault application and its transitions across operational and emergency states.

```mermaid
stateDiagram-v2
    [*] --> Locked : App Launched
    Locked --> Authenticating : Enter Credentials / Biometric
    Authenticating --> Locked : Auth Failed / Canceled
    Authenticating --> DashboardUnlocked : Auth Success

    DashboardUnlocked --> ViewingStandardNote : Open Standard Note
    ViewingStandardNote --> DashboardUnlocked : Close / Back

    DashboardUnlocked --> VaultChallenge : Select Secured Note
    VaultChallenge --> ViewingVaultNote : Biometric Verified
    VaultChallenge --> DashboardUnlocked : Auth Denied / Canceled
    ViewingVaultNote --> DashboardUnlocked : Close / Back

    ViewingStandardNote --> PanicLocked : Proximity / Shake / Flip-Down
    ViewingVaultNote --> PanicLocked : Proximity / Shake / Flip-Down
    DashboardUnlocked --> PanicLocked : Proximity / Shake / Flip-Down

    PanicLocked --> Locked : Auto-Save & Evict Keys
    DashboardUnlocked --> Locked : Session Timeout / Manual Lock
    Locked --> [*]
```

### State Definitions
* **Locked**: Initial and secured state; local decryption keys are evicted from memory, and the login/lock interface is presented.
* **Authenticating**: Primary app entry verification using native biometrics or fallback credentials.
* **DashboardUnlocked**: Main operational hub; standard notes are visible, and vault note contents remain masked.
* **ViewingStandardNote**: Editor/viewer open for an everyday note with active background sensor monitoring.
* **VaultChallenge**: Modal biometric or PIN challenge required prior to decrypting a secured note.
* **ViewingVaultNote**: Decrypted vault note displayed in volatile memory; sensor panic detection is active.
* **PanicLocked**: Immediate emergency state triggered by sensor gestures; persists open changes, purges cached plaintext, and transfers control to `Locked`.

---

## 3. Use-Cases Diagram

The following use-case model illustrates the core functional capabilities of the system and primary interactions with the User, Device Sensors, and the Mobile Platform Security subsystem.

```mermaid
flowchart LR
    User((User))
    Sensors((Device Sensors))
    MobileOS((Mobile OS / Keystore))

    subgraph SecureVault["SecureNotes Vault"]
        UC1(["UC-1: Authenticate User & Unlock App"])
        UC2(["UC-2: Manage Standard Notes (CRUD)"])
        UC3(["UC-3: Access & Manage Vault Notes"])
        UC4(["UC-4: Trigger Hardware Panic Lock"])
        UC5(["UC-5: Configure Sensor Gestures & Security"])
        UC6(["UC-6: Enforce Location Access Rules"])
    end

    User --> UC1
    User --> UC2
    User --> UC3
    User --> UC4
    User --> UC5

    Sensors --> UC4
    MobileOS --> UC1
    MobileOS --> UC3
    MobileOS --> UC6

    UC3 -.->|"Requires Biometric Re-auth"| UC1
    UC4 -.->|"Auto-Saves & Invokes"| UC1
```

### Use Case Summary

| Use Case ID | Name | Primary Actor | Description |
| :--- | :--- | :--- | :--- |
| **UC-1** | Authenticate User & Unlock App | User / Mobile OS | Authenticates identity via native biometrics (Touch/Face ID) or PIN to unlock the app. |
| **UC-2** | Manage Standard Notes | User | Creates, views, updates, filters, and deletes regular unencrypted daily notes. |
| **UC-3** | Access & Manage Vault Notes | User / Mobile OS | Enforces per-access biometric validation to view, edit, or delete encrypted confidential notes. |
| **UC-4** | Trigger Hardware Panic Lock | Sensors / User | Detects flip-down, proximity covering, or shake gestures, auto-saving data and instantly locking the app. |
| **UC-5** | Configure Sensor Gestures | User | Customizes gesture toggles, shake sensitivity sliders, and authentication preferences. |
| **UC-6** | Enforce Location Access Rules | Mobile OS | Validates device GPS against configured safe zones to restrict or allow access to specific notes. |
