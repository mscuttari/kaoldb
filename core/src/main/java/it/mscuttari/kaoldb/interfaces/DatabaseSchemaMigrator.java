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

import java.util.List;

/**
 * Database schema migrator to be used in case of database version change.
 */
public interface DatabaseSchemaMigrator {

    /**
     * Called in case of database version upgrade.
     *
     * <p>All the data is in the dump and all the data that has to be kept must be persisted again
     * using the new version POJOs.<br>
     * <u>The data not persisted again will be lost.</u><br>
     *
     * Moreover, the upgrades are done incrementally, therefore in every call
     * <code>newVersion = oldVersion + 1</code>. For example, if the database version was
     * <code>54</code> and now is <code>57</code>, this method is called three times, with
     * the following values:
     * <ol>
     *     <li><code>oldVersion = 54, newVersion = 55, dump = all data of version 54</code></li>
     *     <li><code>oldVersion = 55, newVersion = 56, dump = all data of version 55</code></li>
     *     <li><code>oldVersion = 56, newVersion = 57, dump = all data of version 56</code></li>
     * </ol>
     * </p>
     *
     * @param oldVersion    old version
     * @param newVersion    new version
     * @param dump          database dump at version <code>oldVersion</code>
     *
     * @return list of actions to be executed on the schema
     */
    List<SchemaAction> onUpgrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception;


    /**
     * Called in case of database version downgrade.
     *
     * <p>All the data is in the dump and all the data that has to be kept must be persisted again
     * using the new version POJOs.<br>
     * <u>The data not persisted again will be lost.</u><br>
     *
     * Moreover, the downgrades are done incrementally, therefore in every call
     * <code>newVersion = oldVersion - 1</code>. For example, if the database version was
     * 57 and now is 54, this method is called three times, with the following values:
     * <ol>
     *     <li><code>oldVersion = 57, newVersion = 56, dump = all data of version 57</code></li>
     *     <li><code>oldVersion = 56, newVersion = 55, dump = all data of version 56</code></li>
     *     <li><code>oldVersion = 55, newVersion = 54, dump = all data of version 55</code></li>
     * </ol>
     * </p>
     *
     * @param oldVersion    old version
     * @param newVersion    new version
     * @param dump          database dump at version <code>oldVersion</code>
     *
     * @return list of actions to be executed on the schema
     */
    default List<SchemaAction> onDowngrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception {
        throw new UnsupportedOperationException();
    }

}
