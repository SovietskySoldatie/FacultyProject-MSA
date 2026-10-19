# FacultyProject-MSA: SecureNotes Vault
MSA (ro. SMA) project - Year 4, Sem 1

This is a faculty project for the Mobile Systems & Apps (en. MSA, ro. SMA) course at Politehnica University of Timișoara.
This project is designed and intended to be completed by a single student, that being the owner of this repo (SovietskySoldatie).

## Project Theme: SecureNotes Vault

**SecureNotes Vault** is a secure mobile application dedicated to managing personal notes and confidential information, combining daily note-taking functionality with strict privacy controls.

### Key Highlights
- **Dual Content Tiers:** Standard notes for everyday access alongside an encrypted **Vault** for sensitive entries requiring per-access biometric (Fingerprint / Touch ID, Facial Recognition / Face ID) or PIN authentication.
- **Hardware-Assisted Panic Lock:** Real-time emergency protection that automatically locks the app and hides sensitive data using device sensors:
  - **Proximity Sensor:** Instant screen lock when covered by hand or placed face-down on a flat surface ($d = 0$).
  - **Accelerometer / Gravity Sensor:** Auto-locks and saves on sudden shakes or when flipped face-down ($g_z < -9.0\text{ m/s}^2$).
- **Haptic Feedback:** Tactile confirmation for lock events and authentication state changes.
- **Access Modes:** Supports Single-Owner (tied to device credentials) and Multi-User architectures with biometric delegation.

For complete details, see the functional and technical specifications in [specs/specs.md](specs/specs.md).

## TO-DOs:
- [x] Add project original specifications ([specs/specs.md](specs/specs.md))
- [ ] Decide on development strategy, discuss/negotiate specs with project responsible (teaching assistant)
- [ ] Decompose project specifications
- [ ] Decompose project requirements

- [ ] Define project architecture
	- [ ] Identify hardware architecture
	- [ ] Define software architecture

- [ ] Define software design
- [ ] Implement tests
- [ ] Implement code