# Changelog — Atlas Commerce Android

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et
[SemVer](https://semver.org/lang/fr/).

## [1.0.5] — 2026-10-21

### Ajouté

- **Sélection du serveur API avant la connexion** : écran dédié au premier lancement, presets
  (production, émulateur, local), adresse personnalisée et test réel de `/api/health`
  (latence, base D1, stockage R2).
- Changement de serveur depuis l'écran de connexion et depuis **Réglages → Serveur**.
- Connexion complète : mot de passe, `needs_otp`, vérification OTP, restauration de session,
  messages d'erreur du worker en français.
- Tableau de bord : trésorerie, valeur du stock, produits, stock faible, créances, tendance
  mensuelle, derniers documents.
- Catalogue produits : recherche nom / SKU / code-barres, filtre stock faible, fiche produit,
  ajout au panier, cache hors ligne du dernier catalogue.
- Scan de codes-barres (CameraX + ML Kit) avec saisie manuelle de secours.
- Vente rapide : panier, quantités, client, note, création d'un ticket VR sans TVA.
- Pages **Licence MIT**, **Sécurité & confidentialité** et **À propos** intégrées.
- Thème système / clair / sombre et densité compacte / confortable.
- Tests unitaires (normalisation d'adresse serveur, formatage fr-MA) et CI GitHub Actions
  (assemble debug, tests, APK en artefact).

### Sécurité

- Jeton de session chiffré en AES-256-GCM avec une clé du Keystore Android.
- HTTPS obligatoire en release ; HTTP réservé aux builds debug pour un serveur local.
- Aucun analytics ni SDK tiers ; caméra traitée localement.
- En-têtes `X-Atlas-Client`, `X-Device-Fingerprint` et `User-Agent` Atlas sur chaque requête.
- `allowBackup=false`, journalisation HTTP désactivée et `Authorization` masqué en release.

### Corrigé

- L'application est désormais compilable et exécutable : `MainActivity`, Application Hilt,
  thème, DI, réseau, écrans et ressources manquaient dans l'ébauche.
- Suppression des références Firebase / Room / identifiants Google inutilisés qui empêchaient
  la compilation et alourdissaient l'APK.
- Manifest nettoyé : plus de service de push fantôme ni de deep link non traité.

## [1.0.4] — ébauche précédente

- Thème Material 3, modèles de données et ébauche de panier. Ne compilait pas (écrans, DI et
  point d'entrée absents).
