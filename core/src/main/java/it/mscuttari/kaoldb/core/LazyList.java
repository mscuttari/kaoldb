package it.mscuttari.kaoldb.core;

import android.os.Build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import it.mscuttari.kaoldb.interfaces.Query;

/**
 * Lazy collection implementation using a {@link List} as data container
 *
 * @param <T>   POJO class
 */
class LazyList<T> extends LazyCollection<T, List<T>> implements List<T> {

    /**
     * Constructor
     *
     * @param container     data container specified by the user
     * @param query         query to be executed to load data
     */
    public LazyList(List<T> container, @NonNull Query<T> query) {
        super(container == null ? new ArrayList<>() : container, query);
    }


    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
        checkInitialization();
        return getContainer().addAll(c);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void replaceAll(@NonNull UnaryOperator<T> operator) {
        checkInitialization();
        getContainer().replaceAll(operator);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void sort(Comparator<? super T> c) {
        checkInitialization();
        getContainer().sort(c);
    }


    @Override
    public T get(int index) {
        checkInitialization();
        return getContainer().get(index);
    }


    @Override
    public T set(int index, T element) {
        checkInitialization();
        return getContainer().set(index, element);
    }


    @Override
    public void add(int index, T element) {
        checkInitialization();
        getContainer().add(index, element);
    }


    @Override
    public T remove(int index) {
        checkInitialization();
        return getContainer().remove(index);
    }


    @Override
    public int indexOf(Object o) {
        checkInitialization();
        return getContainer().indexOf(o);
    }


    @Override
    public int lastIndexOf(Object o) {
        checkInitialization();
        return getContainer().lastIndexOf(o);
    }


    @NonNull
    @Override
    public ListIterator<T> listIterator() {
        checkInitialization();
        return getContainer().listIterator();
    }


    @NonNull
    @Override
    public ListIterator<T> listIterator(int index) {
        checkInitialization();
        return getContainer().listIterator(index);
    }


    @NonNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        checkInitialization();
        return getContainer().subList(fromIndex, toIndex);
    }

}
