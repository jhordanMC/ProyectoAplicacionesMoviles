from pathlib import Path
import numpy as np
from app.services.image_analysis import prepare_image,get_engine
im=np.asarray(prepare_image(Path(r'C:\Users\JHORDAN\AppData\Local\Temp\codex-clipboard-2f8cbe1c-d205-4a8e-97e5-34e570de46b2.png').read_bytes()))
for top,bottom in [(800,1120),(850,1150)]:
 r,_=get_engine()(im[top:bottom])
 print(top,[(b[1],round(b[2],2),b[0]) for b in r or []],flush=True)
