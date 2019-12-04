package com.github.ladicek.oaken_ocean.core.circuit.breaker;

import static com.github.ladicek.oaken_ocean.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Before;
import org.junit.Test;

import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.stopwatch.TestStopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import com.github.ladicek.oaken_ocean.core.util.TestException;

public class CircuitBreakerTest {
    private static final SetOfThrowables testException = SetOfThrowables.create(Collections.singletonList(TestException.class));

    private TestStopwatch stopwatch;

    @Before
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void test1() throws Exception {
        SyncCircuitBreaker<String> cb = new SyncCircuitBreaker<>(invocation(), "test invocation", testException,
                1000, 4, 0.5, 2, stopwatch, null);

        // circuit breaker is closed
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar1"))).isEqualTo("foobar1");
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar2"))).isEqualTo("foobar2");
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> {
            throw new RuntimeException();
        }))).isExactlyInstanceOf(RuntimeException.class); // treated as success
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar3"))).isEqualTo("foobar3");
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar4"))).isEqualTo("foobar4");
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar5"))).isEqualTo("foobar5");
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar6"))).isEqualTo("foobar6");
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar7"))).isEqualTo("foobar7");
        // circuit breaker is half-open
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        stopwatch.setCurrentValue(0);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar8"))).isEqualTo("foobar8");
        // circuit breaker is half-open
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar9"))).isEqualTo("foobar9");
        // circuit breaker is closed
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar10"))).isEqualTo("foobar10");
    }

    @Test
    public void test2() throws Exception {
        SyncCircuitBreaker<String> cb = new SyncCircuitBreaker<>(invocation(), "test invocation", testException,
                1000, 4, 0.5, 2, stopwatch, null);

        // circuit breaker is closed
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar1"))).isEqualTo("foobar1");
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(TestException::doThrow)))
                .isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar2"))).isEqualTo("foobar2");
        // circuit breaker is open
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(() -> cb.apply(new SimpleInvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar3"))).isEqualTo("foobar3");
        // circuit breaker is half-open
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar4"))).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.apply(new SimpleInvocationContext<>(() -> "foobar5"))).isEqualTo("foobar5");
    }
}
