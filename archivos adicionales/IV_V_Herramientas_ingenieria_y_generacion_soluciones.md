# IV. HERRAMIENTAS DE INGENIERÍA

## IV.1 Metodología empleada

Para decidir cómo organizar el desarrollo del aplicativo VILCAS se compararon tres enfoques: Scrum, Programación Extrema (XP) y el Proceso Unificado de Rational (RUP). Los criterios responden a lo que condiciona al proyecto: la forma de gestionar los requisitos, la documentación que se exige, el papel del cliente, la cantidad de roles y el plazo disponible.

**Tabla 1**

*Comparación entre Scrum, XP y RUP*

| Criterio | Scrum | XP | RUP |
|---|---|---|---|
| Naturaleza | Marco ligero, iterativo e incremental, basado en el empirismo (Schwaber y Sutherland, 2020) | Metodología ágil centrada en prácticas técnicas de programación (Beck y Andres, 2004) | Proceso iterativo guiado por casos de uso y centrado en la arquitectura (Kruchten, 2004) |
| Ciclo de vida | Sprints de duración fija, de un mes como máximo, con planificación, Scrum diario, revisión y retrospectiva | Ciclo semanal y ciclo trimestral de entregas | Cuatro fases (Inicio, Elaboración, Construcción y Transición) con iteraciones dentro de cada una |
| Roles | Product Owner, Scrum Master y Developers | Cliente, programadores y un coach que guía las prácticas | Numerosos roles especializados (analista, arquitecto, diseñador, probador, gerente de proyecto, entre otros) |
| Gestión de requisitos | Product Backlog ordenado por el Product Owner y refinado en cada sprint | Historias de usuario negociadas en el juego de la planificación | Casos de uso y especificaciones suplementarias |
| Documentación | Artefactos mínimos: Product Backlog, Sprint Backlog e Incremento | Reducida; el código y las pruebas sirven de especificación | Extensa, con modelos, planes y especificaciones por disciplina |
| Prácticas de ingeniería | No las prescribe; las elige el equipo | Las prescribe como conjunto (programación en pareja, desarrollo guiado por pruebas, integración continua, refactorización) | Recomienda seis prácticas, entre ellas desarrollo iterativo, modelado visual y control de cambios |
| Participación del cliente | El Product Owner prioriza el backlog y revisa cada incremento | El cliente forma parte del equipo de manera permanente | Participa en los hitos de cada fase |
| Tamaño de proyecto habitual | Equipos pequeños | Equipos pequeños con cliente accesible | Proyectos grandes con equipos numerosos |
| Ajuste a VILCAS móvil | Alto | Medio | Bajo |

**Nota.** Elaboración propia (2026), a partir de Schwaber y Sutherland (2020), Beck y Andres (2004) y Kruchten (2004).

### Justificación de la elección

El equipo eligió Scrum. El proyecto tiene un plazo cerrado, del 1 de septiembre al 27 de noviembre de 2026, es decir, unas doce semanas, y un alcance ya descompuesto en 25 historias de usuario con prioridad y estimación en días. Ese formato es el Product Backlog que describe la Guía de Scrum (Schwaber y Sutherland, 2020). El desarrollo se reparte en cuatro sprints dentro de ese plazo, cada uno con un objetivo propio y un incremento que se revisa al cerrar. La planificación detallada de los sprints y su cronograma se presentan en el apartado VI.

Los roles de Scrum coinciden con la organización real del proyecto. ALBA Engineering Development, empresa de software propietaria de VILCAS, actúa como Product Owner y define qué se construye y en qué orden. Yerami Joseph Medina Trebejo ejerce de Scrum Master, y Jhordan Diego Medina Casapayco y Aldair Paolo Vega Carranza forman el equipo de desarrollo (Tabla 2). Tres personas no justifican la distribución de roles que RUP plantea entre analistas, arquitectos, diseñadores y probadores.

**Tabla 2**

*Roles Scrum del proyecto*

