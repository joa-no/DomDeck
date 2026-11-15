<div align="center">

# DomDeck

**An Android-first game library and launcher for external emulators.**

![Android](https://img.shields.io/badge/Android-Native-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Stage](https://img.shields.io/badge/Stage-Early%20development-6B7280)
[![Android Build](https://github.com/joa-no/DomDeck/actions/workflows/android.yml/badge.svg)](https://github.com/joa-no/DomDeck/actions/workflows/android.yml)

</div>

## Overview

DomDeck is a visual frontend for organizing games on Android. It **does not emulate games**, supply BIOS files, or distribute ROMs. Users install and configure their own emulators and select their own local game folders.

The experience is designed around a simple journey: **choose a console → browse a visual library → launch a game**.

## Current scope

- **PlayStation 1 / DuckStation:** external launcher integration tested on-device with a CUE/BIN game using Android's Storage Access Framework (SAF).
- **Library scanning:** read-only access to a user-selected folder and its subfolders.
- **Artwork and descriptions:** initial retrieval from Libretro Thumbnails and Wikimedia sources, with local caching and manual editing.
- **Interface:** Kotlin + Jetpack Compose, dark theme, responsive navigation and an introductory console animation.
- **Beyond PS1:** additional systems, low-poly 3D console models, and richer controller navigation are planned.

Availability of external metadata and compatibility with particular emulator versions may vary. The app does not copy ROMs into private storage.

## Build & test

The [Android Build workflow](https://github.com/joa-no/DomDeck/actions/workflows/android.yml) creates a downloadable **debug APK** in GitHub Actions artifacts when the build succeeds.

Alternatively, open this repository in Android Studio or build with a compatible Android SDK, Java and Gradle.

> **Signing:** GitHub Actions uses an ephemeral debug key until a repository signing secret is configured. APKs with different signing keys cannot update an existing installation in place. Back up settings before switching from an AI Studio build.

## Responsible usage

DomDeck is a launcher. Games, firmware, emulators, and their licenses remain the responsibility of their respective owners. External artwork and descriptions are credited to their respective sources; being publicly accessible does not imply that every asset is freely redistributable.

## Development

This repository is the continuation of the original Google AI Studio Android project. Core functionality should be preserved when adjusting the UI. Experimental changes should be isolated and reverted if unsuccessful.

No open-source license has been selected for this repository yet.
