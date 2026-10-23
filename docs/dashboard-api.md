# Dashboard Android conectado a FastAPI

La pantalla conserva las tarjetas, Inter, colores, topbar y sidebar del diseño móvil. Los layouts viven en res-dashboard y los selectores compartidos en res-shared; no se introducen WebViews ni CSS dentro de Android.

## Capas

- domain: DashboardFilter (fechas de negocio), DashboardSection (tarjetas independientes), DashboardPanel (datos inmutables) y DashboardRepository.
- data: RemoteDashboardRepository interpreta las respuestas GET y verifica que la sesión no cambie durante la consulta.
- presentation: DashboardViewModel mantiene filtros y estados por tarjeta en el ámbito de empresa; DashboardFragment únicamente enlaza los datos a XML.
- VilcasApplication crea el repositorio con API_BASE_URL y el token de la sesión iniciada.

## Fuentes

| Componente | GET | Alcance |
| --- | --- | --- |
| Ganancia, total vendido, ticket, cantidad y crecimiento | /finanzas/resumen | desde/hasta seleccionados |
| Valor del inventario a costo | /dashboard/resumen | Stock actual; no es una cifra histórica |
| Más vendidos | /dashboard/ranking-productos?limit=10 | desde/hasta seleccionados |
| Categorías | /dashboard/top-categorias?limit=5 | desde/hasta seleccionados |
| Métodos de pago | /finanzas/ventas-por-metodo-pago | desde/hasta seleccionados |
| Mayores gastos | /finanzas/gastos-recurrentes | Equivalente mensual estimado, igual que Angular: mensual x1, semanal x4.33, diario x30 |
| Barras de ventas y ganancia real | /dashboard/ventas-ganancia-por-periodo | Rango del gráfico; admite ganancia negativa |
| Fotos y stock bajo | /inventario/productos?limit=300&offset=… | Catálogo actual paginado completo; stock menor que 10 |
| Productos sin ventas | /dashboard/productos-sin-ventas?limit=20 | Histórico; se muestran hasta 3 alertas |
| Más devueltos | /dashboard/top-productos-devueltos?limit=3 | Últimos 30 días, igual que la web |

Los endpoints de ranking y devoluciones no entregan fotos. Se cruzan con el catálogo por ID; para devoluciones el backend solo entrega nombre, así que la foto se utiliza únicamente cuando ese nombre identifica un único producto. No se inventa stock ni se sustituye la foto por zapatillas de demostración.

## Filtros, estado y precisión

Hoy/semana/mes/año usan America/Lima sin depender de la zona del dispositivo. La semana comienza el lunes. Las cifras terminan en hoy; el gráfico llega al domingo, fin de mes o fin de año, igual que Angular. Personalizado usa las fechas inclusivas elegidas en ambos casos.

El selector Canal conserva Próximamente: el backend aún no modela ese filtro. No se envía un parámetro ficticio ni se aparenta filtrar.

Cada tarjeta muestra carga, vacío o error propio. Reintentar consulta solamente su fuente. Un error HTTP nunca se convierte en cero; una respuesta válida sin movimientos sí puede mostrar S/0.00. Las respuestas de filtros anteriores se descartan por versión y se cancelan tareas pendientes. Hay como máximo cuatro consultas simultáneas; no se hacen escrituras ni reintentos automáticos.

401 expira la sesión y vuelve al login. 403 muestra el error de permiso sin botón de reintento. Si faltan costos en ventas, la tarjeta advierte que la ganancia no cubre todo el período. El crecimiento es el porcentaje de ingresos del backend frente al período anterior; no el antiguo +12% de ejemplo.

## Responsive y componentes

Las opciones desplegadas usan un XML separado con Inter 13sp, fondo claro, selección verde, borde redondeado, altura mínima de 48dp y texto que puede ocupar varias líneas. El popup sigue el ancho del selector. El mismo adapter se usa en dashboard, filtros de inventario/ventas y selectores de sede de checkout.

El ranking usa de una a cuatro columnas según el espacio disponible. Las métricas recolocan su nota en pantallas pequeñas o con fuente ampliada. Las leyendas pueden apilarse bajo el gráfico; la pestaña seleccionada se desplaza hasta quedar visible. Se conserva la animación compartida de entrada de componentes.

Para catálogos excepcionalmente grandes, el cliente limita el recorrido a 100 páginas y muestra un error en vez de presentar alertas incompletas como si fueran exhaustivas. A futuro conviene un endpoint de stock bajo con fotos para evitar descargar todo el catálogo. Los datos todavía no se persisten para uso sin conexión.

## Validación

Se verificó el contrato OpenAPI publicado en api.vilcaspe.com sin usar credenciales. Las pruebas de integración emplean repositorios controlados y sesiones de prueba; no modifican datos de producción ni sustituyen el repositorio remoto del APK. Cubren respuestas parciales, sesión expirada, estados vacíos, cambios de filtro, respuestas tardías, reintentos y recreación de la pantalla.

El gráfico reproduce ambas series y la escala del TypeScript original: ingresos y ganancia neta, intervalos 1/2/5/10 y línea de cero que admite pérdidas. Todas las etiquetas se conservan con desplazamiento horizontal; las agrupaciones proceden de la API y cambian con las fechas del filtro.
