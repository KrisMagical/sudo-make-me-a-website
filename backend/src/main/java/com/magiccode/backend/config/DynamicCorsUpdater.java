package com.magiccode.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class DynamicCorsUpdater {
    private final ApplicationContext context;
    private Path frontendPortFile = Paths.get(".frontend-port");
    private long lastModified = 0L;

    @Scheduled(fixedDelay = 3000)
    public void refreshCorsIfNeeded() {
        if (!Files.exists(frontendPortFile)) return;
        try {
            long currentModified = Files.getLastModifiedTime(frontendPortFile).toMillis();
            if (currentModified > lastModified) {
                lastModified = currentModified;
                context.getBean(CorsConfigurationSource.class);
                System.out.println("The frontend port file has been updated; the CORS configuration will take effect on the next request.");
            }
        } catch (IOException e) {
        }
    }
}
