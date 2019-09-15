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

package it.mscuttari.examples.database;

import java.util.List;

import it.mscuttari.kaoldb.interfaces.DatabaseDump;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.SchemaAction;

public class GenericDbMigrator implements DatabaseSchemaMigrator {

    @Override
    public List<SchemaAction> onUpgrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception {
        return null;
    }

    @Override
    public List<SchemaAction> onDowngrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception {
        return null;
    }

}
