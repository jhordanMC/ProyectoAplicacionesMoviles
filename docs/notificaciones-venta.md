# Notificaciones de venta (HU-19)

El dueño recibe en el celular una notificación del sistema por cada venta nueva de su empresa, con la aplicación abierta o cerrada. No requiere cambios en el backend ni Firebase: la aplicación consulta `GET /ventas?estado=pagada&limit=50` y compara con lo que ya conoce.

## Cuándo se revisa

| Situación | Quién revisa | Frecuencia |
|---|---|---|
| Aplicación cerrada | `SaleNoticeWorker` (WorkManager, periódico y único) | Cada 15 minutos, el mínimo de Android; puede tardar más si el sistema ahorra batería |
| Aplicación abierta | Temporizador de `CompanyFragment` | Cada 60 segundos |
| Tras sincronizar ventas sin conexión | `SalesSyncWorker` | Al terminar el envío de la cola |

## Reglas

- La primera revisión de una sesión solo registra el historial como leído, para no avisar de ventas pasadas.
- `SaleNoticeRepository.check()` devuelve las ventas no conocidas, de la más antigua a la más nueva. Si la red falla no se pierde nada: se reintenta en la siguiente revisión.
- Si no se puede leer el detalle de una venta, se notifica igual con lo que trae la lista y se completa al abrirla.
- Solo revisan las cuentas de dueño; los demás roles conservan en la campana los anuncios del equipo.
- Con más de 5 ventas nuevas en una revisión se publica una sola notificación de resumen.

## Datos y privacidad

Los avisos se guardan en la tabla `avisos_venta` de `vilcas_offline.db` (versión 2, con `MIGRATION_1_2`), que ya está excluida de los respaldos. Se conservan los últimos 200 por usuario. `VilcasApplication.clearSession` cancela el trabajo y las notificaciones y borra los avisos.

La notificación usa `VISIBILITY_PUBLIC`: el monto y el nombre del vendedor se ven también con el celular bloqueado, como pide el diseño. Para ocultarlos basta con cambiar la visibilidad en `SaleNotifier.base`.

## Permiso

`POST_NOTIFICATIONS` se pide en Android 13 o superior, solo a la cuenta del dueño. Si lo niega, la campana sigue mostrando la lista y el contador; solo faltan las notificaciones del sistema.

## Pruebas

- `SaleNoticeRepositoryTest` (JUnit): primera revisión, venta nueva una sola vez, orden, rol, fallas de red y de detalle, renovación del token.
- `OfflineDatabaseInstrumentedTest` (dispositivo): orden y recorte de avisos, marcar como leídos y migración desde la versión 1.
