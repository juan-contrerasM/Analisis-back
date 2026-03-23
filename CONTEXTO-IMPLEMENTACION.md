# Contexto de la implementación — Análisis financiero ETL

Este documento resume lo implementado en el proyecto **Analisis-back**: extensión del backend Spring Boot, aplicación Angular tipo dashboard fintech, sincronización automática y consumo de la API `/etl`.

---

## 1. Estructura del repositorio

| Carpeta      | Rol |
|-------------|-----|
| `BACK-END/` | Spring Boot: ETL, similitud, análisis, API REST |
| `FRONT-END/` | Angular 21 (standalone): dashboard, tablas, gráficas, estado global |

---

## 2. Backend (Spring Boot)

### 2.1 Estado global del ETL (`ETLServiceImpl`)

- Lista de símbolos fija del portafolio (`PORTFOLIO_SYMBOLS`).
- Tras un ETL exitoso se guardan en memoria: `datasetGlobal`, `retornosGlobal`, `resultadoReq3`.
- Campos de control: `etlEjecutado`, `ultimaActualizacion` (`Instant`), `dataVersion` (incremental), `EtlEstado` (`IDLE`, `EJECUTANDO`, `LISTO`, `ERROR`), `mensajeError` si aplica.
- `runETL()` es `synchronized` para evitar ejecuciones solapadas.

### 2.2 Interfaz `ETLService`

Expone: `runETL`, `getEtlStatus`, `getSymbols`, `getDatasetRows`, `obtenerAnalisis`, `calcularSimilitud`, `obtenerSeries`, `isEtlReady`.

### 2.3 Endpoints REST (`ETLController`, prefijo `/etl`)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/etl/status` | Estado del ETL, timestamp, versión (respuesta ligera para el front). |
| GET | `/etl/run` | Ejecuta el pipeline ETL (síncrono, puede tardar mucho). |
| GET | `/etl/symbols` | Lista de tickers (siempre disponible). |
| GET | `/etl/dataset` | Dataset unificado; fechas en ISO `yyyy-MM-dd`. **503** si el ETL no está listo. |
| GET | `/etl/analysis` | Análisis (ranking, volatilidad, riesgo, patrones). **503** si no hay datos. |
| GET | `/etl/similarity?asset1=&asset2=` | Métricas de similitud. **503** si no hay datos. |
| GET | `/etl/series?asset1=&asset2=` | Series de retornos alineadas. **503** si no hay datos. |

Cuerpo típico de error **503**: JSON con `error` y `message` indicando que hay que ejecutar el ETL.

### 2.4 CORS y configuración

- `CorsConfig`: origen `http://localhost:4200` (desarrollo Angular).
- `application.properties`: `server.port=8080`, `etl.scheduled.enabled=false`.

### 2.5 ETL programado

- `ETLScheduler` con `@Scheduled(fixedDelay)` si `etl.scheduled.enabled=true` (por defecto **activo** en `application.properties`). El intervalo es **después** de terminar cada ETL (`etl.scheduled.fixed-delay-ms`, p. ej. 1 h). Ajustar si el pipeline dura más que el intervalo.

### 2.6 Enum y modelo

- `EtlEstado.java` en `com.uniquindio.etl.model`.

---

## 3. Frontend (Angular 21)

### 3.1 Arquitectura de carpetas (`src/app/`)

- **`core/`**: servicios globales, interceptor HTTP, configuración.
  - `EtlApiService`: todas las llamadas a `/etl/*`.
  - `AppStatusService`: polling, caché de `symbols` / `dataset` / `analysis`, `runEtlFromUi`, `forceRefreshFromServer`, `loadSymbolsOnly`.
  - `GlobalErrorService` + `httpErrorInterceptor` (errores de red / 5xx).
  - `app-config.ts`: `apiBaseUrl()`, `autoRefreshIntervalMs()`.
- **`shared/`**: layout, modelos TypeScript, UI reutilizable (`metric-card`, `data-table`, `page-skeleton`).
- **`features/`** (lazy loading): `dashboard`, `activos`, `similitud`, `analisis`.

