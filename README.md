[![CI](https://github.com/cronn/commons-lang/workflows/CI/badge.svg)](https://github.com/cronn/commons-lang/actions)
[![Maven Central](https://img.shields.io/maven-central/v/de.cronn/commons-lang?logo=apache%20maven)](https://search.maven.org/#search|ga|1|g:de.cronn%20AND%20commons-lang)
[![Apache 2.0](https://img.shields.io/github/license/cronn/commons-lang.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![codecov](https://codecov.io/gh/cronn/commons-lang/branch/main/graph/badge.svg?token=KD1WJK5ZFK)](https://codecov.io/gh/cronn/commons-lang)
[![Valid Gradle Wrapper](https://github.com/cronn/commons-lang/workflows/Validate%20Gradle%20Wrapper/badge.svg)](https://github.com/cronn/commons-lang/actions/workflows/gradle-wrapper-validation.yml)

# cronn commons-lang #

Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>commons-lang</artifactId>
    <version>1.5</version>
</dependency>
```

This library includes the following classes, which are described in detail in the sections below:

- `StreamUtil`: Collectors and utilities that are useful when working with Java streams
- `Action`: An interface that is similar to `Runnable` but allows to throw checked exceptions
- `AlphanumericComparator`: A comparator that implements [the Alphanum Algorithm][alphanum-algorithm] which is useful to sort versions or filenames in a more

## StreamUtil

### toSingleElement()

Good case:
```java
Object number = Stream.of(1, 2, 3)
    .filter(value -> value > 2)
    .collect(StreamUtil.toSingleElement());
// number = 3
```

Bad case:
```java
Object number = Stream.of(1, 2, 3, 4)
    .filter(value -> value > 2)
    .collect(StreamUtil.toSingleElement());
// Throws IllegalStateException: Exactly one element expected but got 2: [3, 4]
```

### toSingleOptionalElement()

Similar to `toSingleElement()` but returns an `Optional` and throws no exception if no element was found.

```java
Optional<Object> number = Stream.of(1, 2, 3)
    .filter(value -> value > 2)
    .collect(StreamUtil.toSingleOptionalElement());
```

### toLinkedHashSet()

`StreamUtil.toLinkedHashSet()` is a drop-replacement for `Collectors.toSet()` that guarantees a stable/deterministic order.

Example:

```java
// numbers contains 1, 2, 3 and returns the elements in exactly this order when iterating
SequencedSet<Object> numbers = Stream.of(1, 2, 3, 2, 3)
    .collect(StreamUtil.toLinkedHashSet());
```

### hasDuplicates()

Checks whether a stream contains any duplicate elements. Short-circuits on the first duplicate found.

```java
StreamUtil.hasDuplicates(Stream.of(1, 2, 3));    // false
StreamUtil.hasDuplicates(Stream.of(1, 2, 1, 3)); // true
```

An overload accepts a `Comparator` for custom equality semantics, e.g. case-insensitive string comparison:

```java
StreamUtil.hasDuplicates(Stream.of("Hello", "world", "HELLO"), String.CASE_INSENSITIVE_ORDER); // true
StreamUtil.hasDuplicates(Stream.of("a", "A"), Comparator.naturalOrder());                      // false
StreamUtil.hasDuplicates(Stream.of("a", "A"), String.CASE_INSENSITIVE_ORDER);                  // true
```

### distinctByKey()

A stateful `Predicate` for use with `Stream.filter()` that keeps only the first element for each distinct key. Unlike `Stream.distinct()`, deduplication is based on a key extracted from each element rather than the element itself.

```java
// Keep only the first word starting with each letter
List<String> result = Stream.of("one", "two", "three", "four")
    .filter(StreamUtil.distinctByKey(s -> s.charAt(0)))
    .toList();
// result = ["one", "two", "four"]
```

An optional `Consumer` overload collects the duplicates that were filtered out:

```java
List<String> duplicates = new ArrayList<>();
List<String> result = Stream.of("one", "two", "three", "four")
    .filter(StreamUtil.distinctByKey(String::length, duplicates::add))
    .toList();
// result = ["one", "three", "four"]
// duplicates = ["two"]
```

### SetUtils

`SetUtils` provides utility methods for creating ordered sets in Java.

Unlike `Set.of(…)`, `SetUtils.orderedSet(…)` maintains the order of elements as they are added.

#### Example Usage

```java
SequencedSet<String> ordered = SetUtils.orderedSet("abc", "def", "ghi");
// Output: [abc, def, ghi]

SequencedSet<Integer> numbers = SetUtils.orderedSet(3, 1, 2, 1);
// Output: [3, 1, 2]
```

> **Key Difference**: `SetUtils.orderedSet(…)` uses `LinkedHashSet`, ensuring insertion order is preserved.

## AlphanumericComparator

> People sort strings with numbers differently than software does.
> Most sorting algorithms compare ASCII values, which produces an ordering that is inconsistent with human logic.

Consider the following list of filenames:

```
file1.txt
file2.txt
file10.txt
file3.txt
```

If you sort this list using the default sort order, the result will be:

```
file1.txt
file10.txt
file2.txt
file3.txt
```

This order is not intuitive for most people.

However, by using the AlphanumericComparator, you will get:

```
file1.txt
file2.txt
file3.txt
file10.txt
```

This order is more natural and aligns with human expectations.

## Requirements ##

- Java 21+

[alphanum-algorithm]: http://www.davekoelle.com/alphanum.html
