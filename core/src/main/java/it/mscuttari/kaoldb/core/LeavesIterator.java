package it.mscuttari.kaoldb.core;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

abstract class LeavesIterator<T extends TreeNode<T>> implements Iterator<T> {

    private final Stack<T> stack = new Stack<>();
    private T nextNode = null;


    /**
     * Constructor
     *
     * @param   root    tree root
     */
    public LeavesIterator(T root) {
        if (root != null) {
            stack.push(root);
            nextNode = fetchNext();
        }
    }


    @Override
    public boolean hasNext() {
        return nextNode != null;
    }


    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        T n = nextNode;
        nextNode = fetchNext();
        return n;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }


    /**
     * Fetch next node
     *
     * @return  next node
     */
    private T fetchNext() {
        T next = null;

        while (!stack.isEmpty() && next == null) {
            T node = stack.pop();

            if (node.getLeft() == null && node.getRight() == null) {
                next = node;
            }

            if (node.getRight() != null) {
                stack.push(node.getRight());
            }

            if (node.getLeft() != null) {
                stack.push(node.getLeft());
            }
        }

        return next;
    }

}
