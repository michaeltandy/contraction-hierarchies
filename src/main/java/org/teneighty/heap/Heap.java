/*
 * $Id: Heap.java,v 1.6.4.9 2008/05/15 23:59:59 fran Exp $
 * 
 * Copyright (c) 2005, 2006, 2007, 2008 Fran Lattanzio
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.teneighty.heap;

import java.util.Comparator;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * A heap interface.
 * <p>
 * Heaps can be used as very efficient priority queues. Heaps do not work well
 * as general purpose maps: They do not naturally support any sort of searching
 * operations or the deletion of arbitrary keys. In fact, the delete and
 * decrease key methods of this interface require that you have a reference to
 * the entry on which you wish to operate (as opposed to simply the key of the
 * entry). For most applications, this is not a problem... mostly.
 * <p>
 * A few notes:
 * <ol>
 * <li>See the <a href="package-summary.html">Package Manifest</a> for a
 * summary of the available implementations, as well as their runtime
 * performance.</li>
 * <li>In general, the order of items in the Sets/Collections returned from
 * collection-view methods is arbitrary. However, it should always be the case
 * that (barring any changes to the heap) they are consistent and deterministic.</li>
 * <li>The iterators returned by implementations should generally be
 * <i>fail-fast</i>, meaning they should detect changes to the backing heap and
 * throw a <code>ConcurrentModificationException</code> if the backing heap is
 * changed during iteration.</li>
 * <li>Heaps do not maintain insertion order between elements with equal keys.
 * This is not the general contract of the heap ADT. If you need this
 * functionality, it should be programmed externally.</li>
 * <li>It is generally not a problem to use as a key <code>Comparable</code>
 * class whose <code>compareTo()</code> method is inconsistent with equals,
 * since we don't care about equal key collisions. Similarly, a
 * <code>Comparator</code> method whose <code>compare()</code> method is
 * inconsistent with equals is also OK.</li>
 * <li>Be <i>very</i> careful if you use mutable objects as keys. If the
 * properties of a key object change in such a way that it affects the outcome
 * the <code>compareTo()</code>/<code>Comparator.compare()</code> methods,
 * you will probably smash the structure beyond repair. You can, of course
 * change the key associated with a given <code>Entry</code>, but only
 * through the use of the <code>decreaseKey</code> method. </li>
 * </ol>
 * 
 * @param <K> the key type.
 * @param <V> the value type.
 * @author Fran Lattanzio
 * @version $Revision: 1.6.4.9 $ $Date: 2008/05/15 23:59:59 $
 * @see org.teneighty.heap.Heaps
 */
