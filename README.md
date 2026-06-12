[![CI](https://github.com/cronn/commons-lang/workflows/CI/badge.svg)](https://github.com/cronn/commons-lang/actions)
[![Maven Central](https://img.shields.io/maven-central/v/de.cronn/commons-lang?logo=apache%20maven)](https://search.maven.org/#search|ga|1|g:de.cronn%20AND%20commons-lang)
[![Apache 2.0](https://img.shields.io/github/license/cronn/commons-lang.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![codecov](https://codecov.io/gh/cronn/commons-lang/branch/main/graph/badge.svg?token=KD1WJK5ZFK)](https://codecov.io/gh/cronn/commons-lang)
[![Valid Gradle Wrapper](https://github.com/cronn/commons-lang/workflows/Validate%20Gradle%20Wrapper/badge.svg)](https://github.com/cronn/commons-lang/actions/workflows/gradle-wrapper-validation.yml)

# cronn commons-lang

**Small Java utilities that fill gaps in the standard library.** No heavy dependencies, no magic.

## Background

This library collects small Java utilities that come up repeatedly across projects at
[cronn](https://github.com/cronn). Some of them, like `StreamUtil.toLinkedHashSet()` and
`SetUtils.orderedSet(...)`, directly reflect our stance on
[determinism](https://github.com/cronn/.github/blob/main/profile/README.md#determinism): prefer
ordered collections by default so that behaviour is consistent across runs, JVM versions, and
environments, even when order is not required for correctness.

## Installation

**Gradle (Kotlin DSL)**
```kotlin
implementation("de.cronn:commons-lang:1.6")
```

**Maven**
```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>commons-lang</artifactId>
    <version>1.6</version>
</dependency>
```

## Contents

| Class                                               | Description                                             |
|-----------------------------------------------------|---------------------------------------------------------|
| [`StreamUtil`](#streamutil)                         | Collectors and stream utilities missing from the JDK    |
| [`SetUtils`](#setutils)                             | Factory methods for ordered sets                        |
| [`Action`](#action)                                 | `Runnable` that allows throwing checked exceptions      |
| [`AlphanumericComparator`](#alphanumericcomparator) | Human-friendly sorting of strings with embedded numbers |

---

## StreamUtil

`StreamUtil` provides collectors and stream utilities that complement `java.util.stream.Collectors`.
It covers common patterns like collecting to a single element, deduplication by key, and
order-preserving alternatives to standard collectors.

### toSingleElement()

Collects exactly one element, throws `IllegalStateException` otherwise.

```java
// works fine
int number = Stream.of(1, 2, 3)
    .filter(value -> value > 2)
    .collect(StreamUtil.toSingleElement());
// number = 3

// throws: Exactly one element expected but got 2: [3, 4]
Stream.of(1, 2, 3, 4)
    .filter(value -> value > 2)
    .collect(StreamUtil.toSingleElement());
```

### toSingleOptionalElement()

Like `toSingleElement()`, but returns an `Optional` instead of throwing when no element is found.

```java
Optional<Integer> number = Stream.of(1, 2, 3)
    .filter(value -> value > 2)
    .collect(StreamUtil.toSingleOptionalElement());
```

### toLinkedHashSet()

Drop-in replacement for `Collectors.toSet()` with a guaranteed, stable iteration order.

```java
SequencedSet<Integer> numbers = Stream.of(1, 2, 3, 2, 3)
    .collect(StreamUtil.toLinkedHashSet());
// iteration order: 1, 2, 3
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

A stateful `Predicate` for `Stream.filter()` that keeps only the first element per distinct key.
Unlike `Stream.distinct()`, deduplication is based on an extracted key rather than the element itself.

```java
List<String> result = Stream.of("one", "two", "three", "four")
    .filter(StreamUtil.distinctByKey(s -> s.charAt(0)))
    .toList();
// result = ["one", "two", "four"]
```

An optional `Consumer` overload lets you capture the duplicates that were filtered out:

```java
List<String> duplicates = new ArrayList<>();
List<String> result = Stream.of("one", "two", "three", "four")
    .filter(StreamUtil.distinctByKey(String::length, duplicates::add))
    .toList();
// result    = ["one", "three", "four"]
// duplicates = ["two"]
```

---

## SetUtils

Factory methods for ordered sets. Unlike `Set.of(…)`, `SetUtils.orderedSet(…)` preserves insertion
order, silently drops duplicates, and returns a mutable set.

```java
SequencedSet<String> ordered = SetUtils.orderedSet("abc", "def", "ghi");
// iteration order: abc, def, ghi

SequencedSet<Integer> numbers = SetUtils.orderedSet(3, 1, 2, 1);
// iteration order: 3, 1, 2  (duplicate 1 dropped)
```

---

## Action

A functional interface like `Runnable`, but allowed to throw checked exceptions. Useful as a
lambda handle for any void operation that may fail, with adapters to standard JDK types.

```java
Action action = () -> Files.delete(path);

// wrap checked exceptions as RuntimeException
Supplier<Void> supplier = action.toSupplier();

// propagate checked exceptions unchanged
Callable<Void> callable = action.toCallable();
```

---

## AlphanumericComparator

> Humans sort `file2.txt` before `file10.txt`. Computers don't, unless you tell them to.

`AlphanumericComparator` splits strings into text and numeric segments and compares numeric parts
by value, producing the natural order people expect.

| Lexicographic order | Alphanumeric order |
|---------------------|--------------------|
| file1.txt           | file1.txt          |
| file10.txt          | file2.txt          |
| file2.txt           | file3.txt          |
| file3.txt           | file10.txt         |

```java
List<String> files = List.of("file10.txt", "file3.txt", "file1.txt", "file2.txt");
files.stream()
    .sorted(AlphanumericComparator.getInstance())
    .toList();
// [file1.txt, file2.txt, file3.txt, file10.txt]
```

Convenience predicates are also available:

```java
AlphanumericComparator.isBefore("file2.txt", "file10.txt");      // true
AlphanumericComparator.isAfter("file10.txt", "file3.txt");       // true
AlphanumericComparator.isAfterOrEqual("file3.txt", "file3.txt"); // true
```

---

## Requirements

- Java 21+

[alphanum-algorithm]: http://www.davekoelle.com/alphanum.html
