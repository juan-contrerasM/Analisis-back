package com.uniquindio.etl.etl.loader;

import com.uniquindio.etl.model.StockData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Locale;

@Service
public class DatasetWriter {

    @Value("${app.csv.path}")
    private String ruta;

    public void write(List<StockData> data) {

        try {
            File file = new File(ruta);

            // Crear carpetas si no existen
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {

                writer.write("Date,Symbol,Open,Close,High,Low,Volume\n");

                for (StockData d : data) {
                    writer.write(String.format(Locale.US,
                            "%s,%s,%.6f,%.6f,%.6f,%.6f,%d\n",
                            d.getDate(), d.getSymbol(),
                            d.getOpen(), d.getClose(),
                            d.getHigh(), d.getLow(),
                            d.getVolume()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}