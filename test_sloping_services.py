from pathlib import Path
root=Path(r'D:\en html\Sistema de Presupuestos')
p=root/'backend/app/services/handwritten.py';s=p.read_text(encoding='utf-8').replace('confidence-conflicts[0][1]<.12 or confidence>=.9','confidence-conflicts[0][1]<.12 or conflicts[0][1]>=.75');p.write_text(s,encoding='utf-8')
p=root/'backend/tests/test_handwritten.py';s=p.read_text(encoding='utf-8');s+='''
def test_two_credible_prices_require_review():
    from app.services.handwritten import choose_price
    assert choose_price([('950',.97),('250',.84)])[2] is None
    assert choose_price([('900',.72),('200',.96)])[2]==200

def test_scope_is_not_abbreviated():
    kind,name,changed=description('Enchape de pared de tragaluz')
    assert kind=='labor'
    assert name=='Enchape de pared de tragaluz'
    assert not changed

def test_real_sloping_service_ledger():
    data=Path(__file__).parent.joinpath('fixtures/servicios.png').read_bytes()
    response=TestClient(app).post('/api/v1/analyze-image',json={'image':base64.b64encode(data).decode(),'mode':'handwritten'})
    assert response.status_code==200
    items=response.json()['items']
    assert len(items)==7
    assert [i['amount'] for i in items]==[1000,300,150,150,None,600,250]
    assert 'Traga' in items[1]['name']
    assert items[4]['notes']
    assert 'fachada' in items[4]['name'].lower()
    assert all(i['source'] for i in items)
''';p.write_text(s,encoding='utf-8')
