from pathlib import Path
root=Path(r'D:\en html\Sistema de Presupuestos')
p=root/'backend/app/services/handwritten.py';s=p.read_text(encoding='utf-8')
s=s.replace("{'O':'0','o':'0','S':'5','s':'5','I':'1','l':'1','|':'1'}","{'O':'0','o':'0','S':'5','s':'5','I':'1','l':'1','|':'1','(':'1'}")
# retain descriptive bounds separately from the price column
s=s.replace("rows[-1]['cells'].append((min(xs),text))","rows[-1]['cells'].append((min(xs),text))\n            rows[-1]['regions'].append({'x':min(xs),'top':min(ys),'bottom':max(ys)})")
s=s.replace("'cells':[(min(xs),text)]}","'cells':[(min(xs),text)],'regions':[{'x':min(xs),'top':min(ys),'bottom':max(ys)}]}")
s=s.replace("text,text_confidence=read(array[lo:hi,:split])", "left_regions=[r for r in row['regions'] if r['x']<split]\n        text_top=max(0,int(min(r['top'] for r in left_regions)-4)) if left_regions else lo\n        text_bottom=min(height,int(max(r['bottom'] for r in left_regions)+4)) if left_regions else hi\n        text,text_confidence=read(array[text_top:text_bottom,:split])")
s=s.replace("proposals.append({'kind':kind,'name':name[:300],'amount':amount,'source':raw+'\\nLectura por columnas: '+text+' | '+price,'notes':notes})", """entry={'kind':kind,'name':name[:300],'amount':amount,'source':raw+'\\nLectura por columnas: '+text+' | '+price,'notes':notes}
        is_action=any(has_term(text,t) for t in ['demolicion','asentado','encofrado','tarrajeo','acarreo','construccion','enchape','instalacion','excavacion'])
        continuation=amount is None and proposals and not is_action and row['y']-previous_y<max(90,row['height']*2.3)
        if continuation:
            proposals[-1]['name']=(proposals[-1]['name']+' '+text.strip())[:300]
            proposals[-1]['source']+='\\nContinuación: '+raw
            proposals[-1]['notes'].append('Se unió una línea sin importe a la descripción anterior: revisa el alcance completo.')
        else:
            proposals.append(entry)
        previous_y=row['y']""")
s+='''

def orient_handwritten(image, results, rotation="auto"):
    engine=get_engine()
    def price_count(candidate, boxes):
        width=candidate.width
        return sum(1 for box,text,confidence in boxes or [] if min(p[0] for p in box)>width*.70 and numeric_candidate(text)[0] is not None)
    if rotation != "auto":
        angle=int(rotation)
        if not angle:return image,results,0
        candidate=image.rotate(angle,expand=True)
        boxes,_=engine(np.asarray(candidate))
        return candidate,boxes,angle
    best=(image,results,0);best_score=price_count(image,results)
    if best_score>=2:return best
    for angle in [90,180,270]:
        candidate=image.rotate(angle,expand=True)
        boxes,_=engine(np.asarray(candidate))
        score=price_count(candidate,boxes)
        if score>best_score:best=(candidate,boxes,angle);best_score=score
        if score>=2:break
    return best
'''
p.write_text(s,encoding='utf-8')
p=root/'backend/app/services/image_analysis.py';s=p.read_text(encoding='utf-8').replace('mode: str = "document")','mode: str = "document", rotation: str = "auto")');s=s.replace('from app.services.handwritten import read_handwritten','from app.services.handwritten import read_handwritten, orient_handwritten');s=s.replace('items, _ = read_handwritten(image, result)','image, result, applied_rotation = orient_handwritten(image, result, rotation)\n            items, _ = read_handwritten(image, result)\n            lines = group_rows(result)');s=s.replace('lines = group_rows(result)\n    items =','applied_rotation = 0\n    lines = group_rows(result)\n    items =');s=s.replace('"method":"local_ocr", "text":','"method":"local_ocr", "rotation":applied_rotation, "text":');p.write_text(s,encoding='utf-8')
p=root/'backend/app/api/v1/endpoints/images.py';s=p.read_text(encoding='utf-8').replace('mode: Literal["document", "handwritten"] = "document"','mode: Literal["document", "handwritten"] = "document"\n    rotation: Literal["auto", "0", "90", "180", "270"] = "auto"');s=s.replace('analyze, payload.image, payload.mode)','analyze, payload.image, payload.mode, payload.rotation)');p.write_text(s,encoding='utf-8')
