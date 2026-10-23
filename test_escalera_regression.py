from pathlib import Path
import shutil
root=Path(r'D:\en html\Sistema de Presupuestos')
p=root/'backend/app/services/handwritten.py';s=p.read_text(encoding='utf-8');s=s.replace("objects=[('drywall'","objects=[('escalera','escalera'),('drywall'");s=s.replace("actions=[('demolicion'","actions=[('construccion','Construcción'),('enchape','Enchape'),('demolicion'");p.write_text(s,encoding='utf-8')
shutil.copy2(r'C:\Users\JHORDAN\Downloads\WhatsApp Image 2026-10-13 at 1.40.48 AM.jpeg',root/'backend/tests/fixtures/escalera.jpg')
p=root/'backend/tests/test_handwritten.py';s=p.read_text(encoding='utf-8');s+='''
def test_conflicting_prices_are_reported():
    from app.services.handwritten import choose_price
    result=choose_price([('900',.72),('200',.88)])
    assert result[2]==200
    assert result[4]
    assert choose_price([('900',.80),('200',.83)])[2] is None
    assert choose_price([('560',.34)])[2] is None

def test_real_missing_price_boxes():
    data=Path(__file__).parent.joinpath('fixtures/escalera.jpg').read_bytes()
    response=TestClient(app).post('/api/v1/analyze-image',json={'image':base64.b64encode(data).decode(),'mode':'handwritten'})
    assert response.status_code==200
    items=response.json()['items']
    assert len(items)==5
    assert [i['amount'] for i in items]==[1000,900,None,200,100]
    assert items[0]['name']=='Construcción de escalera'
    assert items[1]['name']=='Tarrajeo de escalera'
    assert items[2]['name']=='Enchape de escalera'
    assert items[2]['notes']
    assert items[3]['notes']
    assert all(i['kind']=='labor' for i in items)
''';p.write_text(s,encoding='utf-8')
