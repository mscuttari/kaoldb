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

import org.junit.Test;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.Table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VariableTest extends AbstractTest {

    @Test
    public void withProperty_checkAlias() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("id"));
        Variable<Long> variable = new Variable<>("alias", property);
        assertEquals("alias", variable.getTableAlias());
    }

    @Test
    public void withProperty_hasProperty() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("id"));
        Variable<Long> variable = new Variable<>("alias", property);
        assertTrue(variable.hasProperty());
    }

    @Test
    public void withProperty_checkProperty() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("id"));
        Variable<Long> variable = new Variable<>("alias", property);
        assertEquals(property, variable.getProperty());
    }

    @Test
    public void withProperty_nullRawData() throws Exception {
        Property<EntityA, Long> property = new SingleProperty<>(EntityA.class, Long.class, EntityA.class.getField("id"));
        Variable<Long> variable = new Variable<>("alias", property);
        assertNull(variable.getRawData());
    }

    @Test
    public void withRawData_nullTableAlias() {
        Variable<Integer> variable = new Variable<>(0);
        assertNull(variable.getTableAlias());
    }

    @Test
    public void withRawData_hasNoProperty() {
        Variable<Integer> variable = new Variable<>(0);
        assertFalse(variable.hasProperty());
    }

    @Test
    public void withRawData_nullProperty() {
        Variable<Integer> variable = new Variable<>(0);
        assertNull(variable.getProperty());
    }

    @Test
    public void withRawData_checkRawData() {
        Variable<Integer> variable = new Variable<>(0);
        assertEquals((Integer) 0, variable.getRawData());
    }

    @Entity
    @Table(name = "table_a")
    private static class EntityA {

        @Id
        @Column(name = "id")
        public long id;

    }

}
