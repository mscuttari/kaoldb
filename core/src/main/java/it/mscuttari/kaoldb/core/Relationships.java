package it.mscuttari.kaoldb.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;

import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;

class Relationships extends HashSet<Relationship> {

    private HashMap<String, Relationship> mapByFieldName = new HashMap<>();


    @Override
    public boolean add(Relationship relationship) {
        mapByFieldName.put(relationship.field.getName(), relationship);
        return super.add(relationship);
    }


    @Override
    public boolean remove(Object o) {
        if (o instanceof Relationship) {
            mapByFieldName.remove(((Relationship) o).field.getName());
        }

        return super.remove(o);
    }


    @Override
    public void clear() {
        mapByFieldName.clear();
        super.clear();
    }


    /**
     * Check if a field leads to a relationship
     *
     * @param fieldName     field name
     * @return true if the field is annotated with {@link OneToOne}, {@link OneToMany},
     *         {@link ManyToOne} or {@link ManyToMany}
     */
    public boolean contains(String fieldName) {
        return mapByFieldName.containsKey(fieldName);
    }


    /**
     * Get a relationship given the name of the field generating it
     *
     * @param fieldName     field name
     * @return relationship
     * @throws NoSuchElementException if there is no relationship linked to that field
     */
    public Relationship getByFieldName(String fieldName) {
        Relationship result = mapByFieldName.get(fieldName);

        if (result != null)
            return result;

        throw new NoSuchElementException("Field \"" + fieldName + "\" doesn't have a relationship");
    }

}
