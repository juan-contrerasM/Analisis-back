package com.uniquindio.etl.etl.extractor;

import com.uniquindio.etl.model.StockData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class ExtractorInfo {

    public List<StockData> extract(String symbol) {

        List<StockData> dataList = new ArrayList<>();

        try {

            String urlStr = "https://stooq.com/q/d/l/?s=" + mapSymbol(symbol) + "&i=d";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
            );

            conn.setRequestMethod("GET");
            conn.connect();

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Error HTTP: " + conn.getResponseCode());
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

                if (p.length < 6) continue;

                try {

                    double open = Double.parseDouble(p[1]);
                    double high = Double.parseDouble(p[2]);
                    double low = Double.parseDouble(p[3]);
                    double close = Double.parseDouble(p[4]);

                    // FILTRO CLAVE
                    if (open == 0 || high == 0 || low == 0 || close == 0) continue;

                    StockData d = new StockData();
                    d.setSymbol(symbol);
                    d.setDate(LocalDate.parse(p[0]));
                    d.setOpen(open);
                    d.setHigh(high);
                    d.setLow(low);
                    d.setClose(close);

                    try {
                        d.setVolume(Long.parseLong(p[5]));
                    } catch (Exception e) {
                        d.setVolume(0);
                    }

                    dataList.add(d);

                } catch (Exception e) {
                    // ignora filas corruptas
                }
            }

            reader.close();
            conn.disconnect();

        } catch (Exception e) {
            System.out.println("Error con " + symbol + ": " + e.getMessage());
        }

        dataList.sort(Comparator.comparing(StockData::getDate));

        return dataList;
    }

    private String mapSymbol(String symbol) {
        return symbol.toLowerCase() + ".us";
    }
}