public interface Heap<K, V>
	extends Iterable<Heap.Entry<K, V>>
{


	/**
	 * Get the comparator used for decision in this heap.
	 * <p>
	 * If this method returns <code>null</code> then this heap uses the keys'
	 * <i>natural ordering</i>.
	 * 
	 * @return the comparator or <code>null</code>.
	 * @see java.util.Comparator
	 * @see java.lang.Comparable
	 */
	public Comparator<? super K> getComparator();


	/**
	 * Add a key/value pair to this heap.
	 * 
	 * @param key the node key.
	 * @param value the node value.
	 * @return the entry created.
	 * @throws ClassCastException If the specified key is not mutually comparable
	 *         with the other keys of this heap.
	 * @throws NullPointerException If <code>key</code> is <code>null</code>
	 *         and this heap does not support <code>null</code> keys.
	 */
	public Entry<K, V> insert( K key, V value )
		throws ClassCastException, NullPointerException;


	/**
	 * Insert all the entries of the specified heap into this heap.
	 * <p>
	 * The other heap will not be cleared, and this heap will simply <i>hold</i>
	 * the entries of <code>other</code>, not <i>contain</i> them.
	 * 
	 * @param other the other heap.
	 * @throws NullPointerException If <code>other</code> is <code>null</code>.
	 * @throws ClassCastException If the keys of <code>other</code> are not
	 *         mutually comparable to the keys of this heap.
	 * @throws IllegalArgumentException If you attempt to insert a heap into
	 *         itself.
	 * @see #union(Heap)
	 */
	public void insertAll( Heap<? extends K, ? extends V> other )
		throws NullPointerException, ClassCastException, IllegalArgumentException;


	/**
	 * Get the entry with the minimum key.
	 * <p>
	 * This method does <u>not</u> remove the returned entry.
	 * 
	 * @return the entry.
	 * @throws NoSuchElementException If this heap is empty.
	 * @see #extractMinimum()
	 */
	public Entry<K, V> getMinimum()
		throws NoSuchElementException;


	/**
	 * Remove and return the entry minimum key.
	 * 
	 * @return the entry.
	 * @throws NoSuchElementException If the heap is empty.
	 * @see #getMinimum()
	 */
	public Entry<K, V> extractMinimum()
		throws NoSuchElementException;


	/**
	 * Decrease the key of the given element.
	 * <p>
	 * Note that <code>e</code> must be <i>held</i> by this heap, or a
	 * <code>IllegalArgumentException</code> will be tossed.
	 * 
	 * @param e the entry for which to decrease the key.
	 * @param key the new key.
	 * @throws IllegalArgumentException If <code>k</code> is larger than
	 *         <code>e</code>'s current key or <code>e</code> is not held by
	 *         this heap.
	 * @throws ClassCastException If the new key is not mutually comparable with
	 *         other keys in the heap.
	 * @throws NullPointerException If <code>e</code> is <code>null</code>.
	 * @see #holdsEntry(Heap.Entry)
	 */
	public void decreaseKey( Entry<K, V> e, K key )
		throws IllegalArgumentException, ClassCastException, NullPointerException;


	/**
	 * Delete the entry from this heap.
	 * <p>
	 * Note that <code>e</code> must be <i>held</i> by this heap, or a
	 * <code>IllegalArgumentException</code> will be tossed.
	 * 
	 * @param e the entry to delete.
	 * @throws IllegalArgumentException If <code>e</code> is not held by this
	 *        heap.
	 * @throws NullPointerException If <code>e</code> is <code>null</code>.
	 * @see #holdsEntry(Heap.Entry)
	 */
	public void delete( Entry<K, V> e )
		throws IllegalArgumentException, NullPointerException;


	/**
	 * Union this heap with another heap.
	 * <p>
	 * Only instances of the same class are capable of being unioned together.
	 * This is a change from previous versions, when the union of different types
	 * resulting in "insertAll" type behavior. However, this meant that the union
	 * method had different semantics based on the runtime-type of the other heap,
	 * which is definitely a bad thing.
	 * <p>
	 * After a union operation, this heap will both <i>contain</i> and <i>hold</i>
	 * the entries of the other heap. The other heap is cleared in the process of
	 * union.
	 * 
	 * @param other the other heap.
	 * @throws NullPointerException If <code>other</code> is <code>null</code>.
	 * @throws ClassCastException If the keys of the nodes are not mutually
	 *         comparable or the classes do not match.
	 * @throws IllegalArgumentException If you attempt to union a heap with itself
	 *         (i.e if <code>other == this</code>).
	 * @see #insertAll(Heap)
	 */
	public void union( Heap<K, V> other )
		throws ClassCastException, NullPointerException, IllegalArgumentException;


	/**
	 * Clear this heap.
	 */
	public void clear();


	/**
	 * Get the number of entries in this heap.
	 * 
	 * @return the number of entries in this heap.
	 */
	public int getSize();


	/**
	 * Is this heap empty?
	 * 
	 * @return <code>true</code> if this heap is empty; <code>false</code>
	 *         otherwise.
	 * @see #getSize()
	 */
	public boolean isEmpty();


	/**
	 * Does this heap hold the specified entry? This method returns true iff there
	 * exists some entry <code>e</code> within this heap such that
	 * <code>e == entry</code>.
	 * <p>
	 * This method can generally be implemented efficiently (i.e. using
	 * <code>O(1)</code> time) if you are clever. See the specified
	 * implementation for info on how long this operation will take.
	 * <p>
	 * Note there is a subtle, but very important, difference between this method
	 * and <code>containsEntry</code>. This method checks to see if the
	 * specified entry is held by this heap in the sense that the specific object
	 * <code>entry</code> could be reached by hopping some arbitrary set of
	 * references (be they weak, strong, etc.) starting from a strong reference
	 * directly held by this object. This is different from
	 * <code>containsEntry</code>, which checks if this heap contains an entry
	 * with exactly the same key and value values. Obviously, if a heap <i>holds</i>
	 * a specific entry it also <i>contains</i> that entry; however, the reverse
	 * is not true.
	 * 
	 * @param entry the entry to check.
	 * @return <code>true</code> if this heap holds the specified entry;
	 *         <code>false</code> otherwise.
	 * @throws NullPointerException If <code>entry</code> is <code>null</code>.
	 * @see #containsEntry(Heap.Entry)
	 */
	public boolean holdsEntry( Entry<K, V> entry )
		throws NullPointerException;


	/**
	 * Does this heap contain the specified entry? In other words, does this heap
	 * contain entry <code>e</code> such that
	 * <code>e.equals( entry ) == true</code>. Note that this does <b>not</b>
	 * imply that <code>e == entry</code>: See
	 * {@link Heap.Entry#equals(Object)}.
	 * <p>
	 * This method generally takes <code>O(n)</code> time, although you should
	 * check the notes of the specific implementation you are using.
	 * <p>
	 * See the implementation notes under {@link #holdsEntry(Heap.Entry)} for the
	 * difference between that method and this one.
	 * 
	 * @param entry the entry to check.
	 * @return <code>true</code> if this heap contains the specified entry;
	 *         <code>false</code> otherwise.
	 * @throws NullPointerException If <code>entry</code> is <code>null</code>.
	 * @see #holdsEntry(Heap.Entry)
	 */
	public boolean containsEntry( Entry<K, V> entry )
		throws NullPointerException;


	/**
	 * Compare this heap for equality with the specified object.
	 * <p>
	 * Equality for two heaps is defined to be that they <i>contain</i>, not
	 * <i>hold</i>, the exact same set of entries. (Otherwise, two heaps could
	 * never be equal, unless they were the same object. This should be obvious
	 * from the definitions of <i>holds</i> and <i>contains</i>.) See
	 * {@link Heap.Entry#equals(Object)} for the definition of <code>Entry</code>
	 * equality. This definition is not open to debate.
	 * <p>
	 * Efficiency of this method interesting question, since it depends only on
	 * which elements are stored, not <u>how</u> they are stored. For example,
	 * it's difficult to efficiently compare a Fibonacci heap and a Binomial heap,
	 * even if they contain the same elements, since their underlying
	 * representations are very different. (In fact, it's very difficult to
	 * compare two Fibonacci heaps with the same set of entries!)
	 * 
	 * @param other the other object.
	 * @return <code>true</code> if equal; <code>false</code> otherwise.
	 */
	public boolean equals( Object other );


	/**
	 * Return the hashcode for this Heap.
	 * <p>
	 * The hashcode for <i>any</i> heap is hereby defined to be sum of the
	 * hashcodes of the entries which this heap <i>holds</i>. Like the equality
	 * definition, this is not debatable. Note that this definition does not
	 * violate the definition of <code>equals</code>, since if a heap <i>holds</i>
	 * a set of entries it must also <i>contain</i> them.
	 * <p>
	 * If you choose to override the equals method, you must also override this
	 * method, unless you really want your objects to violate the general contract
	 * of <code>Object</code>.
	 * 
	 * @return the hashcode.
	 * @see java.lang.Object#hashCode()
	 * @see #equals(Object)
	 */
	public int hashCode();


	/**
	 * Get an iterator over the entries of this heap.
	 * <p>
	 * This the method of <code>java.lang.Iterable</code> fame, allowing you to
	 * use the Heap interface within the <code>foreach(...)</code> construct.
	 * 
	 * @return an iterator over the entries of this heap.
	 */
	public Iterator<Heap.Entry<K, V>> iterator();


	/**
	 * Perform the specified action on each element of this heap.
	 * <p>
	 * It's extremely unwise to attempt to modify the heap (e.g. decrease the keys
	 * of all elements by one). Most implementations of this method are likely to
	 * be implemented atop an iterator over the heap, and thus, if the iterator is
	 * fail-fast and detects concurrent modification, any changes to the heap will
	 * cause the iterator to die.
	 * 
	 * @param action the action to perform.
	 * @throws NullPointerException If <code>action</code> is <code>null</code>.
	 */
	public void forEach( Action<Heap.Entry<K, V>> action )
		throws NullPointerException;


	/**
	 * Get the collection of keys.
	 * <p>
	 * The order of the keys in returned collection is arbitrary.
	 * 
	 * @return the keys.
	 */
	public Collection<K> getKeys();


	/**
	 * Get the collection of values.
	 * <p>
	 * The order of the values in returned collection is arbitrary.
	 * 
	 * @return the values.
	 */
	public Collection<V> getValues();


	/**
	 * Get the entry collection.
	 * <p>
	 * The order of the entries in the returned collection is arbitrary.
	 * 
	 * @return the entry collection.
	 * @see org.teneighty.heap.Heap.Entry
	 */
	public Collection<Heap.Entry<K, V>> getEntries();


	/**
	 * The heap entry interface.
	 * 
	 * @param <K> the key type.
	 * @param <V> the value type.
	 * @author Fran Lattanzio
	 * @version $Revision: 1.6.4.9 $ $Date: 2008/05/15 23:59:59 $
	 */
	public static interface Entry<K, V>
	{


		/**
		 * Get the key of this entry.
		 * 
		 * @return the key.
		 */
		public K getKey();


		/**
		 * Get the value of this entry.
		 * 
		 * @return the value.
		 * @see #setValue(Object)
		 */
		public V getValue();


		/**
		 * Set the value of this entry.
		 * 
		 * @param value the new value.
		 * @return the old value.
		 * @see #getValue()
		 */
		public V setValue( V value );


		/**
		 * A reminder to override equals.
		 * <p>
		 * Two entries are defined to be equal iff they contains exactly equal key
		 * and value objects (or <code>null</code>). Again, this definition is
		 * not open to debate.
		 * 
		 * @param other the object to which to compare.
		 * @return <code>true</code> if equal; <code>false</code> otherwise.
		 * @see java.lang.Object#equals(Object)
		 */
		public boolean equals( Object other );


		/**
		 * A reminder to override hashcode.
		 * <p>
		 * The hashcode of a heap entry is defined to be the hashcodes of this
		 * entry's key and value objects (or 0 if these objects are
		 * <code>null</code>) XOR'ed with each other.
		 * 
		 * @return int the hash code for this object.
		 * @see java.lang.Object#hashCode()
		 * @see #equals(Object)
		 */
		public int hashCode();


	}


}