| Rol | Responsable | Función en el proyecto |
|---|---|---|
| Product Owner | ALBA Engineering Development | Define y prioriza el Product Backlog y valida los incrementos |
| Scrum Master | Medina Trebejo, Yerami Joseph | Facilita el uso de Scrum y retira impedimentos del equipo |
| Developers | Medina Casapayco, Jhordan Diego; Vega Carranza, Aldair Paolo | Construyen el incremento de cada sprint |

**Nota.** Elaboración propia (2026).

Frente a RUP, completar cuatro fases con los modelos y documentos de cada disciplina en unas doce semanas habría restado tiempo a la construcción de la aplicación. Frente a XP, el obstáculo es doble. Por un lado, XP exige un conjunto cerrado de prácticas técnicas y un cliente integrado al equipo de forma permanente (Beck y Andres, 2004), y en este proyecto ALBA participa como empresa propietaria del producto, no como parte del equipo de desarrollo. Por otro, Scrum deja que los desarrolladores elijan sus propias prácticas de ingeniería (Schwaber y Sutherland, 2020), y el equipo prefirió conservar esa libertad.

La elección tiene un costo. Scrum no resuelve por sí solo la ingeniería del producto: arquitectura, pruebas e integración quedan a cargo del equipo. Por eso el proyecto adopta una arquitectura MVVM por capas y pruebas automatizadas con JUnit y Espresso, recogidas en los requerimientos no funcionales (apartado IV.3).

## IV.2 Características del hardware y software

Las siguientes tablas detallan el equipo con el que se desarrolló y probó la aplicación, el software utilizado y los requisitos para ejecutarla.

**Tabla 3**

*Hardware del computador de desarrollo*

| Componente | Característica |
|---|---|
| Placa base | ASRock B860M-H2 (BIOS 1.12) |
| Procesador | Intel Core Ultra 5 245KF, 14 núcleos, aproximadamente 4,2 GHz |
| Memoria RAM | 16 GB (16 384 MB) |
| Tarjeta gráfica | NVIDIA GeForce RTX 3050, 6 GB de VRAM |
| Sistema operativo | Windows 11 Pro de 64 bits, compilación 26200 |
| DirectX | DirectX 12 |

**Nota.** Elaboración propia (2026), con datos de la herramienta de diagnóstico de DirectX de Windows.

**Tabla 4**

*Hardware del dispositivo móvil de pruebas*

| Componente | Característica |
|---|---|
| Modelo | 2410FPCC5G |
| Procesador | MediaTek Helio G81-Ultra, ocho núcleos, hasta 2,00 GHz |
| Memoria RAM | 8 GB, ampliable con 8 GB adicionales de memoria virtual |
| Almacenamiento interno | 256 GB |
| Sistema operativo | Android 16 (nivel de API 36), parche de seguridad del 1 de julio de 2026 |

**Nota.** Elaboración propia (2026), con datos de la pantalla de especificaciones del dispositivo.

La aplicación se compila para el nivel de API 37 y admite desde el nivel 24 (Android 7.0). El celular de pruebas representa el extremo reciente de ese rango.

**Tabla 5**

*Software de desarrollo de la aplicación Android*

| Componente | Versión | Uso |
|---|---|---|
| Android Studio Rabbit 1 | 2026.2.1 (compilación AI-262.9437.185.2621.16467767) | Entorno de desarrollo, emulador y depuración |
| JDK (JetBrains Runtime) | 25 | Entorno de ejecución del IDE y de Gradle |
| Compatibilidad del código Java | 11 | Nivel de lenguaje con el que se compila la aplicación |
| Gradle | 9.6.0 | Automatización de la compilación |
| Android Gradle Plugin | 9.4.1 | Construcción del proyecto Android |
| Lenguajes | Java y XML | Lógica de la aplicación y diseño de pantallas |
| Niveles de SDK | minSdk 24, targetSdk 37, compileSdk 37 | Compatibilidad y compilación |
| androidx.appcompat | 1.8.0 | Base de Activity y Fragments |
| Material Components | 1.14.0 | Componentes visuales y diálogos |
| androidx.constraintlayout | 2.2.2 | Diseño de pantallas |
| androidx.activity | 1.13.0 | Pantalla de borde a borde, selector de archivos y cámara |
| JUnit | 4.13.2 | Pruebas unitarias |
| AndroidX Test (ext.junit) | 1.3.0 | Pruebas instrumentadas |
| Espresso | 3.7.0 | Pruebas de interfaz |
| org.json | 20240303 | Manejo de JSON en pruebas unitarias |
| Git y GitHub | No aplica | Control de versiones y repositorio del proyecto |

