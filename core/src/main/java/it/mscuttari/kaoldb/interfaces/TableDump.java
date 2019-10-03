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

package it.mscuttari.kaoldb.interfaces;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import java.util.List;

public interface TableDump {

    /**
     * Get table name.
     *
     * @return table name
     */
    @CheckResult
    @NonNull
    String getName();


    /**
     * Get the query to create the table.
     *
     * @return SQL query
     */
    @CheckResult
    @NonNull
    String getSql();


    /**
     * Get all the column names.
     *
     * @return columns list
     */
    @CheckResult
    @NonNull
    List<String> getColumns();


    /**
     * Get row dumps.
     *
     * @return row dumps
     */
    @CheckResult
    @NonNull
    List<RowDump> getRows();

}
