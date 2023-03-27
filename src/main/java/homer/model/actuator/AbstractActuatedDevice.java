package homer.model.actuator;

import java.time.Duration;
import java.util.Objects;

import homer.api.DeviceState;
import homer.api.state.ActuatedDeviceState;

/**
 * Abstract implementation of an {@link ActuatedDevice}.
 */
public class AbstractActuatedDevice implements ActuatedDevice {

    private final Actuator actuator;

    /**
     * Creates a {@link AbstractActuatedDevice} whose position is controlled by an
     * {@link Actuator}.
     * 
     * @param actuator The {@link Actuator} controlling the device.
     */
    public AbstractActuatedDevice(final Actuator actuator) {
        this.actuator = Objects.requireNonNull(actuator);
    }

    @Override
    public final ActuatedDeviceState getState() {
        return new ActuatedDeviceState(this.actuator.getPosition(), this.actuator.getBounds());
    }

    @Override
    public final void setState(final DeviceState state) {
        Objects.requireNonNull(state);
        if (state instanceof ActuatedDeviceState) {
            this.actuator.command(((ActuatedDeviceState) state).getPosition());
        } else {
            throw new IllegalArgumentException(
                    "State expected " + ActuatedDeviceState.class + " but got " + state.getClass());
        }
    }

    @Override
    public final void updateTick(final Duration deltaTime) {
        this.actuator.updateTick(deltaTime);
    }

}
