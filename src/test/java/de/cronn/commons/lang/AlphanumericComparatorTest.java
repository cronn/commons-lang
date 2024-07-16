package de.cronn.commons.lang;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class AlphanumericComparatorTest {

	private final AlphanumericComparator comparator = AlphanumericComparator.getInstance();

	@Test
	void compareTwoNonEqual() {
		String s1 = "e";
		String s2 = "è";

		assertComparesLessThan(s1, s2);
	}

	@Test
	void compareNumberWithCharacters() {
		assertComparesLessThan("123", "abc");
		assertComparesGreaterThan("abc", "123");
	}

	@Test
	void compareWithTrailingWhitespaces() {
		assertComparesEqual("abc", "abc  \t\n");
		assertComparesEqual("abc  \t\n", "abc");
	}

	@Test
	void compareTwoEqual() {
		String s1 = "Example-01-String1";
		String s2 = "Example-01-String1";

		assertComparesEqual(s1, s2);
	}

	@Test
	void comparePrefix() {
		String s1 = "Example-01";
		String s2 = "Example-01-String1";

		assertComparesLessThan(s1, s2);
	}

	@Test
	void compareTwoLargeNumbers() {
		assertComparesLessThan("100000000000000000000000000", "200000000000000000000000000");
		assertComparesLessThan("10", "200000000000000000000000000");
		assertComparesGreaterThan("200000000000000000000000000", "10");
	}

	@Test
	void compareTwoSmallNumbers() {
		String s1 = "100000000";
		String s2 = "200000000";

		assertComparesLessThan(s1, s2);
	}

	@Test
	void compareLongMaxValueNumber() {
		String s1 = "abc-" + Long.MAX_VALUE;
		String s2 = "abc-" + (Long.MAX_VALUE - 1);

		assertComparesLessThan(s2, s1);
	}

	@Test
	void compareOneNull() {
		String s1 = "Example-01-String1";
		String s2 = null;

		assertComparesLessThan(s2, s1);
	}

	@Test
	void compareOneEmpty() {
		String s1 = "Example-01-String1";
		String s2 = "";

		assertComparesLessThan(s2, s1);
	}

	@Test
	void compareNullAndEmpty() {
		String s1 = null;
		String s2 = "";

		assertComparesEqual(s1, s2);
	}

	@Test
	void collatorSort() {
		List<String> list = Stream.of("b", "e", "f", "ě", "è", "g", "k")
			.sorted(comparator)
			.toList();

		assertThat(list).containsExactly("b", "e", "è", "ě", "f", "g", "k");
	}

	@Test
	void collatorSortWords() {
		List<String> list = Stream.of(
				"sèle",
				"solo",
				"solè",
				"sola",
				"soli",
				"sole",
				"sold",
				"sila",
				"silè",
				"sölo",
				"sulo",
				"sylo",
				"soly")
			.sorted(comparator)
			.toList();

		assertThat(list).containsExactly(
			"sèle",
			"sila",
			"silè",
			"sola",
			"sold",
			"sole",
			"solè",
			"soli",
			"solo",
			"soly",
			"sölo",
			"sulo",
			"sylo");
	}

	@Test
	void fileNameSort() {
		List<String> list = Stream.of(
				"file-01.doc",
				"file-03.doc",
				"file-2.doc",
				"file-20.doc",
				"file-10.doc",
				"file-3.doc")
			.sorted(comparator)
			.toList();

		assertThat(list).containsExactly("file-01.doc",
			"file-2.doc",
			"file-3.doc",
			"file-03.doc",
			"file-10.doc",
			"file-20.doc");
	}

	@Test
	void versionSort() {
		List<String> list = Stream.of("1.0",
				"1.0.0",
				"1.0.1",
				"1.1",
				"2.0",
				"1.10",
				"21.1",
				"21.2",
				"22.10",
				"2.5",
				"20.9",
				"20.10",
				"20.9-beta1",
				"20.10-beta1",
				"2.5.0",
				"1.10.1",
				"1.0.1-b",
				"1.9")
			.sorted(comparator)
			.toList();

		assertThat(list).containsExactly("1.0",
			"1.0.0",
			"1.0.1",
			"1.0.1-b",
			"1.1",
			"1.9",
			"1.10",
			"1.10.1",
			"2.0",
			"2.5",
			"2.5.0",
			"20.9",
			"20.9-beta1",
			"20.10",
			"20.10-beta1",
			"21.1",
			"21.2",
			"22.10");
	}

	@Test
	void testIsBefore() {
		assertThat(AlphanumericComparator.isBefore("10", "12")).isTrue();
		assertThat(AlphanumericComparator.isBefore("10.0", "12")).isTrue();
		assertThat(AlphanumericComparator.isBefore("12.0", "12")).isFalse();
	}

	@Test
	void testIsAfter() {
		assertThat(AlphanumericComparator.isAfter("13", "12")).isTrue();
		assertThat(AlphanumericComparator.isAfter("13.0", "12")).isTrue();
		assertThat(AlphanumericComparator.isAfter("12", "12")).isFalse();
		assertThat(AlphanumericComparator.isAfter("12.0", "12")).isTrue();
		assertThat(AlphanumericComparator.isAfter("12a", "120")).isFalse();
	}

	@Test
	void testIsAfterOrEqual() {
		assertThat(AlphanumericComparator.isAfterOrEqual("13", "12")).isTrue();
		assertThat(AlphanumericComparator.isAfterOrEqual("13.0", "12")).isTrue();
		assertThat(AlphanumericComparator.isAfterOrEqual("12.0", "12")).isTrue();
		assertThat(AlphanumericComparator.isAfterOrEqual("11.0", "12")).isFalse();
	}

	private void assertComparesLessThan(String s1, String s2) {
		assertThat(comparator.compare(s1, s2)).isEqualTo(-1);
		assertThat(comparator.compare(s2, s1)).isEqualTo(1);
	}

	private void assertComparesGreaterThan(String s1, String s2) {
		assertThat(comparator.compare(s1, s2)).isEqualTo(1);
		assertThat(comparator.compare(s2, s1)).isEqualTo(-1);
	}

	private void assertComparesEqual(String s1, String s2) {
		assertThat(comparator.compare(s1, s2)).isEqualTo(0);
		assertThat(comparator.compare(s2, s1)).isEqualTo(0);
	}

}
