from pathlib import Path
p=Path(r'D:\en html\Sistema de Presupuestos\backend\app\services\handwritten.py');s=p.read_text(encoding='utf-8')
s=s.replace("'(':'1'","'(':'1','C':'0'")
s=s.replace("min(height,rows[-1]['height'])*.6","min(height,rows[-1]['height'])*.75")
s=s.replace("'bottom':max(ys)}","'bottom':max(ys),'text':text,'confidence':float(confidence)}")
s=s.replace("price,price_confidence=read(array[lo:hi,split:])\n        amount,ambiguous=numeric_candidate(price)","""right_regions=[r for r in row['regions'] if r['x']>=split]
        detected_prices=[r for r in right_regions if numeric_candidate(r['text'])[0] is not None]
        if detected_prices:
            region=max(detected_prices,key=lambda r:r['confidence'])
            price,price_confidence=region['text'],region['confidence']
        else:
            price,price_confidence=read(array[lo:hi,split:])
        amount,ambiguous=numeric_candidate(price)""")
s=s.replace("if amount is None and not started:continue", "if not right_regions and not started:continue")
s=s.replace("started=True\n        kind,name,normalized", "is_action=any(has_term(text+' '+raw,t) for t in ['demolicion','asentado','encofrado','tarrajeo','acarreo','construccion','enchape','instalacion','excavacion'])\n        if not right_regions and not is_action:amount=None\n        started=True\n        kind,name,normalized")
s=s.replace("is_action=any(has_term(text,t) for t in ['demolicion','asentado','encofrado','tarrajeo','acarreo','construccion','enchape','instalacion','excavacion'])\n        continuation=amount is None and proposals", "continuation=amount is None and not right_regions and proposals")
p.write_text(s,encoding='utf-8')
