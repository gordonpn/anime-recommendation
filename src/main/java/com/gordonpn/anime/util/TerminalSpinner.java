package com.gordonpn.anime.util;

import org.jline.terminal.Terminal;

import java.io.PrintWriter;

public class TerminalSpinner implements AutoCloseable {
    private static final String[] FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private final Terminal terminal;
    private final String message;
    private final Thread thread;
    private volatile boolean running = true;

    public TerminalSpinner(Terminal terminal, String message) {
        this.terminal = terminal;
        this.message = message;
        if (terminal != null && terminal.writer() != null) {
            this.thread = new Thread(this::runSpinner);
            this.thread.setDaemon(true);
            this.thread.setName("terminal-spinner");
            this.thread.start();
        } else {
            this.thread = null;
        }
    }

    private void runSpinner() {
        PrintWriter writer = terminal.writer();
        int frameIdx = 0;
        try {
            while (running) {
                writer.print("\r\033[K" + FRAMES[frameIdx] + " " + message);
                writer.flush();
                frameIdx = (frameIdx + 1) % FRAMES.length;
                Thread.sleep(80);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (terminal != null && terminal.writer() != null) {
            PrintWriter writer = terminal.writer();
            writer.print("\r\033[K");
            writer.flush();
        }
    }
}
