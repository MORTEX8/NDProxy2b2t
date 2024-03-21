package com.zenith.feature.pathing;

import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.util.math.MathHelper;
import lombok.NonNull;
import org.cloudburstmc.math.vector.Vector2f;

import java.util.Optional;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class Pathing {
    private static final MovementInputRequest DEFAULT_MOVEMENT_INPUT_REQUEST = new MovementInputRequest(Optional.empty(), Optional.empty(), Optional.empty(), Integer.MIN_VALUE);
    private @NonNull MovementInputRequest currentMovementInputRequest = DEFAULT_MOVEMENT_INPUT_REQUEST;

    public Pathing() {
        EVENT_BUS.subscribe(this,
                            // should be next to last in the tick handlers
                            // right before player simulation
                            // but after all modules that send movement inputs
                            of(ClientTickEvent.class, -10000, this::handleTick)
        );
    }

    /**
     * Interface to request movement on the next tick
     */

    public synchronized void moveReq(final MovementInputRequest movementInputRequest) {
        if (movementInputRequest.priority() <= currentMovementInputRequest.priority()) return;
        currentMovementInputRequest = movementInputRequest;
    }

    public synchronized void moveRot(final Input input, final float yaw, final float pitch, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        currentMovementInputRequest = new MovementInputRequest(Optional.of(input), Optional.of(yaw), Optional.of(pitch), priority);
    }

    public synchronized void moveYaw(final Input input, final float yaw, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        currentMovementInputRequest = new MovementInputRequest(Optional.of(input), Optional.of(yaw), Optional.empty(), priority);
    }

    public synchronized void moveRotTowards(final double x, final double z, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        final float yaw = yawToXZ(x, z);
        currentMovementInputRequest = new MovementInputRequest(Optional.of(forwardInput()), Optional.of(yaw), Optional.empty(), priority);
    }

    public synchronized void moveRotTowards(final double x, final double y, final double z, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        final Vector2f rotationTo = rotationTo(x, y, z);
        currentMovementInputRequest = new MovementInputRequest(Optional.of(forwardInput()), Optional.of(rotationTo.getX()), Optional.of(rotationTo.getY()), priority);
    }

    public synchronized void moveRotTowardsBlockPos(final int x, final int z, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        final float yaw = yawToXZ(x + 0.5, z + 0.5);
        currentMovementInputRequest = new MovementInputRequest(Optional.of(forwardInput()), Optional.of(yaw), Optional.empty(), priority);
    }

    public synchronized void moveRotSneakTowardsBlockPos(final int x, final int z, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        final float yaw = yawToXZ(x + 0.5, z + 0.5);
        currentMovementInputRequest = new MovementInputRequest(Optional.of(forwardSneakInput()), Optional.of(yaw), Optional.empty(), priority);
    }

    public synchronized void move(final Input input, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        currentMovementInputRequest = new MovementInputRequest(Optional.of(input), Optional.empty(), Optional.empty(), priority);
    }

    public synchronized void rotate(final float yaw, final float pitch, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        currentMovementInputRequest = new MovementInputRequest(Optional.empty(), Optional.of(yaw), Optional.of(pitch), priority);
    }

    public synchronized void rotateTowards(final double x, final double y, final double z, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        final Vector2f rotationTo = rotationTo(x, y, z);
        currentMovementInputRequest = new MovementInputRequest(Optional.empty(), Optional.of(rotationTo.getX()), Optional.of(rotationTo.getY()), priority);
    }

    public synchronized void rotateYaw(final float yaw, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        currentMovementInputRequest = new MovementInputRequest(Optional.empty(), Optional.of(yaw), Optional.empty(), priority);
    }

    public synchronized void rotatePitch(final float pitch, final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        currentMovementInputRequest = new MovementInputRequest(Optional.empty(), Optional.empty(), Optional.of(pitch), priority);
    }

    public synchronized void stop(final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        currentMovementInputRequest = new MovementInputRequest(Optional.empty(), Optional.empty(), Optional.empty(), priority);
    }

    public synchronized void jump(final int priority) {
        if (priority < currentMovementInputRequest.priority()) return;
        currentMovementInputRequest = new MovementInputRequest(Optional.of(jumpInput()), Optional.empty(), Optional.empty(), priority);
    }

    public synchronized void handleTick(final ClientTickEvent event) {
        if (currentMovementInputRequest != DEFAULT_MOVEMENT_INPUT_REQUEST) {
            MODULE.get(PlayerSimulation.class).doMovement(currentMovementInputRequest);
            currentMovementInputRequest = DEFAULT_MOVEMENT_INPUT_REQUEST;
        }
    }

    // todo: Pathing interface based on goals
    //  i.e. set a destination XYZ and use A* to path to it over multiple ticks

    /**
     * Helper methods for immediate movement mode
     */

    public static Input forwardInput() {
        return new Input(
            true,
            false,
            false,
            false,
            false,
            false,
            false
        );
    }

    public static Input forwardSneakInput() {
        return new Input(
            true,
            false,
            false,
            false,
            false,
            true,
            false
        );
    }

    public static Input jumpInput() {
        return new Input(
            false,
            false,
            false,
            false,
            true,
            false,
            false
        );
    }

    public static float yawToXZ(final double x, final double z) {
        final double dx = x - CACHE.getPlayerCache().getX();
        final double dz = z - CACHE.getPlayerCache().getZ();
        final double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        return (float) yaw;
    }

    public static Vector2f rotationTo(final double x, final double y, final double z) {
        final double dx = x - CACHE.getPlayerCache().getX();
        final double dy = y - (MODULE.get(PlayerSimulation.class).getEyeY());
        final double dz = z - CACHE.getPlayerCache().getZ();
        final double distance = Math.sqrt(dx * dx + dz * dz);
        final double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        final double pitch = -Math.toDegrees(Math.atan2(dy, distance));
        return Vector2f.from((float) yaw, (float) pitch);
    }

    public static Position getCurrentPlayerPos() {
        return new Position(MathHelper.round(CACHE.getPlayerCache().getX(), 5), MathHelper.round(CACHE.getPlayerCache().getY(), 5), MathHelper.round(CACHE.getPlayerCache().getZ(), 5));
    }
}
