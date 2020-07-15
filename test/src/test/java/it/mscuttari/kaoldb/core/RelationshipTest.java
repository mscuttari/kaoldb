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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.annotations.Table;

import static it.mscuttari.kaoldb.core.Relationship.RelationshipType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RelationshipTest extends AbstractTest {

    private DatabaseObject db;
    private EntityObject<EntityA> entityA;
    private EntityObject<EntityB> entityB;

    @Before
    public void setUp() {
        db = new DatabaseObject();
        db.addEntityClass(EntityA.class);
        db.addEntityClass(EntityB.class);
        db.mapEntities();

        entityA = db.getEntity(EntityA.class);
        entityB = db.getEntity(EntityB.class);
    }

    @Test
    public void oneToOne_mapping_field() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(entityA.getField("oneToOne"), relationship.field);
    }

    @Test
    public void oneToOne_mapping_type() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(ONE_TO_ONE, relationship.type);
    }

    @Test
    public void oneToOne_mapping_localClass() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(entityA.clazz, relationship.local);
    }

    @Test
    public void oneToOne_mapping_linkedClass() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(entityB.clazz, relationship.linked);
    }

    @Test
    public void oneToOne_mapping_owning() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertTrue(relationship.owning);
    }

    @Test
    public void oneToOne_mapping_mappingField() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(entityA.getField("oneToOne"), relationship.mappingField);
    }

    @Test
    public void oneToOne_mapping_owningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(entityA.clazz, relationship.getOwningClass());
    }

    @Test
    public void oneToOne_mapping_nonOwningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(entityB.clazz, relationship.getNonOwningClass());
    }

    @Test
    public void oneToOne_nonMapping_field() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(entityB.getField("oneToOne"), relationship.field);
    }

    @Test
    public void oneToOne_nonMapping_type() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(ONE_TO_ONE, relationship.type);
    }

    @Test
    public void oneToOne_nonMapping_localClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(entityB.clazz, relationship.local);
    }

    @Test
    public void oneToOne_nonMapping_linkedClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(entityA.clazz, relationship.linked);
    }

    @Test
    public void oneToOne_nonMapping_owning() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertFalse(relationship.owning);
    }

    @Test
    public void oneToOne_nonMapping_mappingField() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(entityA.getField("oneToOne"), relationship.mappingField);
    }

    @Test
    public void oneToOne_nonMapping_owningClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(entityA.clazz, relationship.getOwningClass());
    }

    @Test
    public void oneToOne_nonMapping_nonOwningClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(entityB.clazz, relationship.getNonOwningClass());
    }

    @Test
    public void manyToOne_field() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(entityA.getField("manyToOne"), relationship.field);
    }

    @Test
    public void manyToOne_type() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(MANY_TO_ONE, relationship.type);
    }

    @Test
    public void manyToOne_localClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(entityA.clazz, relationship.local);
    }

    @Test
    public void manyToOne_linkedClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(entityB.clazz, relationship.linked);
    }

    @Test
    public void manyToOne_owning() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertTrue(relationship.owning);
    }

    @Test
    public void manyToOne_mappingField() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(entityA.getField("manyToOne"), relationship.mappingField);
    }

    @Test
    public void manyToOne_owningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(entityA.clazz, relationship.getOwningClass());
    }

    @Test
    public void manToOne_nonOwningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(entityB.clazz, relationship.getNonOwningClass());
    }

    @Test
    public void oneToMany_field() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(entityB.getField("oneToMany"), relationship.field);
    }

    @Test
    public void oneToMany_type() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(ONE_TO_MANY, relationship.type);
    }

    @Test
    public void oneToMany_localClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(entityB.clazz, relationship.local);
    }

    @Test
    public void oneToMany_linkedClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(entityA.clazz, relationship.linked);
    }

    @Test
    public void oneToMany_owning() {
        Relationship oneToOne = new Relationship(db, entityB.getField("oneToMany"));
        assertFalse(oneToOne.owning);
    }

    @Test
    public void oneToMany_mappingField() {
        Relationship oneToOne = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(entityA.getField("manyToOne"), oneToOne.mappingField);
    }

    @Test
    public void oneToMany_owningClass() {
        Relationship oneToOne = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(entityA.clazz, oneToOne.getOwningClass());
    }

    @Test
    public void oneToMany_nonOwningClass() {
        Relationship oneToOne = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(entityB.clazz, oneToOne.getNonOwningClass());
    }

    @Test
    public void manyToMany_mapping_field() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(entityA.getField("manyToMany"), relationship.field);
    }

    @Test
    public void manyToMany_mapping_type() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(MANY_TO_MANY, relationship.type);
    }

    @Test
    public void manyToMany_mapping_localClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(entityA.clazz, relationship.local);
    }

    @Test
    public void manyToMany_mapping_linkedClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(entityB.clazz, relationship.linked);
    }

    @Test
    public void manyToMany_mapping_owning() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertTrue(relationship.owning);
    }

    @Test
    public void manyToMany_mapping_mappingField() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(entityA.getField("manyToMany"), relationship.mappingField);
    }

    @Test
    public void manyToMany_mapping_owningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(entityA.clazz, relationship.getOwningClass());
    }

    @Test
    public void manyToMany_mapping_nonOwningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(entityB.clazz, relationship.getNonOwningClass());
    }

    @Test
    public void manyToMany_nonMapping_field() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(entityB.getField("manyToMany"), relationship.field);
    }

    @Test
    public void manyToMany_nonMapping_type() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(MANY_TO_MANY, relationship.type);
    }

    @Test
    public void manyToMany_nonMapping_localClass() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(entityB.clazz, relationship.local);
    }

    @Test
    public void manyToMany_nonMapping_linkedClass() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(entityA.clazz, relationship.linked);
    }

    @Test
    public void manyToMany_nonMapping_owning() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertFalse(relationship.owning);
    }

    @Test
    public void manyToMany_nonMapping_mappingField() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(entityA.getField("manyToMany"), relationship.mappingField);
    }

    @Test
    public void manyToMany_nonMapping_owningClass() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(entityA.clazz, relationship.getOwningClass());
    }

    @Test
    public void manyToMany_nonMapping_nonOwningClass() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(entityB.clazz, relationship.getNonOwningClass());
    }

    @Entity
    @Table(name = "table_a")
    private static class EntityA {

        @Id
        @Column(name = "id")
        public long id;

        @OneToOne
        @JoinColumn(name = "oneToOne_id", referencedColumnName = "id")
        public EntityB oneToOne;

        @ManyToOne
        @JoinColumn(name = "manyToOne_id", referencedColumnName = "id")
        public EntityB manyToOne;

        @ManyToMany
        @JoinTable(
                name = "manyToMany_A_B",
                joinClass = it.mscuttari.examples.generic.EntityA.class,
                joinColumns = {@JoinColumn(name = "a_id", referencedColumnName = "id")},
                inverseJoinClass = it.mscuttari.examples.generic.EntityB.class,
                inverseJoinColumns = {@JoinColumn(name = "b_id", referencedColumnName = "id")}
        )
        public Collection<EntityB> manyToMany;

    }


    @Entity
    @Table(name = "table_b")
    private static class EntityB {

        @Id
        @Column(name = "id")
        public long id;

        @OneToOne(mappedBy = "oneToOne")
        public EntityA oneToOne;

        @OneToMany(mappedBy = "manyToOne")
        public List<EntityA> oneToMany;

        @ManyToMany(mappedBy = "manyToMany")
        public CustomCollectionA manyToMany;

        private static class CustomCollectionA extends ArrayList<EntityA> {

        }

    }

}
