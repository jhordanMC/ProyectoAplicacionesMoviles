from pathlib import Path
root=Path(r'D:\en html\Sistema de Presupuestos')
p=root/'backend/app/services/image_analysis.py';s=p.read_text();s=s.replace('if not text or re.match', 'if normalized.strip("0123456789. |:-") in {"materiales", "mano de obra", "descripcion del material", "descripcion", "detalle de materiales", "detalle de mano de obra"}:\n            continue\n        if not text or re.match');s=s.replace('if len(monetary) > 1:', 'if "|" in name:\n                name = name.split("|")[0].strip()\n            if len(monetary) > 1:');s=s.replace('name = text[:monetary[0].start()].strip(" |;:-")','name = text[:monetary[0].start()].strip(" |;:-").split("|")[0].strip()');p.write_text(s,encoding='utf-8')
# A legible sample list, used both by automated OCR test and user testing.
from PIL import Image, ImageDraw, ImageFont
image=Image.new('RGB',(1400,620),'white');draw=ImageDraw.Draw(image)
font=ImageFont.truetype(r'C:\Windows\Fonts\arial.ttf',38)
for y,line in [(50,'LISTA DE MATERIALES Y SERVICIOS'),(160,'Cemento Portland S/ 1420.00'),(245,'Pintura latex S/ 480.00'),(330,'Construccion de muro S/ 2850.00'),(450,'Total S/ 4750.00')]:draw.text((50,y),line,font=font,fill='black')
p=root/'backend/tests/fixtures/lista.png';p.parent.mkdir(parents=True,exist_ok=True);image.save(p)
image.save(root/'frontend/public/ejemplo-ocr.png')
tests=r'''import base64
from pathlib import Path
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.services.image_analysis import parse_lines, number, group_rows

@pytest.mark.parametrize("value,expected",[("1,420.00",1420),("1.420,00",1420),("28,40",28.4),("1420.00",1420)])
def test_number_formats(value, expected):
    assert number(value) == expected

def test_no_price_invented_from_quantity():
    proposal = parse_lines(["Cemento Portland 50 bolsas"])[0]
    assert proposal['amount'] is None
    assert proposal['kind'] == 'material'

def test_headers_and_total_excluded():
    assert parse_lines(["MATERIALES", "Total S/ 1420.00", "Subtotal materiales S/ 1420.00"]) == []

def test_table_uses_total_column():
    proposal = parse_lines(["Cemento Portland | bolsa | 50 | S/ 28.40 | S/ 1420.00"])[0]
    assert proposal['amount'] == 1420
    assert proposal['name'] == 'Cemento Portland'

def test_group_rows_restores_columns():
    def box(x,y): return [[x,y],[x+70,y],[x+70,y+20],[x,y+20]]
    result = [[box(200,50),'S/ 100.00',.99],[box(10,51),'Cemento',.99],[box(10,100),'Pintura',.99]]
    assert group_rows(result) == ['Cemento | S/ 100.00', 'Pintura']

@pytest.mark.parametrize('payload,status',[({'image':'not base64'},422),({'image':base64.b64encode(b'not an image').decode()},422),({},422)])
def test_invalid_image(payload,status):
    response=TestClient(app).post('/api/v1/analyze-image',json=payload)
    assert response.status_code == status

def test_png_with_wrong_content_type():
    response=TestClient(app).post('/api/v1/analyze-image',content='x',headers={'content-type':'text/plain'})
    assert response.status_code == 415

def test_post_cors():
    response=TestClient(app).options('/api/v1/analyze-image',headers={'Origin':'http://127.0.0.1:5173','Access-Control-Request-Method':'POST','Access-Control-Request-Headers':'content-type'})
    assert response.status_code == 200
    assert 'POST' in response.headers['access-control-allow-methods']

def test_real_local_ocr_end_to_end():
    data=Path(__file__).parent.joinpath('fixtures/lista.png').read_bytes()
    response=TestClient(app).post('/api/v1/analyze-image',json={'image':base64.b64encode(data).decode()})
    assert response.status_code == 200
    result=response.json()
    assert result['method'] == 'local_ocr'
    assert any('Cemento' in i['name'] and i['amount']==1420 for i in result['items'])
    assert any(i['kind']=='labor' and i['amount']==2850 for i in result['items'])
'''
(root/'backend/tests/test_images.py').write_text(tests,encoding='utf-8')
