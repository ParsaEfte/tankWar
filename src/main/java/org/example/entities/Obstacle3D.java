package org.example.entities;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Obstacle3D {

    public enum Type {
        WALL_FULL("Models/template-wall.glb"),
        WALL_HALF("Models/template-wall-half.glb"),
        WALL_CORNER("Models/template-wall-corner.glb"),
        WALL_DETAIL("Models/template-wall-detail-a.glb"),
        GATE_METAL("Models/gate-metal-bars.glb"),
        CORNER_PILLAR("Models/template-corner.glb");

        private final String modelPath;

        Type(String modelPath) {
            this.modelPath = modelPath;
        }

        public String getModelPath() {
            return modelPath;
        }
    }

    private final Spatial spatial;
    private final Type type;

    public Obstacle3D(AssetManager assetManager, Node parentNode, Type type, Vector3f position, float rotationY, float scale) {
        this.type = type;
        this.spatial = assetManager.loadModel(type.getModelPath());

        this.spatial.setLocalTranslation(position);
        this.spatial.setLocalScale(scale);

        if (rotationY != 0) {
            Quaternion rot = new Quaternion().fromAngleAxis(rotationY, Vector3f.UNIT_Y);
            this.spatial.setLocalRotation(rot);
        }

        this.spatial.setModelBound(new BoundingBox());
        this.spatial.updateModelBound();

        parentNode.attachChild(this.spatial);
    }

    public Spatial getSpatial() {
        return spatial;
    }

    public Type getType() {
        return type;
    }

    public Vector3f getPosition() {
        return spatial.getLocalTranslation();
    }
}