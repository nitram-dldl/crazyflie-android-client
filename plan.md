# Plan de maintenance — Crazyflie Android Client

> **Projet :** remise à niveau de l’application Android officielle Crazyflie
>
> **Matériel cible principal :** Crazyflie 2.0
>
> **Téléphone cible principal :** Android 16
>
> **Environnement de développement :** Zorin OS + VS Code
>
> **Date du plan :** 11 juillet 2026 (révisé le même jour après vérification du code de `master`, commit `d629c79`)
>
> **Dépôt amont :** https://github.com/bitcraze/crazyflie-android-client
>
> **Fork de travail :** https://github.com/nitram-dldl/crazyflie-android-client
>
> **Licence :** GPL-2.0-or-later

---

## 1. Objectif du projet

Créer une version de maintenance fiable du client Android Crazyflie permettant de :

- démarrer sans crash sur Android 14, 15 et 16 ;
- détecter un Crazyflie 2.0 en Bluetooth Low Energy ;
- demander correctement les autorisations Bluetooth ;
- se connecter et se déconnecter proprement ;
- piloter le drone avec les commandes tactiles ;
- afficher la batterie et la qualité de la liaison ;
- conserver, si possible, la compatibilité Crazyradio USB-OTG ;
- produire un APK de test installable manuellement ;
- proposer ensuite les correctifs à Bitcraze sous forme de petites pull requests.

Nom de travail de la première version :

```text
0.7.7-maintenance
```

Ce numéro est provisoire. Bitcraze choisira le numéro officiel en cas d’intégration.

---

## 2. Stratégie générale

Ne pas réécrire immédiatement l’application.

Le projet existant est ancien, entièrement en Java et utilise une structure Android historique. Une migration complète vers Kotlin, Compose ou une nouvelle architecture augmenterait fortement le risque de casser la communication avec le drone.

L’ordre retenu est donc :

1. construire le projet actuel sans modification ;
2. reproduire les problèmes sur Android 16 ;
3. corriger uniquement les bugs bloquants ;
4. fiabiliser le Bluetooth et le cycle de vie Android ;
5. ajouter des tests et une CI ;
6. moderniser progressivement le projet ;
7. envisager seulement ensuite une évolution graphique.

---

## 3. Périmètre de la première version

### Inclus dans la version de maintenance

- Crazyflie 2.0 et, sans garantie initiale, Crazyflie 2.1 classique ;
- connexion Bluetooth Low Energy ;
- pilotage tactile ;
- connexion et déconnexion manuelles ;
- batterie et qualité de liaison ;
- paramètres de sensibilité existants ;
- compatibilité Android 8 à Android 16 dans la mesure du possible ;
- compilation avec une chaîne Android moderne ;
- APK debug signé localement ;
- journalisation utile des erreurs.

### Hors périmètre initial

- Crazyflie 2.1 Brushless ;
- nouvelle interface complète ;
- migration générale vers Kotlin ;
- Jetpack Compose ;
- contrôle autonome ;
- essaim de drones ;
- réécriture de la bibliothèque de communication ;
- publication immédiate sur Google Play ;
- mise à jour du firmware depuis le téléphone.

### Décision de sécurité

La fonction de mise à jour du firmware doit être **désactivée ou clairement marquée comme indisponible** dans la première version. Bitcraze indique que cette fonction est actuellement cassée depuis une modification de la pile Bluetooth. L’issue GitHub `#102` (« Firmware update broken — Android 16, Crazyradio 2 ») confirme que la panne touche aussi Android 16 avec un Crazyradio 2.

Les mises à jour du firmware devront être réalisées avec le client Crazyflie sur ordinateur tant que cette partie n’a pas été réétudiée et testée séparément.

---

## 4. État technique constaté

Au 11 juillet 2026, la branche `master` (commit `d629c79`) présente notamment les caractéristiques suivantes — **toutes vérifiées sur le code** :

- langage : Java ;
- version applicative : `versionName 0.7.6`, `versionCode 22` ;
- Android Gradle Plugin : `8.9.1` ;
- Gradle Wrapper : `8.11.1` ;
- `minSdkVersion 26` ;
- `targetSdkVersion 34` ;
- `compileSdk 33` ;
- structure historique sans module `app/` standard (sources dans `src/`, manifeste à la racine) ;
- `settings.gradle` vide, module unique à la racine ;
- dépendances anciennes : Jackson `2.6.3`, SLF4J Android `1.6.1-RC1`, `legacy-support-v4` avec Jetifier activé ;
- `useLibrary 'org.apache.http.legacy'` (utilisé par le téléchargeur de firmware) ;
- autorisations `BLUETOOTH_SCAN` (avec `neverForLocation`) et `BLUETOOTH_CONNECT` présentes ;
- `BLUETOOTH`, `BLUETOOTH_ADMIN` et `ACCESS_COARSE_LOCATION` déjà bornées avec `maxSdkVersion="30"` ;
- `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE` et `DOWNLOAD_WITHOUT_NOTIFICATION` **sans** borne `maxSdkVersion` ;
- le `PendingIntent` de permission USB utilise déjà `FLAG_IMMUTABLE` (`UsbLinkAndroid.java:94`) — pas de crash à corriger de ce côté ;
- attribut `package=` encore présent dans le manifeste alors que `namespace` est défini dans `build.gradle` (avertissement AGP 8) ;
- thèmes Holo encore utilisés (`PreferencesActivity`, `BootloaderActivity`) ;
- code de mise à jour du firmware toujours présent ;
- **aucune CI** : pas de dossier `.github/workflows` ;
- dernier paquet GitHub publié : `v0.7.4` (décembre 2023), en retard sur `master` (0.7.6).

