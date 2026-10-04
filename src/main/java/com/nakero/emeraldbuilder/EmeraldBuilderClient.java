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
        IDLE, BUILDING, MOVE_OFF_TOP, FALLING,
        HOME_WAIT, OPEN_CHEST, LOOTING, BACK_WAIT
    }

    private record Cell(int x, int z) {}

    @Override
    public void onInitializeClient() {
        cfg = BuilderConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emerald_builder.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.emerald_builder"
        ));

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emerald_builder.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.emerald_builder"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {
        while (menuKey.wasPressed()) {
            mc.setScreen(new BuilderConfigScreen(cfg));
        }

        while (toggleKey.wasPressed()) {
            if (running) {
                stop(mc, "§cConstructor detenido");
            } else {
                start(mc);
            }
        }

        if (!running || mc.player == null || mc.world == null
                || mc.currentScreen instanceof BuilderConfigScreen) {
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
            default -> { }
        }
    }

    private void start(MinecraftClient mc) {
        if (mc.player == null) return;

        cfg.normalize();
        cfg.save();
        buildSpiral();

        if (path.isEmpty()) {
            msg(mc, "§cÁrea inválida");
            return;
        }

        pathIndex = nearestPathIndex(mc);
        running = true;
        state = State.BUILDING;
        ticks = 0;

        msg(mc, "§aConstructor ACTIVADO §7| columna "
                + (pathIndex + 1) + "/" + path.size());
    }

    private void stop(MinecraftClient mc, String message) {
        running = false;
        state = State.IDLE;
        release(mc);
        msg(mc, message);
    }

    private void release(MinecraftClient mc) {
        mc.options.forwardKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.useKey.setPressed(false);
    }

    private void msg(MinecraftClient mc, String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), true);
        }
    }

    private void build(MinecraftClient mc) {
        if (!ensureEmerald(mc)) {
            beginRefill(mc);
            return;
        }

        Cell cell = path.get(pathIndex);
        double dx = (cell.x() + 0.5) - mc.player.getX();
        double dz = (cell.z() + 0.5) - mc.player.getZ();

        if (Math.hypot(dx, dz) > 0.42) {
            face(mc, dx, dz);
            mc.player.setPitch(0);
            mc.options.forwardKey.setPressed(true);
            mc.options.jumpKey.setPressed(false);
            mc.options.useKey.setPressed(false);
            return;
        }

        mc.options.forwardKey.setPressed(false);

        if (mc.player.getY() >= cfg.maxY) {
            release(mc);
            state = State.MOVE_OFF_TOP;
            ticks = 0;
            return;
        }

        mc.player.getInventory().selectedSlot = 0;
        mc.player.setPitch(89.5f);
        mc.options.jumpKey.setPressed(true);
        mc.options.useKey.setPressed(true);
    }

    private void moveTop(MinecraftClient mc) {
        if (pathIndex + 1 >= path.size()) {
            stop(mc, "§aÁrea completada");
            return;
        }

        Cell next = path.get(pathIndex + 1);
        double dx = (next.x() + 0.5) - mc.player.getX();
        double dz = (next.z() + 0.5) - mc.player.getZ();

        face(mc, dx, dz);
        mc.player.setPitch(0);
        mc.options.forwardKey.setPressed(true);
        mc.options.jumpKey.setPressed(false);
        mc.options.useKey.setPressed(false);

        if (Math.hypot(dx, dz) < 0.35 || ticks > 50) {
            mc.options.forwardKey.setPressed(false);
            pathIndex++;
            state = State.FALLING;
            ticks = 0;
        }
    }

    private void falling(MinecraftClient mc) {
        release(mc);

        Cell cell = path.get(pathIndex);
        double dx = (cell.x() + 0.5) - mc.player.getX();
        double dz = (cell.z() + 0.5) - mc.player.getZ();

        if (Math.hypot(dx, dz) > 0.45 && mc.player.getY() > cfg.minY + 2) {
            face(mc, dx, dz);
            mc.options.forwardKey.setPressed(true);
        } else {
            mc.options.forwardKey.setPressed(false);
        }

        if (mc.player.isOnGround() || mc.player.getY() <= cfg.minY + 0.15) {
            release(mc);
            state = State.BUILDING;
            ticks = 0;
            msg(mc, "§7Columna " + (pathIndex + 1) + "/" + path.size());
        }
    }

    private boolean ensureEmerald(MinecraftClient mc) {
        ItemStack slotOne = mc.player.getInventory().getStack(0);

        if (slotOne.isOf(Items.EMERALD_BLOCK) && slotOne.getCount() > 0) {
            mc.player.getInventory().selectedSlot = 0;
            return true;
        }

        // Player inventory slots 9..35 map directly to handler slots 9..35.
        for (int inventorySlot = 9; inventorySlot <= 35; inventorySlot++) {
            if (mc.player.getInventory().getStack(inventorySlot).isOf(Items.EMERALD_BLOCK)) {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        inventorySlot,
                        0,
                        SlotActionType.SWAP,
                        mc.player
                );
                mc.player.getInventory().selectedSlot = 0;
                return true;
            }
        }

        // Hotbar inventory slots 1..8 correspond to handler slots 37..44.
        for (int inventorySlot = 1; inventorySlot <= 8; inventorySlot++) {
            if (mc.player.getInventory().getStack(inventorySlot).isOf(Items.EMERALD_BLOCK)) {
                int handlerSlot = 36 + inventorySlot;
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        handlerSlot,
                        0,
                        SlotActionType.SWAP,
                        mc.player
                );
                mc.player.getInventory().selectedSlot = 0;
                return true;
            }
        }

        return false;
    }

    private void beginRefill(MinecraftClient mc) {
        release(mc);

        beforeHomeX = mc.player.getX();
        beforeHomeY = mc.player.getY();
        beforeHomeZ = mc.player.getZ();

        mc.player.networkHandler.sendChatCommand("home bloques");
        state = State.HOME_WAIT;
        ticks = 0;
        msg(mc, "§eSin esmeraldas: /home bloques");
    }

    private void homeWait(MinecraftClient mc) {
        release(mc);

        double distance = Math.sqrt(
                sq(mc.player.getX() - beforeHomeX)
                        + sq(mc.player.getY() - beforeHomeY)
                        + sq(mc.player.getZ() - beforeHomeZ)
        );

        if ((ticks > 30 && distance > 3) || ticks > 120) {
            state = State.OPEN_CHEST;
            ticks = 0;
        }
    }

    private void openChest(MinecraftClient mc) {
        release(mc);

        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            state = State.LOOTING;
            ticks = 0;
            return;
        }

        mc.options.useKey.setPressed(ticks % 10 < 2);

        if (ticks > 100) {
            mc.options.useKey.setPressed(false);
            msg(mc, "§cNo pude abrir el cofre; sigo intentando...");
            ticks = 0;
        }
    }

    private void loot(MinecraftClient mc) {
        release(mc);

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            state = State.OPEN_CHEST;
            ticks = 0;
            return;
        }

        if (ticks % 3 != 0) return;

        int chestSlots = handler.getRows() * 9;
        boolean moved = false;

        for (int i = 0; i < chestSlots; i++) {
            ItemStack stack = handler.getSlot(i).getStack();

            if (stack.isOf(Items.EMERALD_BLOCK)) {
                mc.interactionManager.clickSlot(
                        handler.syncId,
                        i,
                        0,
                        SlotActionType.QUICK_MOVE,
                        mc.player
                );
                moved = true;
                break;
            }
        }

        if (!moved || inventoryFull(mc)) {
            mc.player.closeHandledScreen();
            mc.player.networkHandler.sendChatCommand("back");
            state = State.BACK_WAIT;
            ticks = 0;
            msg(mc, "§eInventario cargado: /back");
        }
    }

    private boolean inventoryFull(MinecraftClient mc) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void backWait(MinecraftClient mc) {
        release(mc);

        if (ticks > 50) {
            state = State.FALLING;
            ticks = 0;
            msg(mc, "§aRegresando al trabajo");
        }
    }

    private void face(MinecraftClient mc, double dx, double dz) {
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        mc.player.setYaw(yaw);
        mc.player.setHeadYaw(yaw);
    }

    private double sq(double value) {
        return value * value;
    }

    private void buildSpiral() {
        path.clear();

        int left = cfg.minX;
        int right = cfg.maxX;
        int top = cfg.minZ;
        int bottom = cfg.maxZ;

        while (left <= right && top <= bottom) {
            for (int x = left; x <= right; x++) {
                path.add(new Cell(x, top));
            }
            top++;

            for (int z = top; z <= bottom; z++) {
                path.add(new Cell(right, z));
            }
            right--;

            if (top <= bottom) {
                for (int x = right; x >= left; x--) {
                    path.add(new Cell(x, bottom));
                }
                bottom--;
            }

            if (left <= right) {
                for (int z = bottom; z >= top; z--) {
                    path.add(new Cell(left, z));
                }
                left++;
            }
        }
    }

    private int nearestPathIndex(MinecraftClient mc) {
        int best = 0;
        double bestDistance = Double.MAX_VALUE;

        for (int i = 0; i < path.size(); i++) {
            Cell cell = path.get(i);

            double distance =
                    sq(mc.player.getX() - (cell.x() + 0.5))
                            + sq(mc.player.getZ() - (cell.z() + 0.5));

            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }

        return best;
    }
}
