from pathlib import Path
p=Path(r'D:\SaaSVilcas\medina-diseno\index.html')
s=p.read_text(encoding='utf-8')
import re
s=re.sub(r'<aside class="sidebar">.*?</aside>','',s,flags=re.S)
s=re.sub(r'<div class="eyebrow">.*?</section>','''<h1>Generador de presupuesto</h1><p class="intro">Agrega los trabajos y materiales de tu proyecto.</p>''',s,count=1,flags=re.S)
# Remove metric updates since this design follows the original single-window layout.
s=re.sub(r"            const l=document.getElementById\('showLaborOption'\).*?(?=            // Recalcular)",'',s,flags=re.S)
s=s.replace("esc(document.getElementById('docNumber').value),esc(document.getElementById('clientName').value||'Cliente por definir'),esc(document.getElementById('projectName').value||'Proyecto por definir')","'PRE-001','Cliente por definir','Proyecto por definir'")
css='''
/* Composición original: encabezado, opciones y dos paneles en una ventana. */
body{padding:28px;background:#f4f7fc}.header{max-width:1200px;margin:auto;text-align:center;padding:30px;background:white;border:1px solid #e6ebf2;border-bottom:3px solid #215bea;border-radius:12px 12px 0 0}.company-name{color:#215bea;font-size:25px;font-weight:750;letter-spacing:.2px}.company-slogan{font-style:italic;margin:10px 0;color:#6d7a90}.company-contact{display:block;color:#66758b;font-size:12px;line-height:1.6}.container{max-width:1200px;background:white;margin:auto;padding:32px;border:1px solid #e6ebf2;border-top:0;border-radius:0 0 12px 12px}h1{text-align:center;font-size:26px;color:#215bea;letter-spacing:0;margin-bottom:10px}.intro{text-align:center;margin-bottom:28px}.options-panel{background:#f8faff}.options-title{text-align:center;color:#215bea}.panel-title{text-align:center;color:#215bea}.panels-container{margin-bottom:0}.summary-title{text-align:center}.btn-pdf{max-width:none}.summary-panel{margin-top:24px}@media(max-width:700px){body{padding:12px}.header{padding:24px 16px}.company-name{font-size:20px}.container{padding:24px 16px}h1{font-size:22px}}
'''
s=s.replace('</style>',css+'</style>')
# Open the printable document in this tab, so the whole flow stays in one window.
s=s.replace("const win=window.open('', '_blank');\n            if(!win){alert('Permite las ventanas emergentes para abrir el documento.');return;}","const win=window;")
p.write_text(s,encoding='utf-8')
