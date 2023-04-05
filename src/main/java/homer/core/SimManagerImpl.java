package homer.core;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import homer.controller.Controller;
import homer.controller.impl.electricalmeter.ElectricalMeterImpl;
import homer.view.sim.SimManagerView;

/**
 * Implementation of {@link SimManagerViewObserver}.
 */
public final class SimManagerImpl implements SimManager, SimManagerViewObserver {

    private static final Duration DEFAULT_SIM_STEP_PERIOD = Duration.of(10, TimeUnit.MILLISECONDS.toChronoUnit());
    private static final Duration DEFAULT_REAL_STEP_PERIOD = Duration.of(10, TimeUnit.MILLISECONDS.toChronoUnit());
    private static final long MIN_TIME_RATE = 1;
    private static final Duration REAL_STEP_PERIOD = DEFAULT_REAL_STEP_PERIOD;
    private static final Duration SIM_STEP_PERIOD = DEFAULT_SIM_STEP_PERIOD;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final SimManagerView view;
    private final Runnable loopRunnable;
    private final Set<DiscreteObject> observers = new HashSet<>();
    private Optional<ScheduledFuture<?>> loopHandle = Optional.empty();
    private long timeRate = MIN_TIME_RATE;

    /**
     * Creates a new {@link SimManagerImpl} with the given {@code Controller} and
     * starts running it indefinitely.
     * 
     * @param view       the simulation manager view.
     * @param controller the controller.
     * @param meter      the electrical meter.
     */
    public SimManagerImpl(final SimManagerView view, final Controller controller, final ElectricalMeterImpl meter) {
        this.view = view;
        updateView();
        this.loopRunnable = () -> {
            final var dt = getSimStepPeriod().multipliedBy(timeRate);
            controller.updateTick(dt);
            meter.updateTick(dt);
            controller.getDeviceManager().getDevices().values().stream()
                    .filter(DiscreteObject.class::isInstance)
                    .forEach(d -> ((DiscreteObject) d).updateTick(dt));
            view.setDateTime(controller.getClock().getDateTime());
            this.observers.forEach(o -> o.updateTick(dt));
        };
        resume();
    }

    @Override
    public void resume() {
        if (this.loopHandle.isEmpty()) {
            this.loopHandle = Optional.of(scheduler.scheduleAtFixedRate(this.loopRunnable, 0,
                    REAL_STEP_PERIOD.toNanos(), TimeUnit.NANOSECONDS));
        }
    }

    @Override
    public void pause() {
        if (this.loopHandle.isPresent()) {
            this.loopHandle.get().cancel(false);
            this.loopHandle = Optional.empty();
        }
    }

    @Override
    public void setTimeRate(final long timeRate) {
        this.timeRate = Math.max(MIN_TIME_RATE, timeRate);
        updateView();
    }

    @Override
    public void addObserver(final DiscreteObject observer) {
        this.observers.add(observer);
    }

    private Duration getSimStepPeriod() {
        return SIM_STEP_PERIOD;
    }

    private void updateView() {
        this.view.setTimeRate(this.timeRate);
    }
}
