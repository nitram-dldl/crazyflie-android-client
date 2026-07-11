# Rapport de référence (baseline)

> Commit de référence : `d629c79` (`master`, 11 juillet 2026)
>
> Vérification logicielle reproduite depuis `maintenance/docs` le 11 juillet 2026 ; cette branche ne modifie pas le code applicatif par rapport à `master`.
>
> Aucun test sur téléphone ou Crazyflie réel n'a été réalisé.

## 1. Structure actuelle du projet

- Module Android unique à la racine, sans dossier `app/`.
- Manifeste : `AndroidManifest.xml` à la racine.
- Code : 61 fichiers Java sous `src/`.
- Ressources Android : 76 fichiers sous `res/` ; aucun dossier `assets/` n'est versionné.
- Paquets principaux : application dans `se.bitcraze.crazyfliecontrol2`, BLE/contrôleurs/préférences/bootloader dans `se.bitcraze.crazyfliecontrol`, protocole et pilotes dans `se.bitcraze.crazyflie.lib`.
- Aucun répertoire de tests unitaires ou instrumentés n'est versionné.

## 2. Chaîne de compilation détectée

- Java : Eclipse Adoptium 17.0.19 (`/home/martin/Android/jdk-17`).
- Gradle Wrapper : 8.11.1.
- Android Gradle Plugin : 8.9.1.
- SDK Android installé : plateforme android-33 et Build Tools 35.0.0.
- Configuration Android : `minSdkVersion 26`, `compileSdk 33`, `targetSdkVersion 34`.

## 3. Incohérences Gradle et SDK

- `compileSdk 33` est inférieur à `targetSdkVersion 34`. Le projet compile, mais ne peut pas référencer ni vérifier complètement les API ajoutées en 34.
- Le seul SDK de plateforme installé est android-33 ; une future cible 35 nécessitera l'installation de la plateforme correspondante.
- `lintOptions`, `packagingOptions`, RenderScript et l'activation implicite de `BuildConfig` sont obsolètes ou annoncés comme prochainement supprimés.
- Le build `release` utilise actuellement la signature debug ; il ne doit pas être distribué comme version de production.
- `JAVA_HOME` et le `PATH` Java doivent être définis explicitement dans le terminal actuel.

## 4. Fonctionnement actuel de `registerReceiver`

Sur `master`, `MainActivity.onCreate()` est annoté `@RequiresApi(TIRAMISU)` alors que le minimum annoncé est l'API 26. Un filtre unique regroupe l'action privée `<package>.USB_PERMISSION` et les actions système `ACTION_USB_DEVICE_ATTACHED`/`DETACHED`, puis appelle inconditionnellement `registerReceiver()` avec `RECEIVER_EXPORTED`, drapeau introduit en API 33. La surcharge à trois arguments existe depuis l'API 26, mais le comportement du nouveau drapeau n'est pas explicitement adapté ni testé sur les API 26 à 32. `onDestroy()` appelle ensuite `unregisterReceiver()` sans mémoriser si l'enregistrement a réussi.

Le correctif est isolé dans `fix/android-receiver-registration` et doit encore être testé sur les versions Android ciblées, notamment avec branchement et retrait d'un Crazyradio USB.

## 5. Permissions Bluetooth utilisées

- API 26 à 30 : `BLUETOOTH`, `BLUETOOTH_ADMIN` et `ACCESS_COARSE_LOCATION`, toutes bornées à `maxSdkVersion="30"` dans le manifeste. `ACCESS_FINE_LOCATION`, nécessaire aux résultats de scan sur Android 10–11 avec cette cible SDK, est absente.
- API 31 et suivantes : `BLUETOOTH_SCAN` avec `neverForLocation` et `BLUETOOTH_CONNECT`.
- Sur `master`, les deux permissions Android 12+ sont demandées successivement et le résultat final repasse par le contrôle GPS malgré `neverForLocation`.
- Android Lint relève 22 appels BLE sans contrôle local de permission ou gestion explicite de `SecurityException`, principalement dans `BleLink.java`.

