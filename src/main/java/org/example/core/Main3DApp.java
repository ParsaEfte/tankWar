package org.example.core;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import org.example.audio.SoundManager;
import org.example.entities.Bullet3D;
import org.example.entities.Explosion3D;
import org.example.entities.Tank3D;
import org.example.map.MazeBuilder;
import org.example.menu.MenuManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Main3DApp extends SimpleApplication implements ActionListener {

    public enum GameState {
        MENU,
        PLAYING
    }

    private GameState currentState = GameState.MENU;

    private Tank3D playerTank;
    private final List<Tank3D> allTanks = new ArrayList<>();

    private MazeBuilder mazeBuilder;
    private SoundManager soundManager;
    private MenuManager menuManager;

    private final List<Bullet3D> bullets = new ArrayList<>();
    private final List<Explosion3D> explosions = new ArrayList<>();
    private Texture[] explosionTextures;

    private BitmapText targetSignText;

    private long lastShotTime = 0;
    private static final long SHOOT_COOLDOWN = 350;

    public static void main(String[] args) {
        Main3DApp app = new Main3DApp();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Cyber Castle - 3D Tank Battle");
        settings.setResolution(1280, 720);
        settings.setSamples(4);
        settings.setVSync(true);

        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
        cam.setFrustumPerspective(68f, (float) cam.getWidth() / cam.getHeight(), 0.1f, 300f);
        viewPort.setBackgroundColor(new ColorRGBA(0.12f, 0.14f, 0.18f, 1.0f));

        soundManager = new SoundManager(assetManager, rootNode);
        mazeBuilder = new MazeBuilder(assetManager, rootNode);
        mazeBuilder.build();

        loadExplosionTextures();
        setupLighting();
        setupKeyBindings();
        setupHUDTargetSign();

        menuManager = new MenuManager(assetManager, guiNode, settings, soundManager, this::initBattleWithPlayers);
        menuManager.showMainMenu();
        setMenuCameraView();
    }

    private void initBattleWithPlayers(int playerCount) {
        currentState = GameState.PLAYING;
        inputManager.setCursorVisible(false);
        targetSignText.setCullHint(Spatial.CullHint.Inherit);

        for (Tank3D t : allTanks) {
            t.getTankNode().removeFromParent();
        }
        allTanks.clear();

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
                playerTank = tank;
            }
        }

        updateFirstPersonCamera();
    }

    private void setMenuCameraView() {
        targetSignText.setCullHint(Spatial.CullHint.Always);
        cam.setLocation(new Vector3f(0, 18f, 22f));
        cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
    }

    private void setupHUDTargetSign() {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        targetSignText = new BitmapText(font, false);
        targetSignText.setSize(font.getCharSet().getRenderedSize() * 1.45f);
        targetSignText.setColor(new ColorRGBA(0.9f, 0.95f, 1.0f, 1.0f));
        targetSignText.setLocalTranslation(settings.getWidth() / 2f - 200, settings.getHeight() - 25, 0);
        guiNode.attachChild(targetSignText);
    }

    private void updateTargetTracker() {
        if (playerTank == null || !playerTank.isAlive()) return;

        Tank3D nearestEnemy = null;
        float minDist = Float.MAX_VALUE;
        Vector3f pPos = playerTank.getPosition();

        for (Tank3D t : allTanks) {
            if (t != playerTank && t.isAlive()) {
                float d = pPos.distance(t.getPosition());
                if (d < minDist) {
                    minDist = d;
                    nearestEnemy = t;
                }
            }
        }

        if (nearestEnemy == null) {
            targetSignText.setText("[ VICTORY - ALL TARGETS DESTROYED ]");
            targetSignText.setColor(new ColorRGBA(0.2f, 1.0f, 0.4f, 1.0f));
            return;
        }

        Vector3f ePos = nearestEnemy.getPosition();
        Vector3f toEnemy = ePos.subtract(pPos).setY(0).normalizeLocal();
        Vector3f forward = playerTank.getForwardVector().setY(0).normalizeLocal();
        Vector3f right = playerTank.getTankNode().getLocalRotation().getRotationColumn(0).setY(0).normalizeLocal();

        float dotForward = forward.dot(toEnemy);
        float dotRight = right.dot(toEnemy);

        String directionGuide;
        if (dotForward > 0.88f) {
            directionGuide = "⬆ [LOCKED AHEAD]";
            nearestEnemy.setHealthBeamVisible(minDist < 45f);
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

        targetSignText.setText(String.format("NEAREST: %s | DIST: %.1fm | %s", nearestEnemy.getName(), minDist, directionGuide));
        targetSignText.setColor(dotForward > 0.88f ? new ColorRGBA(1.0f, 0.3f, 0.3f, 1f) : new ColorRGBA(0.4f, 0.85f, 1f, 1f));
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

    private void setupLighting() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.8f, -0.4f).normalizeLocal());
        sun.setColor(new ColorRGBA(1.2f, 1.2f, 1.25f, 1.0f));
        rootNode.addLight(sun);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.45f, 0.45f, 0.55f, 1.0f));
        rootNode.addLight(ambient);
    }

    private void setupKeyBindings() {
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Shoot", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("MouseClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("ToggleMute", new KeyTrigger(KeyInput.KEY_M));

        inputManager.addListener(this, "Forward", "Backward", "Left", "Right", "Shoot", "MouseClick", "ToggleMute");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("ToggleMute") && isPressed) {
            soundManager.toggleMute();
            return;
        }

        if (name.equals("MouseClick") && isPressed) {
            if (currentState == GameState.MENU) {
                Vector2f click = inputManager.getCursorPosition();
                menuManager.handleClick(click);
            } else {
                shootBullet();
            }
            return;
        }

        if (currentState != GameState.PLAYING) return;
        if (playerTank == null || !playerTank.isAlive()) return;

        switch (name) {
            case "Forward" -> playerTank.setMovingForward(isPressed);
            case "Backward" -> playerTank.setMovingBackward(isPressed);
            case "Left" -> playerTank.setRotatingLeft(isPressed);
            case "Right" -> playerTank.setRotatingRight(isPressed);
            case "Shoot" -> {
                if (isPressed) shootBullet();
            }
        }
    }

    private void shootBullet() {
        long now = System.currentTimeMillis();
        if (now - lastShotTime < SHOOT_COOLDOWN) return;

        lastShotTime = now;
        soundManager.playShoot();

        Vector3f muzzlePos = playerTank.getMuzzlePosition();
        Vector3f shootDir = playerTank.getForwardVector();

        Bullet3D bullet = new Bullet3D(
                muzzlePos,
                shootDir,
                new ColorRGBA(1.0f, 0.85f, 0.1f, 1.0f),
                playerTank,
                assetManager,
                rootNode
        );
        bullets.add(bullet);
    }

    private void updateFirstPersonCamera() {
        if (playerTank == null || !playerTank.isAlive()) return;

        Node tankNode = playerTank.getTankNode();
        Vector3f tankPos = tankNode.getLocalTranslation();
        Quaternion tankRot = tankNode.getLocalRotation();

        Vector3f forwardDir = tankRot.getRotationColumn(2).negate();
        Vector3f eyePos = tankPos.add(0, 0.95f, 0).addLocal(forwardDir.mult(0.35f));

        cam.setLocation(eyePos);
        cam.lookAt(eyePos.add(forwardDir.mult(10f)), Vector3f.UNIT_Y);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (currentState == GameState.MENU) {
            float angle = (float) (System.currentTimeMillis() * 0.0003);
            cam.setLocation(new Vector3f(FastMath.sin(angle) * 35f, 16f, FastMath.cos(angle) * 35f));
            cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
            return;
        }

        Node wallsNode = mazeBuilder.getWallsNode();

        for (Tank3D tank : allTanks) {
            tank.update(tpf, wallsNode, cam);
        }

        updateFirstPersonCamera();
        updateTargetTracker();

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
}