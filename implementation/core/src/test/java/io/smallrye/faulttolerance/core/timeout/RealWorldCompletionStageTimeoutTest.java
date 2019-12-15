package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Percentage.withPercentage;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.assertj.core.data.Percentage;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.stopwatch.SystemStopwatch;
import io.smallrye.faulttolerance.core.util.TestException;

public class RealWorldCompletionStageTimeoutTest {

    private static final Percentage tolerance = withPercentage(30);

    private ScheduledExecutorService executor;
    private ScheduledExecutorTimeoutWatcher watcher;
    private ExecutorService taskExecutor;

    private Stopwatch stopwatch = new SystemStopwatch();

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadScheduledExecutor();
        watcher = new ScheduledExecutorTimeoutWatcher(executor);

        taskExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        taskExecutor.shutdownNow();

        executor.awaitTermination(1, TimeUnit.SECONDS);
        taskExecutor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnRightAway() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        FaultToleranceStrategy<CompletionStage<String>> timeout = new CompletionStageTimeout<>(
                invocation(),
                "completion stage timeout", 1000, watcher, taskExecutor, null);

        assertThat(timeout.apply(new InvocationContext<>(() -> {
            Thread.sleep(200);
            return completedStage("foobar");
        })).toCompletableFuture().get()).isEqualTo("foobar");
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(200, tolerance);
    }

    @Test
    public void shouldPropagateMethodError() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        FaultToleranceStrategy<CompletionStage<String>> timeout = new CompletionStageTimeout<>(
                invocation(),
                "completion stage timeout", 1000, watcher, taskExecutor, null);

        assertThatThrownBy(timeout.apply(new InvocationContext<>(() -> {
            Thread.sleep(200);
            throw new TestException();
        })).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(200, tolerance);
    }

    @Test
    public void shouldPropagateCompletionStageError() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        FaultToleranceStrategy<CompletionStage<String>> timeout = new CompletionStageTimeout<>(
                invocation(),
                "completion stage timeout", 1000, watcher, taskExecutor, null);

        assertThatThrownBy(timeout.apply(new InvocationContext<>(() -> {
            Thread.sleep(200);
            return failedStage(new TestException());
        })).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(200, tolerance);
    }

    @Test
    public void shouldTimeOut() throws Exception {
        RunningStopwatch runningStopwatch = stopwatch.start();

        FaultToleranceStrategy<CompletionStage<String>> timeout = new CompletionStageTimeout<>(
                invocation(),
                "completion stage timeout", 500, watcher, taskExecutor, null);

        assertThatThrownBy(timeout.apply(new InvocationContext<>(() -> {
            Thread.sleep(1000);
            return completedStage("foobar");
        })).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class);
        assertThat(runningStopwatch.elapsedTimeInMillis()).isCloseTo(500, tolerance);
    }
}
