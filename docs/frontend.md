# Primera base del frontend

Se conserva Java, XML y la identidad visual del login existente. La Activity
aloja Fragments y coordina navegación a través de `FrontendNavigator`.
`AuthFragment` reutiliza el formulario XML de recuperación y verificación;
`CompanyFragment` comparte el contenedor entre dueño y vendedor.

## Recorrido de demostración

- Login: correo válido y contraseña no vacía permiten abrir la vista 2FA.
  No se autentica ni almacena la contraseña.
- 2FA: ingresar cualquier combinación de seis dígitos permite elegir el rol
  de demostración. No se valida ni se envía un código real.
- Recuperación: valida el formato del correo y muestra explícitamente que no
  se envió un mensaje.
- El login conserva el prototipo original, incluido «Recordarme» (por ahora
  solo visual) y el enlace de contacto (canal pendiente de configurar).
  Para explorar los roles se utiliza el flujo de demostración de 2FA.
- Dueño: resumen, inventario, ventas, historial, proformas, catálogos,
  finanzas e integraciones. Vendedor: inventario, ventas, historial y proformas.

Las secciones internas solo tienen título, descripción y estado de vista previa;
no contienen todavía listados, formularios de negocio ni datos. La selección de
rol es una herramienta visual, no autorización. Antes de conectar datos reales
debe reemplazarse por la sesión y permisos del backend.

## Verificación

Compilar con `gradlew.bat :app:assembleDebug`. En esta máquina el JDK disponible
está en `D:\AndroidStudio\jbr`; el JAVA_HOME global observado apunta a una carpeta
inexistente. Se usó el JDK disponible solo para el proceso de compilación.

Pendiente verificar apariencia y navegación en un emulador o dispositivo.
Backend, persistencia y sincronización quedan para una etapa posterior.
