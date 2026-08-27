package org.example.entities;

import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import org.example.audio.SoundManager;

import java.util.List;

public class Bullet3D {

    private final Geometry geometry;
    private final Vector3f direction;
    private final Node rootNode;
    private final Tank3D owner;

    private static final float SPEED = 32.0f;
    private static final float RADIUS = 0.16f;
    private static final float MAX_LIFETIME = 4.0f;

    private float lifetime = 0f;
    private boolean active = true;
    private Vector3f lastHitPoint = null;

    public Bullet3D(Vector3f startPos, Vector3f dir, ColorRGBA glowColor, Tank3D owner, AssetManager assetManager, Node rootNode) {
        this.direction = dir.normalize();
        this.rootNode = rootNode;
        this.owner = owner;

        Sphere sphereMesh = new Sphere(16, 16, RADIUS);
        this.geometry = new Geometry("Bullet", sphereMesh);

        Material bulletMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bulletMat.setColor("Color", glowColor);
        this.geometry.setMaterial(bulletMat);

        this.geometry.setLocalTranslation(startPos);
        this.rootNode.attachChild(this.geometry);
    }

    public void update(float tpf, Spatial walls, List<Tank3D> tanks, List<Explosion3D> explosions, Texture[] explosionTextures, AssetManager assetManager, SoundManager soundManager) {
        if (!active) return;

        lifetime += tpf;
        if (lifetime >= MAX_LIFETIME) {
            destroy();
            return;
        }

        Vector3f moveDelta = direction.mult(SPEED * tpf);

        // ۱. بررسی برخورد با سایر تانک‌ها
        for (Tank3D tank : tanks) {
            if (tank != owner && tank.isAlive()) {
                if (checkTankCollision(tank, moveDelta)) {
                    boolean destroyed = tank.takeDamage();
                    this.lastHitPoint = geometry.getLocalTranslation();

                    if (soundManager != null) {
                        soundManager.playExplosion();
                    }

                    if (explosionTextures != null && explosionTextures.length > 0) {
                        explosions.add(new Explosion3D(lastHitPoint, explosionTextures, assetManager, rootNode));
                        if (destroyed) {
                            explosions.add(new Explosion3D(tank.getPosition().add(0, 0.8f, 0), explosionTextures, assetManager, rootNode));
                        }
                    }
                    destroy();
                    return;
                }
            }
        }

        // ۲. بررسی برخورد با دیوارهای مپ
        if (checkWallCollision(moveDelta, walls)) {
            if (soundManager != null) {
                soundManager.playExplosion();
            }

            if (explosionTextures != null && explosionTextures.length > 0) {
                Vector3f spawnPos = getLastHitPoint().add(direction.negate().mult(0.35f));
                explosions.add(new Explosion3D(spawnPos, explosionTextures, assetManager, rootNode));
            }
            destroy();
            return;
        }

        geometry.move(moveDelta);
    }

    // اورلود برای سازگاری در صورت ارسال نشدن soundManager
    public void update(float tpf, Spatial walls, List<Tank3D> tanks, List<Explosion3D> explosions, Texture[] explosionTextures, AssetManager assetManager) {
        update(tpf, walls, tanks, explosions, explosionTextures, assetManager, null);
    }

    private boolean checkTankCollision(Tank3D tank, Vector3f moveDelta) {
        Vector3f tankPos = tank.getPosition().add(0, 0.4f, 0);
        float dist = geometry.getLocalTranslation().distance(tankPos);
        return dist <= (1.2f + moveDelta.length());
    }

    private boolean checkWallCollision(Vector3f moveDelta, Spatial walls) {
        if (walls == null) return false;

        Ray ray = new Ray(geometry.getLocalTranslation(), direction);
        CollisionResults results = new CollisionResults();
        walls.collideWith(ray, results);

        if (results.size() > 0) {
            float dist = results.getClosestCollision().getDistance();
            if (dist <= moveDelta.length() + RADIUS) {
                this.lastHitPoint = results.getClosestCollision().getContactPoint();
                return true;
            }
        }
        return false;
    }

    public void destroy() {
        if (!active) return;
        active = false;
        geometry.removeFromParent();
    }

    public boolean isActive() {
        return active;
    }

    public Vector3f getLastHitPoint() {
        return lastHitPoint != null ? lastHitPoint : geometry.getLocalTranslation();
    }

    public Vector3f getDirection() {
        return direction;
    }
}