# Ventas Android conectadas a FastAPI

La pantalla conserva topbar, sidebar, Inter, filtros compartidos, carrito plegable y modales. Los XML están en app/src/main/res-sales y los componentes comunes en res-shared.

## Capas

- presentation: pantalla, editores, checkout y ViewModel asíncrono.
- domain: variantes, sedes, carrito, importes en céntimos y borrador de venta.
- data: RemoteSalesRepository y preparación de comprobantes.
- shared/network: HTTPS, Bearer, JSON y multipart.

Producción usa la sesión del login y https://api.vilcaspe.com. El repositorio de demostración queda para pruebas; no sustituye datos ante errores. El carrito comienza vacío, permanece durante la navegación de empresa y no tiene persistencia offline todavía.

## Contrato

| Operación | Endpoint |
|---|---|
| Sedes asignadas | GET /ventas/pos/mis-sedes |
| Catálogo y filtros | GET /ventas/pos/productos |
| Subir comprobante | POST /ventas/pos/upload-comprobante |
| Registrar venta | POST /ventas/pos/confirmar |

El catálogo pagina de 30 en 30, busca con debounce y descarta respuestas de filtros anteriores. Muestra carga, errores y reintento. Los filtros envían IDs de sede y las líneas IDs de variante y ubicación. Admite productos sin talla. El carrito valida stock por variante y origen; el backend decide precio y stock definitivo. La caja solo admite locales, aunque los productos pueden salir de almacenes.

El checkout admite efectivo, Yape, Plin, transferencia y pagos divididos. Reduce los comprobantes y los convierte a JPEG para subirlos con el campo multipart archivo. Envía sus URLs, pagos y descuentos al confirmar. Solo una respuesta válida de venta pagada vacía el carrito y muestra el ID real. Reutiliza las subidas al reintentar errores confirmados en la misma sesión.

Mientras se registra se bloquean cambios y confirmaciones repetidas. Los errores conservan el carrito. Si se pierde la respuesta del POST, el resultado queda incierto y se pide comprobar el historial web antes de repetir. El backend actual no ofrece clave de idempotencia y no se reintenta el registro automáticamente. Una sesión vencida devuelve al login.

## Verificación

Compilación y pruebas: gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --offline.

Las pruebas cubren contrato JSON, comprobantes, pagos, errores y stock por origen. Las instrumentadas verifican apertura del checkout y confirmación, doble envío, éxito, conflicto de stock y resultado incierto usando repositorios controlados. No crean ventas en producción. La validación final contra datos reales requiere iniciar sesión y registrar una venta de prueba autorizada.
