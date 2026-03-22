package com.uniquindio.etl.controller;

import com.uniquindio.etl.service.impl.ETLServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/etl")
public class ETLController {

    @Autowired
    private ETLServiceImpl etlService;

    // REQUERIMIENTO 1
    @GetMapping("/run")
    public String runETL() {
        etlService.runETL();
        return "ETL ejecutado correctamente";
    }

    @GetMapping("/similarity")
    public Map<String, Object> similitud(
            @RequestParam String asset1,
            @RequestParam String asset2) {

        return etlService.calcularSimilitud(asset1, asset2);
    }

    @GetMapping("/series")
    public Map<String, List<Double>> getSeries(
            @RequestParam String asset1,
            @RequestParam String asset2) {

        return etlService.obtenerSeries(asset1, asset2);
    }

    // REQUERIMIENTO 3
    @GetMapping("/analysis")
    public Map<String, Object> analizar() {
        return etlService.obtenerAnalisis();
    }
}