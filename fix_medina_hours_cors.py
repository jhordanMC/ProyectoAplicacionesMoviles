from pathlib import Path
root=Path(r'D:\en html\Sistema de Presupuestos')
p=root/'frontend/src/App.tsx';s=p.read_text(encoding='utf-8')
s=s.replace(';hours?:number','').replace(",[hours,setHours]=useState('')",'')
s=s.replace("if(kind==='labor'&&hours&&(!Number.isFinite(Number(hours))||Number(hours)<0)){setNotice('Las horas deben ser un número positivo.');return}",'')
s=s.replace(",...(kind==='labor'?{hours:Number(hours)||0}:{})",'').replace(";setHours('')",'')
s=s.replace('<div className="two-fields"><div><label htmlFor="labor-amount">','<div><label htmlFor="labor-amount">')
start=s.index('<div><label htmlFor="hours">');end=s.index('<button className="outline full" onClick={()=>add(\'labor\')}',start)
s=s[:start]+s[end:]
s=s.replace('{i.hours!==undefined&&<small>{i.hours} horas estimadas</small>}','')
p.write_text(s,encoding='utf-8')
for rel in ['backend/app/core/config.py','backend/.env.example']:
 p=root/rel;s=p.read_text(encoding='utf-8');s=s.replace('"http://127.0.0.1:5173"]','"http://127.0.0.1:5173", "http://localhost:5174", "http://127.0.0.1:5174"]');p.write_text(s,encoding='utf-8')
p=root/'backend/tests/test_images.py';s=p.read_text(encoding='utf-8');s+='''
@pytest.mark.parametrize('origin',['http://localhost:5174','http://127.0.0.1:5174'])
def test_alternate_vite_port_cors(origin):
    response=TestClient(app).options('/api/v1/analyze-image',headers={'Origin':origin,'Access-Control-Request-Method':'POST','Access-Control-Request-Headers':'content-type'})
    assert response.status_code == 200
    assert response.headers['access-control-allow-origin'] == origin
''';p.write_text(s,encoding='utf-8')
