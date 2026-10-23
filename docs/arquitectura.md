# Arquitectura Android y alcance de migración

## Prioridad actual

La fase visual inicial evolucionó a integración escalonada con FastAPI: autenticación, Ventas, Inventario y Dashboard ya tienen repositorios remotos. Se mantienen presentación, dominio y datos separados, con componentes XML compartidos. El diseño offline de este documento queda como referencia futura; no implica que ya exista almacenamiento o sincronización local.

## Alcance confirmado

Migrar login, recuperación de contraseña, verificación 2FA y las funcionalidades
del panel empresa para dueño y vendedor. El panel administrador y las páginas
públicas quedan fuera del alcance. Mantener Java y XML como base actual.

La aplicación debe permitir trabajar con datos locales sin internet. Las
operaciones concretas habilitadas sin conexión están pendientes de definición.

## Organización

Inicialmente mantener un módulo Gradle `app`, organizado por funcionalidades.
Separar módulos Gradle cuando exista una necesidad concreta de aislamiento.

```text
com.example.saasvilcas
  app/                       Inicio, composición de dependencias y navegación
  core/network/              Cliente HTTP y traducción de errores
  core/session/              Sesión, almacenamiento protegido y acceso offline
  core/database/             Room, migraciones y transacciones
  core/sync/                 Cola persistente y trabajo con WorkManager
  core/permissions/          Política de acceso por rol y vistas habilitadas
  shared/ui/                 Formularios, carga, errores y estados vacíos
  feature/auth/              Login, recuperación y 2FA
  feature/dashboard/         Resumen de empresa
  feature/inventory/         Productos, variantes y existencias
  feature/sales/             Venta, historial y proformas
  feature/finance/           Finanzas de dueño
  feature/integrations/      Integraciones de dueño
```

Cada funcionalidad usa `presentation`, `data` y `domain` cuando corresponda.
Las pantallas renderizan estado y emiten acciones. Los ViewModels coordinan
acciones y exponen estado observable; no retienen vistas ni Activities.
Los repositorios encapsulan acceso local y remoto. Los casos de uso contienen
reglas complejas o compartidas; no crear uno por cada llamada HTTP sencilla.
Los modelos de API, persistencia y negocio se separan cuando sus contratos
difieran. Usar composición y componentes pequeños en lugar de Activities base
con múltiples responsabilidades.

## Lecturas sin conexión

La interfaz observa la base local mediante los repositorios. Una actualización
remota escribe en la base local y actualiza las pantallas a través del mismo
flujo. Mostrar última actualización y estado de sincronización. Si nunca se
descargaron datos, mostrar que hace falta una primera conexión.

Los datos se identifican por empresa y, cuando corresponda, por usuario.
Toda consulta y operación pendiente debe respetar esa partición. Cambiar de
cuenta nunca debe mostrar datos de otra empresa.

## Escrituras sin conexión

Para las operaciones que finalmente se autoricen offline, guardar la operación
local y su entrada en la cola pendiente en una sola transacción Room. La cola
debe sobrevivir al cierre y reinicio de la aplicación.

Cada entrada necesita identificador estable, empresa, usuario, tipo de
operación, contenido versionado, fecha, estado, intentos y último error.
WorkManager ejecutará la sincronización con conexión disponible, reutilizando
la misma lógica que la sincronización manual.

Estados previstos: pendiente, enviando, sincronizada y requiere revisión.
Reintentar fallos transitorios con espera progresiva. Un rechazo por permisos,
stock o validación exige revisión; no debe desaparecer de la cola ni presentarse
como una operación confirmada por el servidor.

Las ventas deben distinguir registro local pendiente de confirmación remota.
Dos dispositivos sin conexión pueden vender el mismo stock: el servidor debe
resolver ese conflicto. No sobrescribir existencias del servidor con una copia
local ni asumir que el stock descargado sigue vigente.

Antes de activar el envío automático de ventas, verificar soporte del backend
para idempotencia: repetir el mismo identificador debe devolver el mismo
resultado sin crear otra venta, incluso cuando se pierde la primera respuesta.
El cliente Angular revisado no expone ese identificador; esto no demuestra que
el backend carezca de soporte y debe verificarse en su contrato o código.

## Autenticación y permisos

El primer login, 2FA por correo y recuperación de contraseña requieren internet.
El acceso offline posterior requiere una sesión previamente validada y una
política explícita de duración, bloqueo local y revocación al reconectar.
Un token HTTP vencido no concede autorización remota; la sesión local y la
vigencia del token remoto son conceptos distintos.

Conservar la política de roles y vistas deshabilitadas. Dueño y vendedor
comparten pantallas, con acciones y navegación determinadas por permisos.
El servidor sigue validando cada operación sincronizada. Si requiere nueva
autenticación, pausar el envío y conservar las operaciones pendientes.

No almacenar contraseñas. Proteger tokens mediante claves del Android Keystore
y almacenamiento privado; excluir credenciales de copias de seguridad y logs.
Cerrar sesión debe bloquear acceso local y detener la sincronización de esa
cuenta sin borrar silenciosamente operaciones pendientes.

## Contratos observados en el frontend web

- `POST /iam/auth/login`: formulario `username`, `password`; devuelve
  `login_token` y `expira_en_segundos`.
- `POST /iam/auth/verificar-2fa`: JSON `login_token`, `codigo`; devuelve tokens.
- Perfil: `/iam/mi-perfil`; permisos: `/iam/mis-permisos`; vistas deshabilitadas:
  `/iam/mis-vistas-deshabilitadas`.
- La recuperación de contraseña en `olvidecontra.ts` usa una simulación con
  temporizador: falta identificar un endpoint real. Android no debe mostrar
  un correo enviado si el backend no confirmó la solicitud.
- Ventas: el cliente llama a `/pos/confirmar` bajo su URL base de ventas;
  falta verificar garantías de idempotencia y resolución de conflictos.

El repositorio recibido contiene el frontend, no el backend.

## Orden de implementación

1. Autenticación conectada, estados de pantalla reutilizables y navegación por
   permisos. Conectar recuperación cuando se conozca su contrato real.
2. Persistencia local de sesión y catálogo, con aislamiento por empresa.
3. Operaciones offline acordadas y cola transaccional persistente.
4. Sincronización con idempotencia y tratamiento de conflictos verificados.
5. Incorporar progresivamente las restantes pantallas del panel empresa.

## Verificación necesaria

Verificar rotación durante login y 2FA, expiración del código y fallos de red.
Para offline: primera ejecución sin datos, reinicio con operaciones pendientes,
cambio de empresa, caída tras confirmar una venta antes de recibir respuesta,
rechazo por stock, permisos revocados y reautenticación al sincronizar.

## Referencias

- https://developer.android.com/topic/architecture/recommendations
- https://developer.android.com/topic/architecture/data-layer/offline-first

Este documento define el diseño; no representa funcionalidades implementadas.
