package com.uniquindio.etl.etl.transformer;

import com.uniquindio.etl.model.StockData;

import java.util.*;

public class DataCleaner {

    public List<StockData> clean(List<StockData> data) {

        List<StockData> clean = new ArrayList<>();

        //Eliminación de registros inválidos:
        //Precios negativos
        //Volumen negativo
        for (StockData d : data) {

            if (isValidStockData(d)) {
                clean.add(d);
            }
        }

        //Garantiza que la serie esté en orden cronológico
        clean.sort(Comparator.comparing(StockData::getDate));

        return clean;
    }

    private boolean isValidStockData(StockData d) {

        if (d.getClose() <= 0) {
            return false; // El precio de cierre no puede ser negativo o cero, ya que un activo siempre tiene valor positivo en el mercado
        }

        if (d.getOpen() <= 0) {
            return false; // El precio de apertura debe ser positivo, valores negativos indicarían un error en la fuente de datos
        }

        if (d.getHigh() <= 0) {
            return false; // El precio máximo del día no puede ser negativo, no tiene sentido financiero
        }

        if (d.getLow() <= 0) {
            return false; // El precio mínimo del día debe ser positivo, valores inválidos afectan cualquier análisis posterior
        }

        if (d.getVolume() < 0) {
            return false; // El volumen no puede ser negativo, ya que representa cantidad de transacciones
        }

        if (d.getHigh() < d.getLow()) {
            return false; // El precio más alto no puede ser menor que el más bajo, sería una inconsistencia en la serie
        }

        if (d.getOpen() > d.getHigh()) {
            return false; // El precio de apertura no puede superar el máximo del día, sería un dato incoherente
        }

        if (d.getOpen() < d.getLow()) {
            return false; // El precio de apertura no puede estar por debajo del mínimo del día
        }

        if (d.getClose() > d.getHigh()) {
            return false; // El precio de cierre no puede superar el máximo registrado en el día
        }

        if (d.getClose() < d.getLow()) {
            return false; // El precio de cierre no puede estar por debajo del mínimo del día
        }

        return true; // El registro cumple todas las reglas de consistencia financiera
    }
}