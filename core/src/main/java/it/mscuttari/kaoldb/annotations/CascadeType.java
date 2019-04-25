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

package it.mscuttari.kaoldb.annotations;

/**
 * Defines the set of cascadable operations that are propagated
 * to the associated entity.
 * The value <code>cascade=ALL</code> is equivalent to
 * <code>cascade={PERSIST, MERGE, REMOVE, REFRESH, DETACH}</code>.
 */
public enum CascadeType {

    /** Cascade all operations */
    ALL,

    /** Cascade persist operation */
    PERSIST,

    /** Cascade merge operation */
    MERGE,

    /** Cascade remove operation */
    REMOVE,

}
