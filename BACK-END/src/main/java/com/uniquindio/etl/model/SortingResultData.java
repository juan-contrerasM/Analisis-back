package com.uniquindio.etl.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SortingResultData {

    private String algorithm;
    private int size;
    private long time;

}
