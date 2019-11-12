/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.core;

import org.junit.Before;
import org.junit.Test;

import it.mscuttari.examples.database.GenericDbMigrator;
import it.mscuttari.examples.generic.EntityA;
import it.mscuttari.examples.generic.EntityB;
import it.mscuttari.examples.generic.NotAnEntity;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatabaseObjectTest extends AbstractTest {

    private DatabaseObject db;

    @Before
    public void setUp() {
        db = new DatabaseObject();
    }


    @Test(expected = IllegalStateException.class)
    public void getNameNotSet() {
        db.getName();
    }


    @Test(expected = IllegalArgumentException.class)
    public void setNullName() {
        db.setName(null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void setEmptyName() {
        db.setName("");
    }


    @Test
    public void setName() {
        db.setName("Name");
        assertEquals(db.getName(), "Name");
    }


    @Test(expected = IllegalStateException.class)
    public void getVersionNotSet() {
        db.getVersion();
    }


    @Test(expected = IllegalArgumentException.class)
    public void setNullVersion() {
        db.setVersion(null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void setVersionLessThanZero() {
        db.setVersion(-1);
    }


    @Test
    public void setVersion() {
        db.setVersion(0);
        assertEquals(db.getVersion(), 0);
    }


    @Test(expected = IllegalStateException.class)
    public void getSchemaMigratorNotSet() {
        db.getSchemaMigrator();
    }


    @Test(expected = IllegalArgumentException.class)
    public void setNullSchemaMigrator() {
        db.setSchemaMigrator(null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void setAbstractSchemaMigrator() {
        db.setSchemaMigrator(SchemaMigratorAbstractStub.class);
    }


    @Test
    public void setSchemaMigrator() {
        db.setSchemaMigrator(GenericDbMigrator.class);
        assertEquals(db.getSchemaMigrator(), GenericDbMigrator.class);
    }


    @Test(expected = IllegalArgumentException.class)
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
        assertTrue(db.contains(EntityA.class));
    }


    @Test(expected = IllegalArgumentException.class)
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
