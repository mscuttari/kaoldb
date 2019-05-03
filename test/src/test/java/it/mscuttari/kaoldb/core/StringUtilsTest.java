package it.mscuttari.kaoldb.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class StringUtilsTest extends AbstractTest {

    @Test
    public void implode_regular() {
        Collection<Integer> data = new ArrayList<>();
        data.add(1);
        data.add(2);
        data.add(3);

        String result = StringUtils.implode(data, Object::toString, ", ");
        assertEquals("1, 2, 3", result);
    }


    @Test
    public void implode_emptyData() {
        Collection<Integer> data = Collections.emptyList();
        String result = StringUtils.implode(data, Object::toString, ", ");
        assertEquals("", result);
    }


    @Test
    public void implode_nullData() {
        String result = StringUtils.implode(null, Object::toString, ", ");
        assertEquals("", result);
    }


    @Test
    public void implode_nullStringConverter() {
        Collection<Integer> data = new ArrayList<>();
        data.add(1);
        data.add(2);
        data.add(3);

        String result = StringUtils.implode(data, null, ", ");
        assertEquals("1, 2, 3", result);
    }


    @Test
    public void implode_nullSeparator() {
        Collection<Integer> data = new ArrayList<>();
        data.add(1);
        data.add(2);
        data.add(3);

        String result = StringUtils.implode(data, Object::toString, null);
        assertEquals("1,2,3", result);
    }

}
