from pathlib import Path
import numpy as np
from app.services.image_analysis import get_engine,prepare_image
from app.services.handwritten import detected_rows
im=np.asarray(prepare_image(Path(r'C:\Users\JHORDAN\Downloads\WhatsApp Image 2026-10-13 at 1.40.48 AM.jpeg').read_bytes()))
r,_=get_engine()(im)
for row in detected_rows(r):
 y=row['y'];h=row['height'];crop=im[max(0,int(y-h*.8)):int(y+h*.8),int(im.shape[1]*.83):]
 result,_=get_engine()(crop,use_det=False,use_cls=False,use_rec=True)
 print(round(y),row['cells'],result,flush=True)
