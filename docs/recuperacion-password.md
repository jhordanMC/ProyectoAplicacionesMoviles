# Recuperación de contraseña

Android llama a POST `/iam/auth/recuperar-password` con JSON `{email}`, sin Bearer. La pantalla conserva el fondo, borde animado y diseño de `olvidecontra.html/css`; no simula un envío. Muestra errores, permite reenvío y conserva estado al girar. El mensaje de éxito es condicional para no revelar cuentas registradas.

El backend existente genera un token de un solo uso, guarda únicamente su hash y lo envía por el SMTP de 2FA. El enlace vence a los 30 minutos y lleva el token en el fragmento de URL, para no enviarlo en solicitudes del servidor web. La ruta Angular `/resetear-password` valida las dos contraseñas y llama a POST `/iam/auth/resetear-password` con `{token,password_nueva}`. El formulario borra el token de la URL y no lo guarda en localStorage.

Cambios preparados en los repositorios locales:

- `D:/Alba/Saas/Backend/alba-saas-backend`: mail real y variable `PASSWORD_RESET_URL`; conserva los endpoints existentes.
- `D:/Alba/Saas/ProyectoSaasAlba`: recuperación real y nueva ruta de contraseña con el CSS original.

Desplegar ambos repositorios antes de usar el enlace en producción. `PASSWORD_RESET_URL` debe señalar a la URL pública `/resetear-password` del frontend desplegado; el valor inicial usa el dominio Azure del logo ya configurado. Se reutilizan `SMTP_USER`, `SMTP_PASSWORD` y `SMTP_HOST`. No se hicieron commits, despliegues ni envíos a correos reales durante las pruebas.

Validación: compilación Angular, pruebas unitarias Android del contrato JSON, UI de error/reintento/reenvío/rotación y cuatro pruebas de backend con correo y repositorio mock. Ejecutar estas últimas con `python tests/unit/test_password_recovery_mail.py` para evitar los fixtures de integración que usan infraestructura real.
