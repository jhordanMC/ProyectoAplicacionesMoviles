from pathlib import Path
import base64
from app.services.image_analysis import analyze
import app.services.handwritten as handwritten
original=handwritten.choose_price
def debug(candidates):
 result=original(candidates)
 print('PRICE',candidates,'=>',result[2],flush=True)
 return result
handwritten.choose_price=debug
for name in ['escalera.jpg','servicios.png']:
 p=Path('tests/fixtures')/name
 if p.exists():
  print('FILE',name,flush=True)
  analyze(base64.b64encode(p.read_bytes()).decode(),'handwritten')
