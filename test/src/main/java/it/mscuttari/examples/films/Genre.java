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

package it.mscuttari.examples.films;

import java.util.Arrays;
import java.util.Collection;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.Table;

@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @Column(name = "name")
    public String name;

    @OneToMany(mappedBy = "genre")
    public Collection<Film> films;


    /**
     * Default constructor
     */
    public Genre() {
        this(null);
    }


    /**
     * Constructor
     *
     * @param   name    name
     */
    public Genre(String name) {
        this.name = name;
    }


    @Override
    public int hashCode() {
        Object[] x = {name};
        return Arrays.hashCode(x);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        if (!(obj instanceof Genre)) return false;
        Genre o = (Genre) obj;

        if (name != null && !name.equals(o.name)) return false;
        if (name == null && o.name != null) return false;

        return true;
    }


    @Override
    public String toString() {
        return "[name: " + name + "]";
    }
}
