from pathlib import Path
root=Path(r'D:\en html\Sistema de Presupuestos')
code=r'''"""Reading of handwritten ledgers: description left, total price right."""
import re
from difflib import SequenceMatcher
from statistics import median
import numpy as np
from app.services.image_analysis import get_engine, normalize, number

def numeric_candidate(text: str):
    # Ambiguous letter substitutions apply ONLY inside the dedicated price column.
    tail = text.strip().split()[-1] if text.strip() else ''
    tail = re.sub(r'^S[/\\.]', '', tail, flags=re.I).strip()
    corrected = tail.translate(str.maketrans({'O':'0','o':'0','S':'5','s':'5','I':'1','l':'1','|':'1'}))
    if not re.fullmatch(r'\d+(?:[.,]\d+)*', corrected) or not re.search(r'\d', tail) and len(tail)<3:
        return None, False
    try:
        amount = number(corrected)
    except ValueError:
        return None, False
    return (amount if 0 < amount <= 1_000_000_000 else None), corrected != tail

def has_term(text: str, term: str) -> bool:
    compact=re.sub(r'[^a-z]', '', normalize(text))
    if term in compact:
        return True
    # Longer construction terms tolerate small OCR errors; short words require exact matches.
    if len(term)<6:
        return False
    threshold=.80
    for size in range(len(term)-1,len(term)+2):
        for start in range(max(0,len(compact)-size+1)):
            if SequenceMatcher(None,term,compact[start:start+size]).ratio() >= threshold:
                return True
    return False

def description(raw: str):
    # Suggest vocabulary corrections; retain original OCR separately for human review.
    objects=[('drywall','drywall'),('ladrillo','ladrillo'),('column','columnas'),('pared','pared'),('material','material')]
    obj=next((label for term,label in objects if has_term(raw,term)),None)
    actions=[('demolicion','Demolición'),('asentado','Asentado'),('encofrado','Encofrado'),('vaciado','Vaciado'),('tarrajeo','Tarrajeo'),('acarreo','Acarreo'),('instalacion','Instalación'),('excavacion','Excavación'),('replanteo','Replanteo')]
    action=next((label for term,label in actions if has_term(raw,term)),None)
    if action:
        if action=='Encofrado' and has_term(raw,'vaciado'):
            action += ' y vaciado'
        count=re.search(r'(?<!\d)(\d+)\s*(?:colum|column)',normalize(raw))
        name=action+(' de '+(count.group(1)+' ' if count and obj=='columnas' else '')+obj if obj else '')
        # Never replace an unknown scope by only an action.
        if obj:
            return 'labor',name, name != raw
        return 'labor',raw,False
    materials=['cemento','acero','pintura','arena','ladrillo','fierro','madera','concreto','yeso']
    return ('material' if any(has_term(raw,t) for t in materials) else 'labor'),raw,False

def detected_rows(results):
    rows=[]
    for box,text,confidence in sorted(results or [],key=lambda r:sum(p[1] for p in r[0])/4):
        if not text.strip():continue
        ys=[float(p[1]) for p in box];xs=[float(p[0]) for p in box]
        y=sum(ys)/len(ys);height=max(ys)-min(ys)
        if rows and abs(y-rows[-1]['y'])<=max(8,min(height,rows[-1]['height'])*.6):
            rows[-1]['cells'].append((min(xs),text))
        else:
            rows.append({'y':y,'height':height,'cells':[(min(xs),text)]})
    return rows

def read_handwritten(image, results):
    engine=get_engine();array=np.asarray(image);height,width=array.shape[:2]
    rows=detected_rows(results)
    numeric_x=[min(float(p[0]) for p in box) for box,text,_ in results or [] if re.fullmatch(r'\d+(?:[.,]\d+)*',text.strip()) and min(p[0] for p in box)>width*.65]
    split=int(max(width*.65,min(width*.9,median(numeric_x)-width*.01))) if numeric_x else int(width*.86)
    proposals=[];raw_lines=[];started=False
    for row in rows[:60]:
        radius=max(35,row['height']*.8);lo=max(0,int(row['y']-radius));hi=min(height,int(row['y']+radius))
        def read(crop):
            result,_=engine(crop,use_det=False,use_cls=False,use_rec=True)
            return (str(result[0][0]),float(result[0][1])) if result else ('',0)
        price,price_confidence=read(array[lo:hi,split:])
        amount,ambiguous=numeric_candidate(price)
        raw=' | '.join(t for _,t in sorted(row['cells']))
        # Header/client names have no right-column price and precede the first priced row.
        if amount is None and not started:continue
        text,text_confidence=read(array[lo:hi,:split])
        if not text.strip():text=' '.join(t for x,t in sorted(row['cells']) if x<split)
        if not re.search(r'[a-zA-Z]',text):continue
        if re.match(r'^(total|subtotal|igv|impuesto)\b',normalize(text)):continue
        started=True
        kind,name,normalized=description(text.strip())
        notes=[]
        if normalized:notes.append('Descripción normalizada con vocabulario de construcción: revisa contra la foto.')
        if ambiguous:notes.append('Importe con letras confundidas por el OCR: revisa los dígitos.')
        if price_confidence<.8 or amount is None:notes.append('Importe de lectura incierta: confirma el valor en la imagen.')
        if text_confidence<.8:notes.append('Escritura manuscrita de lectura incierta.')
        proposals.append({'kind':kind,'name':name[:300],'amount':amount,'source':raw+'\nLectura por columnas: '+text+' | '+price,'notes':notes})
        raw_lines.append(raw)
    return proposals,raw_lines
'''
p=root/'backend/app/services/handwritten.py';p.write_text(code,encoding='utf-8')
p=root/'backend/app/services/image_analysis.py';s=p.read_text(encoding='utf-8');s=s.replace('def analyze(data: str) -> dict:','def analyze(data: str, mode: str = "document") -> dict:');s=s.replace('lines = group_rows(result)\n    return', '''lines = group_rows(result)
    items = parse_lines(lines)
    if mode == "handwritten":
        from app.services.handwritten import read_handwritten
        with engine_lock:
            items, _ = read_handwritten(image, result)
    return''');s=s.replace('"items":parse_lines(lines)','"items":items');p.write_text(s,encoding='utf-8')
p=root/'backend/app/api/v1/endpoints/images.py';s=p.read_text(encoding='utf-8');s='from typing import Literal\n'+s;s=s.replace('image: str = Field(min_length=1, max_length=14_000_000)','image: str = Field(min_length=1, max_length=14_000_000)\n    mode: Literal["document", "handwritten"] = "document"');s=s.replace('run_in_threadpool(analyze, payload.image)','run_in_threadpool(analyze, payload.image, payload.mode)');p.write_text(s,encoding='utf-8')
