# Login conectado a FastAPI

Base de API: `https://api.vilcaspe.com`, declarada como `BuildConfig.API_BASE_URL` en `app/build.gradle.kts`. Se comprobó la presencia de las rutas en el OpenAPI público desplegado. No se modificó el backend ni su despliegue.

## Flujo

1. `POST /iam/auth/login` envía `username` (correo) y `password` como `application/x-www-form-urlencoded`.
2. El backend envía por correo el código obligatorio. La respuesta contiene `login_token` y `expira_en_segundos`.
3. La vista existente de 2FA envía `POST /iam/auth/verificar-2fa`, JSON con `login_token` y `codigo` como texto (conserva ceros iniciales).
4. Con el access token se consulta `GET /iam/mi-perfil`. Solo se confirma la sesión después de recibir un perfil activo con rol móvil compatible. Dueño de empresa y Vendedor se asignan desde los roles reales, sin selector de demostración. Los demás roles no acceden a estas vistas.
5. Al cerrar sesión se borra inmediatamente la sesión local y se intenta revocar el refresh token con `POST /iam/auth/logout`, sin bloquear la interfaz. Si no hay conexión, la revocación remota no queda garantizada.

## Capas

- `shared/network`: transporte HTTPS genérico con tiempos de conexión/lectura, sin logs de cuerpos o tokens, sin redirecciones ni reintentos automáticos.
- `feature/auth/data/RemoteAuthRepository`: contrato IAM y traducción de errores al dominio.
- `feature/auth/domain`: challenge, sesión, repositorio y almacén en memoria.
- `feature/auth/presentation/AuthViewModel`: peticiones fuera del hilo principal, protección de envíos duplicados, estado de carga y errores, conservación durante rotación y descarte de respuestas de peticiones abandonadas.
- `VilcasApplication`: composición del repositorio y almacén de sesión.

Las vistas conservan el diseño existente y muestran progreso y errores con `auth_component_status`. No se guarda la contraseña ni el código, y los tokens permanecen solo en memoria. Si el proceso termina, es necesario volver a iniciar sesión. La persistencia cifrada y renovación de tokens se abordarán con la siguiente etapa de sesión; no hay autenticación offline.

Recuperación de contraseña, perfil editable, permisos/vistas deshabilitadas y datos de dashboard/inventario/ventas aún no están conectados. El nombre y las iniciales del usuario sí provienen del perfil real. Los endpoints futuros deberán aplicar autorización del backend y manejar vencimiento/refresh de tokens.

## Validación

`gradlew.bat :app:testDebugUnitTest :app:assembleDebug` verifica codificación del formulario, códigos con ceros iniciales, uso de Bearer en perfil, rechazo de rol no soportado con revocación de sesión y rechazo de códigos inválidos, además de las pruebas existentes. Se verificaron las rutas públicas sin enviar credenciales reales. La prueba completa con correo y código real corresponde hacerla desde Android con una cuenta de dueño o vendedor; no enviar contraseñas en el chat.
