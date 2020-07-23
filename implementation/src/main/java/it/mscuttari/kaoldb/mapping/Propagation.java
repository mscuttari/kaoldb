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

package it.mscuttari.kaoldb.mapping;

import androidx.annotation.NonNull;

/**
 * Describes the actions to be taken when a foreign key constraint changes.
 */
public class Propagation {

    public enum Action {

        NO_ACTION   ("NO ACTION"),
        RESTRICT    ("RESTRICT"),
        SET_NULL    ("SET NULL"),
        SET_DEFAULT ("SET DEFAULT"),
        CASCADE     ("CASCADE");

        private final String action;

        Action(String action) {
            this.action = action;
        }

        @NonNull
        @Override
        public String toString() {
            return action;
        }

    }

    private final Action onUpdate;
    private final Action onDelete;

    /**
     * Constructor.
     *
     * @param onUpdate      on update action
     * @param onDelete      on delete action
     */
    public Propagation(Action onUpdate, Action onDelete) {
        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
    }

    @NonNull
    @Override
    public String toString() {
        return "ON UPDATE " + onUpdate + " ON DELETE " + onDelete + " DEFERRABLE INITIALLY DEFERRED";
    }

}
