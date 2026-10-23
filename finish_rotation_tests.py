from pathlib import Path
import shutil
root=Path(r'D:\en html\Sistema de Presupuestos')
p=root/'frontend/src/features/ImageAnalyzer.tsx';s=p.read_text(encoding='utf-8');s=s.replace('type Result={method:string;', 'type Result={rotation?:number;method:string;');s=s.replace("const [mode,setMode]", "const [rotation,setRotation]=useState('auto');\n const [mode,setMode]");s=s.replace('{image:data,mode},current.signal','{image:data,mode,rotation},current.signal');s=s.replace('</select></label><div className="ocr-upload">','''</select></label>{mode==='handwritten'&&<label className="ocr-format" htmlFor="ocr-rotation">Orientación<select id="ocr-rotation" value={rotation} disabled={busy} onChange={e=>{setRotation(e.target.value);setResult(null);setDrafts([])}}><option value="auto">Detectar automáticamente</option><option value="0">Mantener como está</option><option value="90">Girar 90° a la izquierda</option><option value="180">Girar 180°</option><option value="270">Girar 90° a la derecha</option></select></label>}<div className="ocr-upload">''');s=s.replace('<h3>Revisa las partidas detectadas</h3>', '<h3>Revisa las partidas detectadas</h3>{!!result.rotation&&<p className="ocr-rotation-note">Se aplicó un giro de {result.rotation}° para leer la lista.</p>}');p.write_text(s,encoding='utf-8')
for name,source in [('parrilla_girada.jpg','codex-clipboard-40feed6b-2420-4067-ae3d-ee7f6d8d7dfc.jpg'),('parrilla_vertical.jpg','codex-clipboard-079a9f92-fd6a-44ff-b6ab-e46b72cc49d1.jpg')]:
 shutil.copy2(Path(r'C:\Users\JHORDAN\AppData\Local\Temp')/source,root/'backend/tests/fixtures'/name)
p=root/'backend/tests/test_handwritten.py';s=p.read_text(encoding='utf-8');s+='''
@pytest.mark.parametrize("name,angle",[("parrilla_girada.jpg",90),("parrilla_vertical.jpg",0)])
def test_real_multiline_photos(name,angle):
    data=Path(__file__).parent.joinpath('fixtures',name).read_bytes()
    response=TestClient(app).post('/api/v1/analyze-image',json={'image':base64.b64encode(data).decode(),'mode':'handwritten'})
    assert response.status_code==200
    result=response.json()
    assert result['rotation']==angle
    assert len(result['items'])==3
    assert [i['amount'] for i in result['items']]==[1250,500,1300]
    assert sum(i['amount'] for i in result['items'])==3050
    assert 'Continuación:' in result['items'][0]['source']
    assert all(i['kind']=='labor' for i in result['items'])
    assert not any('Mario' in i['name'] for i in result['items'])

def test_invalid_rotation_rejected():
    response=TestClient(app).post('/api/v1/analyze-image',json={'image':'x','mode':'handwritten','rotation':'45'})
    assert response.status_code==422
''';p.write_text(s,encoding='utf-8')
