package com.uniquindio.etl.service.impl;

import com.uniquindio.etl.etl.extractor.ExtractorInfo;
import com.uniquindio.etl.etl.loader.DatasetWriter;
import com.uniquindio.etl.etl.transformer.DataAligner;
import com.uniquindio.etl.etl.transformer.DataCleaner;
import com.uniquindio.etl.model.EtlEstado;
import com.uniquindio.etl.model.StockData;
import com.uniquindio.etl.service.ETLService;
import com.uniquindio.etl.service.SimilarityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ETLServiceImpl implements ETLService {

    @Value("${app.csv.path}")
    private String ruta;
    private final DatasetWriter datasetWriter;

    private static final List<String> PORTFOLIO_SYMBOLS = List.of(
            "AAPL"
    );
    
    private final SimilarityService similarityService;

    private List<Map<String, Object>> retornosGlobal;
    private Map<String, Object> resultadoReq3;
    private List<Map<String, Object>> datasetGlobal;

    private volatile boolean etlEjecutado;
    private volatile Instant ultimaActualizacion;
    private volatile long dataVersion;
    private volatile EtlEstado estado = EtlEstado.IDLE;
    private volatile String mensajeError;

    @Override
    public synchronized void runETL() {
        estado = EtlEstado.EJECUTANDO;
        mensajeError = null;

        try {
            ExtractorInfo extractor = new ExtractorInfo();
            DataCleaner cleaner = new DataCleaner();
            DataAligner aligner = new DataAligner();


            Map<String, List<StockData>> allData = new HashMap<>();

            for (String symbol : PORTFOLIO_SYMBOLS) {

                log.info("Extrayendo: " + symbol);

                List<StockData> data = extractor.extract(symbol);

                // LIMPIAR
                data = cleaner.clean(data);

                // ALINEAR
                data = aligner.forwardFill(data);


                // FILTRAR últimos 5 años
                data = filtrarUltimos5Anios(data);

                allData.put(symbol, data);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("ETL interrumpido", e);
                }
            }

            log.info("ETL listo");

            // UNIFICAR
            List<Map<String, Object>> dataset = unifyData(allData);

            // GUARDAR DATASET
            guardarDataset(dataset, PORTFOLIO_SYMBOLS, datasetWriter);

            // RETORNOS
            List<Map<String, Object>> retornos = calcularRetornos(dataset);

            // IMPORTANTE
            this.retornosGlobal = retornos;

            // ANALIZAR
            this.resultadoReq3 = analizarRequerimiento3(retornos, PORTFOLIO_SYMBOLS);

            this.datasetGlobal = dataset;
            this.retornosGlobal = retornos;
            this.resultadoReq3 = analizarRequerimiento3(retornos, PORTFOLIO_SYMBOLS);

            this.etlEjecutado = true;
            this.ultimaActualizacion = Instant.now();
            this.dataVersion++;
            this.estado = EtlEstado.LISTO;

            log.info("ETL Finalizado");
        } catch (Exception e) {
            this.estado = EtlEstado.ERROR;
            this.mensajeError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getEtlStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("etlEjecutado", etlEjecutado);
        m.put("ultimaActualizacion", ultimaActualizacion != null ? ultimaActualizacion.toString() : null);
        m.put("estado", estado.name());
        m.put("dataVersion", dataVersion);
        if (mensajeError != null) {
            m.put("mensajeError", mensajeError);
        }
        return m;
    }

    @Override
    public List<String> getSymbols() {
        return PORTFOLIO_SYMBOLS;
    }

    @Override
    public List<Map<String, Object>> getDatasetRows() {
        if (datasetGlobal == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> out = new ArrayList<>(datasetGlobal.size());
        for (Map<String, Object> row : datasetGlobal) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if ("date".equals(e.getKey()) && e.getValue() instanceof LocalDate ld) {
                    copy.put("date", ld.toString());
                } else {
                    copy.put(e.getKey(), e.getValue());
                }
            }
            out.add(copy);
        }
        return out;
    }

    @Override
    public boolean isEtlReady() {
        return etlEjecutado
                && retornosGlobal != null
                && !retornosGlobal.isEmpty()
                && datasetGlobal != null
                && resultadoReq3 != null;
    }

    @Override
    public List<StockData> retrieveVolumeAsc() {
        log.info("TOP 15 POR VOLUMEN:");

        List<StockData> data = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {

            String linea;
            boolean primeraLinea = true;

            while ((linea = br.readLine()) != null) {

                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }

                String[] columnas = linea.split(",");

                StockData stock = new StockData();

                stock.setDate(LocalDate.parse(columnas[0]));
                stock.setSymbol(columnas[1]);
                stock.setOpen(Double.parseDouble(columnas[2]));
                stock.setClose(Double.parseDouble(columnas[3]));
                stock.setHigh(Double.parseDouble(columnas[4]));
                stock.setLow(Double.parseDouble(columnas[5]));
                stock.setVolume(Long.parseLong(columnas[6]));

                data.add(stock);
            }

        } catch (Exception e) {
            log.error("Error leyendo CSV: {}", e.getMessage());
        }

        return data.stream()
                .sorted((a, b) -> Long.compare(b.getVolume(), a.getVolume()))
                .limit(15)
                .toList();
    }

    private void requireData() {
        if (!isEtlReady()) {
            throw new IllegalStateException("ETL no ejecutado o datos no disponibles");
        }
    }

    // FILTRAR 5 AÑOS
    private List<StockData> filtrarUltimos5Anios(List<StockData> data) {

        LocalDate limite = LocalDate.now().minusYears(5);

        List<StockData> filtrado = new ArrayList<>();

        for (StockData d : data) {
            if (!d.getDate().isBefore(limite)) {
                filtrado.add(d);
            }
        }

        return filtrado;
    }

    // UNIFICACIÓN CORRECTA
    private List<Map<String, Object>> unifyData(Map<String, List<StockData>> allData) {

        Map<String, Map<LocalDate, StockData>> dataMap = new HashMap<>();
        Set<LocalDate> todasFechas = new TreeSet<>();

        for (String symbol : allData.keySet()) {

            Map<LocalDate, StockData> series = new HashMap<>();

            for (StockData d : allData.get(symbol)) {
                series.put(d.getDate(), d);
                todasFechas.add(d.getDate());
            }

            dataMap.put(symbol, series);
        }

        List<Map<String, Object>> dataset = new ArrayList<>();

        for (LocalDate fecha : todasFechas) {

            Map<String, Object> fila = new HashMap<>();
            fila.put("date", fecha);

            for (String symbol : dataMap.keySet()) {

                StockData data = dataMap.get(symbol).get(fecha);

                if (data == null) {
                    data = obtenerUltimoValor(dataMap.get(symbol), fecha);
                }

                if (data != null) {
                    fila.put(symbol + "_open", data.getOpen());
                    fila.put(symbol + "_close", data.getClose());
                    fila.put(symbol + "_high", data.getHigh());
                    fila.put(symbol + "_low", data.getLow());
                    fila.put(symbol + "_volume", data.getVolume());

                } else {
                    fila.put(symbol + "_open", null);
                    fila.put(symbol + "_close", null);
                    fila.put(symbol + "_high", null);
                    fila.put(symbol + "_low", null);
                    fila.put(symbol + "_volume", null);
                }
            }

            dataset.add(fila);
        }

        return dataset;
    }

    private StockData obtenerUltimoValor(Map<LocalDate, StockData> serie, LocalDate fecha) {

        LocalDate temp = fecha.minusDays(1);

        while (temp != null) {

            if (serie.containsKey(temp)) {
                return serie.get(temp);
            }

            temp = temp.minusDays(1);
        }

        return null;
    }


    // GUARDAR DATASET
    private void guardarDataset(List<Map<String, Object>> dataset,
                                List<String> symbols,
                                DatasetWriter writer) {

        List<StockData> flatData = new ArrayList<>();

        for (Map<String, Object> fila : dataset) {

            LocalDate fecha = (LocalDate) fila.get("date");

            for (String symbol : symbols) {

                Object openObj = fila.get(symbol + "_open");
                Object highObj = fila.get(symbol + "_high");
                Object lowObj = fila.get(symbol + "_low");
                Object closeObj = fila.get(symbol + "_close");
                Object volumeObj = fila.get(symbol + "_volume");

                // VALIDAR
                if (openObj == null || highObj == null || lowObj == null || closeObj == null) continue;

                StockData d = new StockData();
                d.setDate(fecha);
                d.setSymbol(symbol);

                d.setOpen(((Number) openObj).doubleValue());
                d.setHigh(((Number) highObj).doubleValue());
                d.setLow(((Number) lowObj).doubleValue());
                d.setClose(((Number) closeObj).doubleValue());
                d.setVolume(((Number) volumeObj).longValue());

                flatData.add(d);
            }
        }

        flatData.sort(
                Comparator.comparing(StockData::getDate)
                        .thenComparing(StockData::getClose)
        );

        writer.write(flatData);

//        // TOP 15
//        List<StockData> top15 = flatData.stream()
//                .sorted((a, b) -> Long.compare(b.getVolume(), a.getVolume()))
//                .limit(15)
//                .toList();
//
//        top15.forEach(d -> log.info(d.getDate() + " " + d.getSymbol() + " " + d.getVolume())
//        );
    }

    // RETORNOS
    private List<Map<String, Object>> calcularRetornos(List<Map<String, Object>> dataset) {

        List<Map<String, Object>> retornos = new ArrayList<>();

        for (int i = 1; i < dataset.size(); i++) {

            Map<String, Object> hoy = dataset.get(i);
            Map<String, Object> ayer = dataset.get(i - 1);

            Map<String, Object> fila = new HashMap<>();
            fila.put("date", hoy.get("date"));

            for (String key : hoy.keySet()) {

                if (!key.endsWith("_close")) continue;

                Object vHoy = hoy.get(key);
                Object vAyer = ayer.get(key);

                if (vHoy == null || vAyer == null) continue;

                double precioHoy = ((Number) vHoy).doubleValue();
                double precioAyer = ((Number) vAyer).doubleValue();

                if (precioAyer == 0) continue;

                double retorno = (precioHoy - precioAyer) / precioAyer;

                String symbol = key.replace("_close", "");
                fila.put(symbol, retorno);
            }

            retornos.add(fila);
        }

        return retornos;
    }

    // SIMILITUD
    @Override
    public Map<String, Object> calcularSimilitud(String asset1, String asset2) {
        requireData();
        List<Double> serie1 = new ArrayList<>();
        List<Double> serie2 = new ArrayList<>();

        for (Map<String, Object> fila : retornosGlobal) {
            serie1.add((Double) fila.get(asset1));
            serie2.add((Double) fila.get(asset2));
        }

        Map<String, Object> resultado = new HashMap<>();

        double e = similarityService.euclidean(serie1, serie2);
        double p = similarityService.pearson(serie1, serie2);
        double c = similarityService.cosine(serie1, serie2);
        double d = similarityService.dtw(serie1, serie2);

        resultado.put("euclidiana", e);
        resultado.put("pearson", p);
        resultado.put("coseno", c);
        resultado.put("dtw", d);

        resultado.put("interpretacion", interpretar(p));

        return resultado;
    }

    private String interpretar(double pearson) {

        if (pearson > 0.8) return "Muy altamente correlacionados";
        if (pearson > 0.5) return "Moderadamente correlacionados";
        if (pearson > 0.2) return "Débil correlación";
        if (pearson > -0.2) return "Sin relación";
        if (pearson > -0.5) return "Correlación negativa débil";

        return "Fuertemente inversos";
    }

    @Override
    public Map<String, List<Double>> obtenerSeries(String asset1, String asset2) {

        List<Double> serie1 = new ArrayList<>();
        List<Double> serie2 = new ArrayList<>();

        for (Map<String, Object> fila : retornosGlobal) {
            serie1.add((Double) fila.get(asset1));
            serie2.add((Double) fila.get(asset2));
        }

        Map<String, List<Double>> resultado = new HashMap<>();
        resultado.put(asset1, serie1);
        resultado.put(asset2, serie2);

        return resultado;
    }

    public Map<String, Object> analizarRequerimiento3(
            List<Map<String, Object>> retornos,
            List<String> symbols) {

        Map<String, Object> resultado = new HashMap<>();

        List<Map<String, Object>> activos = new ArrayList<>();

        for (String symbol : symbols) {

            List<Double> serie = new ArrayList<>();

            for (Map<String, Object> fila : retornos) {
                serie.add((Double) fila.get(symbol));
            }

            double vol = volatilidad(serie);
            String riesgo = clasificarRiesgo(vol);

            Map<String, Integer> patrones = detectarPatrones(serie);

            Map<String, Object> activo = new HashMap<>();
            activo.put("activo", symbol);
            activo.put("volatilidad", vol);
            activo.put("riesgo", riesgo);
            activo.put("patrones", patrones);

            activos.add(activo);
        }

        activos.sort(Comparator.comparing(a -> (Double) a.get("volatilidad")));

        resultado.put("ranking", activos);

        return resultado;
    }

    private Map<String, Integer> detectarPatrones(List<Double> serie) {
        Map<String, Integer> patrones = new HashMap<>();

        int subida3 = 0;
        int bajada3 = 0;
        int altaVolatilidad = 0;

        for (int i = 0; i <= serie.size() - 3; i++) {

            double p1 = serie.get(i);
            double p2 = serie.get(i + 1);
            double p3 = serie.get(i + 2);

            if (p1 < p2 && p2 < p3) subida3++;
            if (p1 > p2 && p2 > p3) bajada3++;

            double max = Math.max(p1, Math.max(p2, p3));
            double min = Math.min(p1, Math.min(p2, p3));

            if ((max - min) / Math.abs(min) > 0.05) {
                altaVolatilidad++;
            }
        }

        patrones.put("subida3", subida3);
        patrones.put("bajada3", bajada3);
        patrones.put("altaVolatilidad", altaVolatilidad);

        return patrones;
    }

    public Map<String, Object> obtenerAnalisis() {
        return resultadoReq3;
    }

    private String clasificarRiesgo(double vol) {

        if (vol < 0.15) return "CONSERVADOR";
        if (vol < 0.30) return "MODERADO";
        return "AGRESIVO";
    }

    private double volatilidad(List<Double> retornos) {
        return desviacion(retornos) * Math.sqrt(252);
    }

    private double desviacion(List<Double> retornos) {

        double media = retornos.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        double suma = 0;

        for (double r : retornos) {
            suma += Math.pow(r - media, 2);
        }

        return Math.sqrt(suma / retornos.size());
    }
}
