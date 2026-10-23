from pathlib import Path
p=Path(r'D:\en html\Sistema de Presupuestos\backend\app\services\handwritten.py');s=p.read_text(encoding='utf-8');s=s.replace("'x':min(xs),'top':","'x':min(xs),'right':max(xs),'top':")
s=s.replace("right_regions=[r for r in row['regions'] if r['x']>=split]", "right_regions=[r for r in row['regions'] if r['x']>=split or (r['right']>=width*.92 and numeric_candidate(r['text'])[0] is not None)]")
s=s.replace('if not right_regions and not is_action and price_confidence<.8:amount=None\n        if not right_regions and not started and not is_action:continue','if not right_regions and not is_action:amount=None\n        if not started and amount is None:continue')
p.write_text(s,encoding='utf-8')
