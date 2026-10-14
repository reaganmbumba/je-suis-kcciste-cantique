import json,re,sys,parse
PROPRES={'yawe':'Yawe','njambe':'Njambe','nzambi':'Nzambi','yesu':'Yesu','israël':'Israël','israel':'Israël','kristu':'Kristu','klisto':'Klisto','adonaï':'Adonaï','adonai':'Adonaï','kcc':'KCC','abalayama':'Abalayama','mose':'Mose','davidi':'Davidi','yelusalemi':'Yelusalemi','simon':'Simon','kimbangu':'Kimbangu','nkamba':'Nkamba','tata':'Tata'}
def joli(t):
    t=t.strip().rstrip(',').strip()
    mots=t.lower().split(' ')
    out=[]
    for i,m in enumerate(mots):
        core=re.sub(r'[^\wëï]','',m)
        if core in PROPRES: m=m.replace(core,PROPRES[core])
        elif i==0: m=m[:1].upper()+m[1:]
        out.append(m)
    return ' '.join(out)
def strophes(lignes):
    res=[];cur=None
    for l in lignes:
        s=l.strip()
        if not s: cur=None; continue
        m=re.match(r'^(solo|tous)\s*:\s*(.*)$',s,re.I)
        if m:
            cur={'voix':m.group(1).capitalize(),'texte':m.group(2).strip()}; res.append(cur)
        elif cur is None:
            cur={'voix':'','texte':s}; res.append(cur)
        else:
            cur['texte']+= '\n'+s
    for r in res:
        t=re.sub(r'[ \t]+',' ',r['texte']).strip()
        # Lignes collées dans le .docx (ex. « YaweApesaki ») : on remet le retour à la ligne.
        t=re.sub(r'([a-zëï])([A-Z])',r'\1\n\2',t)
        t=re.sub(r'(\S)\((\d+x)\)',r'\1 (\2)',t)
        r['texte']=t
    return [r for r in res if r['texte'] or r['voix']]
def build(path):
    raw=parse.parse(path); merged=[]
    for c in raw:
        if merged and not any(l.strip() for l in merged[-1]['lignes']):
            merged[-1]['titre']+=' '+c['titre']; merged[-1]['lignes']=c['lignes']
        else: merged.append(c)
    return [{'numero':i,'titre':joli(c['titre']),'strophes':strophes(c['lignes'])} for i,c in enumerate(merged,1)]
# Usage : python3 build_json.py RECUEIL_LINGALA.docx RECUEIL_KIKONGO.docx app/src/main/assets
out=sys.argv[3]
for nom,f in [('lingala',sys.argv[1]),('kikongo',sys.argv[2])]:
    d=build(f)
    json.dump(d,open(f'{out}/cantiques_{nom}.json','w'),ensure_ascii=False,indent=0)
    print(nom,len(d),'vides:',[c['titre'] for c in d if not c['strophes']])
    print(json.dumps(d[0],ensure_ascii=False)[:400])
