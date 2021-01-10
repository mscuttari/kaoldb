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

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorType;
import it.mscuttari.kaoldb.interfaces.EntityManager;

import static it.mscuttari.kaoldb.ConcurrentSession.doAndNotifyAll;

/**
 * Column generated in case the discriminator column of an entity doesn't already exist among
 * the ones generated through class fields.
 */
final class DiscriminatorColumnObject extends BaseColumnObject {

    /** Discriminator column annotation */
    private final DiscriminatorColumn annotation;

    /**
     * Constructor to create a column that is not linked to a field.
     *
     * @param db        database
     * @param entity    entity the column belongs to
     */
    public DiscriminatorColumnObject(@NonNull DatabaseObject db,
                                     @NonNull EntityObject<?> entity) {

        super(db, entity);

        this.annotation = entity.clazz.getAnnotation(DiscriminatorColumn.class);

        if (this.annotation == null) {
            throw new IllegalArgumentException("Entitiy \"" + entity.getName() + "\" has no discriminator column");
        }

        this.name = annotation.name();
    }

    @Override
    protected void mapAsync() {

    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    protected void loadCustomColumnDefinition() {
        doAndNotifyAll(this, () -> customColumnDefinition = null);
    }

    @Override
    protected void loadType() {
        doAndNotifyAll(this, () -> {
            if (annotation.discriminatorType() == DiscriminatorType.CHAR) {
                type = Character.class;
            } else if (annotation.discriminatorType() == DiscriminatorType.STRING) {
                type = String.class;
            } else if (annotation.discriminatorType() == DiscriminatorType.INTEGER) {
                type = Integer.class;
            }
        });
    }

    @Override
    protected void loadNullableProperty() {
        doAndNotifyAll(this, () -> nullable = false);
    }

    @Override
    protected void loadPrimaryKeyProperty() {
        doAndNotifyAll(this, () -> primaryKey = false);
    }

    @Override
    protected void loadUniqueProperty() {
        doAndNotifyAll(this, () -> unique = false);
    }

    @Override
    protected void loadDefaultValue() {
        doAndNotifyAll(this, () -> defaultValue = null);
    }

    @Nullable
    @Override
    public Object getValue(Object obj) {
        Class<?> clazz = obj.getClass();
        EntityObject<?> entity = db.getEntity(clazz);

        for (EntityObject<?> child : entity.children) {
            if (child.clazz.isAssignableFrom(clazz)) {
                return child.discriminatorValue;
            }
        }

        throw new IllegalStateException("Discriminator value not found for class " + obj.getClass().getSimpleName());
    }

    @Override
    public void setValue(Object obj, Object value) {
        // Nothing to be done (there is no real field to bet set)
    }

    @Override
    public boolean hasRelationship() {
        return false;
    }

    @Override
    public boolean isDataExisting(Object obj, EntityManager entityManager) {
        return true;
    }

    @Override
    public void addToContentValues(@NonNull ContentValues cv, Object obj) {
        Class<?> clazz = obj.getClass();

        for (EntityObject<?> child : entity.children) {
            if (child.clazz.isAssignableFrom(clazz)) {
                insertIntoContentValues(cv, name, child.discriminatorValue);
                break;
            }
        }
    }

    @Override
    public <T> T accept(ColumnsContainer.Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
