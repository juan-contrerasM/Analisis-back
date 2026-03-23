package com.uniquindio.etl.etl.transformer;

import com.uniquindio.etl.model.StockData;

import java.time.temporal.ChronoUnit;
import java.util.*;

public class DataAligner {

    public List<StockData> forwardFill(List<StockData> data) {

        List<StockData> result = new ArrayList<>();
        StockData prev = null;

        for (StockData curr : data) {

            if (prev != null) {

                long diff = ChronoUnit.DAYS.between(prev.getDate(), curr.getDate());

                // Si hay diferencia mayor a 1 día, existen fechas faltantes en la serie
                for (int i = 1; i < diff; i++) {

                    StockData fill = new StockData();

                    fill.setSymbol(prev.getSymbol());
                    fill.setDate(prev.getDate().plusDays(i));
                    fill.setOpen(prev.getOpen());
                    fill.setClose(prev.getClose());
                    fill.setHigh(prev.getHigh());
                    fill.setLow(prev.getLow());
                    fill.setVolume(prev.getVolume());

                    result.add(fill);
                }
            }

            result.add(curr);
            prev = curr;
        }

        return result;
    }
}