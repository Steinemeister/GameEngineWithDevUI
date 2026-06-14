package com.engine.core.renderer;

import com.engine.core.core.ThreadManager;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioThread implements Runnable {
    private long audioDevice;
    private long audioContext;

    // Faden-sichere Queue für Audio-Befehle (Main-Thread -> Audio-Thread)
    private final Queue<Runnable> audioQueue = new ConcurrentLinkedQueue<>();

    /**
     * Schickt einen Ton-Befehl (z. B. Sound abspielen) an den Audio-Thread.
     */
    public void runOnAudioThread(Runnable command) {
        audioQueue.add(command);
    }

    @Override
    public void run() {
        // 1. OpenAL Soundkarte / Audiogerät initialisieren
        audioDevice = ALC10.alcOpenDevice((ByteBuffer) null); // Null lädt das Standard-Audiogerät des OS
        if (audioDevice == MemoryUtil.NULL) {
            throw new IllegalStateException("[Audio-Thread] Standard-Audiogerät konnte nicht geöffnet werden!");
        }

        // 2. Audio-Kontext erstellen
        int[] attributes = {0};
        audioContext = ALC10.alcCreateContext(audioDevice, attributes);
        if (audioContext == MemoryUtil.NULL) {
            ALC10.alcCloseDevice(audioDevice);
            throw new IllegalStateException("[Audio-Thread] OpenAL-Kontext konnte nicht erstellt werden!");
        }

        // 3. Kontext an diesen Thread binden
        ALC10.alcMakeContextCurrent(audioContext);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        AL.createCapabilities(alcCapabilities);

        System.out.println("[Audio-Thread] OpenAL erfolgreich initialisiert und Sound-Loop gestartet.");

        // 4. Der Audio-Loop
        while (ThreadManager.getInstance().isEngineRunning()) {
            // Abarbeiten der reingekommenen Audio-Befehle
            while (!audioQueue.isEmpty()) {
                Runnable command = audioQueue.poll();
                if (command != null) {
                    try {
                        command.run();
                    } catch (Exception e) {
                        System.err.println("[Audio-Thread] Fehler bei Audiobefehl-Ausführung: " + e.getMessage());
                    }
                }
            }

            // Der Thread muss nicht mit 1000 FPS rasen.
            // Ein kurzes Schlafen spart enorm CPU-Ressourcen, ohne dass der Sound verzögert.
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 5. Aufräumen und Ressourcen freigeben bei Engine-Stopp
        System.out.println("[Audio-Thread] OpenAL-Kontext wird zerstört...");
        ALC10.alcMakeContextCurrent(MemoryUtil.NULL);
        ALC10.alcDestroyContext(audioContext);
        ALC10.alcCloseDevice(audioDevice);
        System.out.println("[Audio-Thread] Audio-System sauber heruntergefahren.");
    }
}