Le dépôt indique officiellement :

- prise en charge du Crazyflie 2.0 et du Crazyflie 2.1 classique ;
- connexion BLE ou Crazyradio USB-OTG ;
- absence de prise en charge du Crazyflie 2.1 Brushless.

---

## 5. Points critiques déjà identifiés

### 5.1 Récepteurs Android dynamiques

L’issue GitHub `#99` (toujours ouverte) documente un crash sur Android 14 et 15 :

```text
SecurityException:
One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified
```

La branche `master` contient déjà un appel avec `RECEIVER_EXPORTED` (`MainActivity.java:177`), mais cette correction doit être revue.

Le même filtre mélange actuellement :

- une action privée à l’application pour l’autorisation USB ;
- des diffusions système pour connexion et déconnexion USB.

La solution propre consiste à séparer les récepteurs :

- action privée de permission USB : `RECEIVER_NOT_EXPORTED` ;
- événements USB système : `RECEIVER_EXPORTED` lorsque nécessaire ;
- ancien appel sans drapeau sur les versions Android antérieures à l’API qui introduit ces drapeaux.

Cela évite également de rendre inutilement accessible un récepteur privé.

### 5.2 Compatibilité avec Android inférieur à 13

La surcharge à trois arguments de `registerReceiver()` existe depuis l'API 26, mais le code actuel emploie `RECEIVER_EXPORTED` (introduit en API 33) sans garde de version et annote tout `onCreate()` avec `@RequiresApi(TIRAMISU)` (`MainActivity.java:129`) alors que le projet annonce `minSdkVersion 26`. Confirmé sur `master`.

Il faut :

- retirer l’exigence Android 13 sur tout `onCreate()` ;
- utiliser un test sur `Build.VERSION.SDK_INT` ;
- conserver un chemin compatible avec Android 8 à 12.

### 5.3 Autorisations Bluetooth

Sur Android 12 et versions suivantes :

- `BLUETOOTH_SCAN` est nécessaire pour rechercher le drone ;
- `BLUETOOTH_CONNECT` est nécessaire pour communiquer avec lui ;
- ces autorisations doivent être demandées à l’exécution ;
- la permission de localisation ne doit pas être exigée sur Android récent lorsque l’application déclare `neverForLocation`.

Le flux actuel doit être audité, notamment :

- `ACCESS_FINE_LOCATION` pour recevoir les résultats de scan sur Android 10–11, bornée à `maxSdkVersion="30"` ;
- demande des deux permissions ;
- refus temporaire ;
- refus permanent ;
- retour depuis les paramètres ;
- reconnexion après mise en veille ;
- absence de demande GPS inutile sous Android 12 et versions suivantes.

### 5.4 Chute entre les blocs du `switch` — **confirmé**

Vérifié dans `MainActivity.java` (lignes 364–401) : aucun des trois `case` de `onRequestPermissionsResult()` ne contient de `break` ni de retour explicite. La chute entre les blocs est donc réelle, pas seulement suspectée.

Conséquence concrète : un résultat `MY_PERMISSIONS_REQUEST_LOCATION` accordé exécute ensuite le bloc `MY_PERMISSIONS_REQUEST_BLUETOOTH` (qui relance une demande `BLUETOOTH_CONNECT`) puis le bloc `MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT` (qui appelle `checkLocationSettings()`).

Risque :

- exécution du traitement du cas suivant ;
- messages d’erreur incohérents ;
- lancement de plusieurs demandes de permission ;
- comportement imprévisible.

### 5.5 Cycle de vie

Le projet se déconnecte dans `onPause()`. Il faut vérifier le comportement lors :

- de l’ouverture du panneau de permissions ;
- du passage dans les paramètres Android ;
- d’un appel ou d’une notification ;
- de la mise en veille ;
- d’une rotation ou d’un changement de fenêtre ;
- de la fermeture forcée de l’application.

### 5.6 Configuration de compilation incohérente

`compileSdk 33` est inférieur à `targetSdkVersion 34`.

La première remise à niveau doit utiliser au minimum :

```text
compileSdk 35
targetSdk 35
minSdk 26
```

Android 15, API 35, est le niveau actuellement requis pour publier une mise à jour classique sur Google Play. L’application devra malgré tout être testée sur Android 16, API 36.

Ne pas passer directement à une API supérieure sans compiler et tester chaque étape.

**Attention (ajout vérifié) :** cibler l’API 35 impose l’affichage *edge-to-edge* (dessin sous les barres système). L’application utilise un mode immersif basé sur `setSystemUiVisibility()` (API dépréciée) et des thèmes Holo : l’interface devra être contrôlée visuellement après le passage à `targetSdk 35`, avec au besoin l’option de retrait temporaire `windowOptOutEdgeToEdgeEnforcement`.

