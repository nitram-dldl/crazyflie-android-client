# Changelog — fork de maintenance

Toutes les modifications notables de ce fork sont consignées ici.
Base amont : `master` de bitcraze/crazyflie-android-client, commit `d629c79` (0.7.6, versionCode 22).

## [Non publié] — 0.7.7-maintenance (nom de travail)

### Ajouté

- `plan.md` : plan de maintenance révisé (11 juillet 2026).
- `docs/maintenance/` : documentation de suivi (baseline, problèmes connus, permissions, matrice de tests).

### Corrigé (branches en attente de test sur appareil réel)

- `fix/android-receiver-registration` (commit `d01c64c`) : enregistrement des BroadcastReceiver compatible Android 8–16, récepteur privé USB non exporté, garde-fou sur `unregisterReceiver()`. Compilation validée ; test sur appareil réel requis. Réf. issue #99.
- `fix/android-bluetooth-permissions` (commit `94dbaa3`) : demande groupée `BLUETOOTH_SCAN`+`BLUETOOTH_CONNECT`, `break` manquants dans `onRequestPermissionsResult()`, suppression de l'exigence GPS sur Android 12+, dialogue d'aide en cas de refus permanent. Compilation validée ; test sur appareil réel requis.

Ces deux branches sont indépendantes et partent de `master`. Elles ne constituent pas encore une version intégrée contenant les deux correctifs.

### En cours

- Phase 0 : référence logicielle établie — voir `docs/maintenance/test-baseline.md`. `assembleDebug` et `lint` terminent avec succès avec JDK 17 et la plateforme android-33. La baseline Lint contient 23 erreurs et 123 avertissements existants ; aucun test sur téléphone réel n'a encore été réalisé.
