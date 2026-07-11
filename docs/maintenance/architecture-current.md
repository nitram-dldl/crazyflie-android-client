# Architecture actuelle — à compléter en Phase 0

## Vue d'ensemble

- Module unique à la racine (pas de dossier `app/`), sources dans `src/`, ressources dans `res/`, manifeste à la racine.
- Langage : Java. UI : Activities classiques, thèmes Holo pour les préférences et le bootloader.

## Paquets principaux

| Paquet | Rôle |
|---|---|
| `se.bitcraze.crazyfliecontrol2` | `MainActivity`, `MainPresenter`, `UsbLinkAndroid`, `FlightDataView` |
| `se.bitcraze.crazyfliecontrol.controller` | contrôleurs tactile, gamepad, gyroscope |
| `se.bitcraze.crazyfliecontrol.ble` | `BleLink` (connexion BLE) |
| `se.bitcraze.crazyfliecontrol.prefs` | préférences |
| `se.bitcraze.crazyfliecontrol.bootloader` | mise à jour firmware (à désactiver) |
| `se.bitcraze.crazyflie.lib` | bibliothèque de communication embarquée (CRTP, radio, param, log) |
| `com.MobileAnarchy.Android.Widgets.Joystick` | widget joystick embarqué |

## Diagrammes et flux

_À compléter : cycle de vie, flux de connexion, threading._
