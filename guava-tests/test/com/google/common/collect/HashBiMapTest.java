/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@link HashBiMap}.
 *
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
public class HashBiMapTest extends TestCase {

  public static final class HashBiMapGenerator extends TestStringBiMapGenerator {
    @Override
    protected BiMap<String, String> create(Entry<String, String>[] entries) {
      BiMap<String, String> result = HashBiMap.create();
      for (Entry<String, String> entry : entries) {
        result.put(entry.getKey(), entry.getValue());
      }
      return result;
    }
  }

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        BiMapTestSuiteBuilder.using(new HashBiMapGenerator())
            .named("HashBiMap")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE)
            .createTestSuite());
    suite.addTest(
        BiMapTestSuiteBuilder.using(
                new TestStringBiMapGenerator() {
                  @Override
                  protected BiMap<String, String> create(Entry<String, String>[] entries) {
                    HashBiMap<String, String> result = HashBiMap.create();
                    result.convertToHashFloodingResistantImplementation();
                    for (Entry<String, String> entry : entries) {
                      result.put(entry.getKey(), entry.getValue());
                    }
                    return result;
                  }
                })
            .named("HashBiMap with flooding resistance")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE)
            .createTestSuite());
    suite.addTestSuite(HashBiMapTest.class);
    return suite;
  }

  public void testMapConstructor() {
    /* Test with non-empty Map. */
    Map<String, String> map =
        ImmutableMap.of(
            "canada", "dollar",
            "chile", "peso",
            "switzerland", "franc");
    HashBiMap<String, String> bimap = HashBiMap.create(map);
    assertEquals("dollar", bimap.get("canada"));
    assertEquals("canada", bimap.inverse().get("dollar"));
  }

  private static final int N = 1000;

  public void testBashIt() throws Exception {
    BiMap<Integer, Integer> bimap = HashBiMap.create(N);
    BiMap<Integer, Integer> inverse = bimap.inverse();

    for (int i = 0; i < N; i++) {
      assertNull(bimap.put(2 * i, 2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i + 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      int oldValue = bimap.get(2 * i);
      assertEquals(2 * i + 1, (int) bimap.put(2 * i, oldValue - 2));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i - 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i - 1));
    }
    Set<Entry<Integer, Integer>> entries = bimap.entrySet();
    for (Entry<Integer, Integer> entry : entries) {
      entry.setValue(entry.getValue() + 2 * N);
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * N + 2 * i - 1, (int) bimap.get(2 * i));
    }
  }

  public void testBiMapEntrySetIteratorRemove() {
    BiMap<Integer, String> map = HashBiMap.create();
    map.put(1, "one");
    Set<Entry<Integer, String>> entries = map.entrySet();
    Iterator<Entry<Integer, String>> iterator = entries.iterator();
    Entry<Integer, String> entry = iterator.next();
    entry.setValue("two"); // changes the iterator's current entry value
    assertEquals("two", map.get(1));
    assertEquals(Integer.valueOf(1), map.inverse().get("two"));
    iterator.remove(); // removes the updated entry
    assertTrue(map.isEmpty());
  }

  public void testInsertionOrder() {
    BiMap<String, Integer> map = HashBiMap.create();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("quux", 3);
    assertThat(map.entrySet())
        .containsExactly(
            Maps.immutableEntry("foo", 1),
            Maps.immutableEntry("bar", 2),
            Maps.immutableEntry("quux", 3))
        .inOrder();
  }

  public void testInsertionOrderAfterRemoveFirst() {
    BiMap<String, Integer> map = HashBiMap.create();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("quux", 3);

    map.remove("foo");
    assertThat(map.entrySet())
        .containsExactly(Maps.immutableEntry("bar", 2), Maps.immutableEntry("quux", 3))
        .inOrder();
  }

  public void testInsertionOrderAfterRemoveMiddle() {
    BiMap<String, Integer> map = HashBiMap.create();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("quux", 3);

    map.remove("bar");
    assertThat(map.entrySet())
        .containsExactly(Maps.immutableEntry("foo", 1), Maps.immutableEntry("quux", 3))
        .inOrder();
  }

  public void testInsertionOrderAfterRemoveLast() {
    BiMap<String, Integer> map = HashBiMap.create();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("quux", 3);

    map.remove("quux");
    assertThat(map.entrySet())
        .containsExactly(Maps.immutableEntry("foo", 1), Maps.immutableEntry("bar", 2))
        .inOrder();
  }

  public void testInsertionOrderAfterForcePut() {
    BiMap<String, Integer> map = HashBiMap.create();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("quux", 3);

    map.forcePut("quux", 1);
    assertThat(map.entrySet())
        .containsExactly(Maps.immutableEntry("bar", 2), Maps.immutableEntry("quux", 1))
        .inOrder();
  }

  public void testInsertionOrderAfterInverseForcePut() {
    BiMap<String, Integer> map = HashBiMap.create();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("quux", 3);

    map.inverse().forcePut(1, "quux");
    assertThat(map.entrySet())
        .containsExactly(Maps.immutableEntry("bar", 2), Maps.immutableEntry("quux", 1))
        .inOrder();
  }

  public void testInverseInsertionOrderAfterInverse() {
    BiMap<String, Integer> map = HashBiMap.create();
    map.put("bar", 2);
    map.put("quux", 1);

    assertThat(map.inverse().entrySet())
        .containsExactly(Maps.immutableEntry(2, "bar"), Maps.immutableEntry(1, "quux"))
        .inOrder();
  }

  public void testInverseInsertionOrderAfterInverseForcePut() {
    BiMap<String, Integer> map = HashBiMap.create();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("quux", 3);

    map.inverse().forcePut(1, "quux");
    assertThat(map.inverse().entrySet())
        .containsExactly(Maps.immutableEntry(2, "bar"), Maps.immutableEntry(1, "quux"))
        .inOrder();
  }

  public void testInverseInsertionOrderAfterInverseForcePutPresentKey() {
    BiMap<String, Integer> map = HashBiMap.create();
    map.put("foo", 1);
    map.put("bar", 2);
    map.put("quux", 3);
    map.put("nab", 4);

    map.inverse().forcePut(4, "bar");
    assertThat(map.entrySet())
        .containsExactly(
            Maps.immutableEntry("foo", 1),
            Maps.immutableEntry("bar", 4),
            Maps.immutableEntry("quux", 3))
        .inOrder();
  }

  public void testInverseEntrySetValueNewKey() {
    BiMap<Integer, String> map = HashBiMap.create();
    map.put(1, "a");
    map.put(2, "b");
    Iterator<Entry<String, Integer>> inverseEntryItr = map.inverse().entrySet().iterator();
    Entry<String, Integer> entry = inverseEntryItr.next();
    entry.setValue(3);
    assertEquals(Maps.immutableEntry("b", 2), inverseEntryItr.next());
    assertFalse(inverseEntryItr.hasNext());
    assertThat(map.entrySet())
        .containsExactly(Maps.immutableEntry(2, "b"), Maps.immutableEntry(3, "a"))
        .inOrder();
  }

  public void testEntrySetSetValueTriggersFlooding() {
    HashBiMap<Integer, Object> map = HashBiMap.create();
    for (int i = 0; i < 20; i++) {
      map.put(i, "orig-" + i);
    }
    assertFalse(map.isUsingHashFloodingResistance());

    for (Iterator<Entry<Integer, Object>> it = map.entrySet().iterator(); it.hasNext(); ) {
      Entry<Integer, Object> entry = it.next();
      int key = entry.getKey();
      assertEquals("orig-" + key, entry.setValue(new ConstantHashObject(key)));
      if (key == 15) {
        it.remove();
      }
    }
    assertTrue(map.isUsingHashFloodingResistance());

    ImmutableBiMap.Builder<Integer, Object> expectedBuilder = ImmutableBiMap.builder();
    for (int i = 0; i < 20; i++) {
      if (i != 15) {
        expectedBuilder.put(i, new ConstantHashObject(i));
      }
    }
    ImmutableBiMap<Integer, Object> expected = expectedBuilder.build();
    assertThat(map.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
    assertThat(map.inverse().entrySet())
        .containsExactlyElementsIn(expected.inverse().entrySet())
        .inOrder();
  }

  public void testInverseEntrySetSetValueTriggersFlooding() {
    HashBiMap<Object, Integer> map = HashBiMap.create();
    for (int i = 0; i < 20; i++) {
      map.put("orig-" + i, i);
    }
    assertFalse(map.isUsingHashFloodingResistance());

    for (Iterator<Entry<Integer, Object>> it = map.inverse().entrySet().iterator();
        it.hasNext(); ) {
      Entry<Integer, Object> entry = it.next();
      int value = entry.getKey();
      assertEquals("orig-" + value, entry.setValue(new ConstantHashObject(value)));
    }
    assertTrue(map.isUsingHashFloodingResistance());

    ImmutableBiMap.Builder<Object, Integer> expectedBuilder = ImmutableBiMap.builder();
    for (int i = 0; i < 20; i++) {
      expectedBuilder.put(new ConstantHashObject(i), i);
    }
    ImmutableBiMap<Object, Integer> expected = expectedBuilder.build();
    assertThat(map.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
    assertThat(map.inverse().entrySet())
        .containsExactlyElementsIn(expected.inverse().entrySet())
        .inOrder();
  }

  // 30.1.1 replaceAll is clear() then put(), so unlike upstream (which restores the original
  // contents on failure) the entries already re-put stay and the rest are dropped.
  public void testReplaceAllTriggersFloodingAndThrows() {
    HashBiMap<Integer, Object> map = HashBiMap.create();
    for (int i = 0; i < 20; i++) {
      map.put(i, "orig-" + i);
    }
    assertFalse(map.isUsingHashFloodingResistance());

    try {
      map.replaceAll(
          (k, v) -> {
            if (k < 15) {
              return new ConstantHashObject(k);
            }
            throw new IllegalStateException();
          });
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertTrue(map.isUsingHashFloodingResistance());
    assertReplacedPrefix(map);
  }

  public void testReplaceAllTriggersFloodingAndDuplicateValue() {
    HashBiMap<Integer, Object> map = HashBiMap.create();
    for (int i = 0; i < 20; i++) {
      map.put(i, "orig-" + i);
    }
    assertFalse(map.isUsingHashFloodingResistance());

    try {
      map.replaceAll(
          (k, v) -> {
            if (k < 15) {
              return new ConstantHashObject(k);
            }
            return new ConstantHashObject(0);
          });
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    assertTrue(map.isUsingHashFloodingResistance());
    assertReplacedPrefix(map);
  }

  public void testInverseReplaceAllTriggersFloodingAndThrows() {
    HashBiMap<Object, Integer> map = HashBiMap.create();
    for (int i = 0; i < 20; i++) {
      map.put("orig-" + i, i);
    }
    assertFalse(map.isUsingHashFloodingResistance());

    BiMap<Integer, Object> inverse = map.inverse();
    try {
      inverse.replaceAll(
          (v, k) -> {
            if (v < 15) {
              return new ConstantHashObject(v);
            }
            throw new IllegalStateException();
          });
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertTrue(map.isUsingHashFloodingResistance());
    assertReplacedPrefix(map.inverse());
  }

  public void testInverseReplaceAllTriggersFloodingAndDuplicateKey() {
    HashBiMap<Object, Integer> map = HashBiMap.create();
    for (int i = 0; i < 20; i++) {
      map.put("orig-" + i, i);
    }
    assertFalse(map.isUsingHashFloodingResistance());

    BiMap<Integer, Object> inverse = map.inverse();
    try {
      inverse.replaceAll(
          (v, k) -> {
            if (v < 15) {
              return new ConstantHashObject(v);
            }
            return new ConstantHashObject(0);
          });
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
    assertTrue(map.isUsingHashFloodingResistance());
    assertReplacedPrefix(map.inverse());
  }

  /** Asserts {@code map} holds exactly i -> ConstantHashObject(i) for i < 15, in order. */
  private static void assertReplacedPrefix(BiMap<Integer, Object> map) {
    ImmutableBiMap.Builder<Integer, Object> expectedBuilder = ImmutableBiMap.builder();
    for (int i = 0; i < 15; i++) {
      expectedBuilder.put(i, new ConstantHashObject(i));
    }
    ImmutableBiMap<Integer, Object> expected = expectedBuilder.build();
    assertThat(map.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
    assertThat(map.inverse().entrySet())
        .containsExactlyElementsIn(expected.inverse().entrySet())
        .inOrder();
    for (int i = 0; i < 15; i++) {
      assertEquals(Integer.valueOf(i), map.inverse().get(new ConstantHashObject(i)));
    }
  }

  public void testPutTriggersFloodingAndKeepsInsertionOrder() {
    HashBiMap<Object, Object> map = HashBiMap.create();
    for (int i = 0; i < 20; i++) {
      map.put(new ConstantHashObject(i), new ConstantHashObject(i + 100));
    }
    assertTrue(map.isUsingHashFloodingResistance());
    assertEquals(20, map.size());
    for (int i = 0; i < 20; i++) {
      assertEquals(new ConstantHashObject(i + 100), map.get(new ConstantHashObject(i)));
      assertEquals(new ConstantHashObject(i), map.inverse().get(new ConstantHashObject(i + 100)));
    }
    int expectedId = 0;
    for (Entry<Object, Object> entry : map.entrySet()) {
      assertEquals(new ConstantHashObject(expectedId++), entry.getKey());
    }
    map.remove(new ConstantHashObject(3));
    map.inverse().remove(new ConstantHashObject(105));
    assertEquals(18, map.size());
    assertFalse(map.containsKey(new ConstantHashObject(3)));
    assertFalse(map.containsKey(new ConstantHashObject(5)));
    map.clear();
    assertTrue(map.isEmpty());
    map.put(new ConstantHashObject(1), new ConstantHashObject(2));
    assertEquals(new ConstantHashObject(2), map.get(new ConstantHashObject(1)));
  }

  private static final class ConstantHashObject {
    final int id;

    ConstantHashObject(int id) {
      this.id = id;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ConstantHashObject && this.id == ((ConstantHashObject) obj).id;
    }
  }
}
