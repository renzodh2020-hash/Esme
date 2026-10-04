package com.nakero.emeraldbuilder;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class BuilderConfigScreen extends Screen {

    private final BuilderConfig config;

    private TextFieldWidget minX;
    private TextFieldWidget maxX;
    private TextFieldWidget minZ;
    private TextFieldWidget maxZ;
    private TextFieldWidget minY;
    private TextFieldWidget maxY;

    public BuilderConfigScreen(BuilderConfig config) {
        super(Text.literal("Emerald Builder"));
        this.config = config;
    }

    @Override
    protected void init() {

        int center = width / 2;
        int fieldWidth = 90;

        // X mínima / máxima
        minX = new TextFieldWidget(
                textRenderer,
                center - 100, 55,
                fieldWidth, 20,
                Text.literal("X mínima")
        );
        minX.setText(String.valueOf(config.minX));
        addDrawableChild(minX);

        maxX = new TextFieldWidget(
                textRenderer,
                center + 10, 55,
                fieldWidth, 20,
                Text.literal("X máxima")
        );
        maxX.setText(String.valueOf(config.maxX));
        addDrawableChild(maxX);

        // Z mínima / máxima
        minZ = new TextFieldWidget(
                textRenderer,
                center - 100, 95,
                fieldWidth, 20,
                Text.literal("Z mínima")
        );
        minZ.setText(String.valueOf(config.minZ));
        addDrawableChild(minZ);

        maxZ = new TextFieldWidget(
                textRenderer,
                center + 10, 95,
                fieldWidth, 20,
                Text.literal("Z máxima")
        );
        maxZ.setText(String.valueOf(config.maxZ));
        addDrawableChild(maxZ);

        // Y mínima / máxima
        minY = new TextFieldWidget(
                textRenderer,
                center - 100, 135,
                fieldWidth, 20,
                Text.literal("Y mínima")
        );
        minY.setText(String.valueOf(config.minY));
        addDrawableChild(minY);

        maxY = new TextFieldWidget(
                textRenderer,
                center + 10, 135,
                fieldWidth, 20,
                Text.literal("Y máxima")
        );
        maxY.setText(String.valueOf(config.maxY));
        addDrawableChild(maxY);

        addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Guardar"),
                        button -> save()
                ).dimensions(
                        center - 100,
                        185,
                        95,
                        20
                ).build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Cancelar"),
                        button -> close()
                ).dimensions(
                        center + 5,
                        185,
                        95,
                        20
                ).build()
        );
    }

    private void save() {

        try {
            config.minX = Integer.parseInt(minX.getText().trim());
            config.maxX = Integer.parseInt(maxX.getText().trim());

            config.minZ = Integer.parseInt(minZ.getText().trim());
            config.maxZ = Integer.parseInt(maxZ.getText().trim());

            config.minY = Integer.parseInt(minY.getText().trim());
            config.maxY = Integer.parseInt(maxY.getText().trim());

            config.normalize();
            config.save();

            if (client != null && client.player != null) {
                client.player.sendMessage(
                        Text.literal(
                                "§aConfiguración guardada §7| "
                                        + config.width()
                                        + "x"
                                        + config.depth()
                                        + " = "
                                        + config.totalColumns()
                                        + " columnas"
                        ),
                        true
                );
            }

            close();

        } catch (NumberFormatException e) {

            if (client != null && client.player != null) {
                client.player.sendMessage(
                        Text.literal(
                                "§cIntroduce solamente coordenadas numéricas."
                        ),
                        true
                );
            }
        }
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta) {

        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        int center = width / 2;

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Emerald Builder - Configuración"),
                center,
                20,
                0xFFFFFF
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("X mínima"),
                center - 100,
                43,
                0xAAAAAA
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("X máxima"),
                center + 10,
                43,
                0xAAAAAA
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Z mínima"),
                center - 100,
                83,
                0xAAAAAA
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Z máxima"),
                center + 10,
                83,
                0xAAAAAA
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Y mínima"),
                center - 100,
                123,
                0xAAAAAA
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Y máxima"),
                center + 10,
                123,
                0xAAAAAA
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("P = menú   |   O = iniciar/detener"),
                center,
                220,
                0xAAAAAA
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
