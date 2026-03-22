package com.uniquindio.etl.etl.extractor;

import com.uniquindio.etl.model.StockData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class YahooFinanceExtractor {

    public List<StockData> extract(String symbol) {

        List<StockData> dataList = new ArrayList<>();

        try {

            //URL
            String urlStr = "https://stooq.com/q/d/l/?s=" + mapSymbol(symbol) + "&i=d";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //USER-AGENT
            conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            );

            conn.setRequestMethod("GET");
            conn.connect();

            int responseCode = conn.getResponseCode();

            if (responseCode != 200) {
                throw new RuntimeException("Error HTTP: " + responseCode);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {

                if (header) {
                    header = false;
                    continue;
                }

                String[] p = line.split(",");

                // Validación
                if (p.length < 6 || p[1].equals("null")) continue;

                StockData d = new StockData();
                d.setSymbol(symbol);
                d.setDate(LocalDate.parse(p[0]));
                d.setOpen(Double.parseDouble(p[1]));
                d.setHigh(Double.parseDouble(p[2]));
                d.setLow(Double.parseDouble(p[3]));
                d.setClose(Double.parseDouble(p[4]));
                try {
                    d.setVolume((long) Double.parseDouble(p[5]));
                } catch (Exception e) {
                    d.setVolume(0);
                }
                //SE UTILIZA EL TRY  YA QUE ALGUNOS TDATOS ESTAN COMO DECIMAL Y OTROS COMO ENTEROS

                dataList.add(d);
            }

            reader.close();
            conn.disconnect();

        } catch (Exception e) {
            System.out.println("Error con " + symbol + ": " + e.getMessage());
        }

        //ORDENAR POR FECHA
        dataList.sort(Comparator.comparing(StockData::getDate));

        return dataList;
    }

    //MAPEO DE SÍMBOLOS
    private String mapSymbol(String symbol) {

        switch (symbol) {
            case "EC": return "ec.us";
            default: return symbol.toLowerCase() + ".us";
        }
    }
}