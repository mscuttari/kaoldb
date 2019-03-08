package it.mscuttari.kaoldb.core;

import java.lang.reflect.Field;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;

/**
 * This class allows to group the join columns that are declared together
 *
 * @see JoinColumns
 */
final class JoinColumnsObject extends Columns implements ColumnsContainer {

    private final Field field;


    /**
     * Constructor
     *
     * @param db        database
     * @param entity    entity the columns belong to
     * @param field     field the columns are generated from
     */
    private JoinColumnsObject(@NonNull DatabaseObject db,
                              @NonNull EntityObject<?> entity,
                              @NonNull Field field) {

        super(entity);
        this.field = field;
    }


    /**
     * Create the JoinColumnsObject linked to a field annotated with {@link JoinColumns}
     * and start the mapping process.
     *
     * @param db        database
     * @param entity    entity the columns belong to
     * @param field     field the columns are generated from
     *
     * @return columns container
     */
    public static JoinColumnsObject map(@NonNull DatabaseObject db,
                                        @NonNull EntityObject<?> entity,
                                        @NonNull Field field) {

        JoinColumnsObject result = new JoinColumnsObject(db, entity, field);

        try {
            return result;

        } finally {
            JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);

            for (JoinColumn joinColumnAnnotation : joinColumnsAnnotation.value()) {
                JoinColumnObject joinColumn = JoinColumnObject.map(db, entity, field, joinColumnAnnotation);
                result.add(joinColumn);
            }
        }
    }

}