**Nota.** Elaboración propia (2026), con datos de los archivos de configuración del proyecto.

### Entorno proporcionado por ALBA

El backend, la base de datos en Supabase, la aplicación web y el servidor VPS pertenecen a ALBA Engineering Development, que los proporciona al equipo. La aplicación móvil consume la API REST publicada en api.vilcaspe.com. El equipo tiene acceso para realizar los ajustes que la aplicación requiere, como el control de duplicados en las ventas sincronizadas y el envío de avisos de venta, pero esos componentes quedan fuera del alcance de este informe.

**Tabla 6**

*Requisitos del servicio que consume la aplicación*

| Requisito | Detalle |
|---|---|
| API | API REST de VILCAS accesible por HTTPS, con certificado válido, en api.vilcaspe.com |
| Autenticación | Servicios de inicio de sesión, verificación en dos pasos y renovación de sesión |
| Datos | Base de datos con inventario, ventas e indicadores del dashboard, alojada en Supabase |
| Correo | Servicio de envío de correos para el código de verificación y el enlace de restablecimiento |
| Archivos | Almacenamiento de las fotos de productos y de los comprobantes de pago |
| Ajustes para la app | Control de duplicados en ventas sincronizadas y envío de avisos de venta (HU-18 y HU-19) |

**Nota.** Elaboración propia (2026).

**Tabla 7**

*Requisitos del usuario final*

| Requisito | Detalle |
|---|---|
| Sistema operativo | Android 7.0 (nivel de API 24) o superior |
| Permisos | Acceso a Internet |
| Conexión | Internet para iniciar sesión, verificar la identidad y consultar datos; el registro de ventas sin conexión corresponde al Sprint 3 |
| Cámara y galería | Para fotografiar productos y comprobantes de pago, mediante la cámara del sistema |
| Impresión | Servicio de impresión de Android con una impresora compatible, solo para imprimir el ticket |

**Nota.** Elaboración propia (2026).

## IV.3 Requerimientos funcionales y no funcionales

Los requerimientos funcionales se derivan de las 25 historias de usuario del Product Backlog y se agrupan por funcionalidad.

**Tabla 8**

*Requerimientos funcionales*

