# Workout Tracker

An Android app for logging and managing workout sessions. Built with Kotlin and MVVM architecture, with full offline support via Room database.

<br>

<img src="screenshots/home.jpeg" width="250"> <img src="screenshots/exercises.jpeg" width="250"> <img src="screenshots/programs.jpeg" width="250"> <img src="screenshots/tools.jpeg" width="250">

<br>

## Download

**[Download APK v1.0.0](https://github.com/cr0sz/Workout-Tracker/releases/tag/v1.0.0)**

> Enable "Install from unknown sources" in your Android settings before installing.

<br>

## Features

- Create and manage workout programs
- Log exercises with sets, reps, and weights
- Full offline support — no internet required
- Input validation and error handling
- Clean, minimal UI

<br>

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM |
| Local database | Room (SQLite) |
| UI | Android SDK, Jetpack components |
| Auth (planned) | Firebase Authentication |
| Cloud sync (planned) | Cloud Firestore |

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
Room Database (local persistence)
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

4. Build and run on an emulator or physical device

<br>

## Roadmap

- [ ] Firebase cloud sync
- [ ] Workout analytics and progress charts
- [ ] Export / import workouts
- [ ] Widget for quick logging

<br>

## Author

**Bekir Akyüz**
[github.com/cr0sz](https://github.com/cr0sz) · [linkedin.com/in/bekirakyüz](https://www.linkedin.com/in/bekiraky%C3%BCz/)
