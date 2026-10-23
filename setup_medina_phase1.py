from pathlib import Path
import shutil
root=Path(r'D:\en html\Sistema de Presupuestos')
files={
'frontend/package.json': '''{"name":"medina-frontend","private":true,"version":"0.1.0","type":"module","scripts":{"dev":"vite","build":"tsc -b && vite build","preview":"vite preview"},"dependencies":{"react":"^19.0.0","react-dom":"^19.0.0"},"devDependencies":{"@types/react":"^19.0.0","@types/react-dom":"^19.0.0","@vitejs/plugin-react":"^4.3.4","typescript":"~5.7.3","vite":"^6.4.1"}}''',
'frontend/index.html': '<!doctype html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Medina Construcciones</title></head><body><div id="root"></div><script type="module" src="/src/main.tsx"></script></body></html>',
'frontend/tsconfig.json': '{"compilerOptions":{"target":"ES2022","lib":["ES2022","DOM","DOM.Iterable"],"module":"ESNext","moduleResolution":"Bundler","jsx":"react-jsx","strict":true,"skipLibCheck":true,"noEmit":true,"allowImportingTsExtensions":true},"include":["src"]}',
'frontend/vite.config.ts': '''import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({plugins:[react()],base:process.env.GITHUB_PAGES_BASE || '/'});
''',
'frontend/.env.example': 'VITE_API_BASE_URL=http://127.0.0.1:8000/api/v1\n',
'frontend/src/vite-env.d.ts': '/// <reference types="vite/client" />\n',
'frontend/src/main.tsx': '''import React from 'react';
import {createRoot} from 'react-dom/client';
import App from './App';
import './styles.css';
createRoot(document.getElementById('root')!).render(<React.StrictMode><App/></React.StrictMode>);
''',
'frontend/src/services/http.ts': '''const base=(import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8000/api/v1').replace(/\\/$/, '');
export async function get<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response=await fetch(`${base}/${path.replace(/^\\//,'')}`, {signal: signal ?? AbortSignal.timeout(8000), headers:{Accept:'application/json'}});
  if(!response.ok) throw new Error(`La API respondió con estado ${response.status}`);
  return response.json() as Promise<T>;
}
''',
'frontend/src/App.tsx': '''import {useEffect,useState} from 'react';
import {get} from './services/http';
type Health={status:string;service:string;supabase_configured:boolean};
export default function App(){
 const [state,setState]=useState('Comprobando conexión con la API…');
 const [retry,setRetry]=useState(0);
 useEffect(()=>{const controller=new AbortController();const timer=setTimeout(()=>controller.abort(),8000);setState('Comprobando conexión con la API…');
 get<Health>('health',controller.signal).then(data=>{if(!controller.signal.aborted)setState(data.status==='ok'?'API conectada · '+(data.supabase_configured?'Supabase configurado (sin verificar conexión)':'Supabase pendiente de configurar'):'La API no está disponible');}).catch(()=>{if(!controller.signal.aborted)setState('Sin conexión con la API. Puedes usar el generador original.');else if(!cancelled)setState('La API tardó demasiado en responder.');});
 let cancelled=false;return()=>{cancelled=true;clearTimeout(timer);controller.abort();};},[retry]);
 return <><div className="connection" role="status"><span>{state}</span><button onClick={()=>setRetry(x=>x+1)}>Comprobar conexión</button></div><iframe className="legacy" title="Generador de presupuestos existente" src={`${import.meta.env.BASE_URL}legacy/index.html`} /></>;
}
''',
'frontend/src/styles.css': '''*{box-sizing:border-box}body{margin:0;font-family:Segoe UI,Arial,sans-serif}.connection{display:flex;gap:16px;align-items:center;justify-content:space-between;padding:12px 20px;background:#fff;color:#23446b;border-bottom:1px solid #dce7f5;font-size:13px}.connection button{background:#eff5ff;color:#215bea;border:1px solid #cddcf4;border-radius:6px;padding:8px 12px;cursor:pointer}.legacy{display:block;width:100%;height:calc(100dvh - 62px);border:0}@media(max-width:600px){.connection{flex-wrap:wrap}.legacy{height:calc(100dvh - 110px)}}''',
'backend/pyproject.toml': '''[build-system]
requires = ["setuptools>=68"]
build-backend = "setuptools.build_meta"
[project]
name = "medina-backend"
version = "0.1.0"
requires-python = ">=3.10"
dependencies = ["fastapi>=0.115,<1", "uvicorn>=0.30,<1", "pydantic-settings>=2.5,<3", "httpx>=0.27,<1"]
[project.optional-dependencies]
test = ["pytest>=8,<10"]
[tool.setuptools.packages.find]
include = ["app*"]
[tool.pytest.ini_options]
pythonpath = ["."]
testpaths = ["tests"]
''',
'backend/.env.example': '''APP_ENV=development
CORS_ORIGINS=["http://localhost:5173","http://127.0.0.1:5173"]
SUPABASE_URL=
SUPABASE_SECRET_KEY=
''',
'backend/app/core/config.py': '''from functools import lru_cache
from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict
class Settings(BaseSettings):
    app_env: str = "development"
    cors_origins: list[str] = ["http://localhost:5173", "http://127.0.0.1:5173"]
    supabase_url: str = ""
    supabase_secret_key: SecretStr = SecretStr("")
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")
    @property
    def supabase_configured(self) -> bool:
        return bool(self.supabase_url and self.supabase_secret_key.get_secret_value())
@lru_cache
def get_settings() -> Settings:
    return Settings()
''',
'backend/app/api/v1/endpoints/health.py': '''from fastapi import APIRouter, Depends
from app.core.config import Settings, get_settings
router = APIRouter()
@router.get("/health")
def health(settings: Settings = Depends(get_settings)):
    return {"status": "ok", "service": "medina-api", "supabase_configured": settings.supabase_configured}
''',
'backend/app/db/supabase.py': '''"""Server-only REST client. No connection is made during health checks."""
import httpx
from app.core.config import Settings

def create_supabase_client(settings: Settings) -> httpx.AsyncClient:
    if not settings.supabase_configured:
        raise RuntimeError("Supabase no está configurado")
    return httpx.AsyncClient(
        base_url=settings.supabase_url.rstrip("/") + "/rest/v1/",
        headers={"apikey": settings.supabase_secret_key.get_secret_value()},
        timeout=10,
    )
# Use with `async with create_supabase_client(settings) as client:`.
# Future endpoints must verify authentication and authorization before using it.
''',
'backend/app/main.py': '''import logging
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException
from app.api.v1.endpoints.health import router
from app.core.config import get_settings
settings = get_settings()
app = FastAPI(title="Medina Construcciones API", version="0.1.0")
app.add_middleware(CORSMiddleware, allow_origins=settings.cors_origins, allow_credentials=False, allow_methods=["GET"], allow_headers=["Content-Type", "Authorization"])
app.include_router(router, prefix="/api/v1", tags=["health"])
@app.exception_handler(HTTPException)
async def http_error(request: Request, exc: HTTPException):
    return JSONResponse(status_code=exc.status_code, content={"error":{"code":"http_error", "message":str(exc.detail)}}, headers=exc.headers)
@app.exception_handler(RequestValidationError)
async def validation_error(request: Request, exc: RequestValidationError):
    return JSONResponse(status_code=422, content={"error":{"code":"validation_error", "message":"Solicitud inválida"}})
@app.exception_handler(Exception)
async def internal_error(request: Request, exc: Exception):
    logging.getLogger("medina").exception("Unhandled API error")
    return JSONResponse(status_code=500, content={"error":{"code":"internal_error", "message":"Error interno del servidor"}})
''',
'backend/tests/test_health.py': '''from fastapi.testclient import TestClient
from app.main import app
from app.core.config import Settings, get_settings

def test_health_without_credentials():
    app.dependency_overrides[get_settings] = lambda: Settings(_env_file=None, supabase_url="", supabase_secret_key="")
    try:
        response = TestClient(app).get("/api/v1/health")
        assert response.status_code == 200
        assert response.json() == {"status":"ok", "service":"medina-api", "supabase_configured":False}
    finally:
        app.dependency_overrides.clear()

def test_allowed_origin():
    origin = get_settings().cors_origins[0]
    response = TestClient(app).get("/api/v1/health", headers={"Origin":origin})
    assert response.headers["access-control-allow-origin"] == origin

def test_unknown_origin():
    response = TestClient(app).get("/api/v1/health", headers={"Origin":"https://unauthorized.example"})
    assert "access-control-allow-origin" not in response.headers

def test_not_found():
    response = TestClient(app).get("/api/v1/missing")
    assert response.status_code == 404
    assert response.json()["error"]["code"] == "http_error"
''',
'supabase/config.toml': '''project_id = "medina-construcciones"
[api]
enabled = true
port = 54321
schemas = ["public", "graphql_public"]
extra_search_path = ["public", "extensions"]
max_rows = 1000
[db]
port = 54322
major_version = 15
[studio]
enabled = true
port = 54323
[auth]
enabled = true
site_url = "http://localhost:5173"
[storage]
enabled = true
file_size_limit = "10MiB"
''',
'supabase/migrations/.gitkeep':'',
'supabase/README.md': '''# Supabase
Esta carpeta versiona configuración y futuras migraciones; la base de datos real estará en Supabase PostgreSQL.
No se crean tablas, roles ni buckets en fase 1. Auth y Storage están pendientes.
La configuración local requiere Supabase CLI y Docker; no se han ejecutado.
Antes de exponer datos: diseñar tablas, migraciones y políticas RLS. Nunca usar claves secretas en React.
''',
'.gitignore': '''node_modules/
dist/
.venv/
__pycache__/
.pytest_cache/
*.egg-info/
*.tsbuildinfo
.env
.env.*
!.env.example
supabase/.temp/
''',
'.github/workflows/ci.yml': '''name: CI
on: [push, pull_request, workflow_dispatch]
jobs:
  frontend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run build
  backend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.12'
      - run: pip install -e '.[test]'
      - run: pytest -q
''',
'.github/workflows/pages.yml': '''name: Deploy frontend to GitHub Pages
on: workflow_dispatch
permissions:
  contents: read
  pages: write
  id-token: write
concurrency:
  group: pages
  cancel-in-progress: false
jobs:
  deploy:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Validate API configuration
        env:
          API_URL: ${{ vars.VITE_API_BASE_URL }}
        run: test -n "$API_URL"
      - run: npm ci
        working-directory: frontend
      - run: npm run build
        working-directory: frontend
        env:
          VITE_API_BASE_URL: ${{ vars.VITE_API_BASE_URL }}
          GITHUB_PAGES_BASE: ${{ vars.GITHUB_PAGES_BASE || format('/{0}/', github.event.repository.name) }}
      - uses: actions/configure-pages@v5
      - uses: actions/upload-pages-artifact@v3
        with:
          path: frontend/dist
      - uses: actions/deploy-pages@v4
        id: deployment
''',
'render.yaml': '''services:
  - type: web
    name: medina-api
    runtime: python
    rootDir: backend
    buildCommand: pip install .
    startCommand: uvicorn app.main:app --host 0.0.0.0 --port $PORT
    healthCheckPath: /api/v1/health
    autoDeploy: false
    envVars:
      - key: APP_ENV
        value: production
      - key: CORS_ORIGINS
        sync: false
      - key: SUPABASE_URL
        sync: false
      - key: SUPABASE_SECRET_KEY
        sync: false
''',
'README.md': '''# Medina Construcciones — Fase 1

Tres carpetas: `frontend` (React + TypeScript + Vite), `backend` (FastAPI), `supabase` (configuración y futuras migraciones).
El `index.html` original permanece intacto. React lo muestra desde una copia en `frontend/public/legacy/index.html` para conservar su diseño y funcionalidad durante esta fase. El generador todavía no está migrado a componentes React.

## Ejecución local (PowerShell)
Backend, desde `backend`:
```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -e ".[test]"
Copy-Item .env.example .env
.\.venv\Scripts\python.exe -m uvicorn app.main:app --reload
```
Frontend, en otra terminal desde `frontend`:
```powershell
Copy-Item .env.example .env
npm install
npm run dev
```
Abrir la URL que imprime Vite. La barra de conexión consulta `/api/v1/health`; el generador conserva su funcionamiento si la API no responde.

## Verificación
Desde `backend`: `python -m pytest -q`.
Desde `frontend`: `npm run build`.
La salud confirma el proceso de API y presencia de configuración, no acceso real a Supabase.

## Variables
- Frontend: `VITE_API_BASE_URL`: URL completa del prefijo `/api/v1`, sin secretos.
- Build: `GITHUB_PAGES_BASE`: `/nombre-del-repo/`; usar `/` para dominio propio o repositorio de usuario.
- Backend: `APP_ENV`, `CORS_ORIGINS` (lista JSON de orígenes exactos), `SUPABASE_URL`, `SUPABASE_SECRET_KEY` (solo servidor).
Para Pages el origen CORS es `https://usuario.github.io`, sin ruta de repositorio.

## Despliegue pendiente
1. Confirmar repositorio y subir cambios solo con autorización.
2. Configurar Pages con GitHub Actions y variables `VITE_API_BASE_URL`, `GITHUB_PAGES_BASE`.
3. Crear servicio Render a partir de `render.yaml` y configurar variables; no desplegado en esta fase.
4. Crear/configurar proyecto Supabase. No se ha conectado ningún proyecto real.
5. Ejecutar workflow Pages manualmente tras aprobación; CI no publica.

## Límites de fase 1
No se implementan Auth, presupuestos nuevos, historial, PDF nuevo, QR, IA ni tablas. La lógica original mantiene impuesto fijo de 8%, inserción HTML de descripciones sin escapar y ausencia de persistencia; estos riesgos deben corregirse antes de exponer el generador a usuarios no confiables. La copia legacy usa bibliotecas PDF de CDN y necesita internet para descargarlas.
El cliente Supabase preparado es privilegiado: los futuros endpoints deberán autenticar y autorizar antes de usarlo; la configuración CORS no sustituye autenticación.
Las referencias visuales guiarán fases posteriores. El PDF futuro no incluirá datos de cliente, proyecto ni vigencia, según el documento de fase 1.
'''
}
# Fail before mutations if any planned file already exists.
for name in files:
    if (root/name).exists(): raise RuntimeError('File already exists: '+name)
legacy=root/'frontend/public/legacy/index.html'
if legacy.exists(): raise RuntimeError('Legacy copy already exists')
for name,content in files.items():
    path=root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(content,encoding='utf-8')
legacy.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(root/'index.html',legacy)
for folder in ['frontend/src/components/ui','frontend/src/components/layout','frontend/src/features','frontend/src/lib','frontend/src/types','backend/app/schemas','backend/app/repositories','backend/app/services']:
    path=root/folder;path.mkdir(parents=True,exist_ok=True);(path/'.gitkeep').touch()
for folder in ['backend/app','backend/app/core','backend/app/api','backend/app/api/v1','backend/app/api/v1/endpoints','backend/app/db']:
    (root/folder/'__init__.py').touch()
print('Created phase 1 files; original HTML preserved.')
