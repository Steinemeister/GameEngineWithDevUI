package com.engine.core.core;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadManager {
    private static ThreadManager instance;

    // Threads
    private Thread renderThread;
    private Thread audioThread;

    // Status-Flags (Faden-sicher via AtomicBoolean)
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // Eine faden-sichere Queue für Grafik-Befehle (Main-Thread -> Render-Thread)
    private final Queue<Runnable> renderQueue = new ConcurrentLinkedQueue<>();

    private ThreadManager() {}

    public static ThreadManager getInstance() {
        if (instance == null) {
            instance = new ThreadManager();
        }
        return instance;
    }

    /**
     * Startet die sekundären Threads für Rendering und Audio.
     * @param renderTarget Die Logikschleife, die der Render-Thread ausführen soll.
     * @param audioTarget Die Logikschleife, die der Audio-Thread ausführen soll.
     */
    public void startThreads(Runnable renderTarget, Runnable audioTarget) {
        if (isRunning.get()) return;
        isRunning.set(true);

        // 1. Render-Thread initialisieren & starten
        renderThread = new Thread(renderTarget, "Render-Thread");
        // Wichtig: Priority hochschrauben für flüssige Bildraten
        renderThread.setPriority(Thread.MAX_PRIORITY);
        renderThread.start();

        // 2. Audio-Thread initialisieren & starten
        audioThread = new Thread(audioTarget, "Audio-Thread");
        audioThread.start();

        System.out.println("[ThreadManager] Render- und Audio-Threads erfolgreich gestartet.");
    }

    /**
     * Stoppt alle Threads sauber.
     */
    public void stopThreads() {
        isRunning.set(false);
        System.out.println("[ThreadManager] Threads werden heruntergefahren...");

        try {
            if (renderThread != null) renderThread.join(2000);
            if (audioThread != null) audioThread.join(2000);
        } catch (InterruptedException e) {
            System.err.println("[ThreadManager] Fehler beim Beenden der Threads: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Schickt einen Befehl zur Ausführung an den Render-Thread.
     * Nutze dies, wenn du aus der Spiellogik heraus Texturen oder Shader laden willst!
     */
    public void runOnRenderThread(Runnable command) {
        renderQueue.add(command);
    }

    /**
     * Arbeitet alle ausstehenden Grafikbefehle ab.
     * WIRD EXKLUSIV VOM RENDER-THREAD AUFGERUFEN!
     */
    public void executeRenderQueue() {
        while (!renderQueue.isEmpty()) {
            Runnable command = renderQueue.poll();
            if (command != null) {
                try {
                    command.run();
                } catch (Exception e) {
                    System.err.println("[ThreadManager] Fehler bei Grafikbefehl-Ausführung: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    // Getter für den Status
    public boolean isEngineRunning() {
        return isRunning.get();
    }
}
