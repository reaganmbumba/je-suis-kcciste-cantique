# Construit la version web (web/index.html) à partir de web/modele.html,
# des recueils JSON et de la photo d'accueil.
# Usage : python3 outils/construire_web.py
import base64, json, pathlib
racine = pathlib.Path(__file__).resolve().parent.parent
assets = racine / "app/src/main/assets"
d = {l: json.loads((assets / f"cantiques_{l}.json").read_text(encoding="utf-8")) for l in ("lingala", "kikongo")}
for l in d:
    for c in d[l]:
        c["strophes"] = [{"voix": s["voix"], "texte": s["texte"]} for s in c["strophes"]]
data = json.dumps(d, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/")
photo = "data:image/jpeg;base64," + base64.b64encode((racine / "app/src/main/res/drawable-nodpi/banniere_accueil.jpg").read_bytes()).decode()
page = (racine / "web/modele.html").read_text(encoding="utf-8").replace("__DATA__", data).replace("__PHOTO__", photo)
(racine / "web/index.html").write_text(page, encoding="utf-8")
print("web/index.html :", len(page), "caractères")
