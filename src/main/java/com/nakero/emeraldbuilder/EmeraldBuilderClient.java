package com.nakero.emeraldbuilder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class EmeraldBuilderClient implements ClientModInitializer {

    private KeyBinding toggleKey;
    private KeyBinding menuKey;

    private BuilderConfig cfg;

    private boolean running = false;
    private State state = State.IDLE;

    private final List<Cell> path = new ArrayList<>();

    private int pathIndex = 0;
    private int ticks = 0;

    private double beforeHomeX;
    private double beforeHomeY;
    private double beforeHomeZ;

    private enum State {
        IDLE,
        BUILDING,
        MOVE_OFF_TOP,
        FALLING,
        HOME_WAIT,
        OPEN_CHEST,
        LOOTING,
        BACK_WAIT
    }

    private record Cell(int x, int z) {}

    @Override
    public void onInitializeClient() {

        cfg = BuilderConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.emerald_builder.toggle",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_O,
                        "category.emerald_builder"
                )
        );

        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.emerald_builder.menu",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_P,
                        "category.emerald_builder"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {

        if (menuKey.wasPressed()) {
            mc.setScreen(new BuilderConfigScreen(cfg));
        }

        if (toggleKey.wasPressed()) {

            if (running) {
                stop(mc, "§cConstructor detenido");
            } else {
                start(mc);
            }
        }

        if (!running ||
                mc.player == null ||
                mc.world == null ||
                mc.currentScreen instanceof BuilderConfigScreen) {
            return;
        }

        ticks++;

        switch (state) {

            case BUILDING -> build(mc);

            case MOVE_OFF_TOP -> moveTop(mc);

            case FALLING -> falling(mc);

            case HOME_WAIT -> homeWait(mc);

            case OPEN_CHEST -> openChest(mc);

            case LOOTING -> loot(mc);

            case BACK_WAIT -> backWait(mc);

            default -> {
            }
        }
    }

    private void start(MinecraftClient mc) {

        if (mc.player == null) {
            return;
        }

        cfg.normalize();
        cfg.save();

        buildSpiral();

        if (path.isEmpty()) {
            msg(mc, "§cÁrea inválida");
            return;
        }

        pathIndex = nearestPathIndex(mc);

        running = true;
