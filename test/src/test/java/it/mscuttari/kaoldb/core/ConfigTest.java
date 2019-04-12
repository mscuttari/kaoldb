package it.mscuttari.kaoldb.core;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigTest extends AbstractTest {

    private Config config;

    @Before
    public void setUp() throws Exception {
        config = new Config();
    }


    @Test
    public void debugEnabled() {
        config.setDebugMode(true);
        assertTrue(config.isDebugEnabled());
    }


    @Test
    public void debugNotEnabled() {
        config.setDebugMode(false);
        assertFalse(config.isDebugEnabled());
    }

}
