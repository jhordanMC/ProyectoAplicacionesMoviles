from pathlib import Path
root=Path(r'D:\en html\Sistema de Presupuestos')
files={
'backend/app/services/image_analysis.py':r'''"""Local OCR + conservative parsing. Images never leave this server."""
import base64
import binascii
import io
import math
import re
import threading
import unicodedata
import warnings
from functools import lru_cache
from PIL import Image, ImageOps, UnidentifiedImageError
from fastapi import HTTPException

MAX_IMAGE_BYTES = 10 * 1024 * 1024
MAX_PIXELS = 16_000_000
engine_lock = threading.Lock()

def number(value: str) -> float:
    value = value.replace(" ", "")
    if "," in value and "." in value:
        decimal = "," if value.rfind(",") > value.rfind(".") else "."
        value = value.replace("." if decimal == "," else ",", "").replace(decimal, ".")
    elif "," in value:
        value = value.replace(",", ".") if len(value.rsplit(",", 1)[1]) <= 2 else value.replace(",", "")
    return float(value)

def normalize(value: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFD", value.lower()) if unicodedata.category(c) != "Mn")

MATERIALS = re.compile(r"cement|acero|pintura|arena|ladrillo|bloque|fierro|tubo|cable|madera|concreto|yeso|ceramic|clavo|tornillo|grava|piedra|malla|sellador|masilla|calamina|aditivo|material")
LABOR = re.compile(r"mano de obra|instalacion|construccion|tarrajeo|pintado|colocacion|demolicion|excavacion|trazo|replanteo|acabados|limpieza|servicio")
PRICE = re.compile(r"(?<![\w])(?:S\s*[/\\.]\s*)?(\d+(?:[.,]\d+)*)(?![\w])", re.I)

def parse_lines(lines: list[str]) -> list[dict]:
    proposals = []
    for raw in lines[:500]:
        text = raw.strip()
        normalized = normalize(text)
        if not text or re.match(r"^(?:subtotal|total|igv|impuesto|ruc|dni|fecha|telefono|tel:|presupuesto|boleta|factura)\b", normalized):
            continue
        kind = "labor" if LABOR.search(normalized) else "material" if MATERIALS.search(normalized) else None
        if not kind:
            continue
        matches = list(PRICE.finditer(text))
        # Currency or a decimal amount is required; never treat a bare quantity as a price.
        monetary = [m for m in matches if re.search(r"S\s*[/\\.]", m.group(), re.I) or re.search(r"[.,]\d{2}$", m.group(1))]
        amount = None
        name = text
        if monetary:
            candidate = monetary[-1]
            try:
                parsed = number(candidate.group(1))
                if math.isfinite(parsed) and 0 < parsed <= 1_000_000_000:
                    amount = parsed
            except ValueError:
                pass
            name = text[:candidate.start()].strip(" |;:-")
            # Trim trailing quantity/unit/price columns, preserving dimensions in the description.
            name = re.sub(r"\s+(?:bolsas?|kg|und|unidades?|m[23²³]?|litros?|galones?|horas?|h)\s+\d.*$", "", name, flags=re.I).strip()
            if len(monetary) > 1:
                name = text[:monetary[0].start()].strip(" |;:-")
        if not name:
            name = text
        proposals.append({"kind":kind, "name":name[:300], "amount":amount, "source":text})
    return proposals[:100]

def image_bytes(data: str) -> bytes:
    try:
        binary = base64.b64decode(data, validate=True)
    except (binascii.Error, ValueError):
        raise HTTPException(422, "La imagen no tiene una codificación válida")
    if not binary or len(binary) > MAX_IMAGE_BYTES:
        raise HTTPException(413, "La imagen debe pesar como máximo 10 MB")
    return binary

def prepare_image(binary: bytes):
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(io.BytesIO(binary)) as source:
                if source.format not in {"PNG", "JPEG", "WEBP"}:
                    raise HTTPException(415, "Usa una imagen PNG, JPG o WEBP")
                if source.width * source.height > MAX_PIXELS:
                    raise HTTPException(413, "La imagen excede 16 megapíxeles")
                image = ImageOps.exif_transpose(source).convert("RGB")
                image.thumbnail((2400, 2400))
                return image
    except HTTPException:
        raise
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError, Image.DecompressionBombWarning, ValueError):
        raise HTTPException(422, "No se pudo leer la imagen; puede estar dañada")

@lru_cache
def get_engine():
    from rapidocr_onnxruntime import RapidOCR
    return RapidOCR(intra_op_num_threads=2, inter_op_num_threads=2)

def group_rows(results) -> list[str]:
    boxes = []
    for box, text, confidence in results or []:
        if not text.strip():
            continue
        xs = [float(p[0]) for p in box]
        ys = [float(p[1]) for p in box]
        boxes.append((sum(ys)/len(ys), min(xs), max(ys)-min(ys), text))
    rows = []
    for y, x, height, text in sorted(boxes):
        if rows and abs(y-rows[-1]['y']) <= max(8, min(height, rows[-1]['height'])*0.6):
            rows[-1]['cells'].append((x,text))
        else:
            rows.append({'y':y,'height':height,'cells':[(x,text)]})
    return [' | '.join(t for _,t in sorted(row['cells'])) for row in rows]

def analyze(data: str) -> dict:
    image = prepare_image(image_bytes(data))
    import numpy as np
    with engine_lock:
        result, _ = get_engine()(np.asarray(image))
    lines = group_rows(result)
    return {"method":"local_ocr", "text":"\n".join(lines), "items":parse_lines(lines), "warnings":["Revisa las descripciones y los importes. OCR puede confundir dígitos y signos.", "Los importes desconocidos quedan vacíos; no se estiman precios ni cantidades de planos."]}
''',
'backend/app/api/v1/endpoints/images.py':r'''import asyncio
import json
from fastapi import APIRouter, Request, HTTPException
from pydantic import BaseModel, Field, ValidationError
from starlette.concurrency import run_in_threadpool
from app.services.image_analysis import analyze

router = APIRouter()
analysis_slot = asyncio.Semaphore(1)
class ImageRequest(BaseModel):
    image: str = Field(min_length=1, max_length=14_000_000)

@router.post("/analyze-image")
async def analyze_image(request: Request):
    if request.headers.get('content-type', '').split(';')[0].strip() != 'application/json':
        raise HTTPException(415, "Envía la imagen como JSON")
    body = bytearray()
    async for chunk in request.stream():
        body.extend(chunk)
        if len(body) > 14_000_100:
            raise HTTPException(413, "La imagen debe pesar como máximo 10 MB")
    try:
        payload = ImageRequest.model_validate(json.loads(body))
    except (ValueError, ValidationError, UnicodeDecodeError):
        raise HTTPException(422, "La solicitud de imagen no es válida")
    if analysis_slot.locked():
        raise HTTPException(429, "Hay un análisis en curso. Intenta de nuevo en unos segundos")
    async with analysis_slot:
        return await run_in_threadpool(analyze, payload.image)
'''
}
for name,content in files.items():
 p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content,encoding='utf-8')
p=root/'backend/app/main.py';s=p.read_text();s=s.replace('from app.core.config import get_settings','from app.core.config import get_settings\nfrom app.api.v1.endpoints.images import router as images_router').replace('allow_methods=["GET"]','allow_methods=["GET", "POST"]').replace('app.include_router(router, prefix="/api/v1", tags=["health"])','app.include_router(router, prefix="/api/v1", tags=["health"])\napp.include_router(images_router, prefix="/api/v1", tags=["images"])');p.write_text(s,encoding='utf-8')
p=root/'backend/pyproject.toml';s=p.read_text().replace('"httpx>=0.27,<1"]','"httpx>=0.27,<1", "rapidocr-onnxruntime==1.4.4", "pillow>=11,<13"]');p.write_text(s,encoding='utf-8')
print('Local OCR backend prepared')
