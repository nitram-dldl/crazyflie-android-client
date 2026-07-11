# Permissions — état et cible

## Déclarées dans le manifeste (état au 11 juillet 2026)

| Permission | Borne | Statut |
|---|---|---|
| `BLUETOOTH` | `maxSdkVersion 30` | correct |
| `BLUETOOTH_ADMIN` | `maxSdkVersion 30` | correct |
| `ACCESS_COARSE_LOCATION` | `maxSdkVersion 30` | insuffisant sur Android 10–11 |
| `ACCESS_FINE_LOCATION` | absente | à ajouter avec `maxSdkVersion 30` pour Android 10–11 |
| `BLUETOOTH_SCAN` (`neverForLocation`) | — | correct |
| `BLUETOOTH_CONNECT` | — | correct |
| `INTERNET` | — | à justifier (firmware downloader ?) |
| `ACCESS_NETWORK_STATE` | — | à justifier |
| `WRITE_EXTERNAL_STORAGE` | aucune | obsolète, à borner/supprimer |
| `READ_EXTERNAL_STORAGE` | aucune | obsolète, à borner/supprimer |
| `DOWNLOAD_WITHOUT_NOTIFICATION` | aucune | liée au firmware downloader, à retirer avec lui |
| `WAKE_LOCK` | — | à vérifier |

## Flux d'exécution (runtime)

Problèmes confirmés : chute entre les `case` de `onRequestPermissionsResult()` (`MainActivity.java:364-401`), dialogue GPS imposé après `BLUETOOTH_CONNECT` sur Android 12+ (`checkLocationSettings()`) et absence de `ACCESS_FINE_LOCATION` requise pour recevoir les résultats de scan sur Android 10–11 avec cette cible SDK.

## Scénarios de test (Phase 2)

1. autoriser dès la première demande ;
2. refuser puis réessayer ;
3. refuser définitivement (« Ne plus demander ») ;
4. autoriser depuis les paramètres Android ;
5. désactiver puis réactiver le Bluetooth ;
6. fermer puis relancer l'application ;
7. mettre le téléphone en veille puis le réveiller.
