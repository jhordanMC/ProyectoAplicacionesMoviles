# Topbar y perfil conectados a FastAPI

Referencia: `shared/topbar/topbar.ts`, `.html`, `.css`, `core/auth.ts`, `services/anuncios.ts` y `services/satisfaccion.ts` del frontend Angular original.

El shell de empresa comparte `ProfileViewModel`. Los datos se cargan fuera del hilo principal, con token Bearer y controles de sesión de origen: una respuesta pendiente de otra cuenta no modifica la cuenta activa.

La apertura de perfil pertenece al contenedor único `companyProfileButton`; la foto y las iniciales son elementos visuales y no interceptan los toques. Se evita duplicar el modal y se descarta una instancia anterior sin diálogo visible. Las pruebas de UI utilizan toques Espresso sobre el botón real, con foto visible y sin foto, y comprueban cierre y reapertura en vertical y horizontal. El menú conserva los tamaños, Inter, iconos y colores del CSS original.

- Perfil, nombre, roles, correo, teléfono, DNI, empresa: GET `/iam/mi-perfil`.
- Foto: POST multipart `/iam/mi-perfil/avatar`, campo `archivo`; JPEG preparado con `AndroidImageSource`. El avatar del modal y del topbar se actualizan juntos.
- Campana y punto rojo: GET `/anuncios/activos`; POST `/anuncios/{id}/marcar-visto` al abrir la lista. Encuestas: POST `/anuncios/{id}/votar`, `id_opcion`. Los resultados aparecen solo después de un voto confirmado por el servidor.
- Opinión: POST `/satisfaccion/topbar`, estrellas 1–5 y comentario opcional de hasta 500 caracteres.
- Contraseña: POST `/iam/auth/solicitar-cambio-password` con contraseña actual y nueva; POST `/iam/auth/confirmar-cambio-password` con código de seis dígitos, incluidos ceros iniciales. Un 401 por contraseña incorrecta o código agotado se distingue de una sesión expirada consultando el perfil.
- Agregar cuenta: login y 2FA separados del login principal. Cambiar cuenta: POST `/iam/auth/refresh`, seguido de perfil. El shell se reconstruye después del cambio para reiniciar inventario, dashboard y carrito de la cuenta anterior.
- Las cuentas y la selección activa se guardan con AES-GCM y una clave de Android Keystore, en un archivo atómico dentro de `noBackupFilesDir`. No se almacenan contraseñas ni códigos 2FA. Olvidar una cuenta revoca su refresh; cerrar sesión elimina y revoca la activa y conserva las otras cuentas.
- Al iniciar un proceso nuevo, el logo giratorio cubre la restauración: POST `/iam/auth/refresh` y GET `/iam/mi-perfil` antes de abrir el shell. Los tokens rotados se guardan antes de consultar el perfil, para conservarlos si falla esa segunda petición. Un fallo de red permite reintentar sin borrar la cuenta; un 401/403 devuelve al login.
- Claro/oscuro: preferencia local persistente, igual que en la web; no necesita endpoint.

Las subvistas de perfil comparten el modal original, Inter, campos y botones; permiten volver al menú. El cambio de contraseña y el ingreso de otra cuenta reutilizan `CodeInputView` con animación líquida. Se impide enviar dos operaciones simultáneas y se muestran errores del backend sin confirmar éxitos ficticios. Acerca de VILCAS mantiene versión y eslogan; los enlaces legales originales todavía carecen de destinos publicados.

Validación: pruebas unitarias de contratos, multipart, token, estados de encuesta y rotación de sesión; pruebas de UI con repositorios de prueba para información personal, calificación, contraseña/código y campana/voto; pruebas del ViewModel para errores 401 y sesiones múltiples. Las pruebas no modifican cuentas reales ni envían correos reales.
