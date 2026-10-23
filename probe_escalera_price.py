from pathlib import Path
import numpy as np,cv2
from app.services.image_analysis import get_engine,prepare_image
im=np.asarray(prepare_image(Path(r'C:\Users\JHORDAN\Downloads\WhatsApp Image 2026-10-13 at 1.40.48 AM.jpeg').read_bytes()))
for x in [.83,.85,.87,.89]:
 crop=im[485:585,int(im.shape[1]*x):]
 for name,c in [('rgb',crop),('red',cv2.cvtColor(crop[:,:,0],cv2.COLOR_GRAY2RGB))]:
  result,_=get_engine()(c,use_det=False,use_cls=False,use_rec=True)
  print(x,name,result,flush=True)
