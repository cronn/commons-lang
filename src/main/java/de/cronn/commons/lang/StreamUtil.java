package de.cronn.commons.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StreamUtil {

  private StreamUtil() {}

  /**
   * Drop-in replacement for {@link Collectors#toUnmodifiableSet()} that guarantees a
   * stable/deterministic insertion order.
   *
   * @return a collector that accumulates elements into a sequenced set preserving encounter order
   */
  public static <T> Collector<T, ?, SequencedSet<T>> toLinkedHashSet() {
    return Collectors.toCollection(LinkedHashSet::new);
  }

  /**
   * Drop-in replacement for {@link Collectors#toList()} that guarantees a modifiable {@link List}.
   *
   * <p>Use this when you need to mutate the resulting list after collection.
   *
   * @return a collector that accumulates elements into a modifiable list
   */
  public static <T> Collector<T, ?, List<T>> toModifiableList() {
    return Collectors.toCollection(ArrayList::new);
  }

  /**
   * Drop-in replacement for {@link Collectors#groupingBy(Function)} which guarantees a
   * deterministic order of the map.
   *
   * <p>The resulting map preserves the encounter order of the first occurrence of each key.
   *
   * @param classifier function mapping elements to keys
   * @return a collector that groups elements into an ordered sequenced map
   */
  public static <T, K> Collector<T, ?, SequencedMap<K, List<T>>> groupingBy(
      Function<? super T, ? extends K> classifier) {
    return Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList());
  }

  /**
   * Collector that maps each element to itself, keyed by the given key extractor. Throws {@link
   * IllegalArgumentException} on duplicate keys.
   *
   * @param keyMapper function producing the map key for each element
   * @return a collector that accumulates elements into an ordered sequenced map
   * @see #toLinkedHashMap(Function, Function)
   * @see #toLinkedHashMap(Function, Function, DuplicateKeyExceptionSupplier)
   */
  public static <T, K> Collector<T, ?, SequencedMap<K, T>> toLinkedHashMap(
      Function<? super T, ? extends K> keyMapper) {
    Function<T, T> identity = Function.identity();
    return toLinkedHashMap(keyMapper, identity);
  }

  /**
   * Collector that maps each element to a key-value pair using the given mapper functions. Throws
   * {@link IllegalArgumentException} on duplicate keys.
   *
   * @param keyMapper function producing the map key for each element
   * @param valueMapper function producing the map value for each element
   * @return a collector that accumulates elements into an ordered sequenced map
   * @see #toLinkedHashMap(Function, Function, DuplicateKeyExceptionSupplier)
   */
  public static <T, K, V> Collector<T, ?, SequencedMap<K, V>> toLinkedHashMap(
      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
    return toLinkedHashMap(
        keyMapper,
        valueMapper,
        (key, newValue, existingValue) -> {
          String message =
              "Duplicate key '%s' with values '%s' and '%s'"
                  .formatted(key, newValue, existingValue);
          return new IllegalArgumentException(message);
        });
  }

  /**
   * Collector that maps each element to a key-value pair using the given mapper functions. When a
   * duplicate key is encountered, the given {@code exceptionSupplier} is called to produce the
   * exception to throw.
   *
   * @param keyMapper function producing the map key for each element
   * @param valueMapper function producing the map value for each element
   * @param exceptionSupplier called with the duplicate key and both conflicting values to produce
   *     the exception
   * @return a collector that accumulates elements into an ordered sequenced map
   */
  public static <T, K, V> Collector<T, ?, SequencedMap<K, V>> toLinkedHashMap(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper,
      DuplicateKeyExceptionSupplier<K, V> exceptionSupplier) {
    return new UniqueKeyLinkedHashMapCollector<>(keyMapper, valueMapper, exceptionSupplier);
  }

  /**
   * Functional interface for supplying an exception when a stream collector encounters more
   * elements than expected.
   *
   * @param <T> the element type
   */
  @FunctionalInterface
  public interface ExceptionSupplier<T> {
    RuntimeException get(List<T> foundElements);
  }

  /**
   * Collector that expects the stream to contain at most one element, returning it wrapped in an
   * {@link Optional}. Returns {@link Optional#empty()} for an empty stream. Throws {@link
   * IllegalStateException} if more than one element is present.
   *
   * @return a collector that returns an {@link Optional} of the single element, if any
   * @see #toSingleOptionalElement(ExceptionSupplier)
   */
  public static <T> Collector<T, ?, Optional<T>> toSingleOptionalElement() {
    return toSingleOptionalElement(
        list ->
            new IllegalStateException(
                "One or zero elements expected but got " + list.size() + ": " + list));
  }

  /**
   * Collector that expects the stream to contain at most one element, returning it wrapped in an
   * {@link Optional}. Returns {@link Optional#empty()} for an empty stream. If more than one
   * element is present, the given {@code exceptionSupplier} is called to produce the exception.
   *
   * @param exceptionSupplier called with all collected elements when more than one is found
   * @return a collector that returns an {@link Optional} of the single element, if any
   */
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
            return Optional.of(list.getFirst());
          }
          return Optional.empty();
        });
  }

  /**
   * Collector that expects the stream to contain exactly one element and returns it directly.
   * Throws {@link IllegalStateException} if the stream is empty or contains more than one element.
   *
   * @return a collector that returns the single element
   * @see #toSingleElement(ExceptionSupplier)
   * @see #toSingleElement(Supplier)
   */
  public static <T> Collector<T, ?, T> toSingleElement() {
    return toSingleElement((ExceptionSupplier<T>) null);
  }

  /**
   * Collector that expects the stream to contain exactly one element and returns it directly. If
   * the stream is empty or contains more than one element, the given {@code exceptionSupplier} is
   * called to produce the exception.
   *
   * @param exceptionSupplier called when the element count is not exactly one
   * @return a collector that returns the single element
   * @see #toSingleElement(ExceptionSupplier)
   */
  public static <T> Collector<T, ?, T> toSingleElement(
      Supplier<RuntimeException> exceptionSupplier) {
    return toSingleElement(list -> exceptionSupplier.get());
  }

  /**
   * Collector that expects the stream to contain exactly one element and returns it directly. If
   * the stream is empty or contains more than one element, the given {@code exceptionSupplier} is
   * called with all collected elements to produce the exception.
   *
   * @param exceptionSupplier called with all collected elements when the count is not exactly one
   * @return a collector that returns the single element
   */
  public static <T> Collector<T, ?, T> toSingleElement(ExceptionSupplier<T> exceptionSupplier) {
    return Collectors.collectingAndThen(
        Collectors.toList(),
        list -> {
          int size = list.size();
          if (size != 1) {
            if (exceptionSupplier != null) {
              throw exceptionSupplier.get(list);
            } else {
              throw new IllegalStateException(
                  "Exactly one element expected but got " + size + ": " + list);
            }
          }
          return list.getFirst();
        });
  }

  /**
   * Returns {@code true} if the given stream contains any duplicate elements (as determined by
   * {@link Object#equals}).
   *
   * <p>This is a short-circuiting terminal operation: the stream is consumed only until the first
   * duplicate is found.
   *
   * <p>Supports {@code null} elements.
   *
   * @param entries the stream to check; must not be used after this call
   * @return {@code true} if a duplicate element was found, {@code false} otherwise
   * @see #hasDuplicates(Stream, Comparator)
   */
  public static <T> boolean hasDuplicates(Stream<T> entries) {
    Set<T> seenEntries = new HashSet<>();
    return entries.anyMatch(entry -> !seenEntries.add(entry));
  }

  /**
   * Returns {@code true} if the given stream contains any duplicate elements, where equality is
   * determined by the given {@link Comparator} (i.e. two elements are considered equal when the
   * comparator returns {@code 0}).
   *
   * <p>This is useful when the natural {@link Object#equals} behaviour is not suitable, e.g. for
   * case-insensitive string comparison or custom domain equality.
   *
   * <p>This is a short-circuiting terminal operation: the stream is consumed only until the first
   * duplicate is found.
   *
   * @param entries the stream to check; must not be used after this call
   * @param comparator the comparator used to determine equality between elements
   * @return {@code true} if a duplicate element was found, {@code false} otherwise
   */
  public static <T> boolean hasDuplicates(Stream<T> entries, Comparator<? super T> comparator) {
    Set<T> seenEntries = new TreeSet<>(comparator);
    return entries.anyMatch(entry -> !seenEntries.add(entry));
  }

  /**
   * Returns a stateful {@link Predicate} that keeps only the first element for each distinct key,
   * as extracted by {@code keyExtractor}. Subsequent elements that map to an already-seen key are
   * filtered out silently.
   *
   * <p>Intended to be used as a {@link java.util.stream.Stream#filter(Predicate)} argument:
   *
   * <pre>{@code
   * stream.filter(StreamUtil.distinctByKey(MyObject::getName))
   * }</pre>
   *
   * <p><b>Note:</b> The predicate is thread-safe and can be used with parallel streams. {@code
   * null} keys are not supported.
   *
   * @param keyExtractor function that produces the key used for deduplication
   * @return a predicate that returns {@code true} only for the first element with each key
   * @see #distinctByKey(Function, Consumer)
   */
  public static <T, K> Predicate<T> distinctByKey(Function<? super T, K> keyExtractor) {
    return distinctByKey(
        keyExtractor,
        duplicate -> {
          // ignore
        });
  }

  /**
   * Returns a stateful {@link Predicate} that keeps only the first element for each distinct key,
   * as extracted by {@code keyExtractor}. Subsequent elements that map to an already-seen key are
   * passed to the {@code duplicates} consumer and then filtered out.
   *
   * <p><b>Note:</b> The predicate is thread-safe and can be used with parallel streams. {@code
   * null} keys are not supported.
   *
   * @param keyExtractor function that produces the key used for deduplication
   * @param duplicates consumer called with each element that is a duplicate
   * @return a predicate that returns {@code true} only for the first element with each key
   */
  public static <T, K> Predicate<T> distinctByKey(
      Function<? super T, K> keyExtractor, Consumer<T> duplicates) {
    Set<K> seen = ConcurrentHashMap.newKeySet();
    return value -> {
      K key = keyExtractor.apply(value);
      boolean added = seen.add(key);
      if (!added) {
        duplicates.accept(value);
      }
      return added;
    };
  }

  /**
   * Functional interface for supplying an exception when a {@link #toLinkedHashMap} collector
   * encounters a duplicate key.
   *
   * @param <K> the key type
   * @param <V> the value type
   */
  @FunctionalInterface
  public interface DuplicateKeyExceptionSupplier<K, V> {
    /**
     * Produces an exception for the given duplicate key situation.
     *
     * @param key the duplicate key
     * @param newValue the value of the element that triggered the conflict
     * @param existingValue the value already stored under {@code key}
     */
    RuntimeException get(K key, V newValue, V existingValue);
  }

  @SuppressWarnings("ClassCanBeRecord")
  private static class UniqueKeyLinkedHashMapCollector<T, K, V>
      implements Collector<T, SequencedMap<K, V>, SequencedMap<K, V>> {
    private final Function<? super T, ? extends K> keyMapper;
    private final Function<? super T, ? extends V> valueMapper;
    private final DuplicateKeyExceptionSupplier<K, V> exceptionSupplier;

    UniqueKeyLinkedHashMapCollector(
        Function<? super T, ? extends K> keyMapper,
        Function<? super T, ? extends V> valueMapper,
        DuplicateKeyExceptionSupplier<K, V> exceptionSupplier) {
      this.keyMapper = keyMapper;
      this.valueMapper = valueMapper;
      this.exceptionSupplier = exceptionSupplier;
    }

    @Override
    public Set<Characteristics> characteristics() {
      return Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
    }

    @Override
    public BiConsumer<SequencedMap<K, V>, T> accumulator() {
      return (map, element) -> {
        K key = keyMapper.apply(element);
        V value = valueMapper.apply(element);
        accumulate(map, key, value);
      };
    }

    @Override
    public Supplier<SequencedMap<K, V>> supplier() {
      return LinkedHashMap::new;
    }

    @Override
    public BinaryOperator<SequencedMap<K, V>> combiner() {
      return (m1, m2) -> {
        for (Map.Entry<K, V> e : m2.entrySet()) {
          accumulate(m1, e.getKey(), e.getValue());
        }
        return m1;
      };
    }

    @Override
    public Function<SequencedMap<K, V>, SequencedMap<K, V>> finisher() {
      return Function.identity();
    }

    private void accumulate(Map<K, V> map, K key, V value) {
      V existing = map.putIfAbsent(key, value);
      if (existing != null) {
        throw exceptionSupplier.get(key, value, existing);
      }
    }
  }
}
