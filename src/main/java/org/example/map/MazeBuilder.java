package org.example.map;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

public class MazeBuilder {

    public static final float TILE_SIZE = 6.0f;
    public static final float MODEL_SCALE = 1.5f;

    private static final String[][] MAZE_MAP = {
            {"L0", "W0", "W0", "W0", "W0", "W0", "W0", "W0", "W0", "W0", "L1"},
            {"W3", "E0", "W0", "L0", "W0", "T0", "W0", "L1", "W0", "E1", "W1"},
            {"W3", "C0", "W3", "C0", "W3", "C0", "W1", "C0", "W1", "C0", "W1"},
            {"W3", "T3", "W2", "X0", "W0", "T1", "W2", "X0", "W0", "T1", "W1"},
            {"W3", "C0", "W3", "C0", "W3", "G0", "W1", "C0", "W1", "C0", "W1"},
            {"W3", "L3", "W2", "T2", "W0", "R0", "W2", "T2", "W0", "L2", "W1"},
            {"W3", "C0", "W3", "C0", "W3", "G2", "W1", "C0", "W1", "C0", "W1"},
            {"W3", "T3", "W2", "X0", "W2", "T1", "W0", "X0", "W2", "T1", "W1"},
            {"W3", "C0", "W3", "C0", "W3", "C0", "W1", "C0", "W1", "C0", "W1"},
            {"W3", "E3", "W2", "L3", "W2", "T2", "W2", "L2", "W2", "E2", "W1"},
            {"L3", "W2", "W2", "W2", "W2", "W2", "W2", "W2", "W2", "W2", "L2"}
    };

    private final AssetManager assetManager;
    private final Node rootNode;
    private final Node floorNode = new Node("MazeFloorNode");
    private final Node wallsNode = new Node("WallsNode");
    private final Node ceilingNode = new Node("CeilingNode");

    private Material sharedCastleMaterial;

    public MazeBuilder(AssetManager assetManager, Node rootNode) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;

        rootNode.attachChild(floorNode);
        rootNode.attachChild(wallsNode);
        rootNode.attachChild(ceilingNode);

        setupSharedMaterial();
    }

    private void setupSharedMaterial() {
        try {
            TextureKey key = new TextureKey("Models/Textures/colormap.png", false);
            Texture colormap = assetManager.loadTexture(key);
            colormap.setMagFilter(Texture.MagFilter.Nearest);
            colormap.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            colormap.setWrap(Texture.WrapMode.Repeat);

            sharedCastleMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            sharedCastleMaterial.setTexture("DiffuseMap", colormap);
            sharedCastleMaterial.setBoolean("UseMaterialColors", true);
            sharedCastleMaterial.setColor("Diffuse", new ColorRGBA(0.85f, 0.85f, 0.9f, 1.0f));
            sharedCastleMaterial.setColor("Ambient", new ColorRGBA(0.5f, 0.5f, 0.6f, 1.0f));
            sharedCastleMaterial.setColor("Specular", ColorRGBA.Black);
            sharedCastleMaterial.setFloat("Shininess", 1f);
            sharedCastleMaterial.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
        } catch (Exception e) {
            sharedCastleMaterial = null;
        }
    }

    private void applyMaterial(Spatial spatial) {
        if (sharedCastleMaterial == null) return;
        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial sp) {
                if (sp instanceof Geometry geo) {
                    geo.setMaterial(sharedCastleMaterial);
                }
            }
        });
    }

    public void build() {
        buildCeiling();
        buildMazeGrid();
    }

    private void buildMazeGrid() {
        int rows = MAZE_MAP.length;
        int cols = MAZE_MAP[0].length;
        float offsetX = (cols * TILE_SIZE) / 2.0f;
        float offsetZ = (rows * TILE_SIZE) / 2.0f;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String tileCode = MAZE_MAP[r][c];
                char type = tileCode.charAt(0);
                int rotIndex = Character.getNumericValue(tileCode.charAt(1));
                float rotY = rotIndex * FastMath.HALF_PI;

                float x = (c * TILE_SIZE) - offsetX;
                float z = (r * TILE_SIZE) - offsetZ;
                Vector3f pos = new Vector3f(x, 0, z);

                spawnFloor(pos);
                spawnPiece(type, pos, rotY);
            }
        }
    }

    private void spawnFloor(Vector3f pos) {
        try {
            Spatial floor = assetManager.loadModel("Models/template-floor-detail-a.glb");
            floor.setLocalTranslation(pos);
            floor.setLocalScale(MODEL_SCALE);
            applyMaterial(floor);
            floorNode.attachChild(floor);
        } catch (Exception ignored) {}
    }

    private void spawnPiece(char type, Vector3f pos, float rotY) {
        String modelPath = switch (type) {
            case 'W' -> "Models/template-wall.glb";
            case 'H' -> "Models/template-wall-half.glb";
            case 'L' -> "Models/corridor-corner.glb";
            case 'C' -> "Models/corridor.glb";
            case 'T' -> "Models/corridor-junction.glb";
            case 'X' -> "Models/corridor-intersection.glb";
            case 'E' -> "Models/corridor-end.glb";
            case 'G' -> "Models/gate-metal-bars.glb";
            case 'R' -> "Models/room-small.glb";
            case 'D' -> "Models/template-detail.glb";
            default -> null;
        };

        if (modelPath != null) {
            try {
                Spatial piece = assetManager.loadModel(modelPath);
                piece.setLocalTranslation(pos);
                piece.setLocalScale(MODEL_SCALE);
                if (rotY != 0) {
                    Quaternion rot = new Quaternion().fromAngleAxis(rotY, Vector3f.UNIT_Y);
                    piece.setLocalRotation(rot);
                }
                applyMaterial(piece);
                wallsNode.attachChild(piece);
            } catch (Exception ignored) {
                try {
                    Spatial fallback = assetManager.loadModel("Models/template-wall.glb");
                    fallback.setLocalTranslation(pos);
                    fallback.setLocalScale(MODEL_SCALE);
                    applyMaterial(fallback);
                    wallsNode.attachChild(fallback);
                } catch (Exception ignored2) {}
            }
        }
    }

    private void buildCeiling() {
        int rows = MAZE_MAP.length;
        int cols = MAZE_MAP[0].length;
        float offsetX = (cols * TILE_SIZE) / 2.0f;
        float offsetZ = (rows * TILE_SIZE) / 2.0f;
        float ceilingHeight = 4.05f;

        Quaternion ceilingRot = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float x = (c * TILE_SIZE) - offsetX;
                float z = (r * TILE_SIZE) - offsetZ;

                try {
                    Spatial roofTile = assetManager.loadModel("Models/template-wall-top.glb");
                    roofTile.setLocalTranslation(x, ceilingHeight, z);
                    roofTile.setLocalRotation(ceilingRot);
                    roofTile.setLocalScale(MODEL_SCALE * 1.05f);

                    applyMaterial(roofTile);
                    ceilingNode.attachChild(roofTile);
                } catch (Exception ignored) {}
            }
        }
    }

    public static Vector3f getTileWorldPos(int r, int c) {
        int rows = MAZE_MAP.length;
        int cols = MAZE_MAP[0].length;
        float offsetX = (cols * TILE_SIZE) / 2.0f;
        float offsetZ = (rows * TILE_SIZE) / 2.0f;
        return new Vector3f((c * TILE_SIZE) - offsetX, 0, (r * TILE_SIZE) - offsetZ);
    }

    public Node getWallsNode() {
        return wallsNode;
    }
}