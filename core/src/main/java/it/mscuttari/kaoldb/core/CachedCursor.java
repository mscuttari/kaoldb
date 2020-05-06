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

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import java.util.Collections;
import java.util.Map;

import androidx.annotation.RequiresApi;
import androidx.collection.ArrayMap;

/**
 * Wrapper for {@link Cursor} which enables some performance improvements.
 *
 * <p>
 * The default {@link SQLiteCursor#getColumnIndex(String)} implementation doesn't exploit any
 * caching capability and each column name lookup can cost up to the number of the column and this
 * is is obviously quite expensive if there are a lot of columns. This cost is avoid by first
 * creating a map between each column name and its index; thus the linear cost is spent only on
 * startup and the subsequent calls to {@link #getColumnIndex(String)} will have fixed cost.
 * By doing this, this wrapper also enables the usage of column names containing a dot, such
 * as <code>tableName.columnName</code>. In fact, the default implementation has a section aimed
 * to fix bug 903852, but this workaround actually breaks the usage of dots in column names.
 * </p>
 */
class CachedCursor implements Cursor {

    /** Original cursor */
    private final Cursor c;

    /** Map between columns name and index */
    private final Map<String, Integer> columnIndexMap;

    /**
     * Constructor.
     *
     * @param c     original cursor
     */
    public CachedCursor(Cursor c) {
        this.c = c;

        // Build column name-index map
        String[] columnNames = c.getColumnNames();
        Map<String, Integer> columnIndexMap = new ArrayMap<>(columnNames.length);

        for (int i = 0; i < columnNames.length; i++) {
            columnIndexMap.put(columnNames[i], i);
        }

        this.columnIndexMap = Collections.unmodifiableMap(columnIndexMap);
    }

    @Override
    public int getCount() {
        return c.getCount();
    }

    @Override
    public int getPosition() {
        return c.getPosition();
    }

    @Override
    public boolean move(int offset) {
        return c.move(offset);
    }

    @Override
    public boolean moveToPosition(int position) {
        return c.moveToPosition(position);
    }

    @Override
    public boolean moveToFirst() {
        return c.moveToFirst();
    }

    @Override
    public boolean moveToLast() {
        return c.moveToLast();
    }

    @Override
    public boolean moveToNext() {
        return c.moveToNext();
    }

    @Override
    public boolean moveToPrevious() {
        return c.moveToPrevious();
    }

    @Override
    public boolean isFirst() {
        return c.isFirst();
    }

    @Override
    public boolean isLast() {
        return c.isLast();
    }

    @Override
    public boolean isBeforeFirst() {
        return c.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast() {
        return c.isAfterLast();
    }

    @Override
    public int getColumnIndex(String columnName) {
        Integer result = columnIndexMap.get(columnName);

        if (result != null)
            return result;

        return -1;
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        Integer result = columnIndexMap.get(columnName);

        if (result != null)
            return result;

        throw new IllegalArgumentException("Column \"" + columnName + "\" not found");
    }

    @Override
    public String getColumnName(int columnIndex) {
        return c.getColumnName(columnIndex);
    }

    @Override
    public String[] getColumnNames() {
        return c.getColumnNames();
    }

    @Override
    public int getColumnCount() {
        return c.getColumnCount();
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        return c.getBlob(columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        return c.getString(columnIndex);
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        c.copyStringToBuffer(columnIndex, buffer);
    }

    @Override
    public short getShort(int columnIndex) {
        return c.getShort(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        return c.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        return c.getLong(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) {
        return c.getFloat(columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) {
        return c.getDouble(columnIndex);
    }

    @Override
    public int getType(int columnIndex) {
        return c.getType(columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex) {
        return c.isNull(columnIndex);
    }

    @Override
    public void deactivate() {
        c.deactivate();
    }

    @Override
    public boolean requery() {
        return c.requery();
    }

    @Override
    public void close() {
        c.close();
    }

    @Override
    public boolean isClosed() {
        return c.isClosed();
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        c.registerContentObserver(observer);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        c.unregisterContentObserver(observer);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        c.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        c.unregisterDataSetObserver(observer);
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {
        c.setNotificationUri(cr, uri);
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @Override
    public Uri getNotificationUri() {
        return c.getNotificationUri();
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return c.getWantsAllOnMoveCalls();
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @Override
    public void setExtras(Bundle extras) {
        c.setExtras(extras);
    }

    @Override
    public Bundle getExtras() {
        return c.getExtras();
    }

    @Override
    public Bundle respond(Bundle extras) {
        return c.respond(extras);
    }

}
