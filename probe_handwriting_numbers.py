from pathlib import Path
import numpy as np
from app.services.image_analysis import get_engine,prepare_image
im=np.asarray(prepare_image(Path(r'C:\Users\JHORDAN\AppData\Local\Temp\codex-clipboard-bbef8777-d2ab-41a1-b96a-4e145fe55228.jpg').read_bytes()))
for y in [274,413,553,700,852]:
 crop=im[y-60:y+60,int(im.shape[1]*.86):]
 for name,c in [('original',crop)]:
  result,_=get_engine()(c,use_det=False,use_cls=False,use_rec=True)
  print(y,result,flush=True)
