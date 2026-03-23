package com.uniquindio.etl.scheduler;

import com.uniquindio.etl.service.ETLService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ejecuta el ETL de forma periódica en el hilo del task scheduler de Spring (no bloquea peticiones HTTP).
 * <p>
 * {@code fixedDelay}: el siguiente ciclo empieza tras el intervalo <em>después de terminar</em> el ETL
 * anterior, evitando solapes.
 */
@Component
public class ETLScheduler {

    private final ETLService service;

    public ETLScheduler(ETLService service) {
        this.service = service;
    }

    @Scheduled(
            initialDelayString = "${etl.scheduled.initial-delay-ms:120000}",
            fixedDelayString = "${etl.scheduled.fixed-delay-ms:3600000}"
    )
    public void runEtlInBackground() {
        System.out.println("[ETL programado] Inicio en segundo plano");
        try {
            service.runETL();
            System.out.println("[ETL programado] Finalizado correctamente");
        } catch (Exception e) {
            System.err.println("[ETL programado] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
