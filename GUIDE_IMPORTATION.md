# Guide d'importation des cantiques et des audios

Règles qui s'appliquent toujours :
- aucune parole n'est inventée : seuls les textes fournis sont importés ;
- chaque cantique a un **identifiant stable** `langue-numéro` (ex. `lingala-012`) ;
- un import **ne remplace jamais** un cantique existant : il est signalé « déjà présent » et ignoré.
  Pour modifier un cantique existant, utiliser l'espace administrateur (nouvelle version).

## 1. Depuis Word (.docx) : recueils intégrés à l'application
Format attendu : titre en MAJUSCULES, puis lignes « Solo : » / « Tous : », strophes séparées par une ligne vide.

```bash
python3 outils/build_json.py RECUEIL_CANTIQUES_LINGALA.docx RECUEIL_CANTIQUES_KIKONGO.docx app/src/main/assets
```

Relire ensuite le JSON produit (le script affiche les cantiques sans paroles), puis pousser sur
GitHub : une nouvelle version de l'application est alors nécessaire.

## 2. Depuis l'application : Administration › Importer en lot
Sans nouvelle version Google Play. L'application vérifie tout **avant** d'envoyer :
champs obligatoires, doublons dans le fichier, doublons avec le recueil.

### JSON
```json
[
  {
    "numero": 114,
    "titre": "Titre du cantique",
    "langue": "lingala",
    "categorie": "Louange",
    "credits": "Paroles : …, musique : …, autorisation : …",
    "strophes": [
      { "voix": "Solo", "texte": "Première ligne\nDeuxième ligne" },
      { "voix": "Tous", "texte": "…" },
      { "voix": "", "type": "refrain", "texte": "…" }
    ],
    "traductions": [
      { "langue": "pt", "validee": false, "strophes": ["…", "…", "…"] }
    ],
    "audios": [
      { "type": "CHANTEE", "url": "https://…/114.mp3", "credits": "Chorale KCC" }
    ]
  }
]
```
- `langue` : `lingala` ou `kikongo` (ou choisie dans l'écran d'import si absente).
- `id` facultatif (sinon `langue-numéro`).
- `traductions[].strophes` : une entrée par strophe originale, dans le même ordre.
- `audios[].type` : `CHANTEE`, `INSTRUMENTALE` ou `REPETITION`.

### CSV (Excel, LibreOffice)
En-tête obligatoire : `numero,titre,langue,paroles` (séparateur `,` ou `;`), colonnes facultatives
`categorie,credits,id`. Dans `paroles`, entre guillemets, les strophes sont séparées par une ligne
vide et peuvent commencer par `Solo :`, `Tous :` ou `Refrain :`.

```
numero;titre;langue;paroles;credits
114;Titre;lingala;"Solo : ligne 1
ligne 2

Tous : ligne 3";Chorale KCC
```

## 3. Audios
Administration › Gestion des cantiques › (cantique) › **Pistes audio** :
- **Importer un fichier** (m4a, mp3, ogg… 50 Mo max) : envoyé dans Firebase Storage ;
- ou **URL autorisée** (`https://…`) d'un fichier que vous avez le droit de diffuser.
Cocher « J'ai l'autorisation de diffuser cet audio » est obligatoire. Plusieurs pistes par cantique
(chantée, instrumentale, répétition). Ne jamais ajouter d'audio appartenant à des tiers sans accord.

Les utilisateurs peuvent ensuite écouter en ligne ou télécharger chaque piste pour l'écouter hors connexion.
