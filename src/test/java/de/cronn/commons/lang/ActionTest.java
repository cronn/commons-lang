package de.cronn.commons.lang;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

class ActionTest {

	private final AtomicInteger numberOfExecutions = new AtomicInteger();

	@Test
	void testToSupplier() {
		Action action = numberOfExecutions::incrementAndGet;

		Void result = action.toSupplier().get();

		assertThat(result).isNull();
		assertThat(numberOfExecutions.get()).isEqualTo(1);
	}

	@Test
	void testToSupplier_catchesRuntimeExceptionWithoutWrapping() {
		Action action = () -> {
			throw new IllegalArgumentException("some test exception");
		};

		Supplier<Void> supplier = action.toSupplier();

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(supplier::get)
			.withMessage("some test exception")
			.withNoCause();
	}

	@Test
	void testToCallable() throws Exception {
		Action action = numberOfExecutions::incrementAndGet;

		Void result = action.toCallable().call();

		assertThat(result).isNull();
		assertThat(numberOfExecutions.get()).isEqualTo(1);
	}

}

