/*
 * Copyright Matt Palmer 2011, All rights reserved.
 *
 */

package net.domesdaybook.searcher.multisequence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class iterates all the permutations of byte strings which can be produced
 * from a list of byte arrays.
 * 
 * It is not thread-safe, as it maintains state as it iterates.
 * In addition, the byte array returned by next() is always the same
 * underlying byte array, so each call to next() modifies the values in it
 * 
 * @author matt
 */
public class BytePermutationIterator implements Iterator<byte[]> {

    private final List<byte[]> byteArrays;
    private final int[] permutationState;
    private final int length;
    private final byte[] permutation;

    public BytePermutationIterator(final List<byte[]> byteArrays) {
        if (byteArrays == null) {
            throw new IllegalArgumentException("Null byteArrays passed in to PermutationIterator.");
        }
        this.byteArrays = new ArrayList<byte[]>(byteArrays);
        for (byte[] array : this.byteArrays) {
            if (array == null || array.length == 0) {
                throw new IllegalArgumentException("Null or empty byte array passed in to PermutationIterator.");
            }
        }
        this.length = byteArrays.size();
        this.permutationState = new int[length];
        this.permutation = new byte[length];
    }

    
    /**
     * @inheritDoc 
     */
    @Override
    public boolean hasNext() {
        return permutationState[0] < byteArrays.get(0).length;
    }

    
    /**
     * @inheritDoc 
     * 
     * Note: the values of the byte array returned are correct in this iteration.
     * However, it is always the same underlying byte array. If you need a record
     * of the byte arrays returned, you must copy them into new ones.
     * @throws NoSuchElementException if there are no more permutations.
     */
    @Override
    public byte[] next() {
        if (hasNext()) {
            buildCurrentPermutation();
            buildNextPermutationState();
            return permutation;
        } else {
            throw new NoSuchElementException("No more permutations available for the byte arrays.");
        }
    }

    
    /**
     * @inheritDoc 
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException("Permutation iterator cannot remove generated permutations.");
    }

    
    private void buildCurrentPermutation() {
        final byte[] localperm = permutation;
        for (int arrayIndex = 0; arrayIndex < length; arrayIndex++) {
            final int permutationIndex = permutationState[arrayIndex];
            final byte[] array = byteArrays.get(arrayIndex);
            localperm[arrayIndex] = array[permutationIndex];
        }
    }

    
    private void buildNextPermutationState() {
        boolean finished = false;
        int stateIndex = length - 1;
        while (!finished) {
            final byte[] array = byteArrays.get(stateIndex);

            // Get a next possible state of the permutation:
            final int state = permutationState[stateIndex] + 1;
            
            // We're now done if there are still more bytes to process in the current
            // state, or if we're at the first state (can't go back any more states)
            finished = state < array.length || stateIndex == 0;
            
            // If we're done, set the current permutation state to our new state:
            if (finished) {
                permutationState[stateIndex] = state;
            } else { 
                // We overflowed the current state - reset it back to zero and
                // go back a state to try again.
                permutationState[stateIndex] = 0;
                stateIndex--;
            }
        }
    }
}