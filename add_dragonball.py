from pathlib import Path
import shutil
p=Path(r'D:\SaaSVilcas\semana7-preparado/app/src/main/res')
shutil.copyfile(r'C:\Users\JHORDAN\AppData\Local\Temp\codex-clipboard-38033960-04a6-4c44-ac8f-7576cadae473.png',p/'drawable/esfera_dragon.png')
f=p/'layout/activity_main.xml'
s=f.read_text(encoding='utf-8')
s=s.replace('<TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/registro"', '<ImageView android:layout_width="match_parent" android:layout_height="120dp" android:layout_marginBottom="12dp" android:src="@drawable/esfera_dragon" android:scaleType="fitCenter" android:contentDescription="@string/esfera_dragon"/><TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/registro"')
f.write_text(s,encoding='utf-8')
f=p/'values/strings.xml'; f.write_text(f.read_text(encoding='utf-8').replace('</resources>','<string name="esfera_dragon">Esfera del dragón de siete estrellas</string>\n</resources>'),encoding='utf-8')
(p/'drawable/esfera_icono.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
 <item android:gravity="center" android:width="90dp" android:height="60dp" android:drawable="@drawable/esfera_dragon"/>
</layer-list>''',encoding='utf-8')
for name in ['dragon_launcher','dragon_launcher_round']:
 (p/f'mipmap-anydpi-v26/{name}.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
 <background android:drawable="@android:color/black"/>
 <foreground android:drawable="@drawable/esfera_icono"/>
</adaptive-icon>''',encoding='utf-8')
f=p.parent/'AndroidManifest.xml'; f.write_text(f.read_text(encoding='utf-8').replace('android:icon="@mipmap/ic_launcher"','android:icon="@drawable/esfera_dragon"').replace('android:roundIcon="@mipmap/ic_launcher_round"','android:roundIcon="@drawable/esfera_dragon"'),encoding='utf-8')
# Android 8+ resolves adaptive icons; older versions use the supplied PNG.
f=p.parent/'AndroidManifest.xml'; f.write_text(f.read_text(encoding='utf-8').replace('android:icon="@drawable/esfera_dragon"','android:icon="@mipmap/ic_launcher"').replace('android:roundIcon="@drawable/esfera_dragon"','android:roundIcon="@mipmap/ic_launcher_round"'),encoding='utf-8')
d=p/'mipmap'; d.mkdir(exist_ok=True)
for name in ['dragon_launcher','dragon_launcher_round']:
 (d/f'{name}.xml').write_text('''<?xml version="1.0" encoding="utf-8"?><bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:src="@drawable/esfera_dragon" android:gravity="center"/>''',encoding='utf-8')
print('Imagen incorporada')