| ID | Requerimiento funcional |
|---|---|
| RF-01 | El sistema debe permitir iniciar sesión con correo electrónico y contraseña, validando que ambos campos estén completos y que el correo tenga formato válido. |
| RF-02 | El sistema debe permitir mostrar u ocultar la contraseña y mantener la sesión iniciada en el dispositivo cuando el usuario elige «Recordarme». |
| RF-03 | El sistema debe mostrar un mensaje de error ante credenciales incorrectas sin indicar cuál de los dos datos falló. |
| RF-04 | El sistema debe enviar al correo registrado un enlace de restablecimiento de contraseña con vigencia limitada y confirmar el envío sin revelar si el correo existe. |
| RF-05 | El sistema debe exigir que la nueva contraseña cumpla una longitud mínima y contenga letras y números. |
| RF-06 | El sistema debe solicitar, tras unas credenciales válidas, un código de seis dígitos enviado al correo, con avance automático entre casillas. |
| RF-07 | El sistema debe limitar la vigencia del código a 5 minutos con un contador visible, informar si el código es incorrecto o venció y permitir solicitar uno nuevo. |
| RF-08 | El sistema debe limitar los módulos según el rol (el dueño accede a Dashboard, Inventario y Ventas; el encargado de inventario, a Inventario; el vendedor, a Ventas) y redirigir al módulo por defecto ante un acceso no permitido. |
| RF-09 | El sistema debe mostrar un menú lateral con la sección activa resaltada, una barra superior con notificaciones, modo oscuro e iniciales del usuario, y permitir cerrar sesión desde el avatar. |
| RF-10 | El sistema debe permitir crear, buscar, editar y eliminar categorías, sin nombres vacíos ni repetidos, con confirmación al eliminar y una advertencia si la categoría tiene productos. |
| RF-11 | El sistema debe permitir registrar, editar y eliminar proveedores, sin nombres vacíos ni repetidos, y ofrecerlos en el formulario de producto. |
| RF-12 | El sistema debe permitir registrar almacenes con nombre obligatorio y dirección y descripción opcionales, editarlos y eliminarlos, e impedir la eliminación de un almacén con stock asignado. |
| RF-13 | El sistema debe permitir registrar un producto con foto (cámara o galería), nombre, categoría, sexo, proveedor opcional, costo de compra y precio de venta. |
| RF-14 | El sistema debe calcular el margen de ganancia como precio de venta menos costo de compra, recalcularlo al modificar cualquiera de los dos valores, rechazar montos negativos y advertir si el precio es menor que el costo. |
| RF-15 | El sistema debe permitir asignar a cada producto tallas con stock, SKU y código de barras por almacén, sin aceptar stock negativo ni SKU repetidos. |
| RF-16 | El sistema debe permitir buscar productos por nombre y filtrarlos por categoría y sexo, mostrando foto, precio, margen, etiquetas y unidades en stock. |
| RF-17 | El sistema debe permitir editar un producto (incluida su foto), duplicarlo sin copiar el SKU ni el código de barras y eliminarlo con confirmación. |
| RF-18 | El sistema debe ofrecer un catálogo de ventas con buscador y filtros por talla, sede y sexo, y mostrar como agotados los productos sin stock en la sede seleccionada, que no pueden agregarse al carrito. |
| RF-19 | El sistema debe permitir agregar un producto al carrito eligiendo talla con stock, sede de origen, cantidad hasta el stock disponible y descuento en soles, y recalcular el total a pagar. |
| RF-20 | El sistema debe permitir revisar, editar y quitar productos del carrito, actualizar el total con cada cambio y mantener deshabilitado «Completar venta» cuando el carrito está vacío. |
| RF-21 | El sistema debe permitir seleccionar la caja de cobro y el método de pago (Efectivo, Yape, Plin o Transferencia), y dividir el pago en más de un método cuando la suma de los montos iguale el total. |
| RF-22 | El sistema debe registrar el monto recibido en efectivo, calcular el vuelto y rechazar montos menores al total. |
| RF-23 | El sistema debe exigir la foto del comprobante en los pagos digitales, con vista previa y opción de reemplazarla. |
| RF-24 | El sistema debe mostrar el resumen de la venta y pedir confirmación; al confirmar, debe registrar la venta con un ID único, descontar el stock de la talla y sede correspondientes y vaciar el carrito. |
| RF-25 | El sistema debe permitir imprimir el ticket de la venta. |
| RF-26 | El sistema debe detectar la pérdida de conexión, indicar el modo sin conexión y guardar las ventas y los movimientos de stock como operaciones pendientes en el celular. |
| RF-27 | El sistema debe enviar las operaciones pendientes al recuperar la conexión sin duplicar registros, mostrar cuántas quedan por sincronizar y notificar los conflictos de stock. |
| RF-28 | El sistema debe notificar al dueño cada venta registrada, con monto, local y vendedor (también las realizadas sin conexión, al sincronizarse), con contador de no leídas y detalle de la venta. |
| RF-29 | El sistema debe permitir elegir el periodo (Hoy, Esta semana, Este mes, Este año o Personalizado) y aplicarlo a todos los indicadores del dashboard. |
| RF-30 | El sistema debe mostrar la ganancia real con la etiqueta Rentable o No rentable, la variación porcentual frente al periodo anterior y un gráfico de tendencia, descontando los gastos recurrentes. |
| RF-31 | El sistema debe mostrar el total vendido con su cantidad de ventas, la venta promedio y el dinero invertido en inventario. |
| RF-32 | El sistema debe mostrar el ranking de productos más vendidos por unidades, con los cuatro primeros visibles y la opción «Ver más». |
| RF-33 | El sistema debe mostrar un gráfico circular de categorías más vendidas, con porcentaje y monto, y un gráfico de barras de ventas por método de pago. |
| RF-34 | El sistema debe mostrar un gráfico circular con la distribución de gastos por tipo. |
| RF-35 | El sistema debe listar los productos sin ventas, con bajo stock y más devueltos, distinguiendo cada alerta por color. |

