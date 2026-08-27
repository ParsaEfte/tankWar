package org.example.game;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import org.example.ai.TankAIController;
import org.example.audio.SoundManager;
import org.example.entities.Bullet3D;
import org.example.entities.Explosion3D;
import org.example.entities.Tank3D;
import org.example.map.MazeBuilder;
import org.example.util.GameLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TankCombatManager {

    private final AssetManager assetManager;
    private final Node rootNode;
    private final SoundManager soundManager;

    private Tank3D localPlayerTank;
    private final List<Tank3D> allTanks = new ArrayList<>();
    private final List<TankAIController> aiControllers = new ArrayList<>();
    private final List<Bullet3D> bullets = new ArrayList<>();
    private final List<Explosion3D> explosions = new ArrayList<>();
    private Texture[] explosionTextures;

    private long lastShotTime = 0;
    private static final long SHOOT_COOLDOWN = 350;

    public TankCombatManager(AssetManager assetManager, Node rootNode, SoundManager soundManager) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.soundManager = soundManager;
        loadExplosionTextures();
    }

    public void initCombatants(int playerCount) {
        GameLogger.info("GameMode", "Arena initialized with " + playerCount + " combat tanks.");

        for (Tank3D t : allTanks) {
            t.getTankNode().removeFromParent();
        }
        allTanks.clear();
        aiControllers.clear();
        bullets.clear();
        explosions.clear();

        record SpawnPoint(Vector3f pos, float yaw, String name, ColorRGBA color) {}

        SpawnPoint[] spawns = new SpawnPoint[]{
                new SpawnPoint(MazeBuilder.getTileWorldPos(1, 1), FastMath.PI, "Blue Titan (You)", new ColorRGBA(0.2f, 0.75f, 1.0f, 1.0f)),
                new SpawnPoint(MazeBuilder.getTileWorldPos(9, 9), 0f, "Red Baron", new ColorRGBA(0.95f, 0.25f, 0.25f, 1.0f)),
                new SpawnPoint(MazeBuilder.getTileWorldPos(1, 9), -FastMath.HALF_PI, "Neon Viper", new ColorRGBA(0.2f, 0.95f, 0.35f, 1.0f)),
                new SpawnPoint(MazeBuilder.getTileWorldPos(9, 1), FastMath.HALF_PI, "Solar Flare", new ColorRGBA(1.0f, 0.75f, 0.1f, 1.0f))
        };

        for (int i = 0; i < playerCount; i++) {
            SpawnPoint sp = spawns[i];
            Tank3D tank = new Tank3D(sp.name, sp.pos, sp.color, assetManager, rootNode);
            tank.setInitialYaw(sp.yaw);
            allTanks.add(tank);

            if (i == 0) {
                localPlayerTank = tank;
            } else {
                aiControllers.add(new TankAIController(tank, this));
            }
        }
    }

    public void handleLocalShoot() {
        if (localPlayerTank == null || !localPlayerTank.isAlive()) return;
        long now = System.currentTimeMillis();
        if (now - lastShotTime < SHOOT_COOLDOWN) return;

        lastShotTime = now;
        soundManager.playShoot();

        spawnBullet(localPlayerTank.getMuzzlePosition(), localPlayerTank.getForwardVector(), localPlayerTank);
        GameLogger.combat("FIRE", "Player fired plasma projectile.");
    }

    public void spawnBullet(Vector3f pos, Vector3f dir, Tank3D owner) {
        Bullet3D bullet = new Bullet3D(
                pos,
                dir,
                new ColorRGBA(1.0f, 0.85f, 0.1f, 1.0f),
                owner,
                assetManager,
                rootNode
        );
        bullets.add(bullet);
    }

    public void update(float tpf, Node wallsNode, Camera cam) {
        for (TankAIController ai : aiControllers) {
            ai.update(tpf, allTanks, wallsNode);
        }

        for (Tank3D tank : allTanks) {
            tank.update(tpf, wallsNode, cam);
        }

        Iterator<Bullet3D> bulletIt = bullets.iterator();
        while (bulletIt.hasNext()) {
            Bullet3D b = bulletIt.next();
            b.update(tpf, wallsNode, allTanks, explosions, explosionTextures, assetManager, soundManager);
            if (!b.isActive()) {
                bulletIt.remove();
            }
        }

        Iterator<Explosion3D> expIt = explosions.iterator();
        while (expIt.hasNext()) {
            Explosion3D exp = expIt.next();
            exp.update(tpf, cam);
            if (exp.isFinished()) {
                expIt.remove();
            }
        }
    }

    private void loadExplosionTextures() {
        List<Texture> loadedList = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            String path = String.format("Textures/Explosion/explosion%02d.png", i);
            try {
                TextureKey key = new TextureKey(path, false);
                Texture tex = assetManager.loadTexture(key);
                tex.setMagFilter(Texture.MagFilter.Bilinear);
                tex.setMinFilter(Texture.MinFilter.BilinearNearestMipMap);
                loadedList.add(tex);
            } catch (Exception ignored) {}
        }
        if (!loadedList.isEmpty()) {
            explosionTextures = loadedList.toArray(new Texture[0]);
        }
    }

    public Tank3D getLocalPlayerTank() {
        return localPlayerTank;
    }

    public List<Tank3D> getAllTanks() {
        return allTanks;
    }
}