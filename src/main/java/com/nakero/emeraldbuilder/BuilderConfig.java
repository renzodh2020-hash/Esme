package com.nakero.emeraldbuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BuilderConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "emerald-builder.json");

    public int minX = 0, maxX = 299;
    public int minZ = 0, maxZ = 299;
    public int minY = -64, maxY = 319;
    public String startDirection = "EAST";

    public static BuilderConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                BuilderConfig c = GSON.fromJson(Files.readString(CONFIG_PATH), BuilderConfig.class);
                if (c != null) { c.normalize(); return c; }
            }
        } catch (Exception e) { e.printStackTrace(); }
        BuilderConfig c = new BuilderConfig();
        c.save();
        return c;
    }

    public void save() {
        normalize();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void normalize() {
        if (minX > maxX) { int t=minX; minX=maxX; maxX=t; }
        if (minZ > maxZ) { int t=minZ; minZ=maxZ; maxZ=t; }
        if (minY > maxY) { int t=minY; minY=maxY; maxY=t; }
        minY = Math.max(-64, minY);
        maxY = Math.min(319, maxY);
        if ((long)maxX-minX > 999) maxX=minX+999;
        if ((long)maxZ-minZ > 999) maxZ=minZ+999;
        if (startDirection == null) startDirection="EAST";
        startDirection=startDirection.toUpperCase();
        if (!startDirection.equals("NORTH") && !startDirection.equals("EAST")
                && !startDirection.equals("SOUTH") && !startDirection.equals("WEST"))
            startDirection="EAST";
    }

    public int width(){ return maxX-minX+1; }
    public int depth(){ return maxZ-minZ+1; }
    public long totalColumns(){ return (long)width()*depth(); }
}
