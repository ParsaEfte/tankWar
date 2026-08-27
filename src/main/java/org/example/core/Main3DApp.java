package org.example.core;

import com.jme3.app.SimpleApplication;
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
import com.jme3.system.AppSettings;
import org.example.audio.SoundManager;
import org.example.entities.Tank3D;
import org.example.game.TankCombatManager;
import org.example.map.MazeBuilder;
import org.example.ui.HUDTracker;
import org.example.ui.MenuManager;
import org.example.util.GameLogger;

public class Main3DApp extends SimpleApplication implements ActionListener {

    public enum GameState {
        MENU,
        PLAYING
    }

    private GameState currentState = GameState.MENU;

    private MazeBuilder mazeBuilder;
    private SoundManager soundManager;
    private MenuManager menuManager;
    private HUDTracker hudTracker;
    private TankCombatManager combatManager;

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

        GameLogger.info("Engine", "Graphics & Audio subsystems loaded successfully.");
        GameLogger.info("Maze", "Map built with symmetric 4-corner bases.");

        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
        cam.setFrustumPerspective(68f, (float) cam.getWidth() / cam.getHeight(), 0.1f, 300f);
        viewPort.setBackgroundColor(new ColorRGBA(0.12f, 0.14f, 0.18f, 1.0f));

        soundManager = new SoundManager(assetManager, rootNode);
        mazeBuilder = new MazeBuilder(assetManager, rootNode);
        mazeBuilder.build();

        combatManager = new TankCombatManager(assetManager, rootNode, soundManager);
        hudTracker = new HUDTracker(assetManager, guiNode, settings);

        setupLighting();
        setupKeyBindings();

        menuManager = new MenuManager(assetManager, guiNode, settings, soundManager, this::startGame);
        menuManager.showMainMenu();
        setMenuCameraView();
    }

    private void startGame(int playerCount) {
        currentState = GameState.PLAYING;
        inputManager.setCursorVisible(false);
        hudTracker.setVisible(true);

        combatManager.initCombatants(playerCount);
        updateFirstPersonCamera();
    }

    private void setMenuCameraView() {
        hudTracker.setVisible(false);
        cam.setLocation(new Vector3f(0, 18f, 22f));
        cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
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
                combatManager.handleLocalShoot();
            }
            return;
        }

        if (currentState != GameState.PLAYING) return;

        Tank3D localTank = combatManager.getLocalPlayerTank();
        if (localTank == null || !localTank.isAlive()) return;

        switch (name) {
            case "Forward" -> localTank.setMovingForward(isPressed);
            case "Backward" -> localTank.setMovingBackward(isPressed);
            case "Left" -> localTank.setRotatingLeft(isPressed);
            case "Right" -> localTank.setRotatingRight(isPressed);
            case "Shoot" -> {
                if (isPressed) combatManager.handleLocalShoot();
            }
        }
    }

    private void updateFirstPersonCamera() {
        Tank3D localTank = combatManager.getLocalPlayerTank();
        if (localTank == null || !localTank.isAlive()) return;

        Node tankNode = localTank.getTankNode();
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

        combatManager.update(tpf, mazeBuilder.getWallsNode(), cam);
        updateFirstPersonCamera();
        hudTracker.updateTracker(combatManager.getLocalPlayerTank(), combatManager.getAllTanks());
    }
}