package it.mscuttari.kaoldb.core;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

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
        assertEquals(relationship.field, entityA.getField("oneToOne"));
    }


    @Test
    public void oneToOne_mapping_type() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(relationship.type, ONE_TO_ONE);
    }


    @Test
    public void oneToOne_mapping_localClass() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(relationship.local, entityA.clazz);
    }


    @Test
    public void oneToOne_mapping_linkedClass() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(relationship.linked, entityB.clazz);
    }


    @Test
    public void oneToOne_mapping_owning() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertTrue(relationship.owning);
    }


    @Test
    public void oneToOne_mapping_mappingField() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(relationship.mappingField, entityA.getField("oneToOne"));
    }


    @Test
    public void oneToOne_mapping_owningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(relationship.getOwningClass(), entityA.clazz);
    }


    @Test
    public void oneToOne_mapping_nonOwningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("oneToOne"));
        assertEquals(relationship.getNonOwningClass(), entityB.clazz);
    }


    @Test
    public void oneToOne_nonMapping_field() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(relationship.field, entityB.getField("oneToOne"));
    }


    @Test
    public void oneToOne_nonMapping_type() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(relationship.type, ONE_TO_ONE);
    }


    @Test
    public void oneToOne_nonMapping_localClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(relationship.local, entityB.clazz);
    }


    @Test
    public void oneToOne_nonMapping_linkedClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(relationship.linked, entityA.clazz);
    }


    @Test
    public void oneToOne_nonMapping_owning() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertFalse(relationship.owning);
    }


    @Test
    public void oneToOne_nonMapping_mappingField() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(relationship.mappingField, entityA.getField("oneToOne"));
    }


    @Test
    public void oneToOne_nonMapping_owningClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(relationship.getOwningClass(), entityA.clazz);
    }


    @Test
    public void oneToOne_nonMapping_nonOwningClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToOne"));
        assertEquals(relationship.getNonOwningClass(), entityB.clazz);
    }


    @Test
    public void manyToOne_field() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(relationship.field, entityA.getField("manyToOne"));
    }


    @Test
    public void manyToOne_type() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(relationship.type, MANY_TO_ONE);
    }


    @Test
    public void manyToOne_localClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(relationship.local, entityA.clazz);
    }


    @Test
    public void manyToOne_linkedClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(relationship.linked, entityB.clazz);
    }


    @Test
    public void manyToOne_owning() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertTrue(relationship.owning);
    }


    @Test
    public void manyToOne_mappingField() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(relationship.mappingField, entityA.getField("manyToOne"));
    }


    @Test
    public void manyToOne_owningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(relationship.getOwningClass(), entityA.clazz);
    }


    @Test
    public void manToOne_nonOwningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToOne"));
        assertEquals(relationship.getNonOwningClass(), entityB.clazz);
    }


    @Test
    public void oneToMany_field() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(relationship.field, entityB.getField("oneToMany"));
    }


    @Test
    public void oneToMany_type() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(relationship.type, ONE_TO_MANY);
    }


    @Test
    public void oneToMany_localClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(relationship.local, entityB.clazz);
    }


    @Test
    public void oneToMany_linkedClass() {
        Relationship relationship = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(relationship.linked, entityA.clazz);
    }


    @Test
    public void oneToMany_owning() {
        Relationship oneToOne = new Relationship(db, entityB.getField("oneToMany"));
        assertFalse(oneToOne.owning);
    }


    @Test
    public void oneToMany_mappingField() {
        Relationship oneToOne = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(oneToOne.mappingField, entityA.getField("manyToOne"));
    }


    @Test
    public void oneToMany_owningClass() {
        Relationship oneToOne = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(oneToOne.getOwningClass(), entityA.clazz);
    }


    @Test
    public void oneToMany_nonOwningClass() {
        Relationship oneToOne = new Relationship(db, entityB.getField("oneToMany"));
        assertEquals(oneToOne.getNonOwningClass(), entityB.clazz);
    }


    @Test
    public void manyToMany_mapping_field() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(relationship.field, entityA.getField("manyToMany"));
    }


    @Test
    public void manyToMany_mapping_type() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(relationship.type, MANY_TO_MANY);
    }


    @Test
    public void manyToMany_mapping_localClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(relationship.local, entityA.clazz);
    }


    @Test
    public void manyToMany_mapping_linkedClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(relationship.linked, entityB.clazz);
    }


    @Test
    public void manyToMany_mapping_owning() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertTrue(relationship.owning);
    }


    @Test
    public void manyToMany_mapping_mappingField() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(relationship.mappingField, entityA.getField("manyToMany"));
    }


    @Test
    public void manyToMany_mapping_owningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(relationship.getOwningClass(), entityA.clazz);
    }


    @Test
    public void manyToMany_mapping_nonOwningClass() {
        Relationship relationship = new Relationship(db, entityA.getField("manyToMany"));
        assertEquals(relationship.getNonOwningClass(), entityB.clazz);
    }


    @Test
    public void manyToMany_nonMapping_field() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(relationship.field, entityB.getField("manyToMany"));
    }


    @Test
    public void manyToMany_nonMapping_type() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(relationship.type, MANY_TO_MANY);
    }


    @Test
    public void manyToMany_nonMapping_localClass() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(relationship.local, entityB.clazz);
    }


    @Test
    public void manyToMany_nonMapping_linkedClass() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(relationship.linked, entityA.clazz);
    }


    @Test
    public void manyToMany_nonMapping_owning() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertFalse(relationship.owning);
    }


    @Test
    public void manyToMany_nonMapping_mappingField() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(relationship.mappingField, entityA.getField("manyToMany"));
    }


    @Test
    public void manyToMany_nonMapping_owningClass() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(relationship.getOwningClass(), entityA.clazz);
    }


    @Test
    public void manyToMany_nonMapping_nonOwningClass() {
        Relationship relationship = new Relationship(db, entityB.getField("manyToMany"));
        assertEquals(relationship.getNonOwningClass(), entityB.clazz);
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
        public Collection<EntityA> oneToMany;

        @ManyToMany(mappedBy = "manyToMany")
        public Collection<EntityA> manyToMany;
        
    }

}
