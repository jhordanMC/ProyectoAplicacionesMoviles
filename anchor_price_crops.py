from pathlib import Path
p=Path(r'D:\en html\Sistema de Presupuestos\backend\app\services\handwritten.py');s=p.read_text(encoding='utf-8');s=s.replace("        direct_price,direct_confidence=read(array[lo:hi,split:])", """        anchors=[r for r in detected_prices if r['x']>=split]
        if anchors:
            anchor=max(anchors,key=lambda r:r['confidence'])
            padding=max(3,(anchor['bottom']-anchor['top'])*.15)
            price_top=max(0,int(anchor['top']-padding));price_bottom=min(height,int(anchor['bottom']+padding))
        else:
            price_top,price_bottom=lo,hi
        direct_price,direct_confidence=read(array[price_top:price_bottom,split:])""");p.write_text(s,encoding='utf-8')
