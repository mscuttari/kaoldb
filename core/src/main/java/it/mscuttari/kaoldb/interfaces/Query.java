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
import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * A query object contains the SQL query to be executed on the database to retrieve
 * the desired data.<br>
 * A query can be executed more than once, and will execute every time the same SQL query.
 *
 * @param <M>   result objects class
 */
public interface Query<M> {

    /**
     * Run the query and get the results list.
     *
     * <p>
     * The list will not change in case of database changes.<br>
     * For a database observing list see {@link #getLiveResults()}.
     * </p>
     *
     * @return static data
     */
    @CheckResult
    @NonNull
    List<M> getResults();

    /**
     * Run the query and get the results list.
     *
     * <p>
     * The returned data will reflect future database changes, both in term of list results and
     * internal values of each element.
     * </p>
     *
     * @return live data
     */
    @CheckResult
    @NonNull
    LiveData<List<M>> getLiveResults();

    /**
     * Run the query and get the first query result object.
     *
     * @return first result object
     */
    @CheckResult
    M getSingleResult();

}
