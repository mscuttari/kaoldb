package it.mscuttari.kaoldb.core;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ConcatIteratorTest extends AbstractTest {

    private List<Integer> list1;
    private List<Integer> list2;
    private Iterator<Integer> iterator;


    @Before
    public void setUp() {
        list1 = new ArrayList<>();
        list2 = new ArrayList<>();
        iterator = new ConcatIterator<>(list1, list2);
    }


    @Test(expected = NoSuchElementException.class)
    public void bothEmpty() {
        assertFalse(iterator.hasNext());
        iterator.next();
    }


    @Test
    public void firstEmpty() {
        list2.add(1);
        list2.add(2);

        assertEquals(iterator.next().intValue(), 1);
        assertEquals(iterator.next().intValue(), 2);
    }


    @Test
    public void secondEmpty() {
        list1.add(1);
        list1.add(2);

        assertEquals(iterator.next().intValue(), 1);
        assertEquals(iterator.next().intValue(), 2);
    }


    @Test
    public void bothNotEmpty() {
        list1.add(1);
        list1.add(2);
        list2.add(3);
        list2.add(4);

        assertEquals(iterator.next().intValue(), 1);
        assertEquals(iterator.next().intValue(), 2);
        assertEquals(iterator.next().intValue(), 3);
        assertEquals(iterator.next().intValue(), 4);
    }


    @Test
    public void removeFromFirst() {
        list1.add(1);
        list1.add(2);
        list2.add(3);

        iterator.next();
        iterator.next();
        iterator.remove();
        assertEquals(iterator.next().intValue(), 3);
    }


    @Test
    public void removeFromSecond() {
        list1.add(1);
        list2.add(2);
        list2.add(3);

        iterator.next();
        iterator.next();
        iterator.remove();
        assertEquals(iterator.next().intValue(), 3);
    }


    @Test(expected = IllegalStateException.class)
    public void removeWithNoNextDone() {
        iterator.remove();
    }

}
