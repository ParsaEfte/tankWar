package org.example.menu;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import org.example.audio.SoundManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MenuManager {

    private final AssetManager assetManager;
    private final Node guiNode;
    private final AppSettings settings;
    private final SoundManager soundManager;
    private final Consumer<Integer> onStartBattle;

    private Node menuGuiNode;
    private final List<ClickableButton> menuButtons = new ArrayList<>();
    private BitmapFont defaultFont;

    private record ClickableButton(float x, float y, float width, float height, Runnable onClick) {
        public boolean contains(float px, float py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }

    public MenuManager(AssetManager assetManager, Node guiNode, AppSettings settings, SoundManager soundManager, Consumer<Integer> onStartBattle) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        this.settings = settings;
        this.soundManager = soundManager;
        this.onStartBattle = onStartBattle;
        this.defaultFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
    }

    public void showMainMenu() {
        clearMenu();

        float screenW = settings.getWidth();
        float screenH = settings.getHeight();

        addBackgroundOverlay(screenW, screenH);

        float cardW = 560f;
        float cardH = 430f;
        float cardX = (screenW - cardW) / 2f;
        float cardY = (screenH - cardH) / 2f;

        createGlassCard(cardX, cardY, cardW, cardH, new ColorRGBA(0.0f, 0.85f, 1.0f, 0.7f));

        BitmapText titleText = new BitmapText(defaultFont, false);
        titleText.setSize(defaultFont.getCharSet().getRenderedSize() * 2.8f);
        titleText.setText("CYBER CASTLE");
        titleText.setColor(new ColorRGBA(0.0f, 0.95f, 1.0f, 1.0f));
        titleText.setLocalTranslation(cardX + 90, cardY + cardH - 45, 2);
        menuGuiNode.attachChild(titleText);

        BitmapText subTitle = new BitmapText(defaultFont, false);
        subTitle.setSize(defaultFont.getCharSet().getRenderedSize() * 1.15f);
        subTitle.setText("TACTICAL 3D TANK ARENA");
        subTitle.setColor(new ColorRGBA(0.6f, 0.75f, 0.9f, 1.0f));
        subTitle.setLocalTranslation(cardX + 160, cardY + cardH - 100, 2);
        menuGuiNode.attachChild(subTitle);

        createCyberButton(
                cardX + (cardW - 320) / 2f,
                cardY + 180f,
                320f,
                54f,
                "START BATTLE",
                new ColorRGBA(0.0f, 0.9f, 1.0f, 1.0f),
                this::showPlayerSelectMenu
        );

        BitmapText guide = new BitmapText(defaultFont, false);
        guide.setSize(defaultFont.getCharSet().getRenderedSize() * 1.05f);
        guide.setText("Controls: W/S - Speed | A/D - Steer | SPACE / Click - Fire | M - Mute");
        guide.setColor(new ColorRGBA(0.45f, 0.6f, 0.75f, 1.0f));
        guide.setLocalTranslation(cardX + 25, cardY + 70, 2);
        menuGuiNode.attachChild(guide);

        guiNode.attachChild(menuGuiNode);
    }

    public void showPlayerSelectMenu() {
        clearMenu();

        float screenW = settings.getWidth();
        float screenH = settings.getHeight();

        addBackgroundOverlay(screenW, screenH);

        float cardW = 600f;
        float cardH = 440f;
        float cardX = (screenW - cardW) / 2f;
        float cardY = (screenH - cardH) / 2f;

        createGlassCard(cardX, cardY, cardW, cardH, new ColorRGBA(0.7f, 0.3f, 1.0f, 0.7f));

        BitmapText title = new BitmapText(defaultFont, false);
        title.setSize(defaultFont.getCharSet().getRenderedSize() * 2.2f);
        title.setText("SELECT TANK COUNT");
        title.setColor(new ColorRGBA(0.85f, 0.45f, 1.0f, 1.0f));
        title.setLocalTranslation(cardX + 110, cardY + cardH - 45, 2);
        menuGuiNode.attachChild(title);

        BitmapText sub = new BitmapText(defaultFont, false);
        sub.setSize(defaultFont.getCharSet().getRenderedSize() * 1.1f);
        sub.setText("Choose combatants to spawn in arena bases:");
        sub.setColor(new ColorRGBA(0.7f, 0.8f, 0.9f, 1.0f));
        sub.setLocalTranslation(cardX + 120, cardY + cardH - 100, 2);
        menuGuiNode.attachChild(sub);

        createCyberButton(cardX + 60, cardY + 190, 140, 52, "2 TANKS", new ColorRGBA(0.2f, 0.8f, 1.0f, 1f), () -> launchGame(2));
        createCyberButton(cardX + 230, cardY + 190, 140, 52, "3 TANKS", new ColorRGBA(1.0f, 0.75f, 0.2f, 1f), () -> launchGame(3));
        createCyberButton(cardX + 400, cardY + 190, 140, 52, "4 TANKS", new ColorRGBA(1.0f, 0.25f, 0.4f, 1f), () -> launchGame(4));

        createCyberButton(cardX + (cardW - 180) / 2f, cardY + 70, 180, 44, "<- BACK", new ColorRGBA(0.5f, 0.6f, 0.7f, 1f), this::showMainMenu);

        guiNode.attachChild(menuGuiNode);
    }

    private void launchGame(int count) {
        clearMenu();
        if (onStartBattle != null) {
            onStartBattle.accept(count);
        }
    }

    public boolean handleClick(Vector2f clickPos) {
        for (ClickableButton btn : menuButtons) {
            if (btn.contains(clickPos.x, clickPos.y)) {
                if (soundManager != null) {
                    soundManager.playShoot();
                }
                btn.onClick().run();
                return true;
            }
        }
        return false;
    }

    public void clearMenu() {
        if (menuGuiNode != null) {
            menuGuiNode.removeFromParent();
        }
        menuGuiNode = new Node("MenuGuiNode");
        menuButtons.clear();
    }

    private void createGlassCard(float x, float y, float w, float h, ColorRGBA glowColor) {
        Geometry card = new Geometry("Card", new Quad(w, h));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.06f, 0.08f, 0.14f, 0.94f));
        card.setMaterial(mat);
        card.setLocalTranslation(x, y, 1);
        menuGuiNode.attachChild(card);

        Geometry border = new Geometry("CardBorder", new Quad(w + 4, h + 4));
        Material borderMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        borderMat.setColor("Color", glowColor);
        border.setMaterial(borderMat);
        border.setLocalTranslation(x - 2, y - 2, 0.5f);
        menuGuiNode.attachChild(border);
    }

    private void createCyberButton(float x, float y, float w, float h, String text, ColorRGBA color, Runnable action) {
        Geometry btn = new Geometry("Btn_" + text, new Quad(w, h));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        btn.setMaterial(mat);
        btn.setLocalTranslation(x, y, 2);
        menuGuiNode.attachChild(btn);

        BitmapText txt = new BitmapText(defaultFont, false);
        txt.setSize(defaultFont.getCharSet().getRenderedSize() * 1.25f);
        txt.setText(text);
        txt.setColor(new ColorRGBA(0.04f, 0.05f, 0.08f, 1.0f));

        float textX = x + (w - txt.getLineWidth()) / 2f;
        float textY = y + (h + txt.getLineHeight()) / 2f - 4;
        txt.setLocalTranslation(textX, textY, 3);
        menuGuiNode.attachChild(txt);

        menuButtons.add(new ClickableButton(x, y, w, h, action));
    }

    private void addBackgroundOverlay(float w, float h) {
        Geometry bg = new Geometry("MenuDimmer", new Quad(w, h));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.04f, 0.05f, 0.09f, 0.85f));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        bg.setMaterial(mat);
        menuGuiNode.attachChild(bg);
    }
}