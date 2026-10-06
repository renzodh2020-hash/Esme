package com.nakero.emeraldbuilder;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class BuilderConfigScreen extends Screen {
    private final BuilderConfig config;
    private TextFieldWidget minX,maxX,minZ,maxZ,minY,maxY,cps;
    private ButtonWidget directionButton;

    public BuilderConfigScreen(BuilderConfig config) {
        super(Text.literal("Emerald Builder"));
        this.config=config;
    }

    @Override
    protected void init() {
        int c=width/2, w=90;
        minX=field(c-100,55,w,config.minX,"X mínima");
        maxX=field(c+10,55,w,config.maxX,"X máxima");
        minZ=field(c-100,95,w,config.minZ,"Z mínima");
        maxZ=field(c+10,95,w,config.maxZ,"Z máxima");
        minY=field(c-100,135,w,config.minY,"Y mínima");
        maxY=field(c+10,135,w,config.maxY,"Y máxima");
        cps=field(c-45,175,w,config.placementCps,"CPS");

        directionButton=addDrawableChild(ButtonWidget.builder(directionText(),b->nextDirection())
                .dimensions(c-100,215,200,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Guardar"),b->save())
                .dimensions(c-100,255,95,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancelar"),b->close())
                .dimensions(c+5,255,95,20).build());
    }

    private TextFieldWidget field(int x,int y,int w,int value,String label) {
        TextFieldWidget f=new TextFieldWidget(textRenderer,x,y,w,20,Text.literal(label));
        f.setText(String.valueOf(value));
        addDrawableChild(f);
        return f;
    }

    private Text directionText() {
        return Text.literal("Dirección inicial: "+spanish(config.startDirection));
    }

    private String spanish(String d) {
        return switch(d) {
            case "NORTH" -> "Norte";
            case "SOUTH" -> "Sur";
            case "WEST" -> "Oeste";
            default -> "Este";
        };
    }

    private void nextDirection() {
        config.startDirection=switch(config.startDirection) {
            case "NORTH" -> "EAST";
            case "EAST" -> "SOUTH";
            case "SOUTH" -> "WEST";
            default -> "NORTH";
        };
        directionButton.setMessage(directionText());
    }

    private void save() {
        try {
            config.minX=Integer.parseInt(minX.getText().trim());
            config.maxX=Integer.parseInt(maxX.getText().trim());
            config.minZ=Integer.parseInt(minZ.getText().trim());
            config.maxZ=Integer.parseInt(maxZ.getText().trim());
            config.minY=Integer.parseInt(minY.getText().trim());
            config.maxY=Integer.parseInt(maxY.getText().trim());
            config.placementCps=Integer.parseInt(cps.getText().trim());
            config.normalize();
            config.save();
            if(client!=null && client.player!=null)
                client.player.sendMessage(Text.literal("§aConfiguración guardada §7| "
                        +config.width()+"x"+config.depth()+" | "+spanish(config.startDirection)),true);
            close();
        } catch(NumberFormatException e) {
            if(client!=null && client.player!=null)
                client.player.sendMessage(Text.literal("§cIntroduce solamente coordenadas numéricas."),true);
        }
    }

    @Override
    public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        renderBackground(context,mouseX,mouseY,delta);
        super.render(context,mouseX,mouseY,delta);
        int c=width/2;
        context.drawCenteredTextWithShadow(textRenderer,Text.literal("Emerald Builder - Configuración"),c,20,0xFFFFFF);
        context.drawTextWithShadow(textRenderer,Text.literal("X mínima"),c-100,43,0xAAAAAA);
        context.drawTextWithShadow(textRenderer,Text.literal("X máxima"),c+10,43,0xAAAAAA);
        context.drawTextWithShadow(textRenderer,Text.literal("Z mínima"),c-100,83,0xAAAAAA);
        context.drawTextWithShadow(textRenderer,Text.literal("Z máxima"),c+10,83,0xAAAAAA);
        context.drawTextWithShadow(textRenderer,Text.literal("Y mínima"),c-100,123,0xAAAAAA);
        context.drawTextWithShadow(textRenderer,Text.literal("Y máxima"),c+10,123,0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer,Text.literal("CPS de colocación (1-20)"),c,163,0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer,Text.literal("P = menú | O = iniciar/detener"),c,290,0xAAAAAA);
    }

    @Override
    public boolean shouldPause(){ return false; }
}
