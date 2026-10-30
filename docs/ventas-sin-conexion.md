# Ventas sin conexión (HU-18)

El vendedor puede seguir cobrando cuando el celular pierde la señal. La venta se guarda en el celular y se envía sola cuando vuelve internet, sin duplicarse.

## Cómo funciona

- `ConnectivityMonitor` sigue la red por defecto del sistema (Internet validado). Requiere el permiso `ACCESS_NETWORK_STATE`, que Android concede sin preguntar.
- `OfflineSalesRepository` envuelve a `RemoteSalesRepository`:
  - Cada carga del catálogo con conexión guarda las sedes y los productos en Room (`OfflineDatabase`, archivo `vilcas_offline.db`), por empresa. Sin conexión, el catálogo sale de esa copia y los filtros se aplican en el celular.
  - Sin conexión, `submit` guarda la venta en la tabla `ventas_pendientes` y copia las fotos de comprobante a `noBackupFilesDir/comprobantes_pendientes`. También la guarda si el comprobante no se pudo subir antes de confirmar.
  - Las unidades de las ventas en cola se descuentan del stock que muestra el catálogo, con o sin conexión, hasta que el servidor las registra.
- `SalesSyncWorker` (WorkManager, requiere red) envía la cola de la más antigua a la más nueva. Se programa al abrir la app, al recuperar la conexión, al entrar a Ventas y al reintentar una venta.
- Si responde 401, se renueva el token una vez con el refresh token. Si no hay red, hay un error 5xx o un 429, la venta queda en cola y se reintenta con espera exponencial.
- Un 409 (stock cambió) marca la venta como `CONFLICT`, y un 400, 403 o 422 como `FAILED`. Ninguna de las dos bloquea las siguientes. En Ventas aparece el aviso «N ventas no se pudieron sincronizar · Revisar», desde donde el vendedor puede reintentar o descartar cada una.
- Al abrir la app sin señal, el diálogo «No se pudo conectar» ofrece «Vender sin conexión» a las cuentas con acceso a Ventas, usando la sesión cifrada ya guardada.

## Idempotencia

`SaleDraft.id` es un UUID por venta que se envía como `clave_idempotencia` en `POST /ventas/pos/confirmar`. El backend devuelve la venta existente si la clave ya se registró en la empresa. Eso hace seguro reenviar la cola después de una respuesta perdida.

Orden de despliegue en el backend (repositorio de ALBA):

1. Ejecutar `Basededatos/AlterTablas_IdempotenciaVentas.sql` en la base de datos.
2. Desplegar el backend (modelo, esquema, repositorio y servicio de ventas).

Mientras el backend no tenga el cambio, el campo se ignora. En ese caso un reenvío tras una respuesta perdida podría duplicar la venta; las ventas encoladas sin conexión que nunca llegaron al servidor se envían una sola vez.

## Datos y respaldo

`res/xml/backup_rules.xml` y `res/xml/data_extraction_rules.xml` excluyen `vilcas_offline.db` de la copia en la nube y de la transferencia a otro equipo: restaurar una cola vieja en otro celular podría reenviar ventas. Las fotos de comprobante pendientes viven en `noBackupFilesDir`, que Android nunca respalda. La cola pertenece al usuario que vendió y solo se envía con su sesión.

## Pruebas

- `OfflineSalesRepositoryTest` (JUnit): cola sin conexión, catálogo guardado con stock reservado, orden y clave en la sincronización, conflicto de stock, renovación del token y separación por usuario.
- `OfflineDatabaseInstrumentedTest` (dispositivo): consultas de Room.
- Backend: `tests/unit/test_ventas_idempotencia.py`.
