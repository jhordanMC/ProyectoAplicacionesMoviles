from pathlib import Path
root=Path(r'D:\en html\Sistema de Presupuestos')
for rel in ['frontend/src/features/ImageAnalyzer.tsx','backend/app/services/image_analysis.py','backend/app/main.py']:
 p=root/rel;s=p.read_text(encoding='utf-8')
 for _ in range(3):
  if not any(c in s for c in ['Ã','Â','â€']):break
  try:s=s.encode('cp1252').decode('utf-8')
  except (UnicodeEncodeError,UnicodeDecodeError):break
 p.write_text(s,encoding='utf-8')
 print(rel, 'UTF-8 checked')
