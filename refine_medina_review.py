from pathlib import Path
p=Path(r'D:\en html\Sistema de Presupuestos\frontend\src\features\ImageAnalyzer.tsx');s=p.read_text();s=s.replace("setBusy(true);setError('');controller", "setBusy(true);setError('');setResult(null);setDrafts([]);controller");s=s.replace('disabled={!drafts.some(d=>d.selected)}','disabled={busy||!drafts.some(d=>d.selected)}');p.write_text(s,encoding='utf-8')