## 6. Risques de compatibilité Android 8 à 16

- API 26 à 32 : compatibilité du drapeau `RECEIVER_EXPORTED` non explicitement gérée et masquée par l'annotation API 33 placée sur tout `onCreate()`.
- API 29 à 30 : absence de `ACCESS_FINE_LOCATION`, avec un risque de scan sans résultats.
- API 31 et suivantes : parcours de permissions BLE fragile et exigence GPS inutile après autorisation.
- Toutes versions : les chutes entre les `case` de permissions peuvent déclencher des traitements qui ne correspondent pas à la demande reçue.
- API récentes : les 22 erreurs Lint `MissingPermission` indiquent un risque de `SecurityException` si une permission Bluetooth est révoquée pendant l'utilisation.
- Android 15/16 : comportement edge-to-edge non encore contrôlé avec les thèmes Holo et le mode immersif.
- Matériel réel : scan, connexion GATT, reconnexion, télémétrie et arrêt des commandes n'ont pas encore été validés.

## 7. Chutes dans `onRequestPermissionsResult`

Les trois branches de `onRequestPermissionsResult()` n'ont aucun `break`. Une réponse à la permission de localisation tombe donc dans les traitements Bluetooth scan puis connect ; une réponse au scan tombe dans celui de connexion. Le correctif du flux est isolé dans `fix/android-bluetooth-permissions`.

## 8. Emplacement du code de mise à jour du firmware

- Activité et téléchargement : `src/se/bitcraze/crazyfliecontrol/bootloader/`.
- Protocole de bootloader : `src/se/bitcraze/crazyflie/lib/bootloader/`.
- Entrée utilisateur : préférence dans `res/xml/preferences.xml` lançant `BootloaderActivity`.
- Déclaration : `BootloaderActivity` reste présente dans `AndroidManifest.xml`.

La fonctionnalité est donc encore accessible et n'est pas désactivée dans cette baseline.

## 9. Commandes exactes pour compiler l'APK debug

```bash
chmod +x gradlew
export JAVA_HOME=/home/martin/Android/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --version
./gradlew assembleDebug lint --stacktrace
```

Résultat reproduit le 11 juillet 2026 :

```text
Gradle 8.11.1
Launcher JVM: 17.0.19 (Eclipse Adoptium 17.0.19+10)
> Task :assembleDebug
> Task :lint
BUILD SUCCESSFUL
```

Le succès de la tâche `lint` ne signifie pas que le rapport est vide : `abortOnError false` est configuré. `build/reports/lint-results-debug.xml` contient 146 constats : 23 erreurs (22 `MissingPermission`, 1 `MissingSuperCall` dans `SliderPreference.java`) et 123 avertissements. Ces nombres constituent la baseline à ne pas dépasser. Les avertissements de configuration observés concernent notamment BuildConfig et RenderScript.

## 10. Résultats sur appareil réel

- Téléphone / version Android : non testé.
- Version du firmware du Crazyflie testé : non testé.
- L'application démarre : non testé.
- Demande des permissions Bluetooth : non testée.
- Détection du Crazyflie (sans hélices) : non testée.
- Lecture des paramètres / affichage batterie (issue #96) : non testée.
- Premier crash rencontré : aucun résultat disponible.

## 11. Cinq premiers correctifs ordonnés

1. Corriger l'enregistrement et la désinscription des BroadcastReceiver (`fix/android-receiver-registration`).
2. Corriger le flux des permissions Bluetooth (`fix/android-bluetooth-permissions`).
3. Désactiver la mise à jour firmware mobile cassée (`safety/disable-mobile-firmware-update`).
4. Aligner `compileSdk` et `targetSdk`, puis traiter les changements Android 15 (`build/target-api-35`).
5. Protéger les appels BLE signalés par les 22 erreurs Lint `MissingPermission`, puis fiabiliser le cycle scan/connexion/déconnexion.
