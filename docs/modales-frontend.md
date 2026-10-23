# Modales del frontend

`shared/ui/BrandedModalFragment` proporciona la superficie redondeada, el fondo atenuado, el cierre superior y el ajuste al tamaño disponible. El contenido puede desplazarse en pantallas pequeñas. Los XML comunes viven en `res-shared`: `modal_shell`, `modal_component_brand`, `modal_item_option` y los estilos de botones.

La presentación del perfil vive en `feature/profile/presentation` y sus XML en `res-profile`. El botón del perfil de la barra abre `ProfileModalFragment`. Su acción de cerrar sesión solicita `LogoutConfirmationFragment`; confirmar vuelve al login mediante el navegador existente. Cancelar o pulsar la X solo cierra la confirmación.

Los eventos entre el perfil, la confirmación y la pantalla de empresa utilizan Fragment Results con el ciclo de vida de la vista. Los diálogos conservan sus argumentos al recrearse.

Por ahora, el perfil utiliza datos de demostración y un avatar con iniciales. Las opciones muestran información de frontend; no modifican cuentas ni contraseñas. El cierre de sesión navega al login del prototipo; la invalidación de una sesión real se incorporará con la autenticación.

Los próximos modales pueden extender la misma base y aportar su XML y acciones, manteniendo su lógica dentro de la funcionalidad correspondiente.

Ventas utiliza esta misma base para agregar/editar productos y mostrar el resumen. El editor conserva sus argumentos y comunica las actualizaciones con Fragment Results. Todos estos modales usan Inter (la fuente del frontend web), incluida en `res-shared/font`; su licencia está en `docs/licenses/Inter-OFL.txt`. La animación de entrada compartida reproduce `modalPop` del frontend original.

## Formularios en pantalla completa

`BrandedModalFragment` ofrece un modo de formulario mediante `fullScreen()`, `formTitleId()` y `fixedActionIds()`. El layout compartido `modal_fullscreen_shell` contiene encabezado compacto, área desplazable y acciones fijas. Aplica los insets de barras, recorte y teclado para mantener accesibles los campos y las acciones.

Producto, talla/stock y editor del carrito usan este modo con guardar y cancelar fijos. Los gestores de categorías, proveedores y almacenes usan pantalla completa y conservan sus acciones junto al formulario de creación/edición. Perfil, confirmaciones de eliminación y salida mantienen el formato centrado. La confirmación de cambios pendientes del producto sigue vigente.

Los formularios amplios se presentan ahora como fragments integrados en `fullscreenFormHost`, dentro del contenido de inventario o ventas. El topbar de empresa permanece visible y su sidebar accesible por encima del formulario. Los formularios secundarios usan el mismo host dentro del formulario padre. Volver cierra primero el sidebar abierto y después el formulario; se conserva la confirmación de cambios pendientes en producto. Las confirmaciones siguen siendo dialogs centrados.

## Completar venta (Figma)

`CheckoutModalFragment` usa el modal centrado compartido, logo VILCAS, caja de cobro de las sedes de demostración, cuatro métodos (Efectivo/Yape/Plin/Transferencia), importe recibido y vuelto. `CheckoutPayment` valida y calcula usando centavos enteros. El pago dividido admite asignaciones a varios métodos cuya suma coincide exactamente con el total; el vuelto se calcula únicamente sobre efectivo. El estado del formulario se conserva durante rotación.

Continuar abre un resumen de revisión de frontend. No registra una venta, no procesa pagos y no limpia el carrito. Las cajas reales y los comprobantes de pagos digitales se integrarán al conectar el backend de ventas.

## Flujo original de checkout migrado

Se revisaron `ventas.html`, `ventas.css` y `ventas.ts` de Angular: métodos activos con animación selectPop, comprobante obligatorio por método digital, líneas mixtas agregables y eliminables, validación de suma exacta, efectivo suficiente, comprobante por línea, confirmación de productos y caja, y regreso sin perder datos.

`SalesModalFragment` centraliza la superficie blanca y el contraste de marca. `ReceiptModalFragment` permite cámara mediante FileProvider limitado a `sales_receipts/`, galería, previsualización y reemplazo; conserva la selección y captura pendiente al rotar. `ReceiptImageLoader` decodifica vistas previas reducidas fuera del hilo principal. `CheckoutConfirmationModal` muestra productos, cantidades, importes, métodos, caja, comprobantes ampliables, efectivo recibido y vuelto.

La confirmación final completa una revisión local de demostración: no genera un ID de venta, no reduce stock ni limpia el carrito, y no muestra “Venta registrada”. Registro real, subida/compresión de comprobantes al backend, manejo de 400/409 por stock y emisión/impresión de boletas dependen de integrar el catálogo, cajas y ventas reales. Los flujos de devoluciones del HTML no se incorporan a este checkout.

## Regresión de apertura de checkout

Se corrigieron dos fallos: dimensiones ausentes en TextViews con `Company.Body`, y una ventana de checkout creada pero invisible. En el segundo caso, `dumpsys window` registró `Surface: shown=false` y `mShownAlpha=0.0`; logcat mostró valores de alpha superiores a uno producidos por el interpolador con rebote de la animación de ventana.

`BrandedModalFragment` mantiene la ventana visible sin animación de superficie y anima su contenido mediante propiedades de alpha y escala con interpoladores acotados. Conserva aparición y salida, el fondo oscurecido y el formato compartido. `SalesFragment` abre checkout con `showNow` y protege el estado guardado del FragmentManager.

`CheckoutModalInstrumentedTest` infla los pasos y simula eventos táctiles desde el decor con dos productos (S/130), verificando dimensiones, alpha final y apertura de confirmación. Pasó en Pixel 7 API 37.1 con animaciones de ventana activas, sin `--no-window-animation`. Genera `checkout-visible.png` como evidencia de la pantalla realmente renderizada. El checkout continúa usando datos estáticos; no llama a la API de ventas.

Tras reiniciar Android en el emulador se verificó visualmente la captura real del checkout en build/reports/checkout-visible.png. La prueba de apertura y confirmación pasó nuevamente con animaciones activas.