**Nota.** Elaboración propia (2026), a partir del Product Backlog de VILCAS.

Los requerimientos no funcionales corresponden a la aplicación móvil y se redactaron con valores que el proyecto ya define en su configuración o en su código, de modo que cada uno puede verificarse.

**Tabla 9**

*Requerimientos no funcionales*

| ID | Categoría | Requerimiento | Criterio de verificación |
|---|---|---|---|
| RNF-01 | Compatibilidad | La aplicación debe funcionar en Android 7.0 (API 24) o superior. | Valor minSdk 24 en la configuración de Gradle y pruebas en un dispositivo con Android 16. |
| RNF-02 | Seguridad en tránsito | La aplicación debe comunicarse con el servidor solo por HTTPS. | La aplicación rechaza URL que no sean https y el manifiesto desactiva el tráfico en texto plano. |
| RNF-03 | Autenticación | La aplicación debe autenticarse ante la API con un token de acceso, renovarlo con el refresh token y exigir la verificación en dos pasos por correo. | El inicio de sesión exige el código de seis dígitos antes de entrar al módulo del rol. |
| RNF-04 | Seguridad en el dispositivo | La sesión debe guardarse cifrada con AES-GCM de 128 bits, con la clave en Android Keystore y fuera de las copias de seguridad. | Revisión de la clase de persistencia de sesión y del directorio de almacenamiento. |
| RNF-05 | Mínimo privilegio | La aplicación solo debe solicitar el permiso de Internet y usar la cámara a través del sistema. | Revisión del AndroidManifest.xml. |
| RNF-06 | Integridad del stock | Ante un conflicto de stock, la aplicación debe informar que el stock cambió y pedir actualizar el carrito. | Prueba de dos ventas simultáneas sobre la misma talla. |
| RNF-07 | Tolerancia a fallos de red | La aplicación debe esperar como máximo 15 s para conectar y 20 s para leer la respuesta, mostrar un mensaje claro si falla y no repetir una venta cuya respuesta quedó incierta. | Prueba con la red desactivada y revisión del cliente HTTP. |
| RNF-08 | Eficiencia en imágenes | La aplicación debe reducir las fotos a 1600 px de lado como máximo en JPEG con calidad 85 antes de subirlas, con un tope de 16 MB por imagen de entrada. | Comparación del tamaño de una foto antes y después de subirla. |
| RNF-09 | Rendimiento de listados | El catálogo debe cargarse en páginas de 30 productos, con respuestas de hasta 1 MiB y una caché de imágenes de 8 MB. | Revisión de los límites configurados y prueba con un catálogo grande. |
| RNF-10 | Usabilidad | La aplicación debe ofrecer modo claro y oscuro, mensajes guía en pantallas vacías y confirmación antes de eliminar o procesar una venta. | Revisión de las pantallas de las historias HU-04, HU-05, HU-14 y HU-17. |
| RNF-11 | Mantenibilidad | El código debe organizarse en una arquitectura MVVM con capas de presentación, dominio y datos por funcionalidad, y contar con pruebas unitarias y de interfaz. | Estructura de paquetes y ejecución de las pruebas con JUnit y Espresso. |

**Nota.** Elaboración propia (2026).

## IV.4 Marco teórico

*[Pendiente. Se redactará al final, cuando el texto del informe esté cerrado, para definir solo los términos que aparecen en él.]*

# V. GENERACIÓN DE SOLUCIONES

