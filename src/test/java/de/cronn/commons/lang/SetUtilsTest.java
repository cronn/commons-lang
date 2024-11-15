package de.cronn.commons.lang;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SetUtilsTest {

	@Test
	void testOrderedSet() {
		assertThat(SetUtils.orderedSet("abc")).containsExactly("abc");
		assertThat(SetUtils.orderedSet("abc", "def", "ghi")).containsExactly("abc", "def", "ghi");
		assertThat(SetUtils.orderedSet(2, 1, 3)).containsExactly(2, 1, 3);
		assertThat(SetUtils.orderedSet(2, 1, 3, 1)).containsExactly(2, 1, 3);
		assertThat(SetUtils.orderedSet(null, "abc")).containsExactly(null, "abc");
		assertThat(SetUtils.orderedSet((Object) null)).singleElement().isNull();
	}

}
