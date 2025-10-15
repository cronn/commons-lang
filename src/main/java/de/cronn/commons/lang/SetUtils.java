package de.cronn.commons.lang;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.SequencedSet;

public final class SetUtils {

	private SetUtils() {
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	public static <E> SequencedSet<E> orderedSet(E... elements) {
		return new LinkedHashSet<>(Arrays.asList(elements));
	}
}
