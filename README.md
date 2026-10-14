# Je suis KCCiste Cantique

Application Android de recueil de cantiques **Kikongo (201)** et **Lingala (113)** avec paroles,
lecteur audio, téléchargement hors connexion, traductions portugais / anglais, partage, corrections
communautaires et espace d'administration.

**KCCISTE CANTIQUE — Je suis KCCiste** · Conception : **Prédicateur Reagan MBUMBA** ·
WhatsApp : **+243 840 238 592** · https://wa.me/243840238592

Cette version 2.0 fusionne l'application KCCiste Cantique existante (enregistrement de sa voix et
envoi au créateur, discussion de groupe) avec les fonctions d'un recueil de cantiques avec audio,
inspirées du type d'application « Recueil des Cantiques » (aucun code, logo ni contenu repris).

## Choix technique : Kotlin + Jetpack Compose (et non Flutter)
L'application existait déjà en Kotlin avec l'identifiant Google Play `com.kcciste.cantique` et sa clé
de signature. Rester sur cette base garde la même fiche Google Play, la même clé et toutes les
fonctions existantes. Material 3, Android 8.0 (API 26) minimum, cible Android 16 (API 36).

## Architecture

```
app/src/main/java/com/kcciste/cantique/
├── KCCisteApp.kt              démarrage : réglages, recueil, synchronisation en ligne
├── MainActivity.kt            écran de lancement + navigation (toutes les routes)
├── data/                      modèles et logique pure (testée sans Android)
│   ├── Cantique.kt            Cantique, Strophe, Traduction, PisteAudio, langues
│   ├── CantiqueJson.kt        format JSON unique (assets, cache, import)
│   ├── Recueil.kt             bibliothèque : assets + contenu publié en ligne (cache hors connexion)
│   ├── FusionRecueil.kt       règle de fusion origine / en ligne par identifiant et version
│   ├── Recherche.kt           numéro, titre, paroles, sans accents
│   ├── ImportCantiques.kt     vérification des imports JSON / CSV (doublons, champs obligatoires)
│   ├── Suggestion.kt          statuts, numéro de référence, validation, types audio acceptés
│   ├── Comparaison.kt         différence ligne par ligne ancien / proposé
│   ├── TexteCantique.kt       texte copié / partagé avec la signature
│   ├── Createur.kt            identité, signature, contact WhatsApp et motifs
│   ├── Preferences.kt         favoris, historique, audios écoutés, réglages (sur le téléphone)
│   ├── Historique.kt
│   └── Formats.kt, Enregistrement*.kt, Discussion.kt   (fonctions existantes)
├── audio/
│   ├── LecteurService.kt      Media3 MediaSessionService : arrière-plan, notification, écran verrouillé
│   ├── LecteurCantiques.kt    commande du lecteur, liste de lecture, reprise, erreurs
│   ├── Telechargements.kt     téléchargement avec reprise (Range), stockage interne, suppression
│   └── Enregistreur.kt, Lecteur.kt, EnregistrementViewModel.kt   (enregistrement de sa voix)
├── enligne/                   Firebase (facultatif)
│   ├── ServiceFirebase.kt     configuration, connexion anonyme, conversions
│   ├── ContenuEnLigne.kt      publication de versions, historique, restauration, import, journal
│   ├── SuggestionsEnLigne.kt  envoi des suggestions (+ message vocal), modération
│   ├── AudiosEnLigne.kt       dépôt des fichiers audio officiels
│   └── Admin.kt               connexion administrateur (e-mail + mot de passe, document admins/{uid})
├── share/                     PartageTexte.kt (WhatsApp, Facebook, menu Android, copier), Partage.kt
└── ui/
    ├── theme/Theme.kt         blanc, or, bleu nuit ; modes clair et sombre
    ├── components/            LogoKCC, MiniLecteur, cartes, lignes de cantique…
    └── screens/               écrans utilisateur ; screens/admin/ écrans d'administration
```

## Écrans (24 demandés)

| # | Écran | Fichier |
|---|---|---|
| 1 | Lancement avec logo KCC (Android 12+ : écran système, puis écran Compose) | `MainActivity.kt`, `res/values-v31/themes.xml` |
| 2 | Accueil : bannière photo, recherche, recueils, raccourcis, récents | `EcranAccueil.kt` |
| 3, 5 | Bibliothèque / liste par langue et catégorie | `EcransListes.kt` |
| 4 | Recherche (numéro, titre, paroles) + « Aller au n° » | `EcransListes.kt`, `EcranAccueil.kt` |
| 6, 11 | Détail du cantique, choix de traduction, plein écran, taille du texte | `EcranCantique.kt` |
| 7 | Lecteur audio | `EcransAudio.kt` |
| 8 | Téléchargements | `EcransAudio.kt` |
| 9, 10 | Favoris, historique (cantiques et audios) | `EcransListes.kt` |
| 12, 13 | Correction écrite, enregistrement ou import d'un message vocal | `EcranCorrection.kt` |
| 14 | Confirmation avec numéro de référence | `EcranCorrection.kt` |
| 15 | Contact WhatsApp avec motifs | `EcransInfos.kt` |
| 16, 17 | Paramètres, À propos et copyright | `EcransInfos.kt` |
| 18, 19 | Connexion administrateur, tableau de bord | `admin/EcransTableauDeBord.kt` |
| 20 | Gestion des cantiques (fiche, édition, ajout) | `admin/EcransGestionCantiques.kt` |
| 21, 22 | Pistes audio, traductions | `admin/EcransGestionCantiques.kt` |
| 23 | Modération des suggestions (comparaison, écoute, statuts) | `admin/EcransModeration.kt` |
| 24 | Historique des versions et restauration | `admin/EcransGestionCantiques.kt` |
| + | Import en lot, journal des actions | `admin/EcransModeration.kt`, `admin/EcransTableauDeBord.kt` |
| + | Mes enregistrements, enregistreur, discussion de groupe, profil, aide (existants) | `EcranEnregistrement.kt`… |