## V.1 Evaluación de tecnologías de aplicaciones móviles

Cada decisión técnica de la aplicación móvil se evaluó frente a sus alternativas más habituales. El backend, la base de datos y el servidor pertenecen a ALBA y no se evalúan aquí. Las tablas siguientes recogen ventajas y desventajas, y debajo de cada una se indica la tecnología seleccionada y por qué.

### V.1.1 Enfoque de desarrollo móvil

**Tabla 10**

*Evaluación de enfoques de desarrollo móvil*

| Tecnología | Ventajas | Desventajas |
|---|---|---|
| Android nativo | Acceso directo a las API del dispositivo (cámara, impresión, Keystore, ejecución en segundo plano); herramientas oficiales de depuración y perfilado. | Genera una aplicación solo para Android; una versión para iOS exigiría otro proyecto. |
| Flutter (Google, s. f.-a) | Una sola base de código para Android e iOS; recarga en caliente durante el desarrollo. | Exige adoptar Dart y su ecosistema, y reescribir el código ya existente. |
| React Native (Meta Platforms, s. f.) | Una base de código en JavaScript para ambas plataformas, con componentes nativos. | Algunas funciones del dispositivo dependen de módulos de terceros. |
| Aplicación web progresiva (MDN Web Docs, s. f.) | No requiere instalación desde una tienda; los Service Workers permiten cierto funcionamiento sin conexión. | Acceso más limitado a las funciones del dispositivo y comportamiento que varía entre navegadores. |

**Nota.** Elaboración propia (2026).

Se seleccionó el desarrollo nativo para Android. El curso Desarrollo de Aplicaciones Móviles exige trabajar con Java y Android Studio, requisito que dejó fuera a Flutter y a la aplicación web progresiva. En lo técnico, el proyecto usa la cámara, el sistema de impresión y el almacenamiento cifrado con Keystore, y prevé la sincronización en segundo plano, y todas estas funciones están disponibles de forma directa en las API de Android.

### V.1.2 Lenguaje de programación

**Tabla 11**

*Evaluación de lenguajes para Android*

| Tecnología | Ventajas | Desventajas |
|---|---|---|
| Java | Es el lenguaje del código ya escrito en el proyecto; amplia documentación y ejemplos. | Sintaxis más extensa; Google orienta las nuevas API y ejemplos hacia Kotlin (Android Developers, s. f.-b). |
| Kotlin | Es el lenguaje preferido por Google para Android; sintaxis concisa y manejo de nulos en el propio lenguaje. | Adoptarlo ahora obligaría a migrar el código existente o a mantener ambos lenguajes. |

**Nota.** Elaboración propia (2026).

Se usa Java y XML, tal como fija la documentación de arquitectura del repositorio. Todo el código de la aplicación está escrito en Java, y los dos lenguajes pueden convivir en un proyecto de Android Studio, por lo que una adopción posterior de Kotlin no exigiría reescribir lo existente.

### V.1.3 Entorno de desarrollo

**Tabla 12**

*Evaluación de entornos de desarrollo*

| Tecnología | Ventajas | Desventajas |
|---|---|---|
| Android Studio | IDE oficial de Android; incluye emulador, editor de diseños, perfilador e integración con Gradle (Android Developers, s. f.-c). | Consume más memoria que un editor ligero. |
| Visual Studio Code (Microsoft, s. f.) | Ligero y extensible. | No incluye de fábrica el editor de diseños ni el perfilador de Android; requiere extensiones y configuración manual del SDK. |

**Nota.** Elaboración propia (2026).

Se utiliza Android Studio Rabbit 1 (2026.2.1). Reúne en una sola herramienta la compilación, el emulador y el perfilador que el proyecto necesita, y el equipo de desarrollo cuenta con 16 GB de RAM, memoria suficiente para su consumo.

### V.1.4 Comunicación con el servidor

**Tabla 13**

*Evaluación de clientes HTTP*

