from pathlib import Path
import cv2,numpy as np
from app.services.image_analysis import get_engine,prepare_image
im=np.asarray(prepare_image(Path(r'C:\Users\JHORDAN\AppData\Local\Temp\codex-clipboard-bbef8777-d2ab-41a1-b96a-4e145fe55228.jpg').read_bytes()))
result,_=get_engine()(im)
rows=[]
for box,text,score in sorted(result,key=lambda r:np.mean([p[1] for p in r[0]])):
 ys=[p[1] for p in box];y=np.mean(ys);h=max(ys)-min(ys)
 if rows and abs(y-rows[-1][0])<h*.6: rows[-1][2].append(text)
 else:rows.append([y,h,[text]])
for y,h,texts in rows:
 lo=max(0,int(y-h*.85));hi=min(im.shape[0],int(y+h*.85))
 for side,x0,x1 in [('desc',0,int(im.shape[1]*.84)),('amount',int(im.shape[1]*.84),im.shape[1])]:
  crop=im[lo:hi,x0:x1];crop=cv2.copyMakeBorder(crop,30,30,30,30,cv2.BORDER_CONSTANT,value=(255,255,255))
  r,_=get_engine()(crop)
  print(round(y),side,[item[1] for item in r or []])