### 3.2 Rutas

- Layout principal con sidebar y barra superior.
- Hijos: `/dashboard`, `/activos`, `/similitud`, `/analisis`; redirección de `''` a `dashboard`.

### 3.3 Entornos

- `environment.ts` (producción): `apiUrl: 'http://localhost:8080'`, `autoRefreshIntervalMs: 60_000`.
- `environment.development.ts`: `apiUrl: ''` (mismo origen con `ng serve`) + **proxy** hacia el backend.
- `angular.json`: `fileReplacements` en build `development` para usar el entorno de desarrollo.

### 3.4 Proxy de desarrollo

- `proxy.conf.json`: reenvía `/etl` → `http://localhost:8080`.
- `angular.json` → `serve.options.proxyConfig`.

### 3.5 Gráficas

- `chart.js` + `ng2-charts` + `provideCharts(withDefaultRegisterables())` en `app.config.ts`.
- `@angular/cdk` como dependencia requerida por `ng2-charts`.

### 3.6 Sincronización automática (cada 1 minuto)

- `AppStatusService.startPolling()` usa `timer(0, environment.autoRefreshIntervalMs)`.
- En cada tick: `GET /etl/status`; si `etlEjecutado` y `estado === LISTO`, se llama siempre a **`loadHeavyData()`** (símbolos + dataset + análisis), **sin** comparar timestamps.
- Mientras `EJECUTANDO`, no se piden endpoints pesados; solo se refleja el estado en la UI.
- `refreshLock` evita solapar dos cargas pesadas.
- La barra superior muestra texto del tipo **“Auto cada 1 min”** (derivado de `autoRefreshIntervalMs`).
- **Refrescar** y **Ejecutar ETL** siguen disponibles; tras ETL exitoso se recarga datos de inmediato.

### 3.7 UI

- Tema oscuro tipo fintech (`styles.css`, variables CSS).
- Navbar: indicador de estado (punto de color), última actualización del servidor, hint de auto-refresh, botones Refrescar / Ejecutar ETL.
- **Dashboard**: métricas, barras (top volatilidad), línea de precios de referencia.
- **Activos**: tabla del dataset con filtro y ordenación.
- **Similitud**: dos selectores, comparación, barras normalizadas, series de retornos opcionales.
- **Análisis**: tabla de ranking, riesgo y patrones.

---

## 4. Modelos TypeScript principales (`shared/models/etl.models.ts`)

- `EtlStatusResponse`, `DatasetRow`, `AnalysisResponse`, `RankingActivo`, `SimilarityResult`, `SeriesResponse`, `SyncState`, etc.

---

## 5. Cómo ejecutar en local

1. **Backend**: levantar Spring Boot en el puerto **8080** (por ejemplo desde el IDE o Maven).
2. **Frontend**: en `FRONT-END/`, `npm install` (si hace falta) y `npm start` / `ng serve`.
3. Abrir la URL que indique el CLI (habitualmente `http://localhost:4200`).
4. Pulsar **Ejecutar ETL** y esperar a que termine (el ETL incluye pausas entre descargas).
5. Los datos se actualizarán solos **cada minuto** mientras la app esté abierta y el servidor siga en `LISTO`.

---

## 6. Ajustes frecuentes

- **Cambiar el intervalo automático**: `autoRefreshIntervalMs` en `src/environments/environment*.ts`.
- **Activar ETL por cron**: en `BACK-END` → `application.properties` → `etl.scheduled.enabled=true`.
- **Producción sin proxy**: usar build de producción y `apiUrl` apuntando al host real del API.

---

## 7. Relación con el documento académic

El archivo `Contexto-proyecto.sty` describe requisitos del curso (ETL, algoritmos, documentación). Esta implementación **visualiza y opera** los resultados del backend sin sustituir la lógica algorítmica del ETL ni de similitud/análisis, que permanece en Java.

---

*Documento generado para dejar registro del alcance técnico implementado en el repositorio.*
