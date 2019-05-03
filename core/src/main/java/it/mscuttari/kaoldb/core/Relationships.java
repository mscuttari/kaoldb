package it.mscuttari.kaoldb.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;

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
     * Get a relationship given the name of the field generating it
     *
     * @param fieldName     field name
     * @return relationship
     * @throws NoSuchElementException if there is no relationship linked to that field
     */
    public Relationship getByField(String fieldName) {
        Relationship result = mapByFieldName.get(fieldName);

        if (result != null)
            return result;

        throw new NoSuchElementException("Field \"" + fieldName + "\" doesn't have a relationship");
    }

}
