# Problèmes connus — état au 11 juillet 2026

Constats vérifiés sur `master` (commit `d629c79`) et issues amont associées.

## Confirmés dans le code

| # | Problème | Emplacement | Issue amont | Branche prévue |
|---|---|---|---|---|
| 1 | `onCreate()` annoté `@RequiresApi(TIRAMISU)` alors que `minSdk 26` | `MainActivity.java:129` | #99 | `fix/android-receiver-registration` |
| 2 | `registerReceiver` unique avec `RECEIVER_EXPORTED` mélangeant action privée `USB_PERMISSION` et événements USB système | `MainActivity.java:173-177` | #99 | `fix/android-receiver-registration` |
| 3 | Aucun `break` entre les `case` de `onRequestPermissionsResult()` | `MainActivity.java:364-401` | — | `fix/android-bluetooth-permissions` |
| 4 | `checkLocationSettings()` exige le GPS après l'octroi de `BLUETOOTH_CONNECT` sur Android 12+ malgré `neverForLocation` | `MainActivity.java:393, 433` | — | `fix/android-bluetooth-permissions` |
| 5 | `compileSdk 33` < `targetSdk 34` | `build.gradle` | — | `build/target-api-35` |
| 6 | Permissions stockage et `DOWNLOAD_WITHOUT_NOTIFICATION` sans `maxSdkVersion` | `AndroidManifest.xml` | — | nettoyage manifeste |
| 7 | Mise à jour du firmware cassée | `bootloader/` | #102 | `safety/disable-mobile-firmware-update` |

## À vérifier en Phase 0

| # | Problème | Issue amont |
|---|---|---|
| 8 | Structure des paramètres incompatible avec firmwares récents (batterie, LED, buzzer concernés) | #96, #86 |
| 9 | Affichage edge-to-edge lors du passage à `targetSdk 35` (mode immersif, thèmes Holo) | — |

## Non-problèmes (vérifiés)

- Le `PendingIntent` de permission USB utilise déjà `FLAG_IMMUTABLE` (`UsbLinkAndroid.java:94`).
- `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_COARSE_LOCATION` sont déjà bornées `maxSdkVersion="30"` dans le manifeste.
- `BLUETOOTH_SCAN` déclare déjà `neverForLocation`.
