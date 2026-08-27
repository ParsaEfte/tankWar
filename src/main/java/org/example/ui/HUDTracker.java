package org.example.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import org.example.entities.Tank3D;

import java.util.List;

public class HUDTracker {

    private final BitmapText targetSignText;

    public HUDTracker(AssetManager assetManager, Node guiNode, AppSettings settings) {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        targetSignText = new BitmapText(font, false);
        targetSignText.setSize(font.getCharSet().getRenderedSize() * 1.45f);
        targetSignText.setColor(new ColorRGBA(0.9f, 0.95f, 1.0f, 1.0f));
        targetSignText.setLocalTranslation(settings.getWidth() / 2f - 200, settings.getHeight() - 25, 0);
        targetSignText.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(targetSignText);
    }

    public void setVisible(boolean visible) {
        targetSignText.setCullHint(visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    public void updateTracker(Tank3D playerTank, List<Tank3D> allTanks) {
        if (playerTank == null || !playerTank.isAlive()) return;

        Tank3D nearestEnemy = null;
        float minDistSq = Float.MAX_VALUE;
        Vector3f pPos = playerTank.getPosition();

        // ۱. پیدا کردن نزدیک‌ترین دشمن زنده بر اساس فاصله دوبعدی سطح زمین (XZ)
        for (Tank3D t : allTanks) {
            if (t != playerTank && t.isAlive()) {
                Vector3f ePos = t.getPosition();
                float dx = pPos.x - ePos.x;
                float dz = pPos.z - ePos.z;
                float distSq = dx * dx + dz * dz;

                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    nearestEnemy = t;
                }
            }
        }

        // ۲. خاموش کردن بیم سلامت تمام تانک‌ها به جز نزدیک‌ترین
        for (Tank3D t : allTanks) {
            if (t != playerTank && t != nearestEnemy) {
                t.setHealthBeamVisible(false);
            }
        }

        if (nearestEnemy == null) {
            targetSignText.setText("[ VICTORY - ALL TARGETS DESTROYED ]");
            targetSignText.setColor(new ColorRGBA(0.2f, 1.0f, 0.4f, 1.0f));
            return;
        }

        float realDistance = (float) Math.sqrt(minDistSq);
        Vector3f ePos = nearestEnemy.getPosition();
        Vector3f toEnemy = new Vector3f(ePos.x - pPos.x, 0, ePos.z - pPos.z).normalizeLocal();
        Vector3f forward = playerTank.getForwardVector().setY(0).normalizeLocal();
        Vector3f right = playerTank.getTankNode().getLocalRotation().getRotationColumn(0).setY(0).normalizeLocal();

        float dotForward = forward.dot(toEnemy);
        float dotRight = right.dot(toEnemy);

        String directionGuide;
        boolean isLockedAhead = dotForward > 0.88f;

        if (isLockedAhead) {
            directionGuide = "⬆ [LOCKED AHEAD]";
            nearestEnemy.setHealthBeamVisible(realDistance < 45f);
        } else {
            nearestEnemy.setHealthBeamVisible(false);
            if (dotForward < -0.65f) {
                directionGuide = "⬇ [BEHIND]";
            } else if (dotRight > 0) {
                directionGuide = "➡ [RIGHT]";
            } else {
                directionGuide = "⬅ [LEFT]";
            }
        }

        targetSignText.setText(String.format("NEAREST: %s | DIST: %.1fm | %s", nearestEnemy.getName(), realDistance, directionGuide));
        targetSignText.setColor(isLockedAhead ? new ColorRGBA(1.0f, 0.3f, 0.3f, 1f) : new ColorRGBA(0.4f, 0.85f, 1f, 1f));
    }
}