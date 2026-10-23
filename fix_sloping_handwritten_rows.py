from pathlib import Path
p=Path(r'D:\en html\Sistema de Presupuestos\backend\app\services\handwritten.py');s=p.read_text(encoding='utf-8')
a=s.index('def detected_rows(results):');b=s.index('\ndef choose_price(',a)
s=s[:a]+'''def detected_rows(results):
    # Align each sloping text baseline at the price column, rather than comparing box centers.
    reference=max((float(p[0]) for box,_,_ in results or [] for p in box),default=1)*.90
    def geometry(box):
        xs=[float(p[0]) for p in box];ys=[float(p[1]) for p in box]
        span=max(xs)-min(xs)
        slope=((box[1][1]-box[0][1])+(box[2][1]-box[3][1]))/(2*max(span,1))
        center_x=sum(xs)/len(xs);center_y=sum(ys)/len(ys)
        return center_y+slope*(reference-center_x), max(10,((box[3][1]-box[0][1])+(box[2][1]-box[1][1]))/2)
    rows=[]
    for box,text,confidence in sorted(results or [],key=lambda r:geometry(r[0])[0]):
        if not text.strip():continue
        xs=[float(p[0]) for p in box];ys=[float(p[1]) for p in box]
        y,height=geometry(box)
        region={'x':min(xs),'right':max(xs),'top':min(ys),'bottom':max(ys),'text':text,'confidence':float(confidence)}
        if rows and abs(y-rows[-1]['y'])<=max(8,min(height,rows[-1]['height'])*.75):
            rows[-1]['cells'].append((min(xs),text));rows[-1]['regions'].append(region)
        else:
            rows.append({'y':y,'height':height,'cells':[(min(xs),text)],'regions':[region]})
    return rows

'''+s[b:]
s=s.replace("if confidence-conflicts[0][1]<.12:","if confidence-conflicts[0][1]<.12 or confidence>=.9:")
s=s.replace("if not started and amount is None:continue","if not started and amount is None and not starts_action(text+' '+raw):continue")
s=s.replace("        if not text.strip():text=' '.join(t for x,t in sorted(row['cells']) if x<split)","        detected_text=' '.join(t for x,t in sorted(row['cells']) if x<split)\n        if len(text.strip())<len(detected_text)*.70:text=detected_text\n        text=re.sub(r'\\d[0-9Oo]{2,}$','',text).strip()")
s=s.replace("    if action:\n", "    if action and any(has_term(raw,t) for t in ['tragaluz','contorno','interior','exterior','fachada','garaje','dormitorio','tuberia','agua','mayolica']):\n        return 'labor',raw,False\n    if action:\n")
s+='''

def starts_action(text):
    compact=re.sub(r'[^a-z]','',normalize(text))
    for term in ['enchape','instalacion','construccion','tarrajeo','asentado','demolicion','encofrado','acarreo','reparacion','sacar','resanar']:
        for size in range(max(3,len(term)-1),len(term)+3):
            if SequenceMatcher(None,term,compact[:size]).ratio()>=.75:return True
    return False
''';p.write_text(s,encoding='utf-8')
