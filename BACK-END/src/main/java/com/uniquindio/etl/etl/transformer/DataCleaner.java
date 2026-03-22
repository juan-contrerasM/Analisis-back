package com.uniquindio.etl.etl.transformer;

import com.uniquindio.etl.model.StockData;

import java.util.*;

public class DataCleaner {

    public List<StockData> clean(List<StockData> data) {

        List<StockData> clean = new ArrayList<>();

        for (StockData d : data) {

            if (d.getClose() > 0 &&
                    d.getOpen() > 0 &&
                    d.getHigh() > 0 &&
                    d.getLow() > 0 &&
                    d.getVolume() >= 0) {

                clean.add(d);
            }
        }

        clean.sort(Comparator.comparing(StockData::getDate));

        return clean;
    }
}