## Mises à jour du contenu sans Google Play
Le recueil d'origine est dans `app/src/main/assets`. Quand un administrateur publie une correction,
une traduction ou un audio, l'ancienne version est conservée (`cantiques/{id}/versions`), la nouvelle
est publiée dans Firestore, et les téléphones la reçoivent à leur prochaine connexion, puis la gardent
hors connexion (`files/contenu_en_ligne.json`). Aucune suggestion n'est publiée automatiquement.
Voir `GUIDE_FIREBASE.md` et `GUIDE_IMPORTATION.md`.

## Ce qui a réellement été testé
- **Tests unitaires de la logique, exécutés** (`app/src/test`) : recherche sans accents / numéro / paroles,
  lecture des 314 cantiques fournis et unicité des identifiants, aller-retour JSON, fusion des versions,
  import JSON et CSV (doublons, erreurs, refrains), texte partagé et signature, référence de suggestion,
  validation du formulaire, lien WhatsApp prérempli, comparaison de textes, historique, noms de fichiers.
- **Compilation Android, lecteur audio, téléchargements, Firebase, écrans** : la compilation se fait
  sur GitHub Actions (`.github/workflows/apk.yml`), l'environnement de développement utilisé n'ayant pas
  accès aux bibliothèques Android. Ces parties n'ont pas encore été essayées sur un téléphone.

### Envoi WhatsApp : ce que l'application fait et ne fait pas

1. Le fichier est sauvegardé localement (privé à l'application).
2. Le message prérempli est préparé (et copié dans le presse-papiers).
3. WhatsApp s'ouvre avec le fichier audio et le message. L'application demande à WhatsApp
   d'ouvrir directement la conversation +243 840 238 592 (extra `jid`) ; si la version de
   WhatsApp l'ignore, l'utilisateur choisit le contact dans la liste.
4. **Rien n'est jamais envoyé automatiquement** et l'application n'affiche jamais « envoyé » :
   Android ne renvoie aucune confirmation d'envoi depuis WhatsApp. Le statut « 📤 Partagé »
   signifie « partage ouvert », et un message rappelle à l'utilisateur d'appuyer lui-même sur Envoyer.

## Réglages administrateur

Dans `app/build.gradle.kts` :

```kotlin
buildConfigField("int", "MAX_RECORDING_SECONDS", "600") // durée max d'un enregistrement
buildConfigField("int", "MAX_SHARE_SIZE_MB", "64")      // taille max partageable
```

## Compiler
- **APK de test** : pousser sur la branche `main` (ou lancer le workflow « Construire APK et AAB »
  dans l'onglet *Actions*) puis télécharger l'artefact `KCCiste-Cantique-APK-test`.
  En local avec le SDK Android : `./gradlew testDebugUnitTest assembleDebug`
  → `app/build/outputs/apk/debug/app-debug.apk`.
- **AAB pour Google Play** : le même workflow produit `KCCiste-Cantique-AAB-Play-Store` quand les
  4 secrets de signature sont présents (voir `PUBLIER_SUR_GOOGLE_PLAY.md`). En local :
  `KEYSTORE_PATH=… KEYSTORE_PASSWORD=… KEY_ALIAS=… KEY_PASSWORD=… VERSION_CODE=… ./gradlew bundleRelease`.

## Limitations et travail restant
- **Aucun audio n'est encore fourni** : le lecteur et les téléchargements fonctionneront dès que
  l'administration aura ajouté des pistes autorisées.
- **Aucune traduction n'est encore fournie** ; l'écran l'indique. La traduction automatique n'est pas
  branchée : elle demanderait un service payant et une clé gardée côté serveur (Cloud Functions).
  Les traductions saisies par l'administration sont marquées « provisoires » jusqu'à validation.
- **Couplets / refrains** : les recueils Word ne distinguent que « Solo » / « Tous ». Le type
  « refrain » s'ajoute dans l'éditeur administrateur ; rien n'est deviné.
- **Catégories** : aucune pour l'instant ; les filtres apparaissent dès que l'administrateur en saisit.
- **Facebook** n'accepte pas de texte prérempli venant d'une application : les paroles sont copiées
  pour être collées dans la publication.
- **Firebase** doit être configuré (fichier `google-services.json`, règles, comptes administrateurs)
  pour la discussion, les suggestions et l'administration. Storage demande le forfait Blaze.
- Les téléchargements reprennent après une coupure tant que l'application est ouverte ; il n'y a
  pas encore de reprise automatique en arrière-plan (WorkManager).
- Pas de notifications push (nouveaux messages, suggestion traitée).
- Versions Web et iOS : non commencées.
