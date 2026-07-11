# Changelog — fork de maintenance

Toutes les modifications notables de ce fork sont consignées ici.
Base amont : `master` de bitcraze/crazyflie-android-client, commit `d629c79` (0.7.6, versionCode 22).

## [Non publié] — 0.7.7-maintenance (nom de travail)

### Ajouté

- `plan.md` : plan de maintenance révisé (11 juillet 2026).
- `docs/maintenance/` : documentation de suivi (baseline, problèmes connus, permissions, matrice de tests).

### Corrigé (branches en attente de test sur appareil réel)

- `fix/android-receiver-registration` (commit `d01c64c`) : enregistrement des BroadcastReceiver compatible Android 8–16, récepteur privé USB non exporté, garde-fou sur `unregisterReceiver()`. Compilé, Lint sans nouvelle erreur. Réf. issue #99.
- `fix/android-bluetooth-permissions` (commit `94dbaa3`) : demande groupée `BLUETOOTH_SCAN`+`BLUETOOTH_CONNECT`, `break` manquants dans `onRequestPermissionsResult()`, suppression de l'exigence GPS sur Android 12+, dialogue d'aide en cas de refus permanent. Compilé, Lint sans nouvelle erreur.

### En cours

- Phase 0 : établissement de la référence (baseline) — voir `docs/maintenance/test-baseline.md`. La compilation (`assembleDebug`, JDK 17, plateforme android-33) est validée ; restent les tests sur téléphone réel.
