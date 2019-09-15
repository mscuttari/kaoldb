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

import androidx.lifecycle.MutableLiveData;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.Query;

/**
 * Stores a query that will be executed again when requested by the {@link EntityManager} in order
 * to update the data to be presented to the user.
 *
 * @param <T>   result objects class
 */
class LiveQuery<T> extends MutableLiveData<List<T>> {

    /** The query to be executed in order to retrieve the updated data */
    private final Query<T> query;

    /** The entities the query observes. If any of them gets an update the {@link #query} is executed */
    private final Collection<EntityObject<?>> observed;


    /**
     * Constructor.
     *
     * @param query     query to be executed upon data change
     * @param observed  observed entities
     */
    public LiveQuery(Query<T> query, Collection<EntityObject<?>> observed) {
        this.query = query;
        this.observed = observed;
    }


    /**
     * Execute the query in order to fetch the updated data from the database.
     */
    public void refresh() {
        postValue(query.getResults());
    }


    /**
     * Get the observed entities.
     *
     * @return observed entities
     */
    public Collection<EntityObject<?>> getObservedEntities() {
        return Collections.unmodifiableCollection(observed);
    }

}
