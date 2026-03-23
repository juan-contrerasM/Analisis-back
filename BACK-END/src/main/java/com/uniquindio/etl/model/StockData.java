package com.uniquindio.etl.model;
import lombok.*;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StockData {

    private Long id;
    private String symbol;
    private LocalDate date;
    private double open;
    private double close;
    private double high;
    private double low;
    private long volume;

}