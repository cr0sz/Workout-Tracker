# Workout Tracker

An Android app for logging and managing workout sessions. Built with Kotlin and MVVM architecture, with full offline support and optional Firebase cloud sync.

<br>

<img src="screenshots/home.jpeg" width="250"> <img src="screenshots/exercises.jpeg" width="250"> <img src="screenshots/programs.jpeg" width="250"> <img src="screenshots/tools.jpeg" width="250">

<br>

## Download

**[Download APK v1.1.0](https://github.com/cr0sz/Workout-Tracker/releases/tag/v1.1.0)**

> Enable "Install from unknown sources" in your Android settings before installing.

<br>

## Features

- Create and manage workout programs
- Log exercises with sets, reps, and weights
- Full offline support — no internet required
- Cloud sync via Firebase Firestore
- Google account authentication
- Export workouts as CSV
- Input validation and error handling
- Clean, minimal UI

<br>

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM |
| Local database | Room (SQLite) |
| Cloud database | Firebase Firestore |
| Authentication | Firebase Auth (Google Sign-In) |
| UI | Android SDK, Jetpack components |

<br>

## Architecture

The app follows MVVM for a clean separation of concerns:

```
UI Layer (Activities / Fragments)
    ↕
ViewModel Layer (business logic, state management)
    ↕
Repository Layer
    ↕
Room Database (local)   ←→   Firebase Firestore (cloud)
```

This structure keeps the UI completely independent from the data layer, making the codebase easy to maintain and extend.

<br>

## Build from Source

1. Clone the repository
```bash
git clone https://github.com/cr0sz/Workout-Tracker.git
```

2. Open in Android Studio

3. Add your own `google-services.json` from [Firebase Console](https://console.firebase.google.com/) to `/app`

4. Register your debug SHA-1 fingerprint in Firebase Console → Project Settings → Your apps

5. Build and run on an emulator or physical device

<br>

## Roadmap

- [ ] Workout analytics and progress charts
- [ ] ML-powered workout recommendations
- [ ] Import workouts from CSV
- [ ] Home screen widget for quick logging

<br>

## Author

**Bekir Akyüz**
[github.com/cr0sz](https://github.com/cr0sz) · [linkedin.com/in/bekirakyüz](www.linkedin.com/in/bekirakyüz)
