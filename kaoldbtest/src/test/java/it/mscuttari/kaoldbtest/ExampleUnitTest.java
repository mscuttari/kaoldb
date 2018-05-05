package it.mscuttari.kaoldbtest;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.internal.runner.InstrumentationConnection;

import org.junit.Test;

import it.mscuttari.kaoldb.core.EntityManagerFactory;
import it.mscuttari.kaoldb.core.KaolDB;
import it.mscuttari.kaoldb.interfaces.EntityManager;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }
}