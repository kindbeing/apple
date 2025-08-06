package com.ncode.microapple.datalake.listener;

import com.ncode.microapple.datalake.service.IcebergService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CdcEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CdcEventListener.class);

    @Autowired
    private IcebergService icebergService;

    @KafkaListener(topics = "microapple-postgres.public.app_purchases", groupId = "iceberg-sink")
    public void handleCdcEvent(String message) {
        logger.info("Received CDC event for Iceberg processing: {}", message);
        
        try {
            icebergService.processCdcEvent(message);
            logger.debug("Successfully processed CDC event for Iceberg");
        } catch (Exception e) {
            logger.error("Failed to process CDC event for Iceberg: {}", message, e);
            // In production, you might want to send to a dead letter queue
        }
    }
}