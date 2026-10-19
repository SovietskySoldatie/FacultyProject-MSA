# SecureNotes Vault – Functional and Technical Specifications

## 1. General Overview

**SecureNotes Vault** is a mobile application dedicated to managing personal notes and confidential data. The application combines day-to-day organization with a high level of privacy, offering two content categories:
* **Standard Notes:** Accessible immediately after logging into the app (ideas, quick lists, everyday notes).
* **Secured Notes (Vault):** Entries with masked content in the main list that require additional verification (biometric or PIN) every time they are viewed, edited, or deleted.

For real-time protection against shoulder surfing and prying eyes, the application integrates a **Panic Lock** mechanism powered by the device's physical sensors, which instantly hides on-screen content or closes the current window upon detecting emergency gestures.

---

## 2. Authentication and Access Control

The application implements user authentication on launch and when accessing protected resources, and can be configured in one of the following two architectural modes:

### Variant A: Single-Owner (Access tied to the device owner)
* Primary authentication relies on the phone's native biometric system (**Fingerprint / Touch ID** or **Facial Recognition / Face ID**).
* Includes a *fallback* mechanism (PIN code or locally stored encrypted Master Password) for scenarios where the biometric sensor is unavailable or recognition fails.

### Variant B: Multi-User (Multiple users on the same device)
* Classic authentication based on **Username/Email and Password/PIN**, where each user has an isolated note space in the database.
* **Biometric Delegation for Owner:** The legitimate owner of the phone can link their personal account with the device's biometric sensor in settings for quick authentication, while other users authenticate exclusively via their own password.

### Additional Note-Level Security
* Regardless of whether the user is already authenticated in the app, any interaction (read, update, delete) with a note marked as **Secured** triggers a new biometric check or PIN request.

---

## 3. Screen Architecture (UI & Navigation)

The application interface is structured into 4 main screens:

1. **Authentication and Lock Screen (Login / Lock Screen):**
   * The entry point into the application.
   * Displays the authentication form (PIN/Password) and triggers the native biometric prompt dialog.
   * Serves as the safety screen that the application immediately redirects to upon activating *Panic Lock*.
2. **Main Screen (Dashboard / Notes List):**
   * Displays the scrollable list of all notes associated with the current user.
   * Includes a search bar by title and quick filters (*All*, *Standard*, *Secured*).
   * For secured notes, the text preview is replaced with masking characters (`••••••••`) and a lock icon.
3. **Note View, Create, and Edit Screen:**
   * Form for note title, category, and body content.
   * Switch/Toggle to enable or disable the **"Additional Secured Note"** attribute.
   * Actively monitors motion and proximity sensors for the entire duration that a note is open on the screen.
4. **Settings and Sensor Configuration Screen:**
   * Individual toggles for enabling/disabling quick-close gestures (sudden movements, flip face-down, physical screen covering).
   * Slider to adjust the sensitivity threshold for sudden movements.
   * Options for session management (*Logout*) and linking biometrics to the current account.

---

## 4. Data Model and CRUD Operations

Data persistence is handled locally on the device, centered around the `Note` entity (and optionally the `User` entity for Multi-User mode).

### `Note` Entity Structure
* `id` – Primary key, auto-generated.
* `userId` – Foreign key (note owner's identifier).
* `title` – String, mandatory field.
* `content` – Extended note text.
* `category` – String (e.g., *Personal*, *University*, *Financial*, *Passwords*).
* `isLocked` – Boolean value (`true` for an additionally secured note, `false` for a standard note).
* `createdAt` / `updatedAt` – Timestamps for sorting.

### Supported Operations (CRUD)
* **Create:** Adding a new note to the database, with field validation to prevent saving empty records.
* **Read:** Loading and displaying the list of notes, filtering them by category or title, and opening details (conditioned on biometric validation if `isLocked == true`).
* **Update:** Modifying title, content, category, or toggling the security status (`isLocked`) of an existing note.
* **Delete:** Permanently deleting a note from the database (requiring confirmation and biometric validation for secured notes).

---

## 5. Integrated Sensors and Actuators

The application utilizes the phone's hardware sensors and an actuator to ensure data privacy in unforeseen circumstances:

### Sensors
1. **Touchscreen & Biometric Sensor (Fingerprint / Facial Recognition):**
   * **Role:** Handling user interactions and validating physical identity during authentication and unlocking notes from the *Vault*.
2. **Proximity Sensor:**
   * **Role:** Detects physical covering of the top portion of the screen with a palm or placing the phone close to a flat surface (when the measured distance drops to $d = 0$).
   * **Triggered Action:** Instantly closes the current screen or locks the application.
3. **Accelerometer / Gravity Sensor:**
   * **Role:** Monitors acceleration across all three axes ($X, Y, Z$) for two scenarios:
     * *Sudden Movement (Shake Detection):* When the resultant acceleration exceeds the configured threshold ($a > a_{\text{threshold}}$).
     * *Flip Face-Down (Flip-Down):* When the gravitational component along the $Z$ axis becomes negative ($g_z < -9.0\text{ m/s}^2$).
   * **Triggered Action:** Automatically saves changes in the open note, closes the current screen, and redirects the application to the lock screen.

### Actuators
1. **Vibration Motor (Haptic Feedback):**
   * **Role:** Tactile extension used strictly for confirming critical actions without requiring the user to look at the screen.
   * **Behavior:**
     * *Single Short Vibration:* Confirms that the application has successfully locked after flipping the phone face-down or covering the screen.
     * *Double Vibration:* Signals a biometric/PIN authentication error or confirms the deletion of a note.

---

## 6. Technical Considerations and Biometric Integration (Android & iOS)

* **Native Android (Java / Kotlin):**
  * Uses the official `androidx.biometric.BiometricPrompt` API, which uniformly handles both the fingerprint sensor and facial recognition, offering a fallback option to device credentials.
  * Motion and proximity sensors are accessed via `SensorManager` and the `SensorEventListener` interface.
* **Cross-Platform – Android & iOS (e.g., Flutter / React Native):**
  * On **iOS**, integration of **Face ID** and **Touch ID** for the device owner is achieved through the native `LocalAuthentication` framework (accessed directly via packages such as `local_auth` in Flutter or `expo-local-authentication` in React Native), requiring only the addition of the `NSFaceIDUsageDescription` key in the `Info.plist` file.
* **Biometric Data Privacy:**
  * On both Android and iOS, validation occurs exclusively at the operating system level (*Trusted Execution Environment* / *Secure Enclave*). The application neither stores nor accesses raw biometric data; it only receives a success or failure response.