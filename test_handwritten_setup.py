from pathlib import Path
import shutil
root=Path(r'D:\en html\Sistema de Presupuestos')
shutil.copy2(r'C:\Users\JHORDAN\AppData\Local\Temp\codex-clipboard-bbef8777-d2ab-41a1-b96a-4e145fe55228.jpg',root/'backend/tests/fixtures/manuscrita.jpg')
p=root/'backend/tests/test_handwritten.py';p.write_text('''import base64
from pathlib import Path
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.services.handwritten import numeric_candidate, description
from app.services.image_analysis import parse_lines

@pytest.mark.parametrize("text,expected,corrected",[("150",150,False),("700",700,False),("as S00",500,True),("400",400,False),("250",250,False),("sol",None,False),("III",None,False),("0",None,False)])
def test_price_column(text,expected,corrected):
    assert numeric_candidate(text)==(expected,corrected)

def test_quantity_in_printed_document_is_not_price():
    assert parse_lines(["Cemento Portland 50 bolsas"])[0]['amount'] is None

def test_unknown_description_is_preserved():
    assert description("Trabajo especial en techo")[1] == "Trabajo especial en techo"

def test_real_handwritten_photo():
    image=Path(__file__).parent.joinpath('fixtures/manuscrita.jpg').read_bytes()
    response=TestClient(app).post('/api/v1/analyze-image',json={'image':base64.b64encode(image).decode(),'mode':'handwritten'})
    assert response.status_code==200
    items=response.json()['items']
    assert [i['amount'] for i in items]==[150,700,500,400,250]
    assert [i['name'] for i in items]==['Demolición de drywall','Asentado de ladrillo','Encofrado y vaciado de 3 columnas','Tarrajeo de pared','Acarreo de material']
    assert all(i['kind']=='labor' for i in items)
    assert sum(i['amount'] for i in items)==2000
    assert items[2]['notes']
    assert all(i['source'] for i in items)

def test_unknown_format_rejected():
    response=TestClient(app).post('/api/v1/analyze-image',json={'image':'x','mode':'invalid'})
    assert response.status_code==422
''',encoding='utf-8')
