package com.uniquindio.etl.service.impl;

import com.uniquindio.etl.etl.extractor.YahooFinanceExtractor;
import com.uniquindio.etl.etl.loader.DatasetWriter;
import com.uniquindio.etl.etl.transformer.DataAligner;
import com.uniquindio.etl.etl.transformer.DataCleaner;
import com.uniquindio.etl.model.StockData;
import com.uniquindio.etl.service.ETLService;
import com.uniquindio.etl.service.SimilarityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class ETLServiceImpl implements ETLService {

    @Autowired
    private SimilarityService similarityService;
    private List<Map<String, Object>> retornosGlobal;
    private Map<String, Object> resultadoReq3;

    @Override
    public void runETL() {

        YahooFinanceExtractor extractor = new YahooFinanceExtractor();
        DataCleaner cleaner = new DataCleaner();
        DataAligner aligner = new DataAligner();
        DatasetWriter writer = new DatasetWriter();

        List<String> symbols = List.of(
                "AAPL", "MSFT", "GOOG", "AMZN", "TSLA",
                "META", "NVDA", "JPM", "WMT", "DIS",
                "NFLX", "KO", "PEP", "INTC", "BAC",
                "CSCO", "ORCL", "IBM", "AMD", "ADBE"
        );

        Map<String, List<StockData>> allData = new HashMap<>();

        for (String symbol : symbols) {

            System.out.println("Extrayendo: " + symbol);

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
                e.printStackTrace();
            }
        }

        System.out.println("ETL finalizado");

        // UNIFICAR
        List<Map<String, Object>> dataset = unifyData(allData);

        // GUARDAR DATASET
        guardarDataset(dataset, symbols, writer);

        // RETORNOS
        List<Map<String, Object>> retornos = calcularRetornos(dataset);

        // IMPORTANTE
        this.retornosGlobal = retornos;

        // ANALIZAR
        this.resultadoReq3 = analizarRequerimiento3(retornos, symbols);

        System.out.println("ETL listo");
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

        Map<String, Map<LocalDate, Double>> dataMap = new HashMap<>();
        Set<LocalDate> todasFechas = new TreeSet<>();

        for (String symbol : allData.keySet()) {

            Map<LocalDate, Double> series = new HashMap<>();

            for (StockData d : allData.get(symbol)) {
                series.put(d.getDate(), d.getClose());
                todasFechas.add(d.getDate());
            }

            dataMap.put(symbol, series);
        }

        List<Map<String, Object>> dataset = new ArrayList<>();

        for (LocalDate fecha : todasFechas) {

            Map<String, Object> fila = new HashMap<>();
            fila.put("date", fecha);

            for (String symbol : dataMap.keySet()) {

                Double valor = dataMap.get(symbol).get(fecha);

                if (valor == null) {
                    valor = obtenerUltimoValor(dataMap.get(symbol), fecha);
                }

                fila.put(symbol, valor);
            }

            dataset.add(fila);
        }

        System.out.println("Dataset unificado: " + dataset.size());

        return dataset;
    }

    // FORWARD FILL GLOBAL
    private Double obtenerUltimoValor(Map<LocalDate, Double> serie, LocalDate fecha) {

        LocalDate temp = fecha.minusDays(1);

        while (temp != null) {

            if (serie.containsKey(temp)) {
                return serie.get(temp);
            }

            temp = temp.minusDays(1);
        }

        return 0.0;
    }

    // GUARDAR DATASET
    private void guardarDataset(List<Map<String, Object>> dataset,
                                List<String> symbols,
                                DatasetWriter writer) {

        List<StockData> flatData = new ArrayList<>();

        for (Map<String, Object> fila : dataset) {

            LocalDate fecha = (LocalDate) fila.get("date");

            for (String symbol : symbols) {

                StockData d = new StockData();
                d.setDate(fecha);
                d.setSymbol(symbol);
                d.setClose((Double) fila.get(symbol));

                flatData.add(d);
            }
        }

        writer.write(flatData);
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

                if (key.equals("date")) continue;

                double precioHoy = (double) hoy.get(key);
                double precioAyer = (double) ayer.get(key);

                double retorno = (precioHoy - precioAyer) / precioAyer;

                fila.put(key, retorno);
            }

            retornos.add(fila);
        }

        return retornos;
    }

    // SIMILITUD
    public Map<String, Object> calcularSimilitud(String asset1, String asset2) {

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

        // INTERPRETACIÓN
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
