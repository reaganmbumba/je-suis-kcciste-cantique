# Guide de configuration Firebase

Sans Firebase, l'application fonctionne déjà : recueil complet, recherche, favoris, historique,
lecture des paroles, partage, contact WhatsApp, enregistrement de sa voix. Firebase active en plus :
la discussion de groupe, l'envoi des suggestions, l'espace administrateur, les corrections publiées
sans mise à jour Google Play, les traductions et les audios en ligne.

Aucune clé secrète n'est mise dans le code : `google-services.json` ne contient que des
identifiants publics, et la sécurité repose sur les règles ci-dessous.

## 1. Créer le projet et relier l'application
1. https://console.firebase.google.com › **Ajouter un projet** (ex. « KCCiste »), Google Analytics : non.
2. Icône **Android** › package `com.kcciste.cantique` › Enregistrer › télécharger **google-services.json**.
3. Placer ce fichier dans `app/` (à côté de `app/build.gradle.kts`) et le pousser sur GitHub.
   Le plugin Google Services n'est appliqué que si ce fichier existe.

## 2. Authentication
**Créer › Authentication › Commencer**, puis activer :
- **Anonyme** (discussion de groupe et suggestions, sans compte pour les utilisateurs) ;
- **Adresse e-mail/Mot de passe** (administrateurs).

Onglet **Users › Ajouter un utilisateur** : créer le compte de chaque administrateur.
Copier son **UID utilisateur** (colonne de droite).

## 3. Firestore
1. **Firestore Database › Créer une base de données** (région `europe-west`, mode production).
2. Onglet **Règles** : coller tout le contenu de `firestore.rules`, puis **Publier**.
3. Onglet **Données** : créer la collection **`admins`**, avec un document par administrateur dont
   l'**ID est l'UID** copié à l'étape 2 (un champ au choix, ex. `nom` = « Reagan MBUMBA »).
   Sans ce document, le compte peut se connecter mais n'a aucun droit.
4. Index : la première ouverture de « Modération » ou du « Journal » peut afficher une erreur
   contenant un lien « create index » : l'ouvrir et valider (index sur `date` décroissant).

Collections utilisées :

| Collection | Contenu | Qui écrit |
|---|---|---|
| `cantiques/{id}` | dernière version publiée d'un cantique corrigé, traduit, avec audio, ou ajouté | administrateurs |
| `cantiques/{id}/versions/{n}` | chaque version, jamais effacée (date, admin, note) | administrateurs |
| `suggestions/{référence}` | suggestions de correction, statut | utilisateurs (création), administrateurs (statut) |
| `journal/{auto}` | journal des actions administratives | administrateurs |
| `admins/{uid}` | liste des administrateurs | console seulement |
| `groupes/general/messages` | discussion de groupe | utilisateurs |

Les 314 cantiques d'origine restent dans l'application (`app/src/main/assets`) ; seuls les cantiques
modifiés ou ajoutés vont dans Firestore. Les téléphones reçoivent les nouvelles versions à leur
prochaine connexion et les gardent pour l'usage hors connexion.

## 4. Cloud Storage
1. **Storage › Commencer** (même région).
2. Onglet **Règles** : coller `storage.rules`, puis **Publier**.
   Ces règles lisent la collection `admins` de Firestore : la console peut demander d'autoriser
   cet accès entre services, accepter.

Dossiers : `audios/{cantique}/…` (audios officiels, écoute publique, dépôt par les administrateurs,
50 Mo max) et `suggestions/{uid}/…` (messages vocaux, 10 Mo max, lisibles seulement par l'administration).

Le forfait gratuit Spark de Firebase ne permet plus de créer un bucket Storage sur les nouveaux
projets : il faut passer au forfait **Blaze** (paiement à l'usage, gratuit sous les quotas).
Sans Storage, tout fonctionne sauf les messages vocaux des suggestions et le dépôt de fichiers audio
(l'ajout d'un audio **par URL** reste possible).

## 5. Vérifier
1. Compiler (GitHub Actions) et installer l'APK.
2. Paramètres › Espace administrateur › se connecter.
3. Proposer une correction depuis un cantique, puis la voir dans « Modération des suggestions ».
