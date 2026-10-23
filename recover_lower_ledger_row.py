from pathlib import Path
p=Path(r'D:\en html\Sistema de Presupuestos\backend\app\services\handwritten.py');s=p.read_text(encoding='utf-8')
s=s.replace("    corrected = tail.translate", "    suffix=re.search(r'(?:\\d[0-9OolISgG]{2,}|[BSsgG][Oo0]{2})$',tail)\n    if suffix and suffix.start()>0:tail=suffix.group()\n    corrected = tail.translate")
s=s.replace("'G':'9'","'G':'9','B':'3'").replace("r'[sSgG][oO0]{2}'","r'[BSsgG][oO0]{2}'")
s=s.replace("'text':text,'confidence':float(confidence)}","'text':text,'confidence':float(confidence),'box':box}")
a="""        else:
            rows.append({'y':y,'height':height,'cells':[(min(xs),text)],'regions':[region]})
    return rows"""
b="""        else:
            joined=False
            for previous in rows[-3:]:
                for neighbor in previous['regions']:
                    if not (region['right']<=neighbor['x'] or neighbor['right']<=region['x']):continue
                    gap=max(region['x'],neighbor['x'])-min(region['right'],neighbor['right'])
                    if gap>reference*.20:continue
                    common=(min(region['right'],neighbor['right'])+max(region['x'],neighbor['x']))/2
                    def at(box,x):
                        cx=sum(q[0] for q in box)/4;cy=sum(q[1] for q in box)/4
                        span=max(q[0] for q in box)-min(q[0] for q in box)
                        slope=((box[1][1]-box[0][1])+(box[2][1]-box[3][1]))/(2*max(span,1))
                        return cy+slope*(x-cx)
                    if abs(at(box,common)-at(neighbor['box'],common))<=max(8,height*.60):
                        previous['cells'].append((min(xs),text));previous['regions'].append(region);joined=True;break
                if joined:break
            if not joined:rows.append({'y':y,'height':height,'cells':[(min(xs),text)],'regions':[region]})
    return rows"""
assert a in s;s=s.replace(a,b)
s+='''

def recover_lower_rows(image,results):
    if not results:return results
    width,height=image.size
    lowest=max(results,key=lambda r:sum(p[1] for p in r[0])/4)[0]
    center=sum(p[1] for p in lowest)/4
    band=max(50,max(p[1] for p in lowest)-min(p[1] for p in lowest))
    top=max(0,int(center-band*1.2));bottom=min(height,int(center+band*2.5))
    if bottom-top<40:return results
    extra,_=get_engine()(np.asarray(image)[top:bottom])
    combined=list(results)
    def rect(box):return min(p[0] for p in box),min(p[1] for p in box),max(p[0] for p in box),max(p[1] for p in box)
    for box,text,score in extra or []:
        moved=[[float(x),float(y)+top] for x,y in box];a=rect(moved);duplicate=False
        for old,_,_ in combined:
            b=rect(old);intersection=max(0,min(a[2],b[2])-max(a[0],b[0]))*max(0,min(a[3],b[3])-max(a[1],b[1]))
            smaller=max(1,min((a[2]-a[0])*(a[3]-a[1]),(b[2]-b[0])*(b[3]-b[1])))
            if intersection/smaller>.5:duplicate=True;break
        if not duplicate:combined.append([moved,text,score])
    return combined
''';p.write_text(s,encoding='utf-8')
p=Path(r'D:\en html\Sistema de Presupuestos\backend\app\services\image_analysis.py');s=p.read_text(encoding='utf-8').replace('read_handwritten, orient_handwritten','read_handwritten, orient_handwritten, recover_lower_rows').replace('items, _ = read_handwritten(image, result)','result = recover_lower_rows(image, result)\n            items, _ = read_handwritten(image, result)');p.write_text(s,encoding='utf-8')
