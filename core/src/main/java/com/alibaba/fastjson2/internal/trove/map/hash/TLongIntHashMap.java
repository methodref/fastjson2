///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2001, Eric D. Friedman All Rights Reserved.
// Copyright (c) 2009, Rob Eden All Rights Reserved.
// Copyright (c) 2009, Jeff Randall All Rights Reserved.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
///////////////////////////////////////////////////////////////////////////////

package com.alibaba.fastjson2.internal.trove.map.hash;

import com.alibaba.fastjson2.internal.trove.impl.PrimeFinder;
import com.alibaba.fastjson2.internal.trove.procedure.TLongIntProcedure;

/**
 * An open addressed Map implementation for long keys and int values.
 *
 * @author Eric D. Friedman
 * @author Rob Eden
 * @author Jeff Randall
 */
public class TLongIntHashMap {
    public static final int DEFAULT_ENTRY_VALUE = -1;

    static final byte FREE = 0;
    static final byte FULL = 1;

    protected int[] values;
    protected long[] set;
    protected boolean consumeFreeSlot;
    protected byte[] states;
    protected int size;
    protected int free;
    protected int maxSize;

    /**
     * Creates a new <code>TLongIntHashMap</code> instance with the default
     * capacity and load factor.
     */
    public TLongIntHashMap() {
        int capacity = 37;
        maxSize = 18;
        free = capacity; // reset the free element count

        states = new byte[capacity];
        set = new long[capacity];
        values = new int[capacity];
    }

    public TLongIntHashMap(long key, int value) {
        int capacity = 37;
        maxSize = 18;

        states = new byte[capacity];
        set = new long[capacity];
        values = new int[capacity];

        int hash = ((int) (key ^ (key >>> 32))) & 0x7fffffff;
        int index = hash % states.length;
        consumeFreeSlot = true;
        set[index] = key;
        states[index] = FULL;

        values[index] = value;

        free = capacity - 1;
    }

    public void put(long key, int value) {
//        int index = insertKey( key );
        int index;
        {
            int hash = ((int) (key ^ (key >>> 32))) & 0x7fffffff;
            index = hash % states.length;
            byte state = states[index];

            consumeFreeSlot = false;

            if (state == FREE) {
                consumeFreeSlot = true;
//                insertKeyAt(index, key);
                set[index] = key;
                states[index] = FULL;
            } else if (state == FULL && set[index] == key) {
                index = -index - 1;   // already stored
            } else {
                // already FULL or REMOVED, must probe
//                index = insertKeyRehash(key, index, hash);
                {
                    // compute the double hash
                    final int length = set.length;
                    int probe = 1 + (hash % (length - 2));
                    final int loopIndex = index;

                    /**
                     * Look until FREE slot or we start to loop
                     */
                    do {
                        index -= probe;
                        if (index < 0) {
                            index += length;
                        }
                        state = states[index];

                        // A FREE slot stops the search
                        if (state == FREE) {
                            consumeFreeSlot = true;
                            set[index] = key;  // insert value
                            states[index] = FULL;
                            break;
                        }

                        if (state == FULL && set[index] == key) {
                            index = -index - 1;
                            break;
                        }

                        // Detect loop
                    } while (index != loopIndex);
                }
            }
        }

        boolean isNewMapping = true;
        if (index < 0) {
            index = -index - 1;
            isNewMapping = false;
        }
        values[index] = value;

        if (isNewMapping) {
            if (consumeFreeSlot) {
                free--;
            }

            // rehash whenever we exhaust the available space in the table
            if (++size > maxSize || free == 0) {
                // choose a new capacity suited to the new state of the table
                // if we've grown beyond our maximum size, double capacity;
                // if we've exhausted the free spots, rehash to the same capacity,
                // which will free up any stale removed slots for reuse.
                int capacity = states.length;
                int newCapacity = size > maxSize ? PrimeFinder.nextPrime(capacity << 1) : capacity;
//                rehash(newCapacity);
                {
                    int oldCapacity = set.length;

                    long[] oldKeys = set;
                    int[] oldVals = values;
                    byte[] oldStates = states;

                    set = new long[newCapacity];
                    values = new int[newCapacity];
                    states = new byte[newCapacity];

                    for (int i = oldCapacity; i-- > 0; ) {
                        if (oldStates[i] == FULL) {
                            long o = oldKeys[i];
                            index = insertKey(o);
                            values[index] = oldVals[i];
                        }
                    }
                }

                capacity = states.length;
                // computeMaxSize(capacity);
                maxSize = Math.min(capacity - 1, (int) (capacity * 0.5f));
                free = capacity - size; // reset the free element count
            }
        }
    }

