package com.uniquindio.etl.etl.loader;

import com.uniquindio.etl.model.StockData;

import java.io.FileWriter;
import java.util.List;

public class DatasetWriter {

    public void write(List<StockData> data) {

        try (FileWriter writer = new FileWriter("dataset.csv")) {

            writer.write("Date,Symbol,Open,Close,High,Low,Volume\n");

            for (StockData d : data) {
                writer.write(String.format("%s,%s,%f,%f,%f,%f,%d\n",
                        d.getDate(), d.getSymbol(),
                        d.getOpen(), d.getClose(),
                        d.getHigh(), d.getLow(),
                        d.getVolume()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}