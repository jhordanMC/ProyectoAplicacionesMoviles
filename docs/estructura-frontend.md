# Estructura del frontend Android

Actualización: autenticación, Ventas, Inventario y Dashboard consultan FastAPI. Se conserva la interfaz original y se comparten estilos, modales y filtros XML.

## Alcance actual

El dueño tiene Dashboard, Inventario y Ventas. Cada funcionalidad separa presentación, dominio y acceso a datos. La sincronización offline queda pendiente. Los contratos y límites actuales están documentados en `dashboard-api.md`, `ventas-api.md` e `inventario-api.md`.

## Código por funcionalidad y capas

```text
java/com/example/saasvilcas/
  app/                           Contrato de navegación
  feature/auth/presentation/     Login, recuperación y 2FA
  feature/company/presentation/  Contenedor, menú y navegación interna
  feature/dashboard/
    presentation/               Fragment y ViewModel
    domain/                     Modelo y contrato del repositorio
    data/                       Repositorio remoto FastAPI
  feature/sales/                Presentación, dominio y datos del catálogo/carrito
  shared/ui/                    Código de seis dígitos y gráficos
```

El contenedor de empresa no renderiza métricas del dashboard. El Fragment del
dashboard renderiza su estado, el ViewModel conserva selección de periodo y
expansión de productos, y el repositorio proporciona datos de muestra. Se usa
una interfaz de repositorio para poder conectar datos reales posteriormente.

## Recursos XML por funcionalidad

Android no admite carpetas arbitrarias dentro de `res/layout`. Por eso se
configuran varios directorios de recursos en `app/build.gradle.kts`:

```text
src/main/
  res/              Recursos originales de identidad visual y aplicación
  res-auth/layout/  Pantallas de autenticación
  res-company/      Contenedor y componentes del menú y barra superior
  res-dashboard/    Pantalla, bloques e ítems reutilizables del dashboard
  res-sales/        Filtros, catálogo, carrito y editor de productos
  res-shared/       Encabezado, correo, botón y pie de autenticación;
                    colores, estilos, iconos y fondos del panel empresa
```

Las referencias `R.layout` conservan sus nombres. Todos los directorios
comparten el espacio de nombres Android: los recursos nuevos usan prefijos
`company_` o `dashboard_` para evitar colisiones. No crear subcarpetas bajo
`layout` ni duplicar un recurso entre los directorios.

Es modularidad por funcionalidades y capas dentro de un módulo Gradle `app`.
Todavía no hay múltiples módulos Gradle. Pueden extraerse después sin introducir
dependencias desde datos o dominio hacia pantallas.

## Estado actual

- El menú animado permite navegar entre las tres secciones.
- Los filtros del dashboard consultan fechas reales, incluyendo un rango personalizado.
- El gráfico compara ingresos y ganancias con escala común, pérdidas y etiquetas de la API.
- Cada tarjeta tiene carga, vacío y error independientes; se puede reintentar la consulta fallida.
- Los gráficos usan cantidades reales; las fotografías se cargan desde el catálogo.
- Los desplegables comparten tipografía, tamaños, colores y estados seleccionados.
- No se ha implementado sincronización offline.
