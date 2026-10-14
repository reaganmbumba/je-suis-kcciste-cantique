# KCCiste Cantique — Enregistrer et envoyer au créateur

Application Android (Kotlin + Jetpack Compose) avec les recueils Lingala (113 cantiques)
et Kikongo (201 cantiques), et la fonction premium d'enregistrement.

Créateur : **Prédicateur Reagan MBUMBA** · WhatsApp uniquement : **+243 840 238 592** · https://wa.me/243840238592

## Ce qui fonctionne réellement

| Fonction | Où dans le code |
|---|---|
| Enregistrement micro réel M4A/AAC (mono, 44,1 kHz, 128 kbit/s ≈ 1 Mo/min), pause/reprise | `audio/Enregistreur.kt` |
| Waveform animée à partir du niveau réel du micro | `ui/components/Composants.kt` (`Waveform`) |
| Chrono, durée maximale configurable, arrêt automatique | `audio/EnregistrementViewModel.kt` |
| Écouter (lecteur avec barre de progression) | `audio/Lecteur.kt`, `ui/components/LecteurAudio.kt` |
| Refaire, Sauvegarder, Supprimer | `audio/EnregistrementViewModel.kt` |
| Nom automatique `KCCiste_Cantique_001_Abalayama_Mosali_Na_Yawe_2026-10-02.m4a` | `data/Formats.kt` |
| « Mes enregistrements » (titre, langue, date, durée, taille, statut 🟢/📤) | `ui/screens/EcranMesEnregistrements.kt` |
| Confirmation avant envoi, partage du fichier + message prérempli vers WhatsApp | `share/Partage.kt`, `ui/components/Envoi.kt` |
| WhatsApp absent → « PARTAGER AUTREMENT » (menu Android : e-mail, autres apps…) | `ui/components/Envoi.kt` |
| Fichier trop volumineux, échec d'enregistrement, micro refusé | idem + `ui/components/PermissionMicro.kt` |
| « 💬 CONTACTER LE CRÉATEUR » (À propos, Aide, Profil, page d'enregistrement) | `CarteContactCreateur` |

### Envoi WhatsApp : ce que l'application fait et ne fait pas

1. Le fichier est sauvegardé localement (privé à l'application).
2. Le message prérempli est préparé (et copié dans le presse-papiers).
3. WhatsApp s'ouvre avec le fichier audio et le message. L'application demande à WhatsApp
   d'ouvrir directement la conversation +243 840 238 592 (extra `jid`) ; si la version de
   WhatsApp l'ignore, l'utilisateur choisit le contact dans la liste.
4. **Rien n'est jamais envoyé automatiquement** et l'application n'affiche jamais « envoyé » :
   Android ne renvoie aucune confirmation d'envoi depuis WhatsApp. Le statut « 📤 Partagé »
   signifie « partage ouvert », et un message rappelle à l'utilisateur d'appuyer lui-même sur Envoyer.

## Discussion de groupe (comme un groupe WhatsApp)

Page « 💬 GROUPE DE DISCUSSION » sur l'accueil : un seul groupe pour tous les utilisateurs, messages texte,
nom choisi par chacun, heure, fonctionne aussi hors ligne (le message part au retour du réseau), appui long
sur un de ses messages pour le supprimer. Code : `data/Discussion.kt`, `ui/screens/EcranDiscussion.kt`.
Les messages sont stockés dans Firebase Firestore (gratuit jusqu'à 50 000 lectures/jour).

**À faire une seule fois pour l'activer** (sans cela, l'appli se compile et la page dit « pas encore activée ») :

1. https://console.firebase.google.com › *Ajouter un projet* (nom : KCCiste), Google Analytics : non.
2. Icône Android › nom du package `com.kcciste.cantique` › *Enregistrer* › télécharger **google-services.json**.
   Mettre ce fichier dans le dossier `app/` (à côté de `build.gradle.kts`), puis le pousser sur GitHub avec le reste.
3. *Créer › Authentication › Commencer › Anonyme* : activer.
4. *Créer › Firestore Database › Créer une base de données* (région `europe-west`, mode production).
   Onglet *Règles* : coller le contenu de `firestore.rules`, puis *Publier*.

Pas de notifications de nouveaux messages pour l'instant : il faut ouvrir la page pour les voir.

## Réglages administrateur

Dans `app/build.gradle.kts` :

```kotlin
buildConfigField("int", "MAX_RECORDING_SECONDS", "600") // durée max d'un enregistrement
buildConfigField("int", "MAX_SHARE_SIZE_MB", "64")      // taille max partageable
```

## Mettre à jour les recueils

```bash
python3 outils/build_json.py RECUEIL_CANTIQUES_LINGALA.docx RECUEIL_CANTIQUES_KIKONGO.docx app/src/main/assets
```

## Compiler l'APK

- **Android Studio** : ouvrir le dossier, puis *Build › Build APK(s)*.
- **Ligne de commande** (SDK Android installé) : `./gradlew assembleDebug`
  → `app/build/outputs/apk/debug/app-debug.apk`
- **GitHub** : pousser ce dossier dans un dépôt ; le workflow `.github/workflows/apk.yml`
  produit l'APK à télécharger dans l'onglet *Actions*.

Android 8.0 (API 26) minimum.
