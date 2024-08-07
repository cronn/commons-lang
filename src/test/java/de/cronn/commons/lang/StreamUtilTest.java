package de.cronn.commons.lang;

import static org.assertj.core.api.Assertions.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class StreamUtilTest {

	private static class TestException extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 1L;
	}

	@Test
	void testToModifiableList() {
		List<Integer> originalList = List.of(1, 2, 3);
		assertThat(originalList.stream().collect(StreamUtil.toModifiableList()))
			.isInstanceOf(ArrayList.class)
			.containsExactlyElementsOf(originalList);
	}

	@Test
	void testToSingleOptionalElement_throwsWhenNonSingleElementStream() throws Exception {
		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> Stream.of(1, 2).collect(StreamUtil.toSingleOptionalElement()))
			.withMessage("One or zero elements expected but got 2: [1, 2]");
	}

	@Test
	void testToSingleOptionalElement_collectSingleElementStream() throws Exception {
		Optional<Integer> collected = Stream.of(1).collect(StreamUtil.toSingleOptionalElement());
		assertThat(collected).hasValue(1);
	}

	@Test
	void testToSingleOptionalElement_collectEmptyStream() throws Exception {
		Optional<Object> collected = Stream.empty().collect(StreamUtil.toSingleOptionalElement());
		assertThat(collected).isNotPresent();
	}

	@Test
	void testToSingleElement_throwsWhenNonSingleElementStream() throws Exception {
		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> Stream.of(1, 2).collect(StreamUtil.toSingleElement()))
			.withMessage("Exactly one element expected but got 2: [1, 2]");
	}

	@Test
	void testToSingleElement_throwsWhenEmptyStream() throws Exception {
		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> Stream.empty().collect(StreamUtil.toSingleElement()))
			.withMessage("Exactly one element expected but got 0: []");
	}

	@Test
	void testToSingleElement_throwsUsingExceptionSupplier() throws Exception {
		assertThatExceptionOfType(TestException.class)
			.isThrownBy(() -> Stream.empty().collect(StreamUtil.toSingleElement(TestException::new)))
			.withMessage(null);
	}

	@Test
	void testToSingleElement_collectSingleElementStream() throws Exception {
		Integer collected = Stream.of(1).collect(StreamUtil.toSingleElement());
		assertThat(collected).isNotNull();
		assertThat(collected).isEqualTo(1);
	}

	@Test
	void testToLinkedHashMap_Simple() throws Exception {
		List<String> elements = List.of("first", "second", "third");
		Map<String, String> map = elements.stream().collect(StreamUtil.toLinkedHashMap(Function.identity()));
		assertThat(map.keySet()).containsExactlyElementsOf(elements);
		assertThat(map.values()).containsExactlyElementsOf(elements);
	}

	@Test
	void testToLinkedHashMap_WithNull() throws Exception {
		Map<String, String> input = new LinkedHashMap<>();
		input.put("first", "a");
		input.put("second", "b");
		input.put("third", null);
		Map<String, String> map = input.keySet().stream()
			.collect(StreamUtil.toLinkedHashMap(Function.identity(), input::get));
		assertThat(map.keySet()).containsExactlyElementsOf(input.keySet());
		assertThat(map.values()).containsExactlyElementsOf(input.values());
	}

	@Test
	void testToLinkedHashMap_LargeMap() throws Exception {
		List<String> elements = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			elements.add(String.valueOf(i));
		}
		Map<String, String> map = elements.stream()
			.collect(StreamUtil.toLinkedHashMap(Function.identity(), element -> String.valueOf(element.length())));
		assertThat(map.keySet()).containsExactlyElementsOf(elements);
	}

	@Test
	void testToLinkedHashMap_LargeMapWithNull_Parallel() throws Exception {
		List<Integer> elements = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			elements.add(Integer.valueOf(i));
		}
		Map<Integer, Integer> map = elements.stream()
			.parallel()
			.collect(StreamUtil.toLinkedHashMap(Function.identity(), element -> {
				if (element.intValue() % 100 == 0) return null;
				return element.hashCode();
			}));
		assertThat(map.keySet()).containsExactlyElementsOf(elements);
	}

	@Test
	void testToLinkedHashMap_Duplicates() throws Exception {
		List<String> elements = List.of("first", "second", "third", "first");

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> elements.stream()
				.collect(StreamUtil.toLinkedHashMap(Function.identity(), Function.identity())))
			.withMessage("Duplicate key 'first' with values 'first' and 'first'");
	}

	@Test
	void testToLinkedHashSet() throws Exception {
		List<String> elements = Arrays.asList("a", "b", "b", "c");
		Set<String> map = elements.stream().collect(StreamUtil.toLinkedHashSet());
		assertThat(new ArrayList<>(map)).containsExactly("a", "b", "c");
	}

	@Test
	void testGroupingBy() throws Exception {
		record Data(String key, int id) {
		}

		Data a1 = new Data("abc 123", 1);
		Data b1 = new Data("def 456", 2);
		Data a2 = new Data("abc 123", 3);
		Data b2 = new Data("def 456", 4);
		Data c1 = new Data("ghi 789", 5);

		assertThat(Stream.of(a1, b1, a2, b2, c1).collect(StreamUtil.groupingBy(Data::key)))
			.containsExactly(
				entry("abc 123", List.of(a1, a2)),
				entry("def 456", List.of(b1, b2)),
				entry("ghi 789", List.of(c1)));
	}

	@Test
	void testHasDuplicates() throws Exception {
		assertThat(StreamUtil.hasDuplicates(Stream.of(1, 2, 3))).isFalse();
		assertThat(StreamUtil.hasDuplicates(Stream.of(1, 2, 1, 3))).isTrue();
	}
}
