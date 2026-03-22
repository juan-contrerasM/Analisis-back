package com.uniquindio.etl.scheduler;

import com.uniquindio.etl.service.ETLService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "etl.scheduled.enabled", havingValue = "true")
public class ETLScheduler {

    private final ETLService service;

    public ETLScheduler(ETLService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void runDaily() {
        service.runETL();
    }
}