| Tecnología | Ventajas | Desventajas |
|---|---|---|
| HttpsURLConnection | Forma parte de la plataforma; no añade dependencias al proyecto. | Los reintentos, el manejo de errores y la conversión de datos se programan a mano. |
| Retrofit (Square, s. f.) | Define las llamadas como interfaces tipadas y facilita la conversión de datos. | Añade dependencias externas al proyecto. |

**Nota.** Elaboración propia (2026).

La aplicación usa HttpsURLConnection a través de un cliente propio. Resulta suficiente para el conjunto de endpoints que consume y mantiene el proyecto sin librerías de red de terceros. A cambio, el cliente no reintenta peticiones de forma automática, una limitación que la sincronización del Sprint 3 debe resolver.

### V.1.5 Almacenamiento local y sincronización sin conexión

**Tabla 14**

*Evaluación de tecnologías de almacenamiento local y sincronización*

| Tecnología | Ventajas | Desventajas |
|---|---|---|
| Room | Capa de abstracción sobre SQLite que verifica las consultas SQL al compilar (Android Developers, s. f.-d). | Añade una dependencia y exige migraciones al cambiar el esquema. |
| SQLite con SQLiteOpenHelper | Incluido en Android, sin dependencias. | Mucho código repetitivo y sin verificación de consultas en compilación. |
| SharedPreferences o DataStore | Simples para configuraciones y valores sueltos. | No están pensados para datos relacionales como una cola de ventas pendientes. |
| WorkManager | Ejecuta tareas diferibles que se completan aunque la app se cierre o el dispositivo se reinicie, con restricciones de red (Android Developers, s. f.-e). | No garantiza una hora exacta de ejecución. |
| Servicio en primer plano | Ejecución inmediata y continua. | Exige una notificación permanente y consume batería. |

**Nota.** Elaboración propia (2026).

La propuesta del proyecto guarda las operaciones pendientes en Room sobre SQLite y las envía mediante una cola de sincronización hacia el backend REST de VILCAS. Room aporta una base de datos local con consultas verificadas en compilación. [POR CONFIRMAR: si el envío de la cola se dispara con WorkManager, que sobrevive al cierre de la aplicación, o con otro mecanismo.] El backend de ALBA no ofrece hoy una clave de idempotencia para las ventas, de modo que el equipo agrega ese ajuste para que un reenvío no duplique registros. Esta funcionalidad corresponde a HU-18 y se construye en el Sprint 3.

### V.1.6 Notificaciones de venta

**Tabla 15**

*Evaluación de mecanismos de notificación*

| Tecnología | Ventajas | Desventajas |
|---|---|---|
| Firebase Cloud Messaging (Google, s. f.-b) | Entrega notificaciones con la aplicación cerrada; el servicio de mensajería no tiene costo. | Depende de Google Play Services y de un proyecto de Firebase; el backend debe enviar los mensajes. |
| Consulta periódica al servidor | Sencilla y sin dependencias externas. | Retrasa el aviso, consume datos y batería, y sin tareas en segundo plano no funciona con la app cerrada. |
| WebSocket (Fette y Melnikov, 2011) | Comunicación en tiempo real. | Exige mantener una conexión abierta, difícil de sostener en segundo plano en Android. |

**Nota.** Elaboración propia (2026).

[POR CONFIRMAR: mecanismo previsto para el Sprint 4.] Para HU-19 conviene Firebase Cloud Messaging, porque el dueño debe enterarse de las ventas sin tener la aplicación abierta. Actualmente la campana de la app muestra anuncios del equipo de ALBA y no avisos de venta, de modo que esta funcionalidad se construye desde cero, con un ajuste puntual en el backend para enviar el mensaje.

### V.1.7 Almacenamiento seguro de la sesión

**Tabla 16**

*Evaluación de alternativas para guardar la sesión en el dispositivo*

| Tecnología | Ventajas | Desventajas |
|---|---|---|
| SharedPreferences sin cifrar | Simple y de acceso inmediato. | El token queda legible si alguien accede al almacenamiento de la aplicación, por ejemplo en un dispositivo con acceso root o en una copia de seguridad. |
| Archivo cifrado con AES-GCM y clave en Android Keystore (Android Developers, s. f.-a) | La clave de cifrado queda protegida por el sistema y no sale del dispositivo. | Más código propio que guardar el token en texto plano. |

