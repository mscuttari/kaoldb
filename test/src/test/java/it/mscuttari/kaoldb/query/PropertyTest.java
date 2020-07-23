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

package it.mscuttari.kaoldb.query;

import org.junit.Test;

import java.util.Collection;
import java.util.List;

import it.mscuttari.kaoldb.AbstractTest;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.annotations.Table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PropertyTest extends AbstractTest {

    @Test
    public void columnAnnotation_column() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("id"));
        assertEquals(Column.class, property.columnAnnotation);
    }

    @Test
    public void columnAnnotation_joinColumn() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("a"));
        assertEquals(JoinColumn.class, property.columnAnnotation);
    }

    @Test
    public void columnAnnotation_joinColumns() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("b"));
        assertEquals(JoinColumns.class, property.columnAnnotation);
    }

    @Test
    public void columnAnnotation_joinTable() throws Exception {
        Property<EntityA, Long> property = new CollectionProperty<>(EntityA.class, Long.class, EntityA.class.getField("c"));
        assertEquals(JoinTable.class, property.columnAnnotation);
    }

    @Test
    public void singleProperty_columnAnnotation_noColumn() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("noAnnotations"));
        assertNull(property.columnAnnotation);
    }

    @Test
    public void relationshipAnnotation_oneToOne() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("a"));
        assertEquals(OneToOne.class, property.relationshipAnnotation);
    }

    @Test
    public void relationshipAnnotation_oneToMany() throws Exception {
        Property<EntityB, Long> property = new CollectionProperty<>(EntityB.class, Long.class, EntityB.class.getField("a"));
        assertEquals(OneToMany.class, property.relationshipAnnotation);
    }

    @Test
    public void relationshipAnnotation_manyToOne() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("b"));
        assertEquals(ManyToOne.class, property.relationshipAnnotation);
    }

    @Test
    public void relationshipAnnotation_manyToMany() throws Exception {
        Property<EntityA, Long> property = new CollectionProperty<>(EntityA.class, Long.class, EntityA.class.getField("d"));
        assertEquals(ManyToMany.class, property.relationshipAnnotation);
    }

    @Test
    public void relationshipAnnotation_none() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("id"));
        assertNull(property.relationshipAnnotation);
    }

    @Entity
    @Table(name = "table_a")
    private static class EntityA {

        @Id
        @Column(name = "id")
        public long id;

        @OneToOne
        @JoinColumn(name = "oneToOne_id1", referencedColumnName = "id1")
        public EntityB a;

        @ManyToOne
        @JoinColumns(value = {
                @JoinColumn(name = "manyToOne_id1", referencedColumnName = "id1"),
                @JoinColumn(name = "manyToOne_id2", referencedColumnName = "id2")
        })
        public EntityB b;

        @ManyToOne
        @JoinTable(
                name = "manyToMany_A_B",
                joinClass = EntityA.class,
                joinColumns = {@JoinColumn(name = "a_id", referencedColumnName = "id")},
                inverseJoinClass = EntityB.class,
                inverseJoinColumns = {
                        @JoinColumn(name = "b_id1", referencedColumnName = "id1"),
                        @JoinColumn(name = "b_id2", referencedColumnName = "id2")
                }
        )
        public EntityB c;

        @ManyToMany
        @JoinTable(
                name = "manyToMany_A_B",
                joinClass = EntityA.class,
                joinColumns = {@JoinColumn(name = "a_id", referencedColumnName = "id")},
                inverseJoinClass = EntityB.class,
                inverseJoinColumns = {
                        @JoinColumn(name = "b_id1", referencedColumnName = "id1"),
                        @JoinColumn(name = "b_id2", referencedColumnName = "id2")
                }
        )
        public Collection<EntityB> d;

        public String noAnnotations;

    }

    @Entity
    @Table(name = "table_b")
    private static class EntityB {

        @Id
        @Column(name = "id1")
        public long id1;

        @Id
        @Column(name = "id2")
        public long id2;

        @OneToMany(mappedBy = "manyToOne")
        public List<EntityA> a;

    }

}
