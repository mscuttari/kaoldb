package it.mscuttari.kaoldb.core;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import it.mscuttari.examples.database.GenericDbMigrator;
import it.mscuttari.examples.generic.EntityA;
import it.mscuttari.examples.generic.EntityB;
import it.mscuttari.examples.generic.NotAnEntity;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DatabaseObjectTest extends AbstractTest {

    private DatabaseObject db;

    @Before
    public void setUp() {
        db = new DatabaseObject();
    }


    @Test(expected = InvalidConfigException.class)
    public void getNameNotSet() {
        db.getName();
    }


    @Test(expected = InvalidConfigException.class)
    public void setNullName() {
        db.setName(null);
    }


    @Test(expected = InvalidConfigException.class)
    public void setEmptyName() {
        db.setName("");
    }


    @Test
    public void setName() {
        db.setName("Name");
        assertEquals(db.getName(), "Name");
    }


    @Test(expected = InvalidConfigException.class)
    public void getVersionNotSet() {
        db.getVersion();
    }


    @Test(expected = InvalidConfigException.class)
    public void setNullVersion() {
        db.setVersion(null);
    }


    @Test(expected = InvalidConfigException.class)
    public void setVersionLessThanZero() {
        db.setVersion(-1);
    }


    @Test
    public void setVersion() {
        db.setVersion(0);
        assertEquals(db.getVersion(), 0);
    }


    @Test(expected = InvalidConfigException.class)
    public void getSchemaMigratorNotSet() {
        db.getSchemaMigrator();
    }


    @Test(expected = InvalidConfigException.class)
    public void setNullSchemaMigrator() {
        db.setSchemaMigrator(null);
    }


    @Test(expected = InvalidConfigException.class)
    public void setAbstractSchemaMigrator() {
        db.setSchemaMigrator(SchemaMigratorAbstractStub.class);
    }


    @Test
    public void setSchemaMigrator() {
        db.setSchemaMigrator(GenericDbMigrator.class);
        assertEquals(db.getSchemaMigrator(), GenericDbMigrator.class);
    }


    @Test(expected = InvalidConfigException.class)
    public void addNonEntityClass() {
        db.addEntityClass(NotAnEntity.class);
    }


    @Test
    public void addNullEntityClass() {
        db.addEntityClass(null);
    }


    @Test
    public void addEntityClass() {
        db.addEntityClass(EntityA.class);
        assertTrue(db.getEntityClasses().contains(EntityA.class));
    }


    @Test(expected = InvalidConfigException.class)
    public void getNonExistingEntity() {
        db.getEntity(EntityA.class);
    }


    @Test
    public void mapEntities() {
        db.addEntityClass(EntityA.class);
        db.addEntityClass(EntityB.class);

        db.mapEntities();

        assertEquals(db.getEntities().size(), 2);

        assertNotNull(db.getEntity(EntityA.class));
        assertNotNull(db.getEntity(EntityB.class));
    }


    private static abstract class SchemaMigratorAbstractStub implements DatabaseSchemaMigrator {

    }

}
