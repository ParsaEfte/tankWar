package org.example.entities;

import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;

public class Tank3D {

    private final Node tankNode;
    private final Node turretNode;
    private final Node healthHudNode;
    private final AssetManager assetManager;
    private final ColorRGBA primaryColor;
    private final String name;

    private int health = 3;
    private boolean alive = true;
    private boolean invincible = false;
    private long invincibleStartTime = 0;
    private static final long INVINCIBLE_DURATION = 1500;

    private final float moveSpeed = 8.0f;
    private final float rotateSpeed = 2.4f;
    private float currentYaw = 0f;

    private boolean movingForward = false;
    private boolean movingBackward = false;
    private boolean rotatingLeft = false;
    private boolean rotatingRight = false;

    private static final float COLLISION_RADIUS = 1.15f;

    // کامپوننت نوار افقی نئونی
    private Geometry healthBeamBar;
    private Material beamMat;
    private static final float MAX_BEAM_LENGTH = 1.8f;
    private static final float BEAM_RADIUS = 0.07f;

    public Tank3D(String name, Vector3f startPos, ColorRGBA color, AssetManager assetManager, Node rootNode) {
        this.name = name;
        this.assetManager = assetManager;
        this.primaryColor = color;

        this.tankNode = new Node("TankNode_" + name);
        this.turretNode = new Node("TurretNode");
        this.healthHudNode = new Node("HealthHudNode");

        buildCompactTankGeometry();
        buildHorizontalHealthBeam();

        tankNode.setLocalTranslation(startPos);
        rootNode.attachChild(tankNode);
    }

    private void buildCompactTankGeometry() {
        Material bodyMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        bodyMat.setBoolean("UseMaterialColors", true);
        bodyMat.setColor("Diffuse", primaryColor);
        bodyMat.setColor("Ambient", primaryColor.mult(0.6f));

        Material darkMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        darkMat.setBoolean("UseMaterialColors", true);
        darkMat.setColor("Diffuse", new ColorRGBA(0.18f, 0.18f, 0.22f, 1.0f));
        darkMat.setColor("Ambient", new ColorRGBA(0.15f, 0.15f, 0.18f, 1.0f));

        Box bodyBox = new Box(0.6f, 0.22f, 0.8f);
        Geometry bodyGeo = new Geometry("Chassis", bodyBox);
        bodyGeo.setMaterial(bodyMat);
        bodyGeo.setLocalTranslation(0, 0.32f, 0);
        tankNode.attachChild(bodyGeo);

        Box trackBox = new Box(0.18f, 0.25f, 0.9f);

        Geometry leftTrack = new Geometry("LeftTrack", trackBox);
        leftTrack.setMaterial(darkMat);
        leftTrack.setLocalTranslation(-0.75f, 0.25f, 0);
        tankNode.attachChild(leftTrack);

        Geometry rightTrack = new Geometry("RightTrack", trackBox);
        rightTrack.setMaterial(darkMat);
        rightTrack.setLocalTranslation(0.75f, 0.25f, 0);
        tankNode.attachChild(rightTrack);

        Box turretBox = new Box(0.4f, 0.2f, 0.45f);
        Geometry turretGeo = new Geometry("Turret", turretBox);
        turretGeo.setMaterial(bodyMat);
        turretGeo.setLocalTranslation(0, 0.2f, 0);
        turretNode.attachChild(turretGeo);

        Cylinder barrelCyl = new Cylinder(16, 16, 0.08f, 1.1f, true);
        Geometry barrelGeo = new Geometry("Barrel", barrelCyl);
        barrelGeo.setMaterial(darkMat);
        barrelGeo.setLocalTranslation(0, 0.2f, -0.8f);
        turretNode.attachChild(barrelGeo);

        turretNode.setLocalTranslation(0, 0.54f, 0);
        tankNode.attachChild(turretNode);
    }

    private void buildHorizontalHealthBeam() {
        // ساخت استوانه افقی نئونی
        Cylinder beamMesh = new Cylinder(16, 16, BEAM_RADIUS, MAX_BEAM_LENGTH, true);
        healthBeamBar = new Geometry("HealthBeam", beamMesh);

        beamMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        beamMat.setColor("Color", new ColorRGBA(0.1f, 1.0f, 0.2f, 1.0f)); // سبز نئونی اولیه
        healthBeamBar.setMaterial(beamMat);

        // چرخش استوانه به حالت افقی (راست به چپ)
        Quaternion rot = new Quaternion().fromAngleAxis(com.jme3.math.FastMath.HALF_PI, Vector3f.UNIT_Y);
        healthBeamBar.setLocalRotation(rot);

        healthHudNode.attachChild(healthBeamBar);
        healthHudNode.setLocalTranslation(0, 1.75f, 0); // قرارگیری بالای سقف تانک
        healthHudNode.setCullHint(Spatial.CullHint.Always); // در ابتدا مخفی است تا نشانه‌گیری شود
        tankNode.attachChild(healthHudNode);
    }

