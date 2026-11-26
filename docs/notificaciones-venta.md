# Notificaciones de venta (HU-19)

El dueño recibe en el celular una notificación del sistema por cada venta nueva de su empresa, con la aplicación abierta o cerrada. El aviso llega por Firebase Cloud Messaging (FCM): el backend lo envía apenas se confirma la venta.

## Flujo

1. Al entrar a la empresa con una cuenta de dueño, `CompanyFragment` llama a `VilcasApplication.registerSalePush()`. La app pide su token a Firebase y lo envía a `POST /notificaciones/dispositivos` (`SalePushRegistration`). El backend lo guarda en `dispositivos_push` con la empresa y el usuario; solo acepta cuentas con el permiso `ventas.ventas:ver`.
2. `POST /ventas/pos/confirmar` programa `avisar_venta` como tarea en segundo plano. Corre después del commit, así que la venta ya existe cuando el celular la consulta. Envía un mensaje solo de datos y prioridad alta (`{"tipo": "venta", "id_venta": ...}`) a los celulares de la empresa, menos al de quien vendió, y borra los tokens que Firebase da por inválidos.
3. `SalePushService` recibe el mensaje, aunque la app esté cerrada, y ejecuta la revisión normal (`SaleNoticeRepository.check()`), que completa el detalle y publica la notificación.
4. Al cerrar sesión, la app borra su token con `POST /notificaciones/dispositivos/eliminar`, usando la sesión que se está cerrando.

Las ventas hechas sin conexión pasan por el mismo endpoint al sincronizarse, así que también generan su aviso.

## Respaldos

| Situación | Quién revisa | Frecuencia |
|---|---|---|
| Aplicación abierta | Temporizador de `CompanyFragment` | Cada 60 segundos |
| Aplicación cerrada | `SaleNoticeWorker` (WorkManager, periódico) | Cada 15 minutos, el mínimo de Android |
| Tras sincronizar ventas sin conexión | `SalesSyncWorker` | Al terminar el envío de la cola |

Cubren un aviso push perdido y los celulares sin servicios de Google Play, donde FCM no funciona. «Forzar detención» desactiva los avisos push hasta que la app se vuelva a abrir; es una regla de Android.

## Configuración

- App: `app/google-services.json` del proyecto Firebase `vilcas-18e96`. No es secreto.
- Backend: `FIREBASE_CREDENTIALS_FILE` en el `.env` del servidor, con la ruta absoluta al JSON de la cuenta de servicio. Ese archivo es una llave privada: va solo en el servidor, nunca en un repositorio. Sin él, los avisos push quedan apagados y las ventas no se afectan.
- Base de datos: `Basededatos/AlterTablas_DispositivosPush.sql`, antes de desplegar el backend.

El backend no usa `firebase-admin`: `app/modules/notificaciones/service/fcm.py` firma el acceso con `python-jose` y llama a la API HTTP v1 con `httpx`.

## Reglas

- La primera revisión de una sesión solo registra el historial como leído, para no avisar de ventas pasadas.
- `SaleNoticeRepository.check()` devuelve las ventas no conocidas, de la más antigua a la más nueva. Si la red falla no se pierde nada: se reintenta en la siguiente revisión.
- Si no se puede leer el detalle de una venta, se notifica igual con lo que trae la lista y se completa al abrirla.
- Solo las cuentas de dueño reciben avisos; los demás roles conservan en la campana los anuncios del equipo.
- Con más de 5 ventas nuevas en una revisión se publica una sola notificación de resumen.

## Diseño

El logo de VILCAS va en la cabecera de la notificación. Junto al texto se muestra la foto del primer producto de la venta (`imagen_url` del detalle de `GET /ventas/{id}`), recortada y redondeada en `SaleNotifier.photo`; si el producto no tiene foto, ese espacio se oculta. La foto solo se usa al publicar la notificación y no se guarda en `avisos_venta`.

## Datos y privacidad

Los avisos se guardan en la tabla `avisos_venta` de `vilcas_offline.db` (versión 2, con `MIGRATION_1_2`), que ya está excluida de los respaldos. Se conservan los últimos 200 por usuario. `VilcasApplication.clearSession` cancela el trabajo y las notificaciones, borra los avisos y da de baja el token push.

El mensaje de Firebase solo lleva el id de la venta; el monto, el vendedor y la foto los pide la app con su propia sesión.

La notificación usa `VISIBILITY_PUBLIC`: el monto y el nombre del vendedor se ven también con el celular bloqueado, como pide el diseño. Para ocultarlos basta con cambiar la visibilidad en `SaleNotifier.base`.

## Permiso

`POST_NOTIFICATIONS` se pide en Android 13 o superior, solo a la cuenta del dueño. Si lo niega, la campana sigue mostrando la lista y el contador; solo faltan las notificaciones del sistema.

## Pruebas

- `SaleNoticeRepositoryTest` (JUnit): primera revisión, venta nueva una sola vez, orden, rol, fallas de red y de detalle, renovación del token y foto del producto.
- `SalePushRegistrationTest` (JUnit): registro, renovación de la sesión y baja al cerrar sesión.
- `OfflineDatabaseInstrumentedTest` (dispositivo): orden y recorte de avisos, marcar como leídos y migración desde la versión 1.
- Backend: `tests/unit/test_notificaciones_push.py` (cliente FCM simulado, tokens inválidos, exclusión del vendedor y aviso después del commit).
