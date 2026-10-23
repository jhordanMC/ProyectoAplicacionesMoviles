# Inventario Android y FastAPI

La funcionalidad vive en feature/inventory, con dominio, datos y presentación. Los XML permanecen en res-inventory y reutilizan filtros, Inter, botones y BrandedModalFragment. Mantiene topbar y sidebar.

## Operaciones

- Productos: listado paginado, búsqueda y filtros remotos, detalle completo, creación, edición, duplicación y eliminación con confirmación.
- Categorías, proveedores y almacenes: listado, creación, edición y eliminación. El backend valida permisos y referencias en uso.
- Locales: consulta de las sedes reales para distribuir stock.
- Fotos: cámara o galería, preparación JPEG compartida y subida a /inventario/productos/upload-imagen. Las fotos remotas usan el cargador de imágenes compartido.

RemoteInventoryRepository usa la sesión Bearer del login y las rutas /inventario/productos, /categorias, /proveedores, /almacenes y /locales de https://api.vilcaspe.com. El catálogo usa páginas de 30 con hay_mas/siguiente_offset y filtros busqueda/id_categoria/sexo. Al entrar nuevamente se actualiza el catálogo; ventas también se actualiza para reflejar cambios de inventario.

El listado contiene resúmenes. Antes de editar o ver detalle se obtiene GET /inventario/productos/{id}. Las variantes conservan ID de ubicación y versión de stock. El formulario envía id_local o id_almacen, talla opcional, cantidad, SKU y código de barras. Si únicamente cambian datos generales, no envía variantes: evita sobrescribir stock que haya cambiado por ventas. Si modifica variantes, incluye su versión y maneja conflictos 409 conservando el borrador.

La duplicación consulta primero el detalle completo y crea un producto nuevo; omite SKU y códigos de barras del original para evitar duplicar códigos únicos. Los importes se envían como decimales desde céntimos enteros. La subida de fotos se reutiliza al reintentar errores confirmados.

## Estado y límites

InventoryViewModel pertenece al contenedor de empresa y serializa las operaciones en un worker; no hace red en el hilo UI. Hay carga, actualización automática al entrar, paginación y errores visibles. Los formularios conservan lo escrito si falla el guardado y se cierran solo después de éxito. El resultado se conserva durante recreación de la vista para evitar repetir una creación tras rotar. No hay sustitución por datos demo ni persistencia offline todavía.

No se reintentan escrituras automáticamente. Si la respuesta de una escritura se pierde, se pide actualizar y verificar antes de repetir. Los permisos y eliminación de referencias usadas los resuelve el backend. Una sesión vencida vuelve al login.

Limitación comprobada en el backend actual: PATCH ignora null para categoría, proveedor y sexo; los SKU/códigos vacíos se normalizan a null y también se ignoran al actualizar una variante. Android avisa antes de estos intentos de borrado en lugar de mostrar un éxito falso. Se pueden reemplazar por valores nuevos. Quitar una imagen sí envía cadena vacía. No se modificó ni desplegó el backend.

## Validación

Compilación: gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --offline.

Pruebas de contrato: resúmenes y detalle, variantes sin talla, IDs, versiones de stock, PATCH sin reemplazo de variantes, fotos multipart, referencias, filtros, paginación, errores de stock, resultado incierto y sesión ausente. Pruebas en emulador verifican que el formulario conserve su borrador ante error, cierre tras éxito y conserve el resultado durante actualización.

Las pruebas usan repositorios controlados y no crean ni eliminan registros en producción. La validación final contra datos reales requiere la sesión del usuario.


## Fidelidad de modales al frontend Angular

Se revisaron inventario.html, inventario.css y styles/tokens.css del frontend original. Los modales de producto, talla y gestión vuelven a tarjetas centradas sobre fondo oscurecido, con scroll y marca VILCAS; el topbar y sidebar permanecen detrás. La tarjeta de producto tiene radio 24 dp, altura máxima 88 %, margen horizontal 20 dp y cabecera con logo de 60 dp. Los gestores conservan radio móvil 16 dp; las confirmaciones compactas, icono rojo y marca pequeña al pie.

Se ajustaron Inter, títulos verdes de 16 sp, etiquetas de 11.5 sp, campos de 13 sp, bordes/foco, espaciado y botones primarios/secundarios según los estilos web. El selector de foto reproduce sus estados vacío y con imagen, con iconos de cámara/galería. No se alteraron los contratos API ni la conservación de borradores ante errores.

Responsive: márgenes compartidos por ancho disponible; catálogo adaptable de 1 a 4 columnas. Los formularios mantienen Guardar/Cancelar fuera del scroll y limitan el modal al espacio visible. Cargar detalle y guardar actualizan el producto en su posición existente (prueba de regresión). Las entradas de componentes se ordenan por posición de arriba hacia abajo con movimiento suave de 8 dp, respetando la desactivación de animaciones del sistema.
