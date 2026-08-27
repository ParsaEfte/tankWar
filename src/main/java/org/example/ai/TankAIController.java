package org.example.ai;

import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import org.example.entities.Tank3D;
import org.example.game.TankCombatManager;
import org.example.util.GameLogger;

import java.util.List;
import java.util.Random;

public class TankAIController {

    private final Tank3D aiTank;
    private final TankCombatManager combatManager;
    private final Random random = new Random();

    private float shootTimer = 0f;
    private static final float SHOOT_INTERVAL = 1.8f;
    private static final float VISION_RANGE = 45.0f;
    private static final float WALL_AVOID_DIST = 2.4f;

    private enum AIState {
        PATROL_AND_NAVIGATE,
        CHASE_AND_FIRE
    }

    private AIState currentState = AIState.PATROL_AND_NAVIGATE;
    private float steerCooldown = 0f;
    private boolean forceTurnRight = true;

    public TankAIController(Tank3D aiTank, TankCombatManager combatManager) {
        this.aiTank = aiTank;
        this.combatManager = combatManager;
        this.forceTurnRight = random.nextBoolean();
    }

    public void update(float tpf, List<Tank3D> allTanks, Spatial wallsNode) {
        if (!aiTank.isAlive()) {
            aiTank.setMovingForward(false);
            aiTank.setRotatingLeft(false);
            aiTank.setRotatingRight(false);
            return;
        }

        Vector3f aiPos = aiTank.getPosition();
        Vector3f forward = aiTank.getForwardVector().setY(0).normalizeLocal();
        Vector3f right = aiTank.getTankNode().getLocalRotation().getRotationColumn(0).setY(0).normalizeLocal();

        Tank3D target = findNearestTarget(allTanks);
        boolean hasSight = false;

        if (target != null) {
            float distToTarget = aiPos.distance(target.getPosition());
            if (distToTarget <= VISION_RANGE && hasClearLineOfSight(aiPos, target.getPosition(), wallsNode)) {
                hasSight = true;
                currentState = AIState.CHASE_AND_FIRE;
            } else {
                currentState = AIState.PATROL_AND_NAVIGATE;
            }
        } else {
            currentState = AIState.PATROL_AND_NAVIGATE;
        }

        float forwardObstacleDist = checkRayDistance(aiPos, forward, wallsNode);
        float leftObstacleDist = checkRayDistance(aiPos, forward.add(right.negate()).normalizeLocal(), wallsNode);
        float rightObstacleDist = checkRayDistance(aiPos, forward.add(right).normalizeLocal(), wallsNode);

        steerCooldown -= tpf;

        if (currentState == AIState.CHASE_AND_FIRE && target != null) {
            Vector3f toTarget = target.getPosition().subtract(aiPos).setY(0).normalizeLocal();
            float dotForward = forward.dot(toTarget);
            float dotRight = right.dot(toTarget);

            if (dotForward < 0.96f) {
                aiTank.setRotatingRight(dotRight > 0);
                aiTank.setRotatingLeft(dotRight <= 0);
            } else {
                aiTank.setRotatingLeft(false);
                aiTank.setRotatingRight(false);
            }

            aiTank.setMovingForward(forwardObstacleDist > 2.0f && aiPos.distance(target.getPosition()) > 5.0f);

            shootTimer += tpf;
            if (dotForward > 0.90f && shootTimer >= SHOOT_INTERVAL) {
                shootTimer = 0f;
                combatManager.spawnBullet(aiTank.getMuzzlePosition(), aiTank.getForwardVector(), aiTank);
                GameLogger.combat("AI_FIRE", aiTank.getName() + " fired at " + target.getName());
            }

        } else {
            if (forwardObstacleDist < WALL_AVOID_DIST) {
                aiTank.setMovingForward(false);

                if (steerCooldown <= 0) {
                    forceTurnRight = rightObstacleDist >= leftObstacleDist;
                    steerCooldown = 0.6f + random.nextFloat() * 0.4f;
                }

                if (forceTurnRight) {
                    aiTank.setRotatingRight(true);
                    aiTank.setRotatingLeft(false);
                } else {
                    aiTank.setRotatingLeft(true);
                    aiTank.setRotatingRight(false);
                }
            } else {
                aiTank.setMovingForward(true);

                if (leftObstacleDist < 1.4f) {
                    aiTank.setRotatingRight(true);
                    aiTank.setRotatingLeft(false);
                } else if (rightObstacleDist < 1.4f) {
                    aiTank.setRotatingLeft(true);
                    aiTank.setRotatingRight(false);
                } else {
                    aiTank.setRotatingLeft(false);
                    aiTank.setRotatingRight(false);
                }
            }
        }
    }

    private float checkRayDistance(Vector3f origin, Vector3f dir, Spatial wallsNode) {
        if (wallsNode == null) return Float.MAX_VALUE;

        Ray ray = new Ray(origin.add(0, 0.45f, 0), dir);
        CollisionResults results = new CollisionResults();
        wallsNode.collideWith(ray, results);

        if (results.size() > 0) {
            return results.getClosestCollision().getDistance();
        }
        return Float.MAX_VALUE;
    }

    private Tank3D findNearestTarget(List<Tank3D> allTanks) {
        Tank3D nearest = null;
        float minDist = Float.MAX_VALUE;
        Vector3f myPos = aiTank.getPosition();

        for (Tank3D tank : allTanks) {
            if (tank != aiTank && tank.isAlive()) {
                float dist = myPos.distance(tank.getPosition());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = tank;
                }
            }
        }
        return nearest;
    }

    private boolean hasClearLineOfSight(Vector3f start, Vector3f target, Spatial wallsNode) {
        if (wallsNode == null) return true;

        Vector3f dir = target.subtract(start);
        float distance = dir.length();
        Ray ray = new Ray(start.add(0, 0.5f, 0), dir.normalizeLocal());

        CollisionResults results = new CollisionResults();
        wallsNode.collideWith(ray, results);

        if (results.size() > 0) {
            return results.getClosestCollision().getDistance() >= distance;
        }
        return true;
    }
}