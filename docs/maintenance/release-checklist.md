# Checklist de version — maintenance

La checklist officielle du dépôt est [RELEASE_CHECKLIST.md](../../RELEASE_CHECKLIST.md) (racine). Cette page ajoute les critères propres à la version de maintenance `0.7.7-maintenance` (voir plan.md, section 21).

- [ ] le projet compile avec Java 17 ;
- [ ] l'APK s'installe sur Android 16 ;
- [ ] l'application démarre sans crash ;
- [ ] les permissions sont correctement demandées ;
- [ ] le GPS n'est pas exigé inutilement sur Android récent ;
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
