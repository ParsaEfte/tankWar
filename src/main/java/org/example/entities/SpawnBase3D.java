package org.example.entities;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;

public class SpawnBase3D {

    private final Spatial spatial;
    private final Vector3f basePosition;
    private final String doorSide;
    private final float scale;

    public SpawnBase3D(AssetManager assetManager, Node parentNode, Material sharedMat, Vector3f position, String doorSide, float scale) {
        this.basePosition = position;
        this.doorSide = doorSide.toLowerCase();
        this.scale = scale;

        // بارگذاری مدل آشیانه corridor-end
        this.spatial = assetManager.loadModel("Models/corridor-end.glb");
        this.spatial.setLocalTranslation(position);
        this.spatial.setLocalScale(scale);

        // تنظیم جهت دهانه باز پایگاه بر اساس doorSide
        float rotY = getRotationForDoorSide(this.doorSide);
        Quaternion rot = new Quaternion().fromAngleAxis(rotY, Vector3f.UNIT_Y);
        this.spatial.setLocalRotation(rot);

        // اعمال متریال با بافت صحیح
        if (sharedMat != null) {
            this.spatial.depthFirstTraversal(new SceneGraphVisitor() {
                @Override
                public void visit(Spatial sp) {
                    if (sp instanceof Geometry) {
                        ((Geometry) sp).setMaterial(sharedMat);
                    }
                }
            });
        }

        this.spatial.setModelBound(new BoundingBox());
        this.spatial.updateModelBound();

        parentNode.attachChild(this.spatial);
    }

    private float getRotationForDoorSide(String side) {
        return switch (side) {
            case "top", "north" -> FastMath.PI;         // دهانه رو به شمال (-Z)
            case "bottom", "south" -> 0f;               // دهانه رو به جنوب (+Z)
            case "left", "west" -> -FastMath.HALF_PI;   // دهانه رو به غرب (-X)
            case "right", "east" -> FastMath.HALF_PI;   // دهانه رو به شرق (+X)
            default -> 0f;
        };
    }

    /**
     * برگرداندن موقعیت مناسب برای اسپاون تانک در داخل پایگاه
     */
    public Vector3f getSpawnLocation() {
        return new Vector3f(basePosition.x, 0.0f, basePosition.z);
    }

    public Spatial getSpatial() {
        return spatial;
    }

    public Vector3f getBasePosition() {
        return basePosition;
    }
}