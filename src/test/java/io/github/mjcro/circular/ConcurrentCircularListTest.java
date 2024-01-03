package io.github.mjcro.circular;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

public class ConcurrentCircularListTest {
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIncorrectCapacity() {
        new ConcurrentCircularList<String>(0);
    }

    @Test(dependsOnMethods = "testIncorrectCapacity")
    public void testBasic() {
        ConcurrentCircularList<String> strings = new ConcurrentCircularList<>(2);

        Assert.assertTrue(strings.isEmpty());
        Assert.assertEquals(strings.size(), 0);
        Assert.assertEquals(strings.getCount(), 0);

        strings.add("a");
        Assert.assertFalse(strings.isEmpty());
        Assert.assertEquals(strings.size(), 1);
        Assert.assertEquals(strings.getCount(), 1);
        Assert.assertEquals(strings.get(0), "a");
        Assert.assertTrue(strings.contains("a"));
        Assert.assertEquals(String.join(",", strings), "a");

        strings.add("b");
        Assert.assertFalse(strings.isEmpty());
        Assert.assertEquals(strings.size(), 2);
        Assert.assertEquals(strings.getCount(), 2);
        Assert.assertEquals(strings.get(0), "a");
        Assert.assertEquals(strings.get(1), "b");
        Assert.assertTrue(strings.contains("a"));
        Assert.assertEquals(String.join(",", strings), "a,b");

        strings.add("c");
        Assert.assertFalse(strings.isEmpty());
        Assert.assertEquals(strings.size(), 2);
        Assert.assertEquals(strings.getCount(), 3);
        Assert.assertEquals(strings.get(0), "c");
        Assert.assertEquals(strings.get(1), "b");
        Assert.assertFalse(strings.contains("a"));
        Assert.assertEquals(String.join(",", strings), "b,c");

        strings.add("d");
        Assert.assertFalse(strings.isEmpty());
        Assert.assertEquals(strings.size(), 2);
        Assert.assertEquals(strings.getCount(), 4);
        Assert.assertEquals(strings.get(0), "c");
        Assert.assertEquals(strings.get(1), "d");
        Assert.assertFalse(strings.contains("a"));
        Assert.assertEquals(String.join(",", strings), "c,d");

        strings.clear();
        Assert.assertTrue(strings.isEmpty());
        Assert.assertEquals(strings.size(), 0);
        Assert.assertEquals(strings.getCount(), 0);
    }

    @Test(dependsOnMethods = "testBasic")
    public void testBigCircularity() {
        CircularList<Integer> ints = new CircularList<>(5);
        for (int i = 1; i <= 100; i++) {
            ints.add(i);
            if (i > 4) {
                Assert.assertEquals(
                        ints.stream().map(Object::toString).collect(Collectors.joining(",")),
                        (i - 4) + "," + (i - 3) + "," + (i - 2) + "," + (i - 1) + "," + i
                );
            }
        }

        Assert.assertEquals(ints.size(), 5);
        Assert.assertEquals(ints.getCount(), 100);
    }

