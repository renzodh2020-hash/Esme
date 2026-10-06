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
        IDLE, CENTER_BOTTOM, FLIGHT_TAP_1, FLIGHT_GAP_1, FLIGHT_TAP_2,
        ASCEND_BUILD, TOP_FLIGHT_TAP_1, TOP_FLIGHT_GAP, TOP_FLIGHT_TAP_2,
        MOVE_TO_NEXT, FALLING, HOME_WAIT, OPEN_CHEST, LOOTING, BACK_WAIT
    }

    private enum Dir {
        NORTH(0,-1), EAST(1,0), SOUTH(0,1), WEST(-1,0);
        final int dx, dz;
        Dir(int dx,int dz){ this.dx=dx; this.dz=dz; }
        Dir right(){ return switch(this){ case NORTH->EAST; case EAST->SOUTH; case SOUTH->WEST; case WEST->NORTH; }; }
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
            case CENTER_BOTTOM -> centerBottom(mc);
            case FLIGHT_TAP_1 -> flightTap1(mc);
            case FLIGHT_GAP_1 -> flightGap1(mc);
            case FLIGHT_TAP_2 -> flightTap2(mc);
            case ASCEND_BUILD -> ascendBuild(mc);
            case TOP_FLIGHT_TAP_1 -> topFlightTap1(mc);
            case TOP_FLIGHT_GAP -> topFlightGap(mc);
            case TOP_FLIGHT_TAP_2 -> topFlightTap2(mc);
            case MOVE_TO_NEXT -> moveToNext(mc);
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
        buildRightTurnSpiral();

        if (path.isEmpty()) {
            msg(mc, "§cÁrea inválida");
            return;
        }

        pathIndex = nearestPathIndex(mc);
        running = true;
        state = State.CENTER_BOTTOM;
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

    private void changeState(State next) {
        state = next;
        ticks = 0;
    }

    private void centerBottom(MinecraftClient mc) {
        if (!ensureEmerald(mc)) { beginRefill(mc); return; }

        Cell cell = path.get(pathIndex);
        double dx = cell.x() + 0.5 - mc.player.getX();
        double dz = cell.z() + 0.5 - mc.player.getZ();

        if (Math.hypot(dx, dz) > 0.28) {
            smoothFace(mc, dx, dz, 10.0f);
            mc.player.setPitch(0);
            mc.options.forwardKey.setPressed(true);
            return;
        }

        release(mc);
        if (mc.player.getY() > cfg.minY + 1.5) {
            changeState(State.FALLING);
            return;
        }
        changeState(State.FLIGHT_TAP_1);
    }

    private void flightTap1(MinecraftClient mc) {
        release(mc);
        mc.options.jumpKey.setPressed(true);
        if (ticks >= 2) changeState(State.FLIGHT_GAP_1);
    }

    private void flightGap1(MinecraftClient mc) {
        mc.options.jumpKey.setPressed(false);
        if (ticks >= 2) changeState(State.FLIGHT_TAP_2);
    }

    private void flightTap2(MinecraftClient mc) {
        mc.options.jumpKey.setPressed(true);
        if (ticks >= 2) changeState(State.ASCEND_BUILD);
    }

    private void ascendBuild(MinecraftClient mc) {
        if (!ensureEmerald(mc)) { beginRefill(mc); return; }

        mc.player.getInventory().selectedSlot = 0;
        mc.options.forwardKey.setPressed(false);
        mc.player.setPitch(89.5f);
        mc.options.jumpKey.setPressed(true);

        // 20 ticks/s -> una pulsación cada 2 ticks = 10 CPS.
        mc.options.useKey.setPressed((ticks & 1) == 0);

        if (mc.player.getY() >= cfg.maxY) {
            mc.options.useKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            changeState(State.TOP_FLIGHT_TAP_1);
        }
    }

    private void topFlightTap1(MinecraftClient mc) {
        release(mc);
        mc.options.jumpKey.setPressed(true);
        if (ticks >= 2) changeState(State.TOP_FLIGHT_GAP);
    }

    private void topFlightGap(MinecraftClient mc) {
        mc.options.jumpKey.setPressed(false);
        if (ticks >= 2) changeState(State.TOP_FLIGHT_TAP_2);
    }

    private void topFlightTap2(MinecraftClient mc) {
        mc.options.jumpKey.setPressed(true);
        if (ticks >= 2) {
            mc.options.jumpKey.setPressed(false);
            changeState(State.MOVE_TO_NEXT);
        }
    }

    private void moveToNext(MinecraftClient mc) {
        if (pathIndex + 1 >= path.size()) {
            stop(mc, "§aÁrea completada");
            return;
        }

        Cell next = path.get(pathIndex + 1);
        double dx = next.x() + 0.5 - mc.player.getX();
        double dz = next.z() + 0.5 - mc.player.getZ();

        smoothFace(mc, dx, dz, 8.0f);
        mc.player.setPitch(0);
        mc.options.forwardKey.setPressed(true);
        mc.options.jumpKey.setPressed(false);
        mc.options.useKey.setPressed(false);

        if (Math.hypot(dx, dz) < 0.28 || ticks > 35) {
            mc.options.forwardKey.setPressed(false);
            pathIndex++;
            changeState(State.FALLING);
        }
    }

    private void falling(MinecraftClient mc) {
        release(mc);

        Cell cell = path.get(pathIndex);
        double dx = cell.x() + 0.5 - mc.player.getX();
        double dz = cell.z() + 0.5 - mc.player.getZ();

        if (Math.hypot(dx, dz) > 0.38 && mc.player.getY() > cfg.minY + 2.0) {
            smoothFace(mc, dx, dz, 5.0f);
            mc.options.forwardKey.setPressed(true);
        }

        if (mc.player.isOnGround() || mc.player.getY() <= cfg.minY + 0.35) {
            release(mc);
            changeState(State.CENTER_BOTTOM);
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

    private void smoothFace(MinecraftClient mc, double dx, double dz, float maxStep) {
        float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float current = mc.player.getYaw();
        float diff = wrapDegrees(target - current);
        diff = Math.max(-maxStep, Math.min(maxStep, diff));
        float next = current + diff;
        mc.player.setYaw(next);
        mc.player.setHeadYaw(next);
    }

    private float wrapDegrees(float value) {
        value %= 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
    }

    private double sq(double value) {
        return value * value;
    }

    private Dir configuredDirection() {
        return switch (cfg.startDirection) {
            case "NORTH" -> Dir.NORTH;
            case "SOUTH" -> Dir.SOUTH;
            case "WEST" -> Dir.WEST;
            default -> Dir.EAST;
        };
    }

    private void buildRightTurnSpiral() {
        path.clear();

        int width = cfg.width();
        int depth = cfg.depth();
        boolean[][] used = new boolean[width][depth];
        Dir dir = configuredDirection();

        int x, z;
        switch (dir) {
            case EAST -> { x = cfg.minX; z = cfg.minZ; }
            case SOUTH -> { x = cfg.maxX; z = cfg.minZ; }
            case WEST -> { x = cfg.maxX; z = cfg.maxZ; }
            case NORTH -> { x = cfg.minX; z = cfg.maxZ; }
            default -> throw new IllegalStateException();
        }

        int total = width * depth;
        for (int count = 0; count < total; count++) {
            path.add(new Cell(x, z));
            used[x - cfg.minX][z - cfg.minZ] = true;
            if (count == total - 1) break;

            int nx = x + dir.dx;
            int nz = z + dir.dz;

            if (!inside(nx, nz) || used[nx - cfg.minX][nz - cfg.minZ]) {
                dir = dir.right();
                nx = x + dir.dx;
                nz = z + dir.dz;
            }

            int guard = 0;
            while ((!inside(nx, nz) || used[nx - cfg.minX][nz - cfg.minZ]) && guard < 3) {
                dir = dir.right();
                nx = x + dir.dx;
                nz = z + dir.dz;
                guard++;
            }

            x = nx;
            z = nz;
        }
    }

    private boolean inside(int x, int z) {
        return x >= cfg.minX && x <= cfg.maxX && z >= cfg.minZ && z <= cfg.maxZ;
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
