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

package it.mscuttari.kaoldb.mapping;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;

import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.exceptions.MappingException;

import static it.mscuttari.kaoldb.mapping.Propagation.Action.CASCADE;
import static it.mscuttari.kaoldb.mapping.Propagation.Action.RESTRICT;
import static it.mscuttari.kaoldb.mapping.Propagation.Action.SET_NULL;

/**
 * This class allows to group the join columns that are declared together.
 *
 * @see JoinColumns
 */
final class JoinColumnsObject extends Columns {

    /** Field annotated with {@link JoinColumns} */
    public final Field field;

    /** Foreign keys constraints */
    public final Propagation propagation;

    /**
     * Constructor.
     *
     * @param db        database
     * @param entity    entity the columns belong to
     * @param field     field the columns are generated from
     */
    public JoinColumnsObject(@NonNull DatabaseObject db,
                              @NonNull EntityObject<?> entity,
                              @NonNull Field field) {

        super(entity);
        this.field = field;

        if (field.isAnnotationPresent(OneToOne.class)) {
            OneToOne annotation = field.getAnnotation(OneToOne.class);
            propagation = new Propagation(CASCADE, annotation.optional() ? SET_NULL : RESTRICT);

        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            ManyToOne annotation = field.getAnnotation(ManyToOne.class);
            propagation = new Propagation(CASCADE, annotation.optional() ? SET_NULL : RESTRICT);

        } else {
            // Normally not reachable
            throw new MappingException("@OneToOne or @ManyToOne annotations not found on field \"" + field.getName() + "\"");
        }

        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);

        for (JoinColumn joinColumnAnnotation : joinColumnsAnnotation.value()) {
            add(new JoinColumnObject(db, entity, field, joinColumnAnnotation));
        }
    }

    @Override
    public void map() {
        for (BaseColumnObject column : this) {
            column.map();
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

}
