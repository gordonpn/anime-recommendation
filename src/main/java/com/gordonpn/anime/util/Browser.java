package com.gordonpn.anime.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

@Slf4j
public final class Browser {
    private Browser() {
    }

    public static void launch(URI uri) throws IOException {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                log.info("Opened browser using desktop API.");
            } else {
                log.info("Desktop API is not supported. Using ProcessBuilder to open browser.");
                launchWithProcessBuilder(uri.toString());
            }
        } catch (IOException e) {
            throw new IOException("Failed to open browser", e);
        }
    }

    private static void launchWithProcessBuilder(String authorizationUrl) throws IOException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        ProcessBuilder processBuilder;

        if (os.contains("win")) {
            processBuilder = new ProcessBuilder("cmd", "/c", "start", authorizationUrl);
        } else if (os.contains("mac")) {
            processBuilder = new ProcessBuilder("open", authorizationUrl);
        } else if (os.contains("nix") || os.contains("nux")) {
            processBuilder = new ProcessBuilder("xdg-open", authorizationUrl);
        } else {
            throw new UnsupportedOperationException("Unsupported operating system");
        }

        processBuilder.start();
    }
}
