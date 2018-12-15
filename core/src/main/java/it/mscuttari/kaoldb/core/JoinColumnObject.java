package it.mscuttari.kaoldb.core;

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;

import static it.mscuttari.kaoldb.core.PojoAdapter.insertDataIntoContentValues;

/**
 * This class allows to map a column acting as a foreign key
 *
 * @see JoinColumn
 */
final class JoinColumnObject extends BaseColumnObject {

    /** The {@link JoinColumn} annotation that is responsible of this column properties */
    @NonNull private final JoinColumn annotation;

    /**
     * Propagation actions for foreign keys
     * Null if the column is not a foreign key
     */
    @Nullable public final Propagation propagation;


    /**
     * Constructor
     *
     * @param db            database
     * @param entity        entity the column belongs to
     * @param field         field the column is generated from
     * @param annotation    JoinColumn annotation the column is generated from
     */
    public JoinColumnObject(@NonNull DatabaseObject db,
                            @NonNull EntityObject entity,
                            @NonNull Field field,
                            @NonNull JoinColumn annotation) {

        super(db, entity, field,
                getColumnName(field, annotation),
                getCustomColumnDefinition(annotation),
                getType(field),
                getNullableProperty(field, annotation),
                getPrimaryKeyProperty(field),
                getUniqueProperty(annotation),
                getDefaultValue(annotation)
        );

        this.annotation = annotation;
        this.propagation = initializePropagation();
    }


    /**
     * Get column name
     *
     * @param field         field the column is generated from
     * @param annotation    JoinColumn annotation the column is generated from
     *
     * @return column name
     */
    @NonNull
    private static String getColumnName(@NonNull Field field, @NonNull JoinColumn annotation) {
        return annotation.name().isEmpty() ? getDefaultName(field) : annotation.name();
    }


    /**
     * Get custom column definition
     *
     * @param annotation    JoinColumn annotation the column is generated from
     * @return custom column definition (null if not provided)
     */
    @Nullable
    private static String getCustomColumnDefinition(@NonNull JoinColumn annotation) {
        return annotation.columnDefinition().isEmpty() ? null : annotation.columnDefinition();
    }


    /**
     * Get column type.
     * The type must be checked later by using {@link #fixType(Map)}.
     *
     * @param field     field the column is generated from
     * @return column type
     */
    @NonNull
    private static Class<?> getType(@NonNull Field field) {
        return field.getType();
    }


    /**
     * Get nullable property
     *
     * @param field         field the column is generated from
     * @param annotation    JoinColumn annotation the column is generated from
     *
     * @return  whether the column is nullable or not
     */
    private static boolean getNullableProperty(@NonNull Field field, @NonNull JoinColumn annotation) {
        // If the columns is nullable by itself, then the relationship optionality doesn't matter
        if (annotation.nullable())
            return true;

        // A non nullable may be null if the relationship is optional
        if (field.isAnnotationPresent(OneToOne.class)) {
            return field.getAnnotation(OneToOne.class).optional();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            return field.getAnnotation(ManyToOne.class).optional();
        }

        return false;
    }


    /**
     * Get primary key property
     *
     * @param field     field the column is generated from
     * @return whether the column is a primary key or not
     */
    private static boolean getPrimaryKeyProperty(@NonNull Field field) {
        return field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(JoinTable.class);
    }


    /**
     * Get unique property
     *
     * @param annotation    JoinColumn annotation the column is generated from
     * @return whether the column value should be unique or not
     */
    private static boolean getUniqueProperty(@NonNull JoinColumn annotation) {
        return annotation.unique();
    }


    /**
     * Get default value
     *
     * @param annotation    JoinColumn annotation the column is generated from
     * @return column default value (null if not provided)
     */
    @Nullable
    private static String getDefaultValue(@NonNull JoinColumn annotation) {
        return annotation.defaultValue().isEmpty() ? null : annotation.defaultValue();
    }


    /**
     * Get the foreign key policy to be used during the table creation phase
     *
     * @return SQL statement (null if there is no custom column definition)
     */
    private Propagation initializePropagation() {
        if (field.isAnnotationPresent(JoinTable.class)) {
            return new Propagation(Propagation.Action.CASCADE, Propagation.Action.CASCADE);
        }

        if (nullable) {
            return new Propagation(Propagation.Action.CASCADE, Propagation.Action.SET_NULL);
        } else {
            return new Propagation(Propagation.Action.CASCADE, Propagation.Action.RESTRICT);
        }
    }


    @Override
    public void fixType(Map<Class<?>, EntityObject> entities) {
        // Class doesn't have column field
        if (field == null)
            return;

        // Determine the correct column type

        Class<?> referencedClass = null;
        String referencedColumnName = null;

        if (field.isAnnotationPresent(JoinColumn.class) || field.isAnnotationPresent(JoinColumns.class)) {
            // The linked column directly belongs to the linked entity class, so to determine the
            // column type is sufficient to search for that foreign key and get its type

            referencedColumnName = annotation.referencedColumnName();
            referencedClass = type;

        } else if (field.isAnnotationPresent(JoinTable.class)) {
            // The linked column belongs to a middle join table, so to determine the column type
            // we need to first determine that column class. Its class is indeed derived from the
            // foreign key of the second entity

            referencedColumnName = annotation.referencedColumnName();
            JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

            // We need to determine if the column is a direct or inverse join column.
            // If it is a direct join column, the column type is the same of the field enclosing class.
            // If it is an inverse join column, the column type is the field one.

            boolean direct = false;

            // Search in the direct join columns.
            // If not found, then the column must be an inverse join one.

            for (JoinColumn joinColumn : joinTableAnnotation.joinColumns()) {
                if (joinColumn.equals(annotation)) {
                    direct = true;
                    break;
                }
            }

            if (direct) {
                referencedClass = field.getDeclaringClass();
            } else {
                referencedClass = type;
            }
        }

        if (referencedClass == null) {
            // Normally not reachable. Just a security check
            throw new InvalidConfigException("Field \"" + field.getName() + "\": can't determine the referenced class");
        }

        // Search the referenced column
        EntityObject referencedEntity = entities.get(referencedClass);

        if (referencedEntity == null)
            throw new InvalidConfigException("Field \"" + field.getName() + "\": \"" + referencedClass.getSimpleName() + "\" is not an entity");

        BaseColumnObject referencedColumn = null;

        while (referencedEntity != null && referencedColumn == null) {
            referencedColumn = referencedEntity.columns.getNamesMap().get(referencedColumnName);
            referencedEntity = referencedEntity.parent;
        }

        if (referencedColumn == null)
            throw new InvalidConfigException("Field \"" + field.getName() + "\": referenced column \"" + referencedColumnName + "\" not found");

        this.type = referencedColumn.type;
    }


    @Override
    public void addToContentValues(@NonNull ContentValues cv, Object obj) {
        Object sourceObject = getValue(obj);

        if (sourceObject == null) {
            insertDataIntoContentValues(cv, annotation.name(), null);
        } else {
            EntityObject destinationEntity = db.getEntity(sourceObject.getClass());
            BaseColumnObject destinationColumn = destinationEntity.columns.getNamesMap().get(annotation.referencedColumnName());
            Object value = destinationColumn.getValue(sourceObject);
            insertDataIntoContentValues(cv, annotation.name(), value);
        }
    }

}
