# DeliveryGo — Projet Android M205

Application de démonstration de suivi de livraison en Kotlin/XML.

## Fonctionnalités incluses
- Carte Google Maps et marqueur du livreur.
- Position en direct avec `FusedLocationProviderClient`.
- Demande des permissions de localisation avec EasyPermissions.
- Calcul de la distance parcourue et du temps écoulé.
- Bouton de démarrage et de fin de livraison.
- Récapitulatif et notification à la fin.
- Profil du livreur et génération locale d'un QR Code.
- Ressources en français, anglais et arabe.

## Configuration obligatoire de Google Maps
1. Créer un projet dans Google Cloud Console.
2. Activer **Maps SDK for Android** et associer une clé API avec la facturation requise par Google.
3. Ouvrir `app/src/main/res/values/strings.xml`.
4. Remplacer `YOUR_GOOGLE_MAPS_API_KEY` par la clé réelle.
5. Faire la même modification dans `values-fr/strings.xml` et `values-ar/strings.xml` (ou supprimer les copies de cette chaîne des fichiers traduits et garder uniquement la valeur par défaut).

Sans clé API valide, l'application peut s'ouvrir mais la carte ne s'affichera pas correctement.

## Ouvrir dans Android Studio
- Décompresser le ZIP.
- Android Studio > Open > sélectionner le dossier `DeliveryGo`.
- Installer un JDK 17 et un Android SDK Platform 35 si Android Studio le demande.
- Laisser Gradle terminer la synchronisation.
- Lancer sur un téléphone Android avec GPS activé ou sur un émulateur configuré avec une position.

## Modifier les informations du livreur
Dans `ProfileActivity.kt`, modifier `driverId`, `firstName`, `lastName`, `age` et `licenseType`.

## Limites de cette version pédagogique
- Le suivi GPS fonctionne lorsque l'écran principal est ouvert. Il s'arrête lorsque l'activité est détruite ; ce n'est pas un suivi en arrière-plan.
- Le profil est un exemple statique, sans serveur ni base de données.
- La distance est calculée entre les positions GPS reçues ; la précision dépend du GPS et de l'environnement.
