package com.uniquindio.etl.service;

import com.uniquindio.etl.model.StockData;

import java.util.List;
import java.util.Map;

public interface ETLService {

    void runETL();

    Map<String, Object> getEtlStatus();

    List<String> getSymbols();

    List<Map<String, Object>> getDatasetRows();

    Map<String, Object> obtenerAnalisis();

    Map<String, Object> calcularSimilitud(String asset1, String asset2);

    Map<String, List<Double>> obtenerSeries(String asset1, String asset2);

    boolean isEtlReady();

    List<StockData> retrieveVolumeAsc();
}
