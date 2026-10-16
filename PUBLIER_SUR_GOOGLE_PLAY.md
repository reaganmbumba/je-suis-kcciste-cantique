# Publier Je suis KCCiste Cantique sur Google Play

Le chemin complet : **dossier du projet → GitHub → fichier .aab signé → Google Play Console.**
Comptez une soirée pour les étapes 1 à 4, puis les délais de validation de Google.

---

## Étape 1 — Mettre le projet sur GitHub

**Option la plus simple : je le fais pour vous.**
Reliez votre compte GitHub à Claude : https://claude.ai/connect-github , créez un dépôt vide
(par exemple `kcciste-cantique`, en **privé**), puis dites-moi son nom. Je pousse le code moi-même
et je corrige les éventuelles erreurs de compilation directement.

**Option manuelle, sans ligne de commande :**
1. Créez un compte sur https://github.com puis **New repository** → nom `kcciste-cantique` → **Private** → *Create repository*.
2. Décompressez `kcciste-cantique.zip` sur votre ordinateur.
3. Installez **GitHub Desktop** (https://desktop.github.com), connectez-vous, puis
   *File › Add local repository* → choisissez le dossier `kcciste-cantique` → *Publish repository*.
   (GitHub Desktop envoie aussi le dossier caché `.github`, indispensable pour la compilation automatique.
   L'envoi par glisser-déposer sur le site l'oublie souvent.)

## Étape 2 — Créer la clé de signature (une seule fois, à garder à vie)

Google Play exige que chaque .aab soit signé avec **votre clé d'importation**.

> **Vous avez déjà une clé ?** Si `upload.jks` a été créé pour l'ancien dépôt `jesuiskcciste`
> (même application `com.kcciste.cantique`), **réutilisez-la** : passez directement à l'étape 3
> avec le même fichier `upload.txt` et le même mot de passe. Google Play refuse une application
> signée avec une autre clé que celle déjà enregistrée.

Sur un ordinateur où Java ou Android Studio est installé, ouvrez un terminal et tapez :

```
keytool -genkeypair -v -keystore upload.jks -alias kcciste -keyalg RSA -keysize 2048 -validity 10000
```

Il demande un mot de passe et quelques informations (nom, ville, pays : CD).
Alternative graphique : Android Studio › *Build › Generate Signed Bundle / APK › Create new…*

⚠️ Sauvegardez `upload.jks` et son mot de passe en lieu sûr (clé USB + cloud privé).
**Ne mettez jamais ce fichier dans GitHub** (le `.gitignore` l'exclut déjà).

## Étape 3 — Donner la clé à GitHub (secrets)

Convertissez la clé en texte :

- **Windows (PowerShell)** :
  `[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload.jks")) | Set-Content upload.txt`
- **Mac / Linux** : `base64 -i upload.jks > upload.txt`

Dans votre dépôt GitHub : **Settings › Secrets and variables › Actions › New repository secret**,
créez ces 4 secrets :

| Nom | Valeur |
|---|---|
| `KEYSTORE_BASE64` | tout le contenu de `upload.txt` |
| `KEYSTORE_PASSWORD` | le mot de passe de la clé |
| `KEY_ALIAS` | `kcciste` |
| `KEY_PASSWORD` | le mot de passe de la clé (le même, en général) |

Supprimez ensuite `upload.txt`.

## Étape 4 — Générer le fichier .aab

1. Dans le dépôt GitHub : onglet **Actions › Construire APK et AAB › Run workflow**.
   (Chaque envoi de code sur `main` le relance aussi automatiquement.)
2. Quand la coche est verte, ouvrez l'exécution et téléchargez en bas de page :
   - **KCCiste-Cantique-AAB-Play-Store** → contient `app-release.aab` (pour Google Play) ;
   - **KCCiste-Cantique-APK-test** → `app-debug.apk` à installer sur votre téléphone pour essayer.
3. Si l'exécution est rouge, envoyez-moi le message d'erreur (ou reliez GitHub, je corrige directement).
   C'est la première compilation complète du projet, une petite correction n'est pas exclue.

Le numéro de version (`versionCode`) augmente tout seul à chaque compilation, comme Google l'exige.

## Étape 5 — Google Play Console

1. **Compte développeur** : https://play.google.com/console (frais uniques de 25 USD, pièce d'identité demandée).
2. **Créer une application** : nom *Je suis KCCiste Cantique*, langue par défaut *Français*, *Application*, *Gratuite*.
3. **Fiche Play Store** : description courte et longue, icône 512×512 px, image de présentation 1024×500 px,
   au moins 2 captures d'écran du téléphone.
4. **Contenu de l'application** (obligatoire) :
   - **Règles de confidentialité** : une adresse web publique. Le texte est prêt dans
     `POLITIQUE_DE_CONFIDENTIALITE.md` ; publiez-le par exemple sur Google Sites et collez le lien.
     Il est obligatoire ici car l'application utilise le microphone.
   - **Sécurité des données** : déclarez la collecte de **Messages (autres messages dans l'appli)**,
     **Nom** et **Identifiants (ID d'appareil ou autre)**, pour la *fonctionnalité de l'appli*, non partagés,
     chiffrés en transit, suppression possible par l'utilisateur. Ce sont les données de la discussion de groupe.
     Les enregistrements audio, eux, restent sur le téléphone et ne sont transmis que si l'utilisateur les partage lui-même.
   - Classification du contenu, public cible, publicités (non), accès à l'application (aucune connexion requise).
5. **Signature** : laissez *Signature d'application Play* activée (proposée par défaut).
   Google garde la clé finale ; votre `upload.jks` sert seulement à envoyer les mises à jour.
6. **Publier** : *Tests › Test interne › Créer une release* → importez `app-release.aab` → enregistrez → déployez.
   Installez depuis le lien de test et vérifiez l'enregistrement et l'envoi WhatsApp sur un vrai téléphone.
7. **Compte personnel récent** : Google exige d'abord un **test fermé avec au moins 12 testeurs
   pendant 14 jours** avant d'ouvrir l'accès *Production*. Ensuite : *Production › Créer une release*
   → même .aab (ou un plus récent) → *Envoyer pour examen* (quelques jours).

## Mises à jour ensuite

Modifier le code → envoyer sur GitHub → télécharger le nouveau .aab → Play Console › *Production › Créer une release*.

## À savoir

- L'identifiant `com.kcciste.cantique` devient **définitif** dès la première publication.
- L'icône actuelle (note dorée sur fond bleu) est provisoire ; envoyez-moi votre logo pour la remplacer.
