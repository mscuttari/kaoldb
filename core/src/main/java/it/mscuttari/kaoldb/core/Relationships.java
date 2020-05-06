package it.mscuttari.kaoldb.core;

import android.os.Build;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;

import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;

class Relationships implements Iterable<Relationship> {

    /** Relationships container */
    private final Collection<Relationship> relationships = new ArraySet<>();

    /** Map the fields by their field name in order to quickly search for the mapped relationship */
    private final Map<String, Relationship> mapByFieldName = new ArrayMap<>();

    @NonNull
    @Override
    public synchronized Iterator<Relationship> iterator() {
        return new RelationshipsIterator(this);
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Override
    public synchronized void forEach(Consumer<? super Relationship> action) {
        relationships.forEach(action);
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Override
    public synchronized Spliterator<Relationship> spliterator() {
        return relationships.spliterator();
    }

    /**
     * Add a relationship.
     *
     * @param relationship      relationship to be added
     * @return <code>true</code> if the relationships collection has changed; <code>false otherwise</code>
     */
    public synchronized boolean add(Relationship relationship) {
        mapByFieldName.put(relationship.field.getName(), relationship);
        return relationships.add(relationship);
    }

    /**
     * Check if a field leads to a relationship.
     *
     * @param fieldName     field name
     * @return <code>true</code> if the field is annotated with {@link OneToOne},
     *         {@link OneToMany}, {@link ManyToOne} or {@link ManyToMany}; <code>false</code>
     *         otherwise
     */
    @CheckResult
    public synchronized boolean contains(String fieldName) {
        return mapByFieldName.containsKey(fieldName);
    }

    /**
     * Get a relationship given the name of the field generating it.
     *
     * @param fieldName     field name
     * @return relationship
     * @throws NoSuchElementException if there is no relationship linked to that field
     */
    @CheckResult
    public synchronized Relationship get(String fieldName) {
        Relationship result = mapByFieldName.get(fieldName);

        if (result != null)
            return result;

        throw new NoSuchElementException("Field \"" + fieldName + "\" doesn't have a relationship");
    }

    /**
     * Iterator for the {@link Relationships} container.
     */
    private static class RelationshipsIterator implements Iterator<Relationship> {

        private final Relationships relationships;
        private final Iterator<Relationship> iterator;
        private Relationship current;

        /**
         * Constructor.
         *
         * @param relationships     relationships container
         */
        public RelationshipsIterator(@NonNull Relationships relationships) {
            this.relationships = relationships;
            this.iterator = relationships.relationships.iterator();
        }

        @Override
        public boolean hasNext() {
            synchronized (relationships) {
                return iterator.hasNext();
            }
        }

        @Override
        public Relationship next() {
            synchronized (relationships) {
                current = iterator.next();
                return current;
            }
        }

        @Override
        public void remove() {
            synchronized (relationships) {
                iterator.remove();
                relationships.mapByFieldName.remove(current.field.getName());
            }
        }

        @RequiresApi(Build.VERSION_CODES.N)
        @Override
        public void forEachRemaining(@NonNull Consumer<? super Relationship> action) {
            synchronized (relationships) {
                iterator.forEachRemaining(action);
            }
        }

    }

}
