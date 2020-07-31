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

package it.mscuttari.kaoldb.schema;

import android.database.sqlite.SQLiteDatabase;

import org.junit.Test;

import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTables;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class SchemaDeleteTableTest extends SchemaAbstractTest {

    @Override
    protected void createDb(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE table_1(id INTEGER PRIMARY KEY)");
    }

    @Test
    public void delete() {
        assertThat(getTables(db), hasItem("table_1"));
        new SchemaDeleteTable("table_1").run(db);
        assertThat(getTables(db), not(hasItem("table_1")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyTable() {
        new SchemaDeleteTable("").run(db);
    }

}
