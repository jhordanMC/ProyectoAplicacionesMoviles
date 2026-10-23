# Medina SQLite · Saiyajin

Alumno: Jhordan Diego Medina Casapayco. Laboratorio de aplicaciones móviles S07.

Aplicación Java con SQLite: registrar, listar, actualizar y eliminar Saiyajins.
Base: BD_SAIYAJIN. Tabla: Saiyajin. Campos: id INTEGER PRIMARY KEY AUTOINCREMENT, nombre VARCHAR, planeta VARCHAR, transformacion VARCHAR.
Los tres campos de texto son obligatorios. El ID se genera automáticamente y no se puede modificar.

## Ejecutar

Abre la carpeta en Android Studio, sincroniza Gradle y ejecuta app en un teléfono o emulador Android 7 o superior.

## Comprobar

1. Registra Goku / Tierra / Super Saiyajin.
2. Pulsa Ver Saiyajins y toca el registro.
3. Cambia la transformación, pulsa Actualizar y revisa la lista.
4. Cierra y abre la app para comprobar que persiste.
5. Prueba campos vacíos, cancelar la eliminación y confirmar la eliminación.

Para inspeccionar la base, usa App Inspection > Database Inspector con la aplicación en ejecución en Android 8 o superior y selecciona BD_SAIYAJIN.

Los nombres de base, tabla y atributos se eligieron para la clase asignada; si el docente proporcionó otros, deben ajustarse.