### 5.7 Dialogue GPS imposé même sur Android 12+ — **oubli du plan initial, confirmé**

Dans le flux actuel, une fois `BLUETOOTH_CONNECT` accordée, le code appelle `checkLocationSettings()` (`MainActivity.java:393` et `:433`). Cette méthode exige que le fournisseur GPS soit activé et affiche un dialogue « Location Access », alors que le manifeste déclare `neverForLocation` et que la localisation n’est pas requise pour le scan BLE sur Android 12+.

Correction attendue : sur API ≥ 31, appeler directement `connectBle()` après l’octroi des permissions Bluetooth ; réserver `checkLocationSettings()` au chemin API < 31.

### 5.8 Paramètres et logging incompatibles avec les firmwares récents — **oubli du plan initial**

Les issues GitHub `#96` (« Latest parameters structure doesn't work anymore ») et `#86` (« Support new logging and param API ») indiquent que la lecture du TOC des paramètres échoue avec les firmwares Crazyflie récents.

Impact direct sur les objectifs de la première version : l’affichage de la batterie, la qualité de liaison et les boutons LED/buzzer dépendent de cette lecture. Réparer le démarrage et le BLE ne suffira donc peut-être pas.

À faire :

- noter la version du firmware du drone dès la Phase 0 ;
- vérifier explicitement la lecture des paramètres pendant la baseline ;
- si le problème est confirmé, l’ajouter comme phase corrective dédiée (bibliothèque `se.bitcraze.crazyflie.lib`), sans le mélanger aux correctifs Android.

---

## 6. Mise en place sur Zorin OS

### 6.1 Paquets de base

```bash
sudo apt update
sudo apt install -y git adb openjdk-17-jdk unzip
```

Vérifier Java :

```bash
java -version
javac -version
```

La version attendue est Java 17.

### 6.2 Android SDK

Le code peut être édité dans VS Code, mais le SDK Android doit être installé.

Deux possibilités :

#### Option recommandée

Installer Android Studio uniquement pour gérer :

- Android SDK ;
- Build Tools ;
- Platform Tools ;
- émulateurs éventuels.

Le développement quotidien peut rester dans VS Code.

#### Option entièrement en ligne de commande

Installer les Android Command Line Tools puis définir :

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
```

Ajouter ces lignes dans `~/.bashrc`, puis :

```bash
source ~/.bashrc
sdkmanager --licenses
sdkmanager \
  "platform-tools" \
  "platforms;android-35" \
  "platforms;android-36" \
  "build-tools;35.0.0"
```

### 6.3 VS Code

Extensions utiles :

- Extension Pack for Java ;
- Gradle for Java ;
- XML ;
- GitLens, facultatif ;
- Error Lens, facultatif.

Ne pas installer plusieurs extensions Android non maintenues qui lancent leur propre système de compilation.

### 6.4 Téléphone Android

Activer :

1. les options développeur ;
2. le débogage USB ;
3. l’installation par USB, si le constructeur le demande.

Vérifier la connexion :

```bash
adb devices
```

Le téléphone doit apparaître avec l’état :

```text
device
```

---

## 7. Création du fork et branches Git

**État au 11 juillet 2026 :** le dépôt et le fork GitHub du compte `nitram-dldl` sont configurés. `origin` pointe vers le fork et `upstream` vers Bitcraze. `master`, `maintenance/docs`, `fix/android-receiver-registration` et `fix/android-bluetooth-permissions` existent sur `origin`.

Vérification :

```bash
git remote -v
git branch -a -vv
```

Organisation des branches retenue :

- `master` : miroir strict de `upstream/master`, jamais modifié directement ;
- `maintenance/docs` : plan, documentation de maintenance et fichiers de suivi (jamais proposée telle quelle à Bitcraze) ;
- une branche par correctif, créée depuis `master`, destinée aux pull requests amont.

Synchronisation régulière :

```bash
git fetch upstream
git checkout master
git pull --ff-only upstream master
```

Créer une branche par correctif.

Exemples :

```bash
git checkout -b fix/android-receiver-registration
git checkout -b fix/android-bluetooth-permissions
git checkout -b build/target-api-35
git checkout -b safety/disable-mobile-firmware-update
```

Ne pas regrouper toute la modernisation dans une seule branche.

### Consignes de contribution Bitcraze (CONTRIBUTING.md et bitcraze.io/development/contribute)

- pull requests courtes, simples, ciblées et testées — une fonctionnalité par PR ; « si la description contient "et", envisager deux PR » ;
- le **titre de la PR apparaît dans les notes de version** : il doit être descriptif ;
- ajouter une description courte et référencer les issues concernées (`#99`, `#96`, `#102`…) ;
- si un sujet nécessite plusieurs PR, les relier par une issue GitHub commune ;
- baser chaque PR sur un point récent de `master` pour limiter les conflits ;
- indiquer les appareils et versions Android testés ;
- respecter le style existant : conventions AOSP, indentation 4 espaces, champs préfixés `m` ;
- éviter le bruit (pas de reformatage massif dans une PR de correctif) ;
- pas de CLA ; canaux : GitHub Discussions (discussions.bitcraze.io) et contact@bitcraze.io.

---

## 8. Phase 0 — Établir une référence

### But

Conserver une trace exacte du comportement avant modification.

### Tâches

- [x] cloner la branche `master` actuelle ;
- [x] noter le hash du commit testé ;
- [x] lancer la compilation sans modifier le code ;
- [x] enregistrer toutes les erreurs ;
- [ ] installer l’APK sur Android 16 ;
- [ ] capturer les logs de démarrage ;
- [ ] vérifier si l’application démarre ;
- [ ] vérifier l’affichage de la demande Bluetooth ;
- [ ] essayer une détection sans hélices ;
- [ ] noter la version du firmware du Crazyflie testé ;
- [ ] vérifier la lecture des paramètres (TOC) et l’affichage batterie avec ce firmware — issue `#96` ;
- [x] documenter les résultats logiciels dans `docs/maintenance/test-baseline.md` ; les résultats matériels restent à compléter.

### Commandes

```bash
chmod +x gradlew
./gradlew --version
./gradlew clean assembleDebug --stacktrace
```

APK attendu :

```text
build/outputs/apk/debug/
```

Installation :

```bash
adb install -r build/outputs/apk/debug/*.apk
```

Logs filtrés :

```bash
adb logcat -c
adb logcat | grep -iE "crazyflie|androidruntime|bluetooth|securityexception"
```

Informations utiles :

```bash
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell dumpsys package se.bitcraze.crazyfliecontrol2
```

### Critère de sortie

Un document décrit précisément :

- ce qui compile ;
- ce qui ne compile pas ;
- ce qui démarre ;
- le premier crash rencontré ;
- les autorisations demandées ;
- la détection ou non du Crazyflie.

---

## 9. Phase 1 — Corriger le démarrage Android 14–16

### Priorité

P0 — bloquant.

### Tâches

- [ ] retirer `@RequiresApi(TIRAMISU)` de `onCreate()` ;
- [ ] séparer le récepteur privé USB du récepteur USB système ;
- [ ] utiliser `RECEIVER_NOT_EXPORTED` pour l’action privée ;
- [ ] utiliser le drapeau adapté pour les événements système ;
- [ ] conserver un chemin compatible Android 8–12 ;
- [ ] éviter `unregisterReceiver()` sur un récepteur jamais enregistré ;
- [ ] protéger l’enregistrement avec un état booléen ;
- [ ] ajouter des logs explicites ;
- [ ] compiler ;
- [ ] tester sur Android 16 ;
- [ ] ajouter un test de démarrage manuel.

### Résultat attendu

- l’application démarre sans `SecurityException` ;
- aucun crash lors de la fermeture ;
- aucun crash en l’absence de Crazyradio ;
- aucun avertissement critique lié à un récepteur exporté inutilement.

### Branche

```text
fix/android-receiver-registration
```

### Commit suggéré

```text
Fix dynamic receiver registration on modern Android
```

---

## 10. Phase 2 — Corriger le flux des permissions Bluetooth

### Priorité

P0 — bloquant pour le BLE.

### Tâches

- [ ] auditer le manifeste ;
- [ ] conserver les permissions historiques avec `maxSdkVersion="30"` ;
- [ ] ajouter et demander `ACCESS_FINE_LOCATION` sur Android 10–11 ;
- [ ] conserver `BLUETOOTH_SCAN` avec `neverForLocation` ;
- [ ] conserver `BLUETOOTH_CONNECT` ;
- [ ] supprimer la dépendance au GPS sur Android 12 et versions suivantes ;
- [ ] demander `SCAN` et `CONNECT` de manière cohérente ;
- [ ] corriger les chutes entre les `case` de `onRequestPermissionsResult()` ;
- [ ] gérer un refus simple ;
- [ ] gérer « Ne plus demander » ;
- [ ] proposer un accès aux paramètres de l’application en cas de refus permanent ;
- [ ] ne lancer la connexion qu’après validation de toutes les permissions ;
- [ ] éviter les demandes répétées en boucle ;
- [ ] tester la première installation ;
- [ ] tester après suppression des autorisations ;
- [ ] tester après réinstallation.

### Scénarios de test

1. autoriser dès la première demande ;
2. refuser puis réessayer ;
3. refuser définitivement ;
4. autoriser depuis les paramètres Android ;
5. désactiver puis réactiver le Bluetooth ;
6. fermer puis relancer l’application ;
7. mettre le téléphone en veille puis le réveiller.

### Résultat attendu

L’utilisateur comprend toujours pourquoi la connexion n’est pas possible et peut corriger la situation sans réinstaller l’application.

### Branche

```text
fix/android-bluetooth-permissions
```

---

## 11. Phase 3 — Fiabiliser la connexion BLE

### Priorité

P0/P1.

### Tâches

- [ ] identifier les classes responsables du scan BLE ;
- [ ] documenter le chemin complet : scan → sélection → GATT → connexion ;
- [ ] ajouter des délais maximum aux étapes bloquantes ;
- [ ] éviter deux scans simultanés ;
- [ ] empêcher plusieurs tentatives de connexion concurrentes ;
- [ ] annuler proprement le scan lors d’une connexion ;
- [ ] fermer proprement les ressources GATT ;
- [ ] gérer la perte de connexion ;
- [ ] gérer un Crazyflie éteint pendant la connexion ;
- [ ] gérer la sortie de portée ;
- [ ] permettre une reconnexion manuelle ;
- [ ] afficher un état lisible : recherche, connexion, connecté, perdu, erreur ;
- [ ] ne pas afficher uniquement une erreur technique brute ;
- [ ] journaliser le code d’erreur GATT lorsqu’il existe.

### Tests minimums

- [ ] 20 connexions/déconnexions consécutives ;
- [ ] 10 extinctions du drone pendant la connexion ;
- [ ] 5 passages hors de portée ;
- [ ] 10 mises en veille du téléphone ;
- [ ] 10 retours dans l’application ;
- [ ] 15 minutes de connexion continue ;
- [ ] absence de fuite évidente ou de ralentissement progressif.

### Critère de sortie

Au moins 19 cycles sur 20 doivent réussir sans redémarrer l’application.

---

## 12. Phase 4 — Mettre la compilation à niveau

### Priorité

P1.

### Première cible

```groovy
android {
    compileSdk 35

    defaultConfig {
        minSdkVersion 26
        targetSdkVersion 35
    }
}
```

### Tâches

- [ ] aligner `compileSdk` et `targetSdk` ;
- [ ] vérifier la compatibilité Java 17 ;
- [ ] remplacer les syntaxes Gradle obsolètes ;
- [ ] remplacer `lintOptions` par le bloc moderne si nécessaire ;
- [ ] remplacer les anciennes options de packaging si nécessaire ;
- [ ] retirer l’attribut `package=` du manifeste (le `namespace` est déjà défini dans `build.gradle`) ;
- [ ] contrôler l’affichage après passage en *edge-to-edge* imposé par `targetSdk 35` (mode immersif, barres système, thèmes Holo) ;
- [ ] conserver la structure actuelle lors du premier correctif ;
- [ ] créer une configuration release sans utiliser la clé debug ;
- [ ] ne jamais committer de clé de signature ;
- [ ] vérifier `./gradlew lint` ;
- [ ] vérifier `./gradlew assembleDebug` ;
- [ ] vérifier `./gradlew assembleRelease` sans signature de production.

### Important

Ne pas migrer dans la même pull request :

- la structure des dossiers ;
- toutes les dépendances ;
- l’interface ;
- le langage Java vers Kotlin.

Chaque changement doit être isolé pour faciliter le diagnostic.

### Branche

```text
build/target-api-35
```

---

## 13. Phase 5 — Désactiver la mise à jour du firmware mobile

### Priorité

P0 sécurité.

### Tâches

- [ ] identifier tous les accès à `BootloaderActivity` ;
- [ ] masquer ou désactiver l’entrée dans l’interface ;
- [ ] afficher une explication claire ;
- [ ] indiquer d’utiliser le client PC Bitcraze ;
- [ ] empêcher un lancement indirect de l’activité ;
- [ ] ajouter un garde-fou dans l’activité elle-même ;
- [ ] conserver le code pour étude ultérieure, sans l’exécuter ;
- [ ] documenter cette limitation dans le README.

### Message utilisateur suggéré

```text
La mise à jour du firmware depuis Android est temporairement désactivée.
Utilisez le client Crazyflie sur ordinateur pour mettre à jour ou restaurer le drone.
```

### Branche

```text
safety/disable-mobile-firmware-update
```

---

## 14. Phase 6 — Nettoyage du manifeste et sécurité

### Tâches

- [ ] vérifier la nécessité de `INTERNET` ;
- [ ] vérifier la nécessité de `ACCESS_NETWORK_STATE` ;
- [ ] supprimer les permissions de stockage global devenues inutiles ;
- [ ] utiliser uniquement le répertoire privé de l’application ;
- [ ] vérifier `allowBackup` et décider s’il doit rester actif ;
- [ ] vérifier chaque composant `exported` ;
- [ ] protéger les activités qui ne doivent pas être appelées de l’extérieur ;
- [ ] vérifier les `PendingIntent` et leurs drapeaux (celui de `UsbLinkAndroid` est déjà `FLAG_IMMUTABLE` — vérifier qu’il n’en existe pas d’autres) ;
- [ ] retirer `DOWNLOAD_WITHOUT_NOTIFICATION` en même temps que la désactivation du téléchargeur de firmware ;
- [ ] rechercher les API Android obsolètes ;
- [ ] vérifier qu’aucune donnée sensible n’est écrite dans les logs ;
- [ ] vérifier qu’aucun secret ou certificat privé n’est versionné ;
- [ ] lancer Android Lint ;
- [ ] lancer une analyse de dépendances.

### Commandes possibles

```bash
./gradlew lint
./gradlew dependencies
grep -R "WRITE_EXTERNAL_STORAGE\|READ_EXTERNAL_STORAGE" -n .
grep -R "PendingIntent" -n src
grep -R "registerReceiver" -n src
```

---

## 15. Phase 7 — Mise à jour prudente des dépendances

Les versions actuelles de Jackson et SLF4J sont très anciennes.

### Règle

Une famille de dépendances par pull request.

### Ordre recommandé

1. bibliothèques AndroidX ;
2. journalisation ;
3. Jackson ;
4. autres bibliothèques embarquées ;
5. remplacement du code copié ou non maintenu.

### Pour chaque mise à jour

- [ ] lire les changements incompatibles ;
- [ ] compiler ;
- [ ] lancer Lint ;
- [ ] tester le scan BLE ;
- [ ] tester la connexion ;
- [ ] tester batterie et qualité de liaison ;
- [ ] tester les préférences ;
- [ ] vérifier la taille de l’APK ;
- [ ] vérifier les licences.

### Ne pas faire

- mettre toutes les dépendances à jour automatiquement ;
- accepter des modifications massives générées sans comprendre les impacts ;
- mélanger mise à jour de dépendance et refonte graphique.

---

## 16. Phase 8 — Tests automatisés et CI

### Tests unitaires à ajouter

Priorité aux composants ne nécessitant pas de Bluetooth réel :

- conversion tension → pourcentage de batterie ;
- validation des paramètres radio ;
- machine d’état de connexion ;
- gestion des erreurs ;
- calcul des commandes ;
- comportement des permissions ;
- formatage des messages.

### Tests instrumentés

- démarrage de `MainActivity` ;
- navigation vers les préférences ;
- refus et acceptation simulés des permissions ;
- destruction et recréation de l’activité ;
- retour en arrière ;
- absence de crash lorsque le Bluetooth est désactivé.

### CI GitHub Actions

La CI doit au minimum :

```text
- utiliser Java 17 ;
- vérifier le Gradle Wrapper ;
- compiler l’APK debug ;
- lancer les tests unitaires ;
- lancer Android Lint ;
- archiver l’APK comme artefact ;
- ne jamais contenir de clé de signature.
```

### Critère de sortie

Chaque pull request doit être refusée si :

- la compilation échoue ;
- les tests échouent ;
- une nouvelle erreur Lint critique apparaît.

---

## 17. Phase 9 — Améliorations d’interface limitées

Cette phase ne commence qu’après stabilisation du BLE.

### Améliorations utiles

- [ ] écran de connexion plus clair ;
- [ ] état Bluetooth visible ;
- [ ] état du drone visible ;
- [ ] bouton de connexion explicite ;
- [ ] batterie plus lisible ;
- [ ] qualité du lien plus lisible ;
- [ ] messages d’erreur traduisibles ;
- [ ] bouton d’arrêt/déconnexion très visible ;
- [ ] écran empêchant un décollage involontaire ;
- [ ] confirmation du mode de contrôle sélectionné ;
- [ ] adaptation correcte aux écrans modernes ;
- [ ] meilleure accessibilité des boutons.

### Non prioritaire

- animations ;
- refonte graphique complète ;
- thèmes complexes ;
- Compose ;
- prise en charge tablette avancée.

---

## 18. Phase 10 — Crazyradio USB-OTG

Le BLE reste prioritaire pour la première version.

### Tâches ultérieures

- [ ] tester la détection du Crazyradio ;
- [ ] tester l’autorisation USB ;
- [ ] tester connexion et déconnexion à chaud ;
- [ ] tester le retrait du dongle en vol, sans hélices dans un premier temps ;
- [ ] vérifier Crazyradio PA ;
- [ ] vérifier Crazyradio 2.0 ;
- [ ] vérifier les versions de firmware compatibles ;
- [ ] documenter les câbles et adaptateurs OTG testés.

### Critère

Ne pas annoncer la compatibilité Crazyradio tant qu’au moins un matériel réel n’a pas été testé.

---

## 19. Matrice de tests

| Élément | Cible minimale | Priorité |
|---|---:|---:|
| Crazyflie | 2.0 | P0 |
| Connexion | BLE | P0 |
| Android | 16 réel | P0 |
| Android | 15 | P1 |
| Android | 14 | P1 |
| Android | 12 ou 13 | P1 |
| Android | 8 à 11 | P2 |
| Commandes | tactiles | P0 |
| Manette Bluetooth | au moins un modèle | P2 |
| Crazyradio USB | un modèle | P2 |
| Mise à jour firmware Android | désactivée | P0 |
| Lecture params/TOC avec firmware récent (#96) | vérifiée | P1 |

Un émulateur Android ne permet pas de valider réellement le BLE. Les tests de communication doivent être effectués sur un téléphone physique.

---

## 20. Protocole de test sécurisé

### Avant les premiers tests

- retirer les hélices ;
- poser le drone sur une surface stable ;
- maintenir une batterie suffisamment chargée ;
- ne pas lancer de mise à jour du firmware ;
- garder le client PC disponible pour diagnostic ;
- tester d’abord uniquement connexion, télémétrie et déconnexion.

### Tests moteurs

Avant tout test avec hélices :

- vérifier que les axes sont au neutre ;
- vérifier que la poussée démarre à zéro ;
- vérifier le comportement lors d’une perte Bluetooth ;
- vérifier que la déconnexion arrête immédiatement les commandes ;
- tester dans une zone dégagée ;
- éviter la proximité du visage, des enfants et des animaux.

### Condition d’arrêt

Arrêter les tests si :

- les moteurs restent commandés après une déconnexion ;
- la poussée n’est pas nulle au démarrage ;
- l’application se fige pendant le pilotage ;
- la connexion se rétablit avec une commande non nulle ;
- la télémétrie est incohérente ;
- le drone redémarre de manière imprévisible.

---

## 21. Définition de « terminé » pour la première version

La version `0.7.7-maintenance` est considérée comme testable lorsque :

- [ ] le projet compile avec Java 17 ;
- [ ] l’APK s’installe sur Android 16 ;
- [ ] l’application démarre sans crash ;
- [ ] les permissions sont correctement demandées ;
- [ ] le GPS n’est pas exigé inutilement sur Android récent ;
- [ ] le Crazyflie 2.0 est détecté en BLE ;
- [ ] la connexion fonctionne ;
- [ ] la batterie est affichée ;
- [ ] la qualité de liaison est affichée ;
- [ ] la déconnexion est propre ;
- [ ] 19 connexions sur 20 réussissent ;
- [ ] aucun moteur ne reçoit de commande non nulle au démarrage ;
- [ ] la perte de connexion ramène les commandes à zéro ;
- [ ] la mise à jour du firmware mobile est désactivée ;
- [ ] `assembleDebug` réussit ;
- [ ] `lint` ne contient aucune nouvelle erreur critique ;
- [ ] un APK et des notes de test sont produits ;
- [ ] les modifications sont réparties en pull requests ciblées.

---

## 22. Structure documentaire à ajouter

```text
docs/
├── maintenance/
│   ├── architecture-current.md
│   ├── bluetooth-flow.md
│   ├── permissions.md
│   ├── test-baseline.md
│   ├── test-matrix.md
│   ├── known-issues.md
│   └── release-checklist.md
├── screenshots/
└── test-logs/
```

À la racine :

```text
CHANGELOG.md
SECURITY.md
CONTRIBUTING.md   (existe déjà)
plan.md
```

Note : le dépôt contient déjà `RELEASE_CHECKLIST.md` à la racine et un dossier `docs/` (guides utilisateur et développement). La documentation de maintenance vit dans `docs/maintenance/` pour ne pas interférer.

Ne pas placer de données personnelles ou d’identifiants de téléphone dans les logs publiés.

---

## 23. Découpage conseillé des pull requests

### PR 1 — Démarrage Android moderne

```text
Fix dynamic receiver registration on Android 13+
```

Contenu :

- récepteurs séparés ;
- drapeaux exportés corrects ;
- compatibilité Android 8–16 ;
- absence de crash au démarrage et à la fermeture.

### PR 2 — Permissions BLE

```text
Fix Bluetooth runtime permission flow
```

Contenu :

- permissions Android 12+ ;
- correction du `switch` ;
- messages utilisateur ;
- suppression de la demande GPS sur Android récent.

### PR 3 — Niveau d’API

```text
Target Android 15 and align compile SDK
```

Contenu :

- `compileSdk 35` ;
- `targetSdk 35` ;
- corrections Gradle strictement nécessaires.

### PR 4 — Sécurité du firmware

```text
Disable unsupported mobile firmware update
```

Contenu :

- accès masqué ;
- garde-fou ;
- documentation.

### PR 5 — Tests et CI

```text
Add Android build, lint and unit test workflow
```

Contenu :

- GitHub Actions ;
- premiers tests unitaires ;
- artefact APK debug.

### PR suivantes

- fiabilité BLE ;
- nettoyage manifeste ;
- dépendances ;
- interface.

---

## 24. Règles pour l’agent IA dans VS Code

Copier ces règles dans le contexte du projet ou dans un fichier dédié tel que `.github/copilot-instructions.md`.

```text
Tu travailles sur un fork du projet GPL Crazyflie Android Client.

Objectif prioritaire :
stabiliser le client existant sur Android 14 à 16 pour un Crazyflie 2.0 en BLE.

Règles obligatoires :
1. Inspecte le code existant avant toute modification.
2. Ne réécris pas l’application.
3. Ne migre pas vers Kotlin ou Compose sans demande explicite.
4. Préserve le fonctionnement BLE existant.
5. Ne modifie pas le protocole Crazyflie sans justification documentée.
6. Désactive la mise à jour du firmware mobile plutôt que de tenter de la réparer.
7. Une tâche et une branche par correctif.
8. Fais de petits commits compréhensibles.
9. Compile après chaque modification significative.
10. Lance les tests et Lint avant de considérer une tâche terminée.
11. N’ajoute aucune clé, aucun secret et aucun APK signé de production.
12. Ne supprime pas une fonctionnalité sans documenter la décision.
13. Toute API Android récente doit avoir un chemin compatible avec minSdk 26.
14. Toute modification de permission doit être testée après une installation propre.
15. Toute modification BLE doit être testée sur un appareil Android réel.
16. La sécurité de vol est prioritaire : commandes à zéro au démarrage, à la perte de connexion et à la fermeture.
17. Ne lance aucune commande Git destructive.
18. Ne fusionne pas les branches automatiquement.
19. Mets à jour CHANGELOG.md et la documentation concernée.
20. À la fin de chaque tâche, fournis :
   - les fichiers modifiés ;
   - la raison de chaque modification ;
   - les commandes exécutées ;
   - les tests réussis ;
   - les tests non réalisés ;
   - les risques restants.
```

---

## 25. Première mission à donner à l’agent VS Code

```text
Lis entièrement plan.md, README.md, CONTRIBUTING.md, build.gradle,
AndroidManifest.xml et MainActivity.java.

Ne modifie encore aucun fichier.

Produis d’abord un rapport nommé docs/maintenance/test-baseline.md contenant :

1. la structure actuelle du projet ;
2. la chaîne de compilation détectée ;
3. les incohérences Gradle et SDK ;
4. le fonctionnement actuel de registerReceiver ;
5. les permissions Bluetooth utilisées ;
6. les risques de compatibilité Android 8 à 16 ;
7. les chutes potentielles dans onRequestPermissionsResult ;
8. l’emplacement du code de mise à jour du firmware ;
9. les commandes exactes pour compiler l’APK debug ;
10. une liste ordonnée des cinq premiers correctifs.

Ensuite seulement, exécute :
./gradlew clean assembleDebug --stacktrace

Ne corrige rien pendant cette première mission.
Inscris l’erreur complète de compilation dans le rapport.
```

---

## 26. Deuxième mission à donner à l’agent

À utiliser uniquement après validation du rapport de référence :

```text
Crée la branche fix/android-receiver-registration.

Corrige uniquement l’enregistrement et la désinscription des BroadcastReceiver.

Contraintes :
- compatibilité minSdk 26 ;
- aucune annotation imposant Android 13 à toute l’activité ;
- action privée USB non exportée ;
- événements USB système traités avec le drapeau adapté ;
- aucun unregisterReceiver sur un récepteur non enregistré ;
- aucun autre refactoring ;
- compilation obligatoire après modification ;
- documente les scénarios de test Android 16 ;
- ne crée pas de commit tant que la compilation échoue.
```

---

## 27. Risques du projet

| Risque | Impact | Réduction du risque |
|---|---|---|
| Code Android ancien | élevé | changements progressifs |
| Pas de tests actuels suffisants | élevé | baseline, CI et tests ciblés |
| BLE dépendant du téléphone | élevé | tests sur plusieurs appareils |
| Firmware mobile cassé | critique | fonction désactivée |
| Régression Crazyradio | moyen | BLE prioritaire, test USB séparé |
| Mise à jour massive des dépendances | élevé | une dépendance par PR |
| Refonte graphique prématurée | moyen | interface après stabilisation |
| Commande moteur résiduelle | critique | remise à zéro systématique |
| Params/log incompatibles firmware récent (#96, #86) | élevé | vérification en Phase 0, phase corrective dédiée si confirmé |
| Publication non signée par Bitcraze | moyen | APK local puis pull requests |
| Fork difficile à maintenir | moyen | petites PR vers l’amont |

---

## 28. Sources techniques

- [Dépôt officiel](https://github.com/bitcraze/crazyflie-android-client)

- [Issue Android 14–15 `#99`](https://github.com/bitcraze/crazyflie-android-client/issues/99)

- [Issue structure des paramètres `#96`](https://github.com/bitcraze/crazyflie-android-client/issues/96)

- [Issue nouvelle API log/param `#86`](https://github.com/bitcraze/crazyflie-android-client/issues/86)

- [Issue firmware update cassé `#102`](https://github.com/bitcraze/crazyflie-android-client/issues/102)

- [État des applications mobiles selon Bitcraze](https://www.bitcraze.io/2025/04/state-of-the-crazyflie-mobile-apps/)

- Consignes de contribution Bitcraze : [vue d'ensemble](https://www.bitcraze.io/development/contribute/) et [règles générales](https://www.bitcraze.io/development/contribute/general-guidelines/)

- [Permissions Bluetooth Android](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)

- [Exigences Google Play relatives au niveau d’API cible](https://developer.android.com/google/play/requirements/target-sdk)

---

## 29. Prochaine action immédiate

État au 11 juillet 2026 : dépôt et fork configurés, remotes `origin` et `upstream` présents, JDK 17 et SDK android-33 installés, baseline logicielle documentée, branches `maintenance/docs`, `fix/android-receiver-registration` et `fix/android-bluetooth-permissions` disponibles localement et sur `origin`.

1. finaliser et compiler séparément les deux branches de correctif ;
2. tester chaque correctif sur un téléphone Android réel après installation propre, sans hélices ;
3. vérifier le Crazyradio USB sur Android 13 à 16 pour le correctif des récepteurs ;
4. vérifier les scénarios d'autorisation, refus temporaire et refus définitif pour le correctif Bluetooth ;
5. préparer ensuite une version d'intégration contenant les deux correctifs, sans fusion automatique ;
6. créer `safety/disable-mobile-firmware-update` et désactiver effectivement la fonctionnalité cassée ;
7. conserver les résultats et logs matériels dans `docs/maintenance/`, sans identifiant personnel.
