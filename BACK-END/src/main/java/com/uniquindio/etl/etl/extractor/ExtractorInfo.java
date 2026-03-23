package com.uniquindio.etl.etl.extractor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.uniquindio.etl.model.StockData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class ExtractorInfo {

    public List<StockData> extract(String symbol) {
        List<StockData> dataList = new ArrayList<>();

        // Timestamps
        long period2 = Instant.now().getEpochSecond();
        long period1 = ZonedDateTime.now()
                .minusYears(5)
                .toInstant()
                .getEpochSecond();

        try {
            // Usamos el endpoint de "download" que sí devuelve un CSV
            String urlStr = String.format(
                    "https://query1.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=1d",
                    symbol, period1, period2
            );

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int status = conn.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("HTTP error: " + status);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);

            }

            reader.close();
            conn.disconnect();
            dataList = getStockData(response.toString(), symbol);


        } catch (Exception e) {
            System.err.println("Error crítico con " + symbol + ": " + e.getMessage());
        }

        return dataList;
    }

    private String mapSymbol(String symbol) {
        return symbol.toLowerCase() + ".us";
    }

    private List<StockData> getStockData(String response, String symbol) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        JsonNode result = root.path("chart").path("result").get(0);

        JsonNode timestamps = result.path("timestamp");
        JsonNode quote = result.path("indicators").path("quote").get(0);
        JsonNode opens = quote.path("open");
        JsonNode highs = quote.path("high");
        JsonNode lows = quote.path("low");
        JsonNode closes = quote.path("close");
        JsonNode volumes = quote.path("volume");

        List<StockData> dataList = new ArrayList<>();

        for (int i = 0; i < timestamps.size(); i++) {

            if (opens.get(i).isNull()) continue;

            StockData d = new StockData();

            d.setSymbol(symbol);

            d.setDate(
                    Instant.ofEpochSecond(timestamps.get(i).asLong())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
            );

            d.setOpen(opens.get(i).asDouble());
            d.setHigh(highs.get(i).asDouble());
            d.setLow(lows.get(i).asDouble());
            d.setClose(closes.get(i).asDouble());
            d.setVolume(volumes.get(i).asLong());

            dataList.add(d);
        }
        return dataList;
    }
}