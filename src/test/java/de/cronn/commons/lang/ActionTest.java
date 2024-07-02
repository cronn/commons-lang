package de.cronn.commons.lang;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

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
	void testToCallable() throws Exception {
		Action action = numberOfExecutions::incrementAndGet;

		Void result = action.toCallable().call();

		assertThat(result).isNull();
		assertThat(numberOfExecutions.get()).isEqualTo(1);
	}

}

