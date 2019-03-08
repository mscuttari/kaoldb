package it.mscuttari.kaoldb.core;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.exceptions.PojoException;

/**
 * The classes implementing this interface can be considered as containers of columns.
 * Also simple columns can be considered as containers of themselves.
 *
 * The columns containers do not directly appear during the iteration and therefore all the
 * columns seem to be part of a single collection.
 *
 * @see BaseColumnObject
 * @see SimpleColumnObject
 * @see JoinColumnObject
 * @see JoinColumnsObject
 * @see JoinTableObject
 */
interface ColumnsContainer extends Iterable<BaseColumnObject> {

    /**
     * Add the columns to a {@link ContentValues} data set
     *
     * @param cv    data set to be populated
     * @param obj   object containing the data to be extracted
     *
     * @throws PojoException if the obj doesn't contain the columns
     * @throws PojoException if the column value can't be retrieved
     */
    void addToContentValues(@NonNull ContentValues cv, Object obj);

}