    public int putIfAbsent(long key, int value) {
//        int index = insertKey( key );
        int index;
        {
            int hash = ((int) (key ^ (key >>> 32))) & 0x7fffffff;
            index = hash % states.length;
            byte state = states[index];

            consumeFreeSlot = false;

            if (state == FREE) {
                consumeFreeSlot = true;
//                insertKeyAt(index, key);
                set[index] = key;
                states[index] = FULL;
            } else if (state == FULL && set[index] == key) {
                index = -index - 1;   // already stored
            } else {
                // already FULL or REMOVED, must probe
//                index = insertKeyRehash(key, index, hash);
                {
                    // compute the double hash
                    final int length = set.length;
                    int probe = 1 + (hash % (length - 2));
                    final int loopIndex = index;

                    /**
                     * Look until FREE slot or we start to loop
                     */
                    do {
                        index -= probe;
                        if (index < 0) {
                            index += length;
                        }
                        state = states[index];

                        // A FREE slot stops the search
                        if (state == FREE) {
                            consumeFreeSlot = true;
                            set[index] = key;  // insert value
                            states[index] = FULL;
                            break;
                        }

                        if (state == FULL && set[index] == key) {
                            index = -index - 1;
                            break;
                        }

                        // Detect loop
                    } while (index != loopIndex);
                }
            }
        }

        if (index < 0) {
            return values[-index - 1];
        }

        boolean isNewMapping = true;
        if (index < 0) {
            index = -index - 1;
            isNewMapping = false;
        }
        values[index] = value;

        if (isNewMapping) {
            if (consumeFreeSlot) {
                free--;
            }

            // rehash whenever we exhaust the available space in the table
            if (++size > maxSize || free == 0) {
                // choose a new capacity suited to the new state of the table
                // if we've grown beyond our maximum size, double capacity;
                // if we've exhausted the free spots, rehash to the same capacity,
                // which will free up any stale removed slots for reuse.
                int capacity = states.length;
                int newCapacity = size > maxSize ? PrimeFinder.nextPrime(capacity << 1) : capacity;
//                rehash(newCapacity);
                {
                    int oldCapacity = set.length;

                    long[] oldKeys = set;
                    int[] oldVals = values;
                    byte[] oldStates = states;

                    set = new long[newCapacity];
                    values = new int[newCapacity];
                    states = new byte[newCapacity];

                    for (int i = oldCapacity; i-- > 0; ) {
                        if (oldStates[i] == FULL) {
                            long o = oldKeys[i];
                            index = insertKey(o);
                            values[index] = oldVals[i];
                        }
                    }
                }

                capacity = states.length;
                // computeMaxSize(capacity);
                maxSize = Math.min(capacity - 1, (int) (capacity * 0.5f));
                free = capacity - size; // reset the free element count
            }
        }

        return value;
    }

    public int get(long key) {
        int length = states.length;
        int hash = ((int) (key ^ (key >>> 32))) & 0x7fffffff;
        int index = hash % length;
        byte state = states[index];

        if (state == FREE) {
            return DEFAULT_ENTRY_VALUE;
        }

        if (state == FULL && set[index] == key) {
            return values[index];
        }

        // indexRehashed
        int setLength = set.length;
        int probe = 1 + (hash % (setLength - 2));
        final int loopIndex = index;

        do {
            index -= probe;
            if (index < 0) {
                index += setLength;
            }
            state = states[index];

            if (state == FREE) {
                return DEFAULT_ENTRY_VALUE;
            }

            if (key == set[index]) {
                return values[index];
            }
        } while (index != loopIndex);

        return DEFAULT_ENTRY_VALUE;
    }

    public boolean forEachEntry(TLongIntProcedure procedure) {
        byte[] states = this.states;
        long[] keys = set;
        int[] values = this.values;
        for (int i = keys.length; i-- > 0; ) {
            if (states[i] == FULL && !procedure.execute(keys[i], values[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder("{");
        forEachEntry(new TLongIntProcedure() {
            private boolean first = true;

            @Override
            public boolean execute(long key, int value) {
                if (first) {
                    first = false;
                } else {
                    buf.append(", ");
                }
                buf.append(key);
                buf.append("=");
                buf.append(value);
                return true;
            }
        });
        buf.append("}");
        return buf.toString();
    }

    public int size() {
        return size;
    }

    /**
     * Locates the index at which <tt>val</tt> can be inserted.  if
     * there is already a value equal()ing <tt>val</tt> in the set,
     * returns that value as a negative integer.
     *
     * @param key an <code>long</code> value
     * @return an <code>int</code> value
     */
    protected int insertKey(long key) {
        int hash, index;

        hash = ((int) (key ^ (key >>> 32))) & 0x7fffffff;
        index = hash % states.length;
        byte state = states[index];

        consumeFreeSlot = false;

        if (state == FREE) {
            consumeFreeSlot = true;
//            insertKeyAt(index, key);
            set[index] = key;  // insert value
            states[index] = FULL;

            return index;      // empty, all done
        }

        if (state == FULL && set[index] == key) {
            return -index - 1;   // already stored
        }

        // already FULL or REMOVED, must probe
//        return insertKeyRehash(key, index, hash);
        {
            // compute the double hash
            final int length = set.length;
            int probe = 1 + (hash % (length - 2));
            final int loopIndex = index;

            /**
             * Look until FREE slot or we start to loop
             */
            do {
                index -= probe;
                if (index < 0) {
                    index += length;
                }

                state = states[index];

                // A FREE slot stops the search
                if (state == FREE) {
                    consumeFreeSlot = true;
//                    insertKeyAt(index, val);
                    set[index] = key;  // insert value
                    states[index] = FULL;
                    return index;
                }

                if (state == FULL && set[index] == key) {
                    return -index - 1;
                }

                // Detect loop
            } while (index != loopIndex);

            // Can a resizing strategy be found that resizes the set?
            throw new IllegalStateException("No free or removed slots available. Key set full?!!");
        }
    }
} // TLongIntHashMap
