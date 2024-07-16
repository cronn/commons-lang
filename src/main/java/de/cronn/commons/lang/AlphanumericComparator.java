package de.cronn.commons.lang;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

// This implementation is based on https://github.com/benjaminsaff/alphanumeric-comparator-java
// Originally written by Farbod Safaei
public final class AlphanumericComparator implements Comparator<String> {

	private static final int MAX_LONG_STRING_SIZE = Long.toString(Long.MAX_VALUE).length();
	private static final AlphanumericComparator INSTANCE = new AlphanumericComparator();

	private final Collator collator = Collator.getInstance(Locale.ROOT);

	private AlphanumericComparator() {
	}

	public static AlphanumericComparator getInstance() {
		return INSTANCE;
	}

	@Override
	public int compare(String s1, String s2) {
		if (isNullOrBlank(s1)) {
			return isNullOrBlank(s2) ? 0 : -1;
		} else if (isNullOrBlank(s2)) {
			return 1;
		}

		s1 = s1.trim();
		s2 = s2.trim();
		int s1Index = 0;
		int s2Index = 0;
		while (s1Index < s1.length() && s2Index < s2.length()) {
			String s1Slice = slice(s1, s1Index);
			String s2Slice = slice(s2, s2Index);
			s1Index += s1Slice.length();
			s2Index += s2Slice.length();

			int result = compareSlices(s1Slice, s2Slice);
			if (result != 0) {
				return result;
			}
		}
		return Integer.compare(s1.length(), s2.length());
	}

	private static boolean isNullOrBlank(String string) {
		return string == null || string.isBlank();
	}

	private int compareSlices(String s1, String s2) {
		if (Character.isDigit(s1.charAt(0)) && Character.isDigit(s2.charAt(0))) {
			return compareDigits(s1, s2);
		} else {
			return compareCollatedStrings(s1, s2);
		}
	}

	private String slice(String s, int index) {
		if (Character.isDigit(s.charAt(index))) {
			StringBuilder result = new StringBuilder();
			while (index < s.length() && Character.isDigit(s.charAt(index))) {
				result.append(s.charAt(index));
				index++;
			}
			return result.toString();
		} else {
			return s.substring(index, index + 1);
		}
	}

	private int compareDigits(String s1, String s2) {
		if (s1.length() < MAX_LONG_STRING_SIZE && s2.length() < MAX_LONG_STRING_SIZE) {
			return Long.compare(Long.parseLong(s1), Long.parseLong(s2));
		} else {
			return new BigDecimal(s1).compareTo(new BigDecimal(s2));
		}
	}

	private int compareCollatedStrings(String s1, String s2) {
		return collator.compare(s1, s2);
	}

	public static boolean isBefore(String string, String stringToCompareWith) {
		return getInstance().compare(string, stringToCompareWith) < 0;
	}

	public static boolean isAfterOrEqual(String string, String stringToCompareWith) {
		return !isBefore(string, stringToCompareWith);
	}

	public static boolean isAfter(String string, String stringToCompareWith) {
		return getInstance().compare(string, stringToCompareWith) > 0;
	}

}
