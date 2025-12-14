<div style="text-align:center"><a href="https://github.com/Safouene1/support-palestine-banner/blob/master/Markdown-pages/Support.md"><img src="https://raw.githubusercontent.com/Safouene1/support-palestine-banner/master/banner-support.svg" alt="Support Palestine" style="width: 100%;"></a></div>

---

<p align="center">
    <a href="https://ffalt.github.io" target="_blank">
        <img height="200" width="200" src="./icon.svg" alt="Starfield logo">
    </a>
</p>

<h1 align="center">Starfield — Android Live Wallpaper</h1>
<p align="center">A lightweight, hardware accelerated, highly configurable live wallpaper that simulates a flight through a starfield.</p>

<p align="center">
  <a href="https://github.com/ffalt/starfield/releases" target="_blank"><img src="https://img.shields.io/github/release/ffalt/starfield.svg" alt="Latest release"></a>
  <a href="https://opensource.org/license/gpl-3-0" target="_blank"><img src="https://img.shields.io/badge/license-GPL%203.0-blue.svg" alt="License: GPL 3.0"></a>
  <img src="https://github.com/ffalt/starfield/workflows/test/badge.svg" alt="CI test badge">
</p>

## Features

Starfield focuses on smooth visuals and configurability without compromising battery life.

- Highly configurable starfield
  - Adjustable star count, size and color
  - Min/max star speeds for varied motion
- Motion & visual effects
  - Star trails with configurable gradient colors
  - Meteors (occasional streaks) with spawn probability and trail colors
- Screen & sensor interaction
  - Follow home screen swiping with optional restore behavior
  - Sensor-based movement with adjustable intensity
  - Battery-aware speed reduction option
- Performance & expert controls
  - Adjustable framerate (FPS) for performance tuning
  - Depth setting to control perceived 3D effect
- Usability
  - Color pickers for star, trail and meteor colors
  - Persistent settings and a Reset-to-defaults option

## Screenshots

<p align="middle">
<img width="200" src="./fastlane/metadata/android/en-US/images/phoneScreenshots/1_lock_screen.jpg" alt="Lock Screen">&nbsp; &nbsp;<img width="200" src="./fastlane/metadata/android/en-US/images/phoneScreenshots/2_wallpaper.jpg" alt="Wallpaper">&nbsp; &nbsp;<img width="200" src="./fastlane/metadata/android/en-US/images/phoneScreenshots/4_settings.jpg" alt="Settings">
</p>

## Installation

<a href="https://github.com/ffalt/starfield/releases" target="_blank"><img height="80" src="./badge-github.png" alt="Get it on Github"></a>
<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22:%22io.github.ffalt.starfield%22,%22url%22:%22https://github.com/ffalt/starfield%22,%22author%22:%22ffalt%22,%22name%22:%22Starfield%22,%22preferredApkIndex%22:%200%7D"  target="_blank"><img height="80" src="./badge-obtainium.png" alt="Get it on Obtainium"></a>



## Contribution & translation

Contributions are welcome. Small ways to help:
- Open issues for bugs or feature requests.
- Send pull requests with small, focused changes.
- Help translate: this project uses Crowdin — contribute translations at https://crowdin.com/project/starfield-wallpaper



## License

This project is licensed under the GPL 3.0 license. See `LICENSE` for details.



## Building from source

- Requirements: Java JDK (11+ recommended), Android SDK, Gradle wrapper (included)

To build a debug APK locally:

- macOS / Linux / WSL

  ./gradlew assembleDebug

- Install to a connected device (ADB required):

  adb install -r app/build/outputs/apk/debug/app-debug.apk

Note: The project uses the standard Android Gradle structure (see `app/` folder).



## Acknowledgments

This project uses https://github.com/jaredrummler/ColorPicker

