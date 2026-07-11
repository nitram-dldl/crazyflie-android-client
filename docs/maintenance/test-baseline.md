# Rapport de référence (baseline) — à remplir en Phase 0

> Commit testé : `d629c79` (master, 11 juillet 2026)
> Ne rien corriger pendant cette phase. Consigner uniquement les faits observés.

## 1. Structure actuelle du projet

_À remplir._

## 2. Chaîne de compilation détectée

- Java :
- Gradle Wrapper :
- Android Gradle Plugin :
- SDK installés :

## 3. Incohérences Gradle et SDK

_À remplir (rappel connu : `compileSdk 33` < `targetSdk 34`)._

## 4. Fonctionnement actuel de `registerReceiver`

_À remplir (rappel connu : appel unique `MainActivity.java:177` avec `RECEIVER_EXPORTED`, filtre mélangeant action privée USB_PERMISSION et événements USB système)._

## 5. Permissions Bluetooth utilisées

_À remplir._

## 6. Risques de compatibilité Android 8 à 16

_À remplir._

## 7. Chutes dans `onRequestPermissionsResult`

_À remplir (rappel connu : aucun `break` entre les trois `case`, `MainActivity.java:364-401`)._

## 8. Emplacement du code de mise à jour du firmware

_À remplir (point de départ : `src/se/bitcraze/crazyfliecontrol/bootloader/`)._

## 9. Commandes exactes pour compiler l'APK debug

```bash
chmod +x gradlew
./gradlew --version
./gradlew clean assembleDebug --stacktrace
```

Sortie complète de la première compilation :

```text
(coller ici)
```

## 10. Résultats sur appareil réel

- Téléphone / version Android :
- Version du firmware du Crazyflie testé :
- L'application démarre :
- Demande des permissions Bluetooth :
- Détection du Crazyflie (sans hélices) :
- Lecture des paramètres / affichage batterie (issue #96) :
- Premier crash rencontré :

## 11. Cinq premiers correctifs ordonnés

1.
2.
3.
4.
5.
