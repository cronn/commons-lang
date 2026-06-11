package de.cronn.commons.lang;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.SequencedSet;

/** Utility methods for creating and working with {@link java.util.Set} instances. */
public final class SetUtils {

  private SetUtils() {}

  /**
   * Creates a sequenced set containing the given elements in encounter order, with duplicates
   * silently ignored.
   *
   * <p>Unlike {@link java.util.Set#of(Object[])}, the returned set preserves insertion order and is
   * mutable.
   *
   * @param elements the elements to include
   * @return a mutable sequenced set containing the given elements in encounter order
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <E> SequencedSet<E> orderedSet(E... elements) {
    return new LinkedHashSet<>(Arrays.asList(elements));
  }
}