**Nota.** Elaboración propia (2026).

La aplicación guarda la sesión en un archivo cifrado con AES-GCM de 128 bits, con la clave en el Keystore, y lo ubica fuera de las copias de seguridad automáticas (RNF-04). Así el token de acceso no queda en texto plano en el celular.

### V.1.8 Impresión del ticket

**Tabla 17**

*Evaluación de alternativas de impresión*

| Tecnología | Ventajas | Desventajas |
|---|---|---|
| Servicio de impresión de Android (PrintManager) | Funciona con las impresoras que el sistema ya reconoce, sin permisos adicionales; permite formatos de 80 mm y A4. | Requiere un servicio de impresión instalado para la impresora. |
| Impresora térmica por Bluetooth (comandos ESC/POS) | Impresión directa del ticket en papel térmico. | Exige permisos de Bluetooth, una librería de terceros y compatibilidad según el modelo de impresora. |

**Nota.** Elaboración propia (2026).

La aplicación arma el ticket como HTML y lo envía al sistema de impresión de Android, en formato de 80 mm o A4. Con esta opción el proyecto conserva el permiso de Internet como único permiso declarado.

### V.1.9 Gráficos del dashboard

**Tabla 18**

*Evaluación de alternativas para los gráficos*

| Tecnología | Ventajas | Desventajas |
|---|---|---|
| Dibujo propio con Canvas | Sin dependencias y control total del aspecto. | Más código y sin interacciones listas para usar. |
| Librería de gráficos para Android (por ejemplo, MPAndroidChart) | Tipos de gráfico y animaciones ya implementados. | Añade una dependencia externa. |
| WebView con Chart.js | Reutiliza los gráficos de la versión web. | Incorpora un navegador embebido a la pantalla y mezcla tecnologías web dentro de la app nativa. |

**Nota.** Elaboración propia (2026).

El dashboard dibuja sus gráficos con Canvas, y la documentación del repositorio descarta de forma explícita el uso de WebView para esta pantalla. El gráfico circular y el de barras que piden HU-23 y HU-24 son de forma simple y no justifican una dependencia adicional.

---

## Referencias de estos apartados (formato APA 7, para unir a la lista general)

Android Developers. (s. f.-a). *Android Keystore system*. Google. https://developer.android.com/privacy-and-security/keystore

Android Developers. (s. f.-b). *Android's Kotlin-first approach*. Google. https://developer.android.com/kotlin/first

Android Developers. (s. f.-c). *Meet Android Studio*. Google. https://developer.android.com/studio/intro

Android Developers. (s. f.-d). *Save data in a local database using Room*. Google. https://developer.android.com/training/data-storage/room

Android Developers. (s. f.-e). *WorkManager*. Google. https://developer.android.com/topic/libraries/architecture/workmanager

Beck, K., & Andres, C. (2004). *Extreme programming explained: Embrace change* (2.ª ed.). Addison-Wesley.

Fette, I., & Melnikov, A. (2011). *The WebSocket protocol* (RFC 6455). Internet Engineering Task Force. https://www.rfc-editor.org/rfc/rfc6455

Google. (s. f.-a). *Flutter*. https://flutter.dev

Google. (s. f.-b). *Firebase Cloud Messaging*. Firebase. https://firebase.google.com/docs/cloud-messaging

Kruchten, P. (2004). *The Rational Unified Process: An introduction* (3.ª ed.). Addison-Wesley.

MDN Web Docs. (s. f.). *Progressive web apps*. Mozilla. https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps

Meta Platforms. (s. f.). *React Native*. https://reactnative.dev

Microsoft. (s. f.). *Visual Studio Code*. https://code.visualstudio.com

Schwaber, K., & Sutherland, J. (2020). *La guía de Scrum: La guía definitiva de Scrum: Las reglas del juego*. https://scrumguides.org

Square. (s. f.). *Retrofit*. https://square.github.io/retrofit/
