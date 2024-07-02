package de.cronn.commons.lang;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StreamUtil {

	private StreamUtil() {
	}

	public static <T> Collector<T, ?, Set<T>> toLinkedHashSet() {
		return Collectors.toCollection(LinkedHashSet::new);
	}

	public static <T> Collector<T, ?, List<T>> toModifiableList() {
		return Collectors.toCollection(ArrayList::new);
	}

	/**
	 * Drop-in replacement for {@link Collectors#groupingBy(Function)} which guarantees a deterministic order of the map
	 */
	public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(
		Function<? super T, ? extends K> classifier) {
		return Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList());
	}

	public static <T, K> Collector<T, ?, Map<K, T>> toLinkedHashMap(
		Function<? super T, ? extends K> keyMapper) {
		Function<T, T> identity = Function.identity();
		return toLinkedHashMap(keyMapper, identity);
	}

	public static <T, K, V> Collector<T, ?, Map<K, V>> toLinkedHashMap(
		Function<? super T, ? extends K> keyMapper,
		Function<? super T, ? extends V> valueMapper) {
		return new UniqueKeyLinkedHashMapCollector<>(keyMapper, valueMapper);
	}

	@FunctionalInterface
	public interface ExceptionSupplier<T> {
		RuntimeException get(List<T> foundElements);
	}

	public static <T> Collector<T, ?, Optional<T>> toSingleOptionalElement() {
		return toSingleOptionalElement(
			list -> new IllegalStateException("One or zero elements expected but got " + list.size() + ": " + list));
	}

	public static <T> Collector<T, ?, Optional<T>> toSingleOptionalElement(
		ExceptionSupplier<T> exceptionSupplier) {
		return Collectors.collectingAndThen(
			Collectors.toList(),
			list -> {
				int size = list.size();
				if (size > 1) {
					throw exceptionSupplier.get(list);
				}
				if (size == 1) {
					return Optional.of(list.get(0));
				}
				return Optional.empty();
			});
	}

	public static <T> Collector<T, ?, T> toSingleElement() {
		return toSingleElement((ExceptionSupplier<T>) null);
	}

	public static <T> Collector<T, ?, T> toSingleElement(
		Supplier<RuntimeException> exceptionSupplier) {
		return toSingleElement(list -> exceptionSupplier.get());
	}

	public static <T> Collector<T, ?, T> toSingleElement(ExceptionSupplier<T> exceptionSupplier) {
		return Collectors.collectingAndThen(
			Collectors.toList(),
			list -> {
				int size = list.size();
				if (size != 1) {
					if (exceptionSupplier != null) {
						throw exceptionSupplier.get(list);
					} else {
						throw new IllegalStateException("Exactly one element expected but got " + size + ": " + list);
					}
				}
				return list.get(0);
			});
	}

	public static <T> boolean hasDuplicates(Stream<T> entries) {
		Set<T> hashSet = new HashSet<>();
		return entries.anyMatch(e -> !hashSet.add(e));
	}

	@SuppressWarnings("ClassCanBeRecord")
	private static class UniqueKeyLinkedHashMapCollector<T, K, V>
		implements Collector<T, Map<K, V>, Map<K, V>> {
		private final Function<? super T, ? extends K> keyMapper;
		private final Function<? super T, ? extends V> valueMapper;

		UniqueKeyLinkedHashMapCollector(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends V> valueMapper) {
			this.keyMapper = keyMapper;
			this.valueMapper = valueMapper;
		}

		@Override
		public Set<Characteristics> characteristics() {
			return Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
		}

		@Override
		public BiConsumer<Map<K, V>, T> accumulator() {
			return (map, element) -> {
				K key = keyMapper.apply(element);
				V value = valueMapper.apply(element);
				accumulate(map, key, value);
			};
		}

		@Override
		public Supplier<Map<K, V>> supplier() {
			return LinkedHashMap::new;
		}

		@Override
		public BinaryOperator<Map<K, V>> combiner() {
			return (m1, m2) -> {
				for (Map.Entry<K, V> e : m2.entrySet()) {
					accumulate(m1, e.getKey(), e.getValue());
				}
				return m1;
			};
		}

		@Override
		public Function<Map<K, V>, Map<K, V>> finisher() {
			return Function.identity();
		}

		private void accumulate(Map<K, V> map, K key, V value) {
			V existing = map.putIfAbsent(key, value);
			if (existing != null) {
				String message = "Duplicate key '%s' with values '%s' and '%s'".formatted(key, value, existing);
				throw new IllegalArgumentException(message);
			}
		}
	}
}
