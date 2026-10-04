package com.nakero.emeraldbuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BuilderConfig {

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_PATH =
            Path.of("config", "emerald-builder.json");

    // Área X/Z
    public int minX = 0;
    public int maxX = 299;

    public int minZ = 0;
    public int maxZ = 299;

    // Alturas configurables
    public int minY = -64;
    public int maxY = 319;

    public static BuilderConfig load() {

        try {
            if (Files.exists(CONFIG_PATH)) {

                String json =
                        Files.readString(CONFIG_PATH);

                BuilderConfig config =
                        GSON.fromJson(
                                json,
                                BuilderConfig.class
                        );

                if (config != null) {
                    config.normalize();
                    return config;
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "[EmeraldBuilder] Error cargando configuración:"
            );
            e.printStackTrace();
        }

        BuilderConfig config =
                new BuilderConfig();

        config.normalize();
        config.save();

        return config;
    }

    public void save() {

        normalize();

        try {
            Files.createDirectories(
                    CONFIG_PATH.getParent()
            );

            Files.writeString(
                    CONFIG_PATH,
                    GSON.toJson(this)
            );

        } catch (IOException e) {

            System.err.println(
                    "[EmeraldBuilder] Error guardando configuración:"
            );

            e.printStackTrace();
        }
    }

    public void normalize() {

        // Ordenar X
        if (minX > maxX) {
            int temp = minX;
            minX = maxX;
            maxX = temp;
        }

        // Ordenar Z
        if (minZ > maxZ) {
            int temp = minZ;
            minZ = maxZ;
            maxZ = temp;
        }

        // Ordenar Y
        if (minY > maxY) {
            int temp = minY;
            minY = maxY;
            maxY = temp;
        }

        /*
         * Límites normales de construcción
         * del Overworld en Minecraft 1.20.4.
         */
        minY = Math.max(-64, minY);
        maxY = Math.min(319, maxY);

        /*
         * Protección contra áreas introducidas
         * accidentalmente demasiado grandes.
         *
         * 1000 x 1000 sigue permitiendo áreas
         * muy superiores a los ~300 x 300 previstos.
         */
        if ((long) maxX - minX > 999) {
            maxX = minX + 999;
        }

        if ((long) maxZ - minZ > 999) {
            maxZ = minZ + 999;
        }
    }

    public int width() {
        return maxX - minX + 1;
    }

    public int depth() {
        return maxZ - minZ + 1;
    }

    public long totalColumns() {
        return (long) width() * depth();
    }
}