    public void update(float tpf, Spatial obstacles, Camera cam) {
        if (!alive) return;

        // چرخش نوار به سمت دید دوربین
        if (cam != null && healthHudNode.getCullHint() != Spatial.CullHint.Always) {
            healthHudNode.setLocalRotation(cam.getRotation());
        }

        if (rotatingLeft) currentYaw += rotateSpeed * tpf;
        if (rotatingRight) currentYaw -= rotateSpeed * tpf;

        Quaternion rot = new Quaternion().fromAngleAxis(currentYaw, Vector3f.UNIT_Y);
        tankNode.setLocalRotation(rot);

        Vector3f forward = rot.getRotationColumn(2).negate();
        Vector3f moveDelta = Vector3f.ZERO;

        if (movingForward) {
            moveDelta = forward.mult(moveSpeed * tpf);
        } else if (movingBackward) {
            moveDelta = forward.mult(-moveSpeed * 0.6f * tpf);
        }

        if (!moveDelta.equals(Vector3f.ZERO)) {
            if (!checkCollision(moveDelta, obstacles)) {
                tankNode.move(moveDelta);
            }
        }

        if (invincible) {
            long elapsed = System.currentTimeMillis() - invincibleStartTime;
            if (elapsed >= INVINCIBLE_DURATION) {
                invincible = false;
                tankNode.setCullHint(Spatial.CullHint.Inherit);
            } else {
                boolean visible = (elapsed / 100) % 2 == 0;
                tankNode.setCullHint(visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
            }
        }
    }

    public void setHealthBeamVisible(boolean visible) {
        if (!alive) {
            healthHudNode.setCullHint(Spatial.CullHint.Always);
            return;
        }
        healthHudNode.setCullHint(visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    public boolean takeDamage() {
        if (!alive || invincible) return false;

        health--;
        invincible = true;
        invincibleStartTime = System.currentTimeMillis();

        if (health == 2) {
            // ۱ ضربه: طول به ۲/۳ کاهش یافته و زرد می‌شود
            healthBeamBar.setLocalScale(1f, 1f, 0.66f);
            beamMat.setColor("Color", new ColorRGBA(1.0f, 0.85f, 0.0f, 1.0f));
        } else if (health == 1) {
            // ۲ ضربه: طول به ۱/۳ کاهش یافته و قرمز می‌شود
            healthBeamBar.setLocalScale(1f, 1f, 0.33f);
            beamMat.setColor("Color", new ColorRGBA(1.0f, 0.15f, 0.15f, 1.0f));
        } else if (health <= 0) {
            // ۳ ضربه: نابودی و تبدیل به لاشه تانک
            turnIntoWreck();
            return true;
        }
        return false;
    }

    private void turnIntoWreck() {
        this.alive = false;
        healthHudNode.removeFromParent();

        // تغییر متریال کل تانک به آهن قراضه و سوخته
        Material burntMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        burntMat.setBoolean("UseMaterialColors", true);
        burntMat.setColor("Diffuse", new ColorRGBA(0.12f, 0.12f, 0.13f, 1.0f));
        burntMat.setColor("Ambient", new ColorRGBA(0.08f, 0.08f, 0.08f, 1.0f));

        tankNode.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial sp) {
                if (sp instanceof Geometry) {
                    ((Geometry) sp).setMaterial(burntMat);
                }
            }
        });

        // کج شدن برجک و لوله تانک به عنوان لاشه
        turretNode.setLocalRotation(new Quaternion().fromAngles(0.25f, 0.45f, -0.3f));
        turretNode.setLocalTranslation(0.1f, 0.45f, 0.1f);
    }

    private boolean checkCollision(Vector3f moveDelta, Spatial obstacles) {
        if (obstacles == null) return false;

        Vector3f dirNorm = moveDelta.normalize();
        float moveDistance = moveDelta.length();

        Vector3f pos = tankNode.getLocalTranslation();
        Vector3f rayOriginCenter = pos.add(0, 0.5f, 0);
        Vector3f rightVec = tankNode.getLocalRotation().getRotationColumn(0);

        Vector3f[] origins = new Vector3f[]{
                rayOriginCenter,
                rayOriginCenter.add(rightVec.mult(0.55f)),
                rayOriginCenter.add(rightVec.mult(-0.55f))
        };

        for (Vector3f origin : origins) {
            Ray ray = new Ray(origin, dirNorm);
            CollisionResults results = new CollisionResults();
            obstacles.collideWith(ray, results);

            if (results.size() > 0) {
                float dist = results.getClosestCollision().getDistance();
                if (dist <= (COLLISION_RADIUS + moveDistance)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setInitialYaw(float yawAngle) {
        this.currentYaw = yawAngle;
        Quaternion rot = new Quaternion().fromAngleAxis(currentYaw, Vector3f.UNIT_Y);
        tankNode.setLocalRotation(rot);
    }

    public Vector3f getMuzzlePosition() {
        Vector3f forward = tankNode.getLocalRotation().getRotationColumn(2).negate();
        return tankNode.getLocalTranslation().add(0, 0.54f, 0).addLocal(forward.mult(0.9f));
    }

    public Vector3f getForwardVector() {
        return tankNode.getLocalRotation().getRotationColumn(2).negate();
    }

    public void setMovingForward(boolean val) { this.movingForward = val; }
    public void setMovingBackward(boolean val) { this.movingBackward = val; }
    public void setRotatingLeft(boolean val) { this.rotatingLeft = val; }
    public void setRotatingRight(boolean val) { this.rotatingRight = val; }

    public Node getTankNode() { return tankNode; }
    public Vector3f getPosition() { return tankNode.getLocalTranslation(); }
    public boolean isAlive() { return alive; }
    public String getName() { return name; }
}