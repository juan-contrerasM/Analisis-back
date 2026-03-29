# Análisis financiero — ETL y dashboard

Monorepo con **backend Spring Boot** (pipeline ETL, análisis y similitud) y **frontend Angular 21** (dashboard tipo fintech). El front consume la API REST bajo el prefijo `/etl` y puede refrescar datos de forma automática y bajo demanda.

---

## Qué se implementó

- **Backend**: servicio ETL con estado global en memoria (dataset, retornos, resultados de análisis), control de concurrencia (`synchronized`), enum de estado (`IDLE`, `EJECUTANDO`, `LISTO`, `ERROR`), versión incremental de datos y timestamp de última actualización.
- **API REST** documentada bajo `/etl`: estado, ejecución manual del ETL, símbolos, dataset, análisis, similitud y series de retornos; respuestas **503** coherentes cuando aún no hay datos tras un ETL.
- **CORS** configurado para desarrollo con Angular en `http://localhost:4200`.
- **ETL programado** (Spring `@Scheduled`): opcional y configurable en `application.properties` (arranque diferido, intervalo fijo tras cada ejecución completa).
- **Frontend**: aplicación standalone con lazy loading por features (`dashboard`, `activos`, `similitud`, `analisis`), servicios de estado y polling, proxy en desarrollo hacia el backend, gráficas con Chart.js / ng2-charts, tema oscuro y barra superior con estado del ETL, refresco manual y ejecución del ETL desde la UI.

La lógica algorítmica del ETL, similitud y análisis permanece en **Java**; el Angular **visualiza y opera** esos resultados vía HTTP.

---

## Estructura del repositorio

| Carpeta | Rol |
|--------|-----|
| `BACK-END/` | Spring Boot: ETL, similitud, análisis, API REST |
| `FRONT-END/` | Angular 21: dashboard, tablas, gráficas, estado global |

---

## Cómo funciona (flujo general)

1. El **backend** expone el estado ligero en `GET /etl/status` (ETL ejecutado, estado, última actualización, versión).
2. **`GET /etl/run`** lanza el pipeline ETL de forma **síncrona** (puede tardar; incluye descargas y pausas según la implementación).
3. Tras un ETL exitoso, en memoria quedan el dataset unificado, retornos y resultados usados por análisis y similitud.
4. El **frontend** hace polling periódico a `/etl/status`. Si el estado es `LISTO` y el ETL se ha ejecutado, carga datos pesados (símbolos, dataset, análisis) sin solapar dos cargas simultáneas (`refreshLock`). Mientras el estado es `EJECUTANDO`, no se disparan esas peticiones pesadas.
5. Las rutas `/dashboard`, `/activos`, `/similitud` y `/analisis` muestran métricas, tablas filtrables/ordenables y gráficas según los endpoints disponibles.

---

## API del backend (`/etl`)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/etl/status` | Estado del ETL, timestamp, versión (respuesta ligera). |
| `GET` | `/etl/run` | Ejecuta el pipeline ETL (síncrono). |
| `GET` | `/etl/symbols` | Lista de tickers del portafolio. |
| `GET` | `/etl/dataset` | Dataset unificado; fechas en ISO `yyyy-MM-dd`. **503** si no hay datos listos. |
| `GET` | `/etl/analysis` | Análisis (ranking, volatilidad, riesgo, patrones). **503** si no hay datos. |
| `GET` | `/etl/similarity?asset1=&asset2=` | Métricas de similitud. **503** si no hay datos. |
| `GET` | `/etl/series?asset1=&asset2=` | Series de retornos alineadas. **503** si no hay datos. |

Puerto por defecto del servidor: **8080** (`application.properties`).

---

## Frontend (resumen técnico)

- **`core/`**: `EtlApiService` (llamadas a `/etl/*`), `AppStatusService` (polling, caché, `runEtlFromUi`, refresco forzado), manejo global de errores HTTP.
- **`shared/`**: layout, modelos TypeScript (`etl.models.ts`), componentes reutilizables.
- **Entornos**: en desarrollo suele usarse **proxy** (`proxy.conf.json`) para enviar `/etl` a `http://localhost:8080`; en producción, `apiUrl` apunta al host real del API.
- **Auto-refresh**: intervalo configurable en `environment*.ts` (`autoRefreshIntervalMs`); la barra superior puede mostrar un texto del tipo “Auto cada X min” según ese valor.

---

## Cómo ejecutar en local

### Backend

Desde `BACK-END/`, con Maven o desde el IDE, levantar Spring Boot en el puerto **8080**.

### Frontend

```bash
cd FRONT-END
npm install
npm start
```

Abrir la URL que indique el CLI (habitualmente `http://localhost:4200`).

### Uso típico

1. Asegurarse de que el backend está arriba.
2. En la UI, usar **Ejecutar ETL** y esperar a que termine (la primera vez es obligatorio para tener datos).
3. Navegar por dashboard, activos, similitud y análisis; el refresco automático seguirá sincronizando mientras la app esté abierta y el servidor en estado `LISTO`.

---

## Ajustes frecuentes

| Necesidad | Dónde |
|-----------|--------|
| Intervalo de auto-refresh en el front | `FRONT-END/src/environments/environment*.ts` → `autoRefreshIntervalMs` |
| ETL programado en segundo plano | `BACK-END/src/main/resources/application.properties` → `etl.scheduled.enabled`, `etl.scheduled.initial-delay-ms`, `etl.scheduled.fixed-delay-ms` |
| Origen CORS | Clase de configuración CORS en el backend (p. ej. otro host/puerto en producción) |
| API en build de producción | `environment.ts` → `apiUrl` sin depender del proxy |

---

## Documentación adicional

En la raíz del repo, **`CONTEXTO-IMPLEMENTACION.md`** amplía detalles de implementación (clases, convenciones y comportamiento del polling).

---

*Proyecto académico de análisis financiero con ETL y visualización en dashboard.*
