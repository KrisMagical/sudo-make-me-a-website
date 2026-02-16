package com.magiccode.backend.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class PortAutoAdjuster implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final int PORT_SCAN_MAX_ATTEMPTS = 100;
    private static final Path BACKEND_PORT_FILE = Paths.get(".backend-port");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        boolean enabled = environment.getProperty("app.port.auto-adjust-enabled", Boolean.class, true);
        if (!enabled) return;

        String portStr = environment.getProperty("server.port");
        int originalPort = portStr != null ? Integer.parseInt(portStr) : 8080;
        if (originalPort == 0) return;

        int availablePort = findAvailablePort(originalPort);
        if (availablePort != originalPort) {
            Properties props = new Properties();
            props.setProperty("server.port", String.valueOf(availablePort));
            environment.getPropertySources().addFirst(
                    new PropertiesPropertySource("adjustedPort", props)
            );
            System.out.printf("Port %d is already in use, automatically switching to port %d%n", originalPort, availablePort);
        }

        writeBackendPort(availablePort);
    }

    private void writeBackendPort(int port) {
        try {
            Files.writeString(BACKEND_PORT_FILE, "http://localhost:" + port);
        } catch (IOException e) {
            System.err.println("Unable to write to the backend port file: " + e.getMessage());
        }
    }

    private int findAvailablePort(int startPort) {
        for (int port = startPort; port < startPort + PORT_SCAN_MAX_ATTEMPTS; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        throw new RuntimeException(
                String.format("Unable to find a usable port, ranges tried %d ~ %d", startPort, startPort + PORT_SCAN_MAX_ATTEMPTS - 1)
        );
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
