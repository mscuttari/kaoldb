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
     * @param entity    entity the column belongs to
     * @param field     field the columns are generated from
     */
    public JoinColumnsObject(@NonNull DatabaseObject db,
                             @NonNull EntityObject<?> entity,
                             @NonNull Field field) {

        super(entity);

        this.field = field;

        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);

        for (JoinColumn joinColumnAnnotation : joinColumnsAnnotation.value()) {
            add(new JoinColumnObject(db, entity, field, joinColumnAnnotation));
        }
    }

}
