from pathlib import Path
p=Path(r'D:\SaaSVilcas\medina-diseno\index.html');s=p.read_text(encoding='utf-8')
s=s.replace('const win=window;', '''const preview=document.getElementById('documentPreview');''')
s=s.replace("win.document.write(documentTemplate(","document.getElementById('documentFrame').srcdoc=documentTemplate(")
s=s.replace("rows,subtotal,tax));win.document.close();","rows,subtotal,tax);preview.showModal();")
s=s.replace('    <script src="documento.js">','''    <dialog id="documentPreview" style="width:min(960px,95vw);height:92vh;max-height:92vh;margin:auto;border:1px solid #e6ebf2;border-radius:12px;padding:14px;background:#f4f7fc"><button onclick="document.getElementById('documentPreview').close()" style="background:white;border:1px solid #dce3ee;padding:10px 16px;border-radius:6px;color:#215bea;cursor:pointer;margin-bottom:12px">← Volver al presupuesto</button><iframe id="documentFrame" title="Vista previa del presupuesto" style="width:100%;height:calc(100% - 58px);border:0"></iframe></dialog>
    <script src="documento.js">''')
p.write_text(s,encoding='utf-8')
