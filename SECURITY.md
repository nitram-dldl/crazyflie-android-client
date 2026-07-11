# Sécurité — fork de maintenance

## Sécurité de vol

- Les tests des phases 0 à 3 se font **sans hélices**.
- Les commandes moteur doivent être à zéro au démarrage, à la perte de connexion et à la fermeture de l'application.
- Conditions d'arrêt des tests : voir `plan.md`, section 20.

## Mise à jour du firmware

La mise à jour du firmware depuis Android est **désactivée** dans ce fork (fonction cassée en amont, issues #102 et billet Bitcraze d'avril 2025). Utiliser le client Crazyflie sur ordinateur pour mettre à jour ou restaurer le drone.

## Signalement

Pour un problème de sécurité touchant le projet amont, contacter Bitcraze : contact@bitcraze.io.
Pour ce fork : ouvrir une issue sur le dépôt du fork.

## Règles de dépôt

- Aucune clé de signature, aucun secret, aucun APK de production versionné.
- Aucune donnée personnelle ni identifiant d'appareil dans les logs publiés.
