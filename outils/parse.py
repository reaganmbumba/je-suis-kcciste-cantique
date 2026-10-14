import zipfile,re,sys,json,html
def paras(path):
    x=zipfile.ZipFile(path).read('word/document.xml').decode()
    out=[]
    for p in re.findall(r'<w:p[ >].*?</w:p>|<w:p/>',x,re.S):
        bold = '<w:b/>' in p or '<w:b ' in p
        p=re.sub(r'<w:br[^>]*/>','\n',p); p=re.sub(r'<w:tab/>',' ',p)
        t=html.unescape(re.sub(r'<[^>]+>','',p))
        out.append((t,bold))
    return out
def is_title(t):
    s=t.strip()
    letters=[c for c in s if c.isalpha()]
    if len(letters)<3 or '\n' in s: return False
    if re.match(r'^(solo|tous)\s*:',s,re.I): return False
    return s==s.upper()
def parse(path):
    res=[];cur=None
    for t,b in paras(path):
        if is_title(t):
            cur={'titre':re.sub(r'\s+',' ',t.strip()).rstrip(','),'lignes':[]}; res.append(cur)
        elif cur is not None:
            cur['lignes'].append(t.rstrip())
    return res
if __name__=='__main__':
    r=parse(sys.argv[1]); print(len(r))
    for i,c in enumerate(r,1): print(i, c['titre'], len([l for l in c['lignes'] if l.strip()]))
