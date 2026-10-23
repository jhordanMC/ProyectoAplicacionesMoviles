from pathlib import Path
p=Path(r'D:\en html\Sistema de Presupuestos\backend\app\services\handwritten.py');s=p.read_text(encoding='utf-8');s=s.replace("'C':'0'","'C':'0','g':'9','G':'9'").replace("r'[sS][oO0]{2}'","r'[sSgG][oO0]{2}'")
s=s.replace("if re.fullmatch(r'\d+(?:[.,]\d+)*',text.strip()) and min(p[0] for p in box)>width*.65","if numeric_candidate(text)[0] is not None and min(p[0] for p in box)>width*.65")
start=s.index('        if detected_prices:');end=s.index("        raw=' | '",start)
s=s[:start]+'''        direct_price,direct_confidence=read(array[lo:hi,split:])
        candidates=[(r['text'],r['confidence']) for r in detected_prices]+[(direct_price,direct_confidence)]
        price,price_confidence,amount,ambiguous,price_notes=choose_price(candidates)
'''+s[end:]
s=s.replace('        if not right_regions and not started:continue\n','')
s=s.replace('        if not right_regions and not is_action:amount=None','        if not right_regions and not is_action and price_confidence<.8:amount=None\n        if not right_regions and not started and not is_action:continue')
s=s.replace('        notes=[]','        notes=list(price_notes)')
insert='''
def choose_price(candidates):
    valid=[]
    for text,confidence in candidates:
        amount,ambiguous=numeric_candidate(text)
        if amount is not None and confidence>=.65:
            valid.append((text,confidence,amount,ambiguous))
    if not valid:
        return '',0,None,False,['No se pudo leer el importe con suficiente confianza: complétalo desde la foto.']
    valid.sort(key=lambda v:v[1],reverse=True)
    text,confidence,amount,ambiguous=valid[0]
    notes=[]
    conflicts=[v for v in valid[1:] if v[2]!=amount]
    if conflicts:
        values=', '.join(str(v) for v in sorted({v[2] for v in valid}))
        notes.append('Las lecturas del importe no coinciden ('+values+'): confirma el valor en la foto.')
        if confidence-conflicts[0][1]<.12:
            amount=None
    return text,confidence,amount,ambiguous,notes

'''
s=s.replace('def read_handwritten(image, results):',insert+'def read_handwritten(image, results):');p.write_text(s,encoding='utf-8')
