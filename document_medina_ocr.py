from pathlib import Path
root=Path(r'D:\en html\Sistema de Presupuestos')
p=root/'README.md';s=p.read_text(encoding='utf-8');s=s.replace('agregar personas y partidas','asignar un cliente titular y agregar partidas').replace('PDF descargable, QR e IA quedan pendientes.','PDF descargable, QR y visión generativa quedan pendientes. El análisis de imágenes ya funciona con OCR local.').replace('personas e historial responsive','cliente titular e historial responsive');s+='''
## Analizador de imágenes local (Python)

`POST /api/v1/analyze-image` usa RapidOCR + ONNX Runtime y Pillow en CPU. No necesita API key, no envía imágenes a proveedores externos y no almacena archivos subidos. Dependencias/modelos incluidos en la instalación del paquete RapidOCR 1.4.4, licencia Apache 2.0; ver https://github.com/RapidAI/RapidOCR/tree/v1.4.4.

Desde backend, usar el entorno aislado instalado:
```powershell
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
.\.venv\Scripts\python.exe -m pytest -q
```
En React: Analizar imagen → PNG/JPG/WEBP de hasta 10 MB y 16 megapíxeles → analizar → corregir tipo, descripción e importe → agregar partidas revisadas.
La imagen `frontend/public/ejemplo-ocr.png` permite probar una lista legible. El OCR extrae texto y el parser propone partidas conocidas; no calcula materiales desde geometría de planos ni estima precios faltantes. Fotos sin texto no producen partidas. Los acentos, escritura manual, imágenes borrosas y formatos complejos pueden necesitar corrección. Una cantidad sin precio se devuelve con importe vacío.
La primera carga del modelo puede tardar más. Se procesa una imagen a la vez. En un servidor distinto las imágenes se enviarán a ese backend para procesarlas allí.

## Cliente del presupuesto

Un único nombre o razón social define al titular. Es obligatorio al generar. Se guarda con el historial local y puede buscarse junto al número del presupuesto. El selector propone clientes usados anteriormente. Presupuestos antiguos sin cliente muestran "Sin cliente (anterior)".
No se asignan trabajadores al presupuesto ni a mano de obra. El cliente no se imprime en la vista del documento, siguiendo la restricción del diseño aprobado.
''';p.write_text(s,encoding='utf-8')
p=root/'docs/verificacion-clientes-ocr.md';p.write_text('''# Clientes y OCR local

- TypeScript y Vite compilaron correctamente.
- Backend aislado: 18 pruebas pasaron, incluida extracción OCR real de imagen PNG.
- Probados: lectura de importes con separadores, no confundir cantidades con precios, omitir totales, reconstruir columnas, formatos inválidos, CORS POST y salud.
- Imagen real de prueba: cemento 1420, pintura 480 y construcción de muro 2850; total 4750.
- El cliente pertenece al presupuesto; no se interpreta como trabajador.
- Revisar y confirmar cada propuesta antes de importarla. Importe desconocido queda vacío.
- Sin API key ni proveedores externos. Sin conexión nueva a Supabase, commits, push o despliegues.
''',encoding='utf-8')
