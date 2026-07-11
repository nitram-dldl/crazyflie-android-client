# Règles pour l'agent de développement

Tu travailles sur un fork du projet GPL Crazyflie Android Client.

Objectif prioritaire :
stabiliser le client existant sur Android 14 à 16 pour un Crazyflie 2.0 en BLE.
Le plan complet est dans `plan.md` à la racine ; l'état des problèmes connus est dans `docs/maintenance/known-issues.md`.

Règles obligatoires :

1. Inspecte le code existant avant toute modification.
2. Ne réécris pas l'application.
3. Ne migre pas vers Kotlin ou Compose sans demande explicite.
4. Préserve le fonctionnement BLE existant.
5. Ne modifie pas le protocole Crazyflie sans justification documentée.
6. Désactive la mise à jour du firmware mobile plutôt que de tenter de la réparer.
7. Une tâche et une branche par correctif.
8. Fais de petits commits compréhensibles, sans aucun trailer de co-auteur ni mention d'outil d'assistance.
9. Compile après chaque modification significative.
10. Lance les tests et Lint avant de considérer une tâche terminée.
11. N'ajoute aucune clé, aucun secret et aucun APK signé de production.
12. Ne supprime pas une fonctionnalité sans documenter la décision.
13. Toute API Android récente doit avoir un chemin compatible avec minSdk 26.
14. Toute modification de permission doit être testée après une installation propre.
15. Toute modification BLE doit être testée sur un appareil Android réel.
16. La sécurité de vol est prioritaire : commandes à zéro au démarrage, à la perte de connexion et à la fermeture.
17. Ne lance aucune commande Git destructive.
18. Ne fusionne pas les branches automatiquement.
19. Mets à jour CHANGELOG.md et la documentation concernée.
20. Respecte le style du code existant : conventions AOSP, indentation 4 espaces, champs préfixés `m`.
21. À la fin de chaque tâche, fournis :
    - les fichiers modifiés ;
    - la raison de chaque modification ;
    - les commandes exécutées ;
    - les tests réussis ;
    - les tests non réalisés ;
    - les risques restants.
