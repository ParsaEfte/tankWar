package org.example.entities;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

public class Explosion3D {

    private final Node pivotNode;
    private final Geometry geometry;
    private final Texture[] frames;
    private final Material material;

    private static final float FRAME_DURATION = 0.045f;
    private static final float SIZE = 3.0f;

    private float timer = 0f;
    private int currentFrame = 0;
    private boolean finished = false;

    public Explosion3D(Vector3f hitPosition, Texture[] frames, AssetManager assetManager, Node rootNode) {
        this.frames = frames;
        this.pivotNode = new Node("ExplosionPivot");

        Quad quad = new Quad(SIZE, SIZE);
        this.geometry = new Geometry("ExplosionQuad", quad);

        // تنظیم مبدا کواد روی مرکز برای چرخش متقارن
        this.geometry.setLocalTranslation(-SIZE / 2f, -SIZE / 2f, 0);

        this.material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        this.material.setTexture("ColorMap", frames[0]);

        // تنظیمات شفافیت و رندر دوطرفه
        this.material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        this.material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        this.material.getAdditionalRenderState().setDepthWrite(false);

        this.geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
        this.geometry.setMaterial(this.material);

        this.pivotNode.attachChild(this.geometry);
        this.pivotNode.setLocalTranslation(hitPosition);
        rootNode.attachChild(this.pivotNode);
    }

    public void update(float tpf, Camera cam) {
        if (finished) return;

        // هم‌جهت کردن دقیق زاویه بیل‌بورد با دوربین
        pivotNode.setLocalRotation(cam.getRotation());

        timer += tpf;
        if (timer >= FRAME_DURATION) {
            timer = 0f;
            currentFrame++;

            if (currentFrame >= frames.length) {
                destroy();
                return;
            }

            material.setTexture("ColorMap", frames[currentFrame]);
        }
    }

    public void destroy() {
        if (finished) return;
        finished = true;
        pivotNode.removeFromParent();
    }

    public boolean isFinished() {
        return finished;
    }
}