    @Test(dependsOnMethods = "testBasic")
    public void testNoConcurrencyErrors() throws InterruptedException {
        ConcurrentCircularList<Integer> ints = new ConcurrentCircularList<>(5);
        Thread[] threads = new Thread[10];

        for (int x = 0; x < threads.length; x++) {
            threads[x] = new Thread(() -> {
                for (int i = 1; i <= 100; i++) {
                    ints.add(i);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Assert.assertEquals(ints.size(), 5);
        Assert.assertEquals(ints.getCount(), 1000);
    }

    @Test(dependsOnMethods = "testBasic")
    public void testPrefill() {
        ArrayList<String> initial = new ArrayList<>();
        initial.add("a");
        initial.add("b");
        initial.add("c");
        initial.add("d");

        ConcurrentCircularList<String> strings = new ConcurrentCircularList<>(3, initial);
        Assert.assertFalse(strings.isEmpty());
        Assert.assertEquals(strings.size(), 3);
        Assert.assertEquals(strings.getCount(), 4);
        Assert.assertEquals(strings.get(0), "d");
        Assert.assertEquals(strings.get(1), "b");
        Assert.assertEquals(strings.get(2), "c");
        Assert.assertEquals(String.join(",", strings), "b,c,d");
    }

    @Test(dependsOnMethods = "testBasic")
    public void testContainsAll() {
        ConcurrentCircularList<String> strings = new ConcurrentCircularList<>(2);
        strings.add("a");
        strings.add("b");

        Assert.assertTrue(strings.containsAll(Collections.singleton("a")));
        Assert.assertTrue(strings.containsAll(Collections.singleton("b")));
        Assert.assertFalse(strings.containsAll(Collections.singleton("c")));

        ArrayList<String> t1 = new ArrayList<>();
        t1.add("a");
        t1.add("b");
        Assert.assertTrue(strings.containsAll(t1));

        ArrayList<String> t2 = new ArrayList<>();
        t2.add("a");
        t2.add("b");
        t2.add("c");
        Assert.assertFalse(strings.containsAll(t2));

        ArrayList<String> t3 = new ArrayList<>();
        t3.add("b");
        t3.add("c");
        Assert.assertFalse(strings.containsAll(t3));
    }

    @Test(dependsOnMethods = "testBasic")
    public void testToArray() {
        ConcurrentCircularList<String> strings = new ConcurrentCircularList<>(3);
        strings.add("a");
        Assert.assertEquals(strings.toArray(), new Object[]{"a"});

        strings.add("b");
        Assert.assertEquals(strings.toArray(), new Object[]{"a", "b"});

        strings.add("c");
        Assert.assertEquals(strings.toArray(), new Object[]{"a", "b", "c"});

        strings.add("d");
        Assert.assertEquals(strings.toArray(), new Object[]{"b", "c", "d"});

        strings.add("e");
        Assert.assertEquals(strings.toArray(), new Object[]{"c", "d", "e"});

        strings.add("f");
        Assert.assertEquals(strings.toArray(), new Object[]{"d", "e", "f"});

        strings.add("g");
        Assert.assertEquals(strings.toArray(), new Object[]{"e", "f", "g"});
    }

    @Test(dependsOnMethods = "testToArray")
    public void testToArrayGeneric() {
        ConcurrentCircularList<String> strings = new ConcurrentCircularList<>(2);
        strings.add("a");
        Assert.assertEquals(strings.toArray(new String[0]), new String[]{"a"});

        strings.add("b");
        Assert.assertEquals(strings.toArray(new String[0]), new String[]{"a", "b"});

        strings.add("c");
        Assert.assertEquals(strings.toArray(new String[0]), new String[]{"b", "c"});
        Assert.assertEquals(strings.toArray(new String[1]), new String[]{"b", "c"});
        Assert.assertEquals(strings.toArray(new String[2]), new String[]{"b", "c"});
        Assert.assertEquals(strings.toArray(new String[3]), new String[]{"b", "c", null});
    }

    @Test(dependsOnMethods = "testBasic")
    public void testTailFunctions() {
        ConcurrentCircularList<String> strings = new ConcurrentCircularList<>(3);
        Assert.assertEquals(strings.tailStream(2).collect(Collectors.joining(",")), "");

        strings.add("a");
        Assert.assertEquals(strings.tailStream(2).collect(Collectors.joining(",")), "a");

        strings.add("b");
        Assert.assertEquals(strings.tailStream(2).collect(Collectors.joining(",")), "a,b");

        strings.add("c");
        Assert.assertEquals(strings.tailStream(2).collect(Collectors.joining(",")), "b,c");

        strings.add("d");
        Assert.assertEquals(strings.tailStream(2).collect(Collectors.joining(",")), "c,d");
        Assert.assertEquals(strings.tailStream(3).collect(Collectors.joining(",")), "b,c,d");
        Assert.assertEquals(strings.tailStream(4).collect(Collectors.joining(",")), "b,c,d");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnsupportedRemove() {
        new ConcurrentCircularList<String>(2).remove("foo");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnsupportedRemoveAll() {
        new ConcurrentCircularList<String>(2).removeAll(new ArrayList<>());
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testUnsupportedRetainAll() {
        new ConcurrentCircularList<String>(2).retainAll(new ArrayList<>());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidTail() {
        new ConcurrentCircularList<String>(5).tailStream(0);
    }
}
