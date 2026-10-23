from pathlib import Path
p=Path(r'D:\SaaSVilcas\medina-diseno\index.html')
s=p.read_text(encoding='utf-8')
paths={
'clipboard':'<rect x="5" y="4" width="14" height="17" rx="2"/><rect x="9" y="2" width="6" height="4" rx="1"/><path d="M9 11h6M9 15h6"/>',
'settings':'<path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="3"/><circle cx="15" cy="17" r="3"/>',
'helmet':'<path d="M4 16v-3a8 8 0 0 1 16 0v3M9 5v7M15 5v7"/><rect x="2" y="16" width="20" height="4" rx="2"/>',
'brick':'<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18M3 15h18M12 4v5M8 9v6M16 9v6M12 15v5"/>',
'plus':'<path d="M12 5v14M5 12h14"/>',
'calculator':'<rect x="5" y="2" width="14" height="20" rx="2"/><path d="M8 6h8M8 10h2M14 10h2M8 14h2M14 14h2M8 18h2M14 18h2"/>',
'file':'<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M8 13h8M8 17h5"/>',
'percent':'<path d="M5 19 19 5"/><circle cx="6.5" cy="6.5" r="2.5"/><circle cx="17.5" cy="17.5" r="2.5"/>',
'back':'<path d="m10 5-7 7 7 7M3 12h18"/>',
'bin':'<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7"/>',
'building':'<path d="M3 21h18M6 21V3h12v18M9 7h1M14 7h1M9 11h1M14 11h1M10 21v-5h4v5"/>'}
def icon(name):return '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+paths[name]+'</svg>'
for old,new in [
('Generador de presupuesto</h1>',icon('clipboard')+' Generador de presupuesto</h1>'),
('02 &nbsp; Configuración del presupuesto',icon('settings')+' Configuración del presupuesto'),
('03 &nbsp; Mano de obra',icon('helmet')+' Mano de obra'),
('04 &nbsp; Materiales',icon('brick')+' Materiales'),
('Resumen del presupuesto</div>',icon('calculator')+' Resumen del presupuesto</div>'),
('+ Agregar mano de obra',icon('plus')+' Agregar mano de obra'),
('+ Agregar material',icon('plus')+' Agregar material'),
('Vista previa / guardar PDF',icon('file')+' Vista previa / guardar PDF'),
('Incluir impuesto 8%',icon('percent')+' Incluir impuesto 8%'),
('Mostrar Mano de Obra',icon('helmet')+' Mostrar mano de obra'),
('Mostrar Materiales',icon('brick')+' Mostrar materiales'),
('🗑️ Eliminar',icon('bin')+' Eliminar'),
('← Volver al presupuesto',icon('back')+' Volver al presupuesto'),
('>MEDINA CONSTRUCCIONES COMPANY</div>','>'+icon('building')+' MEDINA CONSTRUCCIONES COMPANY</div>')]:s=s.replace(old,new)
s=s.replace('</style>','''
.icon{width:20px;height:20px;display:inline-block;vertical-align:middle;flex-shrink:0}h1 .icon{width:29px;height:29px;margin-right:8px}.company-name .icon{width:30px;height:30px;margin-right:8px}.options-title,.panel-title,.summary-title{display:flex;align-items:center;justify-content:center;gap:10px}.panel-title .icon,.options-title .icon,.summary-title .icon{width:23px;height:23px;background:#edf3ff;box-sizing:content-box;padding:8px;border-radius:9px}.option-item label{gap:8px}.option-item input[type=checkbox]{margin-right:0}.option-item .icon{color:#215bea;width:18px;height:18px}.btn-add,.btn-pdf,.delete-btn{display:flex;align-items:center;justify-content:center;gap:8px}.delete-btn .icon{width:14px;height:14px}#documentPreview::backdrop{background:#18243c66}
</style>''')
p.write_text(s,encoding='utf-8')
