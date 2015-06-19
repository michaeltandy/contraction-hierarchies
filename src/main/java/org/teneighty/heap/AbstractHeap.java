/*
 * $Id: AbstractHeap.java,v 1.5.4.13 2008/11/22 22:32:19 fran Exp $
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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * A base class for heap implementations.
 * <p>
 * This class provides the following:
 * <ul>
 * <li><code>equals(Object)</code></li>
 * <li><code>hashCode()</code></li>
 * <li><code>getKeys()</code></li>
 * <li><code>getValues()</code></li>
 * <li><code>getEntries()</code></li>
 * <li><code>toString()</code></li>
 * <li><code>insertAll(Heap)</code></li>
 * <li><code>containsEntry(Heap.Entry)</code></li>
 * <li>Methods for entry and entry key comparison.</li>
 * <li>A base class for entry implementations.</li>
 * </ul>
 * <p>
 * This class is capable of supporting both the <code>Cloneable</code> and
 * <code>Serializable</code> interfaces (although it implements neither
 * directly).
 * 
 * @param <K> the key type.
 * @param <V> the value type.
 * @author Fran Lattanzio
 * @version $Revision: 1.5.4.13 $ $Date: 2008/11/22 22:32:19 $
 */
public abstract class AbstractHeap<K, V>
	extends Object
	implements Heap<K, V>, Iterable<Heap.Entry<K, V>>
{


	/**
	 * Compare two objects for equality, considering <code>null</code> values.
	 * 
	 * @param o1 the first object.
	 * @param o2 the second object.
	 * @return <code>true</code> if <code>o1</code> is equal to
	 *         <code>o2</code>; <code>false</code> otherwise.
	 */
	static boolean objectEquals( final Object o1, final Object o2 )
	{
		return ( ( o1 == null ) ? ( o2 == null ) : ( o1.equals( o2 ) ) );
	}


	/**
	 * Get the hashcode for the specified Object, or 0 if <code>o</code> is
	 * <code>null</code>.
	 * 
	 * @param anObject the Object for which to get a hashcode.
	 * @return the hashcode or 0 if <code>o</code> is <code>null</code>.
	 */
	static int objectHashCode( final Object anObject )
	{
		return ( ( anObject == null ) ? 0 : anObject.hashCode() );
	}


	/**
	 * This field is initialized to contain an instance of the key set view the
	 * first time this view is requested. The view is stateless, so there's no
	 * reason to create more than one.
	 */
	private transient volatile Collection<K> keys;

	/**
	 * This field is initialized to contain an instance of the values collection
	 * view the first time this view is requested.
	 */
	private transient volatile Collection<V> values;

	/**
	 * Entry collection view.
	 * <p>
	 * Like other collection-view fields, this one is lazy initialized the first
	 * time it's accessed.
	 */
	private transient volatile Collection<Heap.Entry<K, V>> entries;


	/**
	 * Constructor.
	 * <p>
	 * Should be considered <code>private protected</code>. Does nothing; here
	 * only for access protection.
	 */
	protected AbstractHeap()
	{
		super();
	}


	/**
	 * Entry compare utility method.
	 * 
	 * @param node1 the first node.
	 * @param node2 the second node.
	 * @return an integer as like {@link java.lang.Comparable#compareTo(Object)}.
	 * @throws ClassCastException If the keys of the nodes are not mutally
	 *         comparable.
	 * @throws NullPointerException If <code>node1</code> or <code>node2</code>
	 *         is <code>null</code>. This probably shouldn't happen.
	 */
	protected int compare( final Entry<K, V> node1, final Entry<K, V> node2 )
		throws ClassCastException, NullPointerException
	{
		return ( this.compareKeys( node1.getKey(), node2.getKey() ) );
	}


	/**
	 * Key compare utility method.
	 * <p>
	 * Note that if this heap uses natural ordering, a <code>null</code> key is
	 * always considered <i>smaller</i> than a non-null key. This is different
	 * from <code>SortedMap</code>, which will generally reject
	 * <code>null</code> keys when using natural ordering. This behavior is
	 * debateably useful (and/or correct) but you'll just have to learn to like
	 * it. Or use your own comparator. Or override this method.
	 * 
	 * @param k1 the first key.
	 * @param k2 the second key.
	 * @return an integer as like <code>Comparable.compare()</code>.
	 * @throws ClassCastException If <code>k1</code> and <code>k2</code> are
	 *         not mutually comparable.
	 * @see java.util.Comparator#compare(Object,Object)
	 */
	@SuppressWarnings( "unchecked" )
	protected int compareKeys( final K k1, final K k2 )
		throws ClassCastException
	{
		return ( this.getComparator() == null
				? ( ( (Comparable)k1 ).compareTo( k2 ) )
				: this.getComparator().compare( k1, k2 ) );
	}


	/**
	 * Insert all the entries of the specified heap into this heap.
	 * <p>
	 * This method takes time <code>O(n * f(m + n))</code>, where
	 * <code>n</code> is number of entries in the other heap, <code>m</code>
	 * is the number of entries in this heap and <code>f(x)</code> is time taken
	 * by the insert function of this heap.
	 * 
	 * @param other the other heap.
	 * @throws NullPointerException If <code>other</code> is <code>null</code>.
	 * @throws ClassCastException If the keys of <code>other</code> are not
	 *         mutually comparable to the keys of this heap.
	 * @throws IllegalArgumentException If you attempt to insert a heap into
	 *         itself.
	 */
	public void insertAll( final Heap<? extends K, ? extends V> other )
		throws NullPointerException, ClassCastException, IllegalArgumentException
	{
		if( other == null )
		{
			throw new NullPointerException();
		}

		if( other == this )
		{
			throw new IllegalArgumentException();
		}

		if( other.isEmpty() )
		{
			return;
		}

		// Loop over entries and stuff.
		Iterator<? extends Heap.Entry<? extends K, ? extends V>> it = other.getEntries().iterator();
		Entry<? extends K, ? extends V> entry;
		while( it.hasNext() )
		{
			entry = it.next();

			// Might throw class cast.
			this.insert( entry.getKey(), entry.getValue() );
		}
	}


	/**
	 * Is empty.
	 * 
	 * @return boolean true if empty.
	 */
	public boolean isEmpty()
	{
		return ( this.getSize() == 0 );
	}


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
	public void forEach( final Action<Heap.Entry<K, V>> action )
		throws NullPointerException
	{
		if( action == null )
		{
			throw new NullPointerException();
		}

		// note that if action changes the state of this heap - and the
		// iterator detects concurrent modification - we'll die right here
		// with a ConcurrentModificationException (or something equally sinister).
		Iterator<Heap.Entry<K,V>> entryIterator = this.iterator();
		while( entryIterator.hasNext() )
		{
			action.action( entryIterator.next() );
		}		
	}


	/**
	 * A dumb (but generic) version of equals.
	 * <p>
	 * Two heaps are considered if they <i>contain</i> exactly the same set of
	 * entries. (Note that two entries are considered equal if they contain
	 * exactly the same key and value.)
	 * <p>
	 * This method works by comparing the entry sets of each class, a process that
	 * takes time <code>O(n<sup>2</sup>)</code> in the worst case.
	 * 
	 * @param other the object to which to compare this heap.
	 * @return <code>true</code> if <code>other</code> is logically equal to
	 *         this heap; <code>false</code> otherwise.
	 * @see java.lang.Object#equals(Object)
	 * @see #hashCode()
	 */
	@SuppressWarnings( "unchecked" )
	@Override
	public boolean equals( final Object other )
	{
		if( other == null )
		{
			return ( false );
		}

		if( this == other )
		{
			return ( true );
		}

		if( Heap.class.isInstance( other ) == false )
		{
			return ( false );
		}

		// erased cast... a little bit evil. We should also figure out
		// here if we want to just cast to Heap<K,V>.
		Heap<? extends K, ? extends V> that = (Heap<? extends K, ? extends V>)other;
		return ( this.getEntries().equals( that.getEntries() ) );
	}


	/**
	 * Return the hashcode for this Heap.
	 * <p>
	 * The hashcode for <i>any</i> heap is hereby defined to be sum of the
	 * hashcodes of the entries which this heap <i>holds</i>. Like the equality
	 * definition, this is not debatable. Note that this definition does not
	 * violate the definition of <code>equals</code>, since if a heap <i>holds</i>
	 * a set of entries it must also <i>contain</i> them.
	 * <p>
	 * Assume, of course, that you're assuming the canonically defined hashcode
	 * for heap entries. (More precisely, this implementation will be correct if
	 * you use the hashcode supplied by <code>AbstractHeapEntry</code>.)
	 * 
	 * @return the hashcode.
	 */
	@Override
	public int hashCode()
	{
		int code = 0;
		Iterator<Heap.Entry<K, V>> it = this.getEntries().iterator();
		while( it.hasNext() )
		{
			code += it.next().hashCode();
		}

		return ( code );
	}


	/**
	 * A happier to string.
	 * <p>
	 * You can override this method if your heap implementation is extra special.
	 * 
	 * @return a string representation of this object.
	 */
	@Override
	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append( this.getClass().getName() );
		buffer.append( "(" );
		buffer.append( this.getSize() );
		buffer.append( ") " );
		buffer.append( "{" );

		Iterator<Heap.Entry<K, V>> it = this.getEntries().iterator();
		boolean next = it.hasNext();
		Heap.Entry<K, V> entry = null;
		K k = null;
		V v = null;
		while( next )
		{
			// Get next entry.
			entry = it.next();
			k = entry.getKey();
			v = entry.getValue();

			// Append mapping.
			buffer.append( ( k == this ) ? "[self-reference]" : String.valueOf( k ) );
			buffer.append( "->" );
			buffer.append( ( v == this ) ? "[self-reference]" : String.valueOf( v ) );

			// Check advance.
			next = it.hasNext();
			if( next )
			{
				buffer.append( ", " );
			}
		}

		buffer.append( "}" );
		return ( buffer.toString() );
	}


	/**
	 * Does this heap contain the specified entry? In other words, does this heap
	 * contain entry <code>e</code> such that
	 * <code>e.equals( entry )</code>. Note that this does <i>not</i>
	 * imply that <code>e == entry</code>: See
	 * {@link Heap.Entry#equals(Object)}.
	 * 
	 * @param entry the entry to check.
	 * @return <code>true</code> if this heap contains the specified entry;
	 *         <code>false</code> otherwise.
	 * @throws NullPointerException If <code>entry</code> is <code>null</code>.
	 */
	public boolean containsEntry( final Entry<K, V> entry )
		throws NullPointerException
	{
		if( entry == null )
		{
			throw new NullPointerException();
		}

		// Iterate over all entries...
		Iterator<Heap.Entry<K, V>> it = this.getEntries().iterator();
		Entry<K, V> next = null;
		while( it.hasNext() )
		{
			next = it.next();
			if( next.equals( entry ) )
			{
				return ( true );
			}
		}

		// Nope.
		return ( false );
	}


	/**
	 * Returns a Collection view of the keys contained in this heap. The
	 * Collection is backed by the heap, so changes to the heap are reflected in
	 * the Collection, and vice-versa. (If the heap is modified while an iteration
	 * over the Collection is in progress, the results of the iteration are
	 * undefined.) The returned collection is readonly.
	 * <p>
	 * The Collection is created the first time this method is called, and
	 * returned in response to all subsequent calls. No synchronization is
	 * performed, so there is a slight chance that multiple calls to this method
	 * will not all return the same Collection.
	 * 
	 * @return a Collection view of the keys contained in this heap.
	 */
	public Collection<K> getKeys()
	{
		if( this.keys == null )
		{
			this.keys = new KeyCollection();
		}

		return ( this.keys );
	}


	/**
	 * Returns a collection view of the values contained in this heap. The
	 * collection is backed by the heap, so changes to the heap are reflected in
	 * the collection, and vice-versa. (If the heap is modified while an iteration
	 * over the collection is in progress, the results of the iteration are
	 * undefined.) The returned collection is read-only.
	 * <p>
	 * The collection is created the first time this method is called, and
	 * returned in response to all subsequent calls. No synchronization is
	 * performed, so there is a slight chance that multiple calls to this method
	 * will not all return the same Collection.
	 * 
	 * @return a collection view of the values contained in this heap.
	 */
	public Collection<V> getValues()
	{
		if( this.values == null )
		{
			this.values = new ValueCollection();
		}

		return ( this.values );
	}


	/**
	 * Returns a Collection view of the entries contained in this heap. The
	 * Collection is backed by the heap, so changes to the heap are reflected in
	 * the Collection, and vice-versa. (If the heap is modified while an iteration
	 * over the Collection is in progress, the results of the iteration are
	 * undefined.) The returned collection is readonly.
	 * <p>
	 * The Collection is created the first time this method is called, and
	 * returned in response to all subsequent calls. No synchronization is
	 * performed, so there is a slight chance that multiple calls to this method
	 * will not all return the same Collection.
	 * 
	 * @return a Collection view of the entries contained in this heap.
	 */
	public Collection<Heap.Entry<K, V>> getEntries()
	{
		if( this.entries == null )
		{
			this.entries = new EntryCollection();
		}

		return ( this.entries );
	}


	/**
	 * Override clone to clear lame fields.
	 * 
	 * @throws CloneNotSupportedException If this object's class does not
	 *         implement (and support) the <code>Cloneable</code> interface.
	 * @return a clone of this object.
	 */
	@SuppressWarnings( "unchecked" )
	@Override
	protected Object clone()
		throws CloneNotSupportedException
	{
		// May throw clone not supported.
		AbstractHeap<K, V> ah = (AbstractHeap<K, V>)super.clone();

		// Clear lame fields.
		ah.keys = null;
		ah.values = null;
		ah.entries = null;

		return ( ah );
	}


	/**
	 * Basic implementation of a collection that's backed by a heap (or, more
	 * precisely, the enclosing heap instance, as this is an inner class).
	 * <p>
	 * This implementation ensures that this collection is read-only. It also
	 * efficiently implements the <code>size()</code> method by simply
	 * delegating to the enclosing heap.
	 * <p>
	 * This class also implements <code>equals(Object)</code> and
	 * <code>hashCode()</code> in a reasonably intelligent fashion.
	 * 
	 * @param <T> the collection type.
	 * @author Fran Lattanzio
	 * @version $Revision: 1.5.4.13 $ $Date: 2008/11/22 22:32:19 $
	 */
	private abstract class AbstractHeapCollection<T>
		extends AbstractCollection<T>
		implements Collection<T>
	{


		/**
		 * Constructor.
		 * <p>
		 * Does nothing. Here only for access protection and should be considered
		 * <code>private protected</code>.
		 */
		protected AbstractHeapCollection()
		{
			super();
		}


		/**
		 * Get the size of this collection.
		 * <p>
		 * Defers to the enclosing heap.
		 * 
		 * @return the size.
		 */
		@Override
		public final int size()
		{
			return ( AbstractHeap.this.getSize() );
		}


		/**
		 * Add the specified element to this collection.
		 * 
		 * @param objectToAdd the object to add.
		 * @return <code>true</code> if the specified element was added.
		 * @throws UnsupportedOperationException always - this collection is
		 *         readonly.
		 */
		@Override
		public boolean add( final T objectToAdd )
			throws UnsupportedOperationException
		{
			throw new UnsupportedOperationException();
		}


		/**
		 * Add all elements in the specified collection.
		 * 
		 * @param collectionToAdd the collection to add.
		 * @return <code>true</code> if any elements were added.
		 * @throws UnsupportedOperationException always - this collection is
		 *         readonly.
		 */
		@Override
		public boolean addAll( final Collection<? extends T> collectionToAdd )
			throws UnsupportedOperationException
		{
			throw new UnsupportedOperationException();
		}


		/**
		 * Clear this collection.
		 * 
		 * @throws UnsupportedOperationException always - this collection is
		 *         readonly.
		 */
		@Override
		public final void clear()
			throws UnsupportedOperationException
		{
			throw new UnsupportedOperationException();
		}


		/**
		 * Remove the specified object from this collection.
		 * 
		 * @param objectToRemove the object to remove.
		 * @return <code>true</code> if <code>objectToRemove</code> was actually
		 *         removed.
		 * @throws UnsupportedOperationException always - this collection is
		 *         readonly.
		 */
		@Override
		public final boolean remove( final Object objectToRemove )
			throws UnsupportedOperationException
		{
			throw new UnsupportedOperationException();
		}


		/**
		 * Remove all object in the specifed collection from this collection.
		 * 
		 * @param objectsToRemove the objects to remove.
		 * @return <code>true</code> if all were removed; <code>false</code>
		 *         otherwise.
		 * @throws NullPointerException If <code>objectsToRemove</code> is
		 *         <code>null</code>.
		 * @throws UnsupportedOperationException If the aforementioned
		 *         <code>NullPointerException</code> isn't thrown.
		 */
		@Override
		public final boolean removeAll( final Collection<?> objectsToRemove )
			throws NullPointerException, UnsupportedOperationException
		{
			if( objectsToRemove == null )
			{
				throw new NullPointerException();
			}

			throw new UnsupportedOperationException();
		}


		/**
		 * Retain only the objects in the specified collection.
		 * 
		 * @param objectsToRetain the objects to retain.
		 * @return <code>true</code> if this collection is modified;
		 *         <code>false</code> otherwise.
		 * @throws NullPointerException If <code>objectsToRetain</code> is
		 *         <code>null</code>.
		 * @throws UnsupportedOperationException If the aforementioned
		 *         <code>NullPointerException</code> isn't thrown.
		 */
		@Override
		public final boolean retainAll( final Collection<?> objectsToRetain )
			throws NullPointerException, UnsupportedOperationException
		{
			if( objectsToRetain == null )
			{
				throw new NullPointerException();
			}

			throw new UnsupportedOperationException();
		}


		/**
		 * Check is this collection contains the specified element.
		 * 
		 * @param objectToCheck the element to check.
		 * @return <code>true</code> if this collection contains the specified
		 *         element.
		 */
		@Override
		public boolean contains( final Object objectToCheck )
		{
			// get an iterator over this collection.
			Iterator<T> iterator = iterator();

			while( iterator.hasNext() )
			{
				if( objectEquals( objectToCheck, iterator.next() ) )
				{
					return ( true );
				}

			}

			return ( false );
		}


		/**
		 * Get the hashcode for this collection.
		 * 
		 * @return the hashcode.
		 */
		@Override
		public final int hashCode()
		{
			int hashCode = 0;

			Iterator<T> iterator = this.iterator();
			T next;
			while( iterator.hasNext() )
			{
				next = iterator.next();
				hashCode ^= objectHashCode( next );
			}

			return ( hashCode );
		}


		/**
		 * Compare with the specifed object for eqaulity.
		 * 
		 * @param other the object to which to compare.
		 * @return <code>true</code> if equal; <code>false</code> otherwise.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public final boolean equals( final Object other )
		{
			if( other == null )
			{
				return ( false );
			}

			if( other == this )
			{
				return ( true );
			}

			if( Collection.class.isInstance( other ) == false )
			{
				return ( false );
			}

			// we have some work to do...

			// erased cast... here so that if/when Java ever starts retaining generic
			// type information
			// at runtime, this method will be "more" correct.
			Collection<T> that = (Collection<T>)other;

			// cheap check sizes.
			if( that.size() != this.size() )
			{
				return ( false );
			}

			// create shallow, mutable clones of both collections (this and that). We
			// use linked lists here because they support removal at point (via their
			// iterator) in O( 1 ) time, as well as O( 1 ) removal of the first
			// element.
			List<T> itemsInThis = new LinkedList<T>( this );
			List<T> itemsInThat = new LinkedList<T>( that );

			boolean foundElement;
			T elementInThis;
			T elementInThat;
			Iterator<T> iteratorOverThat;
			while( itemsInThis.size() > 0 )
			{
				// basically, we're going to start pulling elements off the queue of
				// elements in this collection, and make sure each one exists in the
				// list of elements in that. When we find one, we remove it from the
				// list of elements in that and repeat until both lists are empty, or we
				// don't find a corresponding element.
				foundElement = false;
				elementInThis = itemsInThis.remove( 0 );
				iteratorOverThat = itemsInThat.iterator();

				while( iteratorOverThat.hasNext() )
				{
					elementInThat = iteratorOverThat.next();

					if( objectEquals( elementInThis, elementInThat ) )
					{
						// remove element in that.
						iteratorOverThat.remove();

						// we found one equal - break to next element,
						foundElement = true;
						break;
					}
				}

				if( foundElement == false )
				{
					return ( false );
				}
			}

			// they must be equal!
			return ( true );
		}


	}


	/**
	 * Entry collection helper class.
	 * <p>
	 * This collection is readonly.
	 * <p>
	 * Note that the <code>contains</code> method of the returned collection is
	 * implemented in terms of <code>containsEntry</code>, rather than
	 * <code>holdsEntry</code>.
	 * 
	 * @author Fran Lattanzio
	 * @version $Revision: 1.5.4.13 $ $Date: 2008/11/22 22:32:19 $
	 */
	private final class EntryCollection
		extends AbstractHeapCollection<Heap.Entry<K, V>>
		implements Collection<Heap.Entry<K, V>>
	{


		/**
		 * Constructor.
		 * <p>
		 * Does nothing; here only for access protection.
		 */
		EntryCollection()
		{
			super();
		}


		/**
		 * Get an iterator over this heap's entries.
		 * 
		 * @return Iterator {@literal <Heap.Entry<K,V>>}
		 */
		@Override
		public Iterator<Heap.Entry<K, V>> iterator()
		{
			return ( AbstractHeap.this.iterator() );
		}


		/**
		 * Check if this heap contains the specified entry.
		 * <p>
		 * Defers to enclosing heap, since if the enclosing heap holds the specified
		 * entry, we'll (probably) get much better performance.
		 * 
		 * @param o the object to check.
		 * @return boolean true if contained.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public final boolean contains( final Object o )
		{
			if( o == null )
			{
				return ( false );
			}

			if( Entry.class.isAssignableFrom( o.getClass() ) == false )
			{
				return ( false );
			}

			Heap.Entry<K, V> e = (Heap.Entry<K, V>)o;
			return ( AbstractHeap.this.containsEntry( e ) );
		}


	}


	/**
	 * Collection view over the keys in this enclosing heap.
	 * <p>
	 * Instances of this class are readonly (readonly collections, that is).
	 * 
	 * @author Fran Lattanzio
	 * @version $Revision: 1.5.4.13 $ $Date: 2008/11/22 22:32:19 $
	 */
	private final class KeyCollection
		extends AbstractHeapCollection<K>
		implements Collection<K>
	{


		/**
		 * Constructor.
		 * <p>
		 * Does nothing; here only for access protection.
		 */
		KeyCollection()
		{
			super();
		}


		/**
		 * Get the iterator over the elements in this collection.
		 * 
		 * @return an iterator over the keys.
		 */
		@Override
		public Iterator<K> iterator()
		{
			return ( new KeyIterator() );
		}


		/**
		 * Iterator over the keys in enclosing heap.
		 * 
		 * @author Fran Lattanzio
		 * @version $Revision: 1.5.4.13 $ $Date: 2008/11/22 22:32:19 $
		 */
		private final class KeyIterator
			extends Object
			implements Iterator<K>
		{


			/**
			 * Backing iterator.
			 */
			private Iterator<Heap.Entry<K, V>> backingIterator;


			/**
			 * Constructor.
			 */
			KeyIterator()
			{
				super();

				this.backingIterator = AbstractHeap.this.iterator();
			}


			/**
			 * Does this iterator contain another entry?
			 * 
			 * @return <code>true</code> if more.
			 */
			public boolean hasNext()
			{
				return ( this.backingIterator.hasNext() );
			}


			/**
			 * Get the next key.
			 * 
			 * @return the next key.
			 */
			public K next()
			{
				return ( this.backingIterator.next().getKey() );
			}


			/**
			 * Remove the previously iterated entry.
			 */
			public void remove()
			{
				this.backingIterator.remove();
			}

		}
	}


	/**
	 * Collection view over the values in this heap.
	 * 
	 * @author Fran Lattanzio
	 * @version $Revision: 1.5.4.13 $ $Date: 2008/11/22 22:32:19 $
	 */
	private final class ValueCollection
		extends AbstractHeapCollection<V>
		implements Collection<V>
	{


		/**
		 * Constructor.
		 * <p>
		 * Does nothing; here only for access protection.
		 */
		ValueCollection()
		{
			super();
		}


		/**
		 * Get an iterator over this collection.
		 * 
		 * @return an iterator over this collection.
		 */
		@Override
		public Iterator<V> iterator()
		{
			// Everything is implemented atop the entry collection iterator.
			return ( new ValueIterator() );
		}


		/**
		 * Iterates over the values in this heap.
		 * 
		 * @author Fran Lattanzio
		 * @version $Revision: 1.5.4.13 $ $Date: 2008/11/22 22:32:19 $
		 */
		private final class ValueIterator
			extends Object
			implements Iterator<V>
		{


			/**
			 * Backing iterator.
			 */
			private Iterator<Heap.Entry<K, V>> backingIterator;


			/**
			 * Constructor.
			 */
			ValueIterator()
			{
				super();

				// get backing iterator.
				this.backingIterator = AbstractHeap.this.iterator();
			}


			/**
			 * Has next?
			 * 
			 * @return true if next.
			 */
			public boolean hasNext()
			{
				return ( this.backingIterator.hasNext() );
			}


			/**
			 * Get the next value.
			 * 
			 * @return the next value.
			 */
			public V next()
			{
				return ( this.backingIterator.next().getValue() );
			}


			/**
			 * Remove the previously iterated value.
			 */
			public void remove()
			{
				this.backingIterator.remove();
			}

		}

	}


	/**
	 * Basic heap entry/node.
	 * <p>
	 * This entry stores the keys and values for the node, provides basic
	 * <code>get/set</code> methods on them, and contains the "correct"
	 * definitions for <code>equals(Object)</code> and <code>hashCode()</code>
	 * (based on their by-fiat definitions in the <code>Heap.Entry</code>
	 * interface. This class also provides a handy and safe implementation of
	 * <code>toString()</code>. (It's safe in the sense that it checks for
	 * self-references.)
	 * 
	 * @param <K> the key type.
	 * @param <V> the value type.
	 * @author Fran Lattanzio
	 * @version $Revision: 1.5.4.13 $ $Date: 2008/11/22 22:32:19 $
	 */
	protected static abstract class AbstractHeapEntry<K, V>
		extends Object
		implements Heap.Entry<K, V>
	{


		/**
		 * The key.
		 */
		private K key;

		/**
		 * The value.
		 */
		private V value;


		/**
		 * Constructor.
		 * 
		 * @param key the key.
		 * @param value the value.
		 */
		protected AbstractHeapEntry( final K key, final V value )
		{
			super();

			// Store key and value.
			this.key = key;
			this.value = value;
		}


		/**
		 * Get the key for this entry.
		 * 
		 * @return the key.
		 * @see #setKey(Object)
		 */
		public final K getKey()
		{
			return ( this.key );
		}


		/**
		 * Set the key for this entry.
		 * <p>
		 * Generally, this method call should only be made in the context of a
		 * <code>decreaseKey()</code> call.
		 * <p>
		 * This is not part of the <code>Heap.Entry</code> interface.
		 * 
		 * @param key the new key.
		 * @see #getKey()
		 */
		public final void setKey( final K key )
		{
			this.key = key;
		}


		/**
		 * Get the value for this entry.
		 * 
		 * @return the value.
		 * @see #setValue(Object)
		 */
		public final V getValue()
		{
			return ( this.value );
		}


		/**
		 * Set the value for this entry.
		 * 
		 * @param value the new value.
		 * @return the old value.
		 * @see #getValue()
		 */
		public final V setValue( final V value )
		{
			V tmp = this.value;
			this.value = value;
			return ( tmp );
		}


		/**
		 * Returns <code>true</code> if <code>o</code> equals this object.
		 * <p>
		 * Two heap nodes are defined to be equal if they have the same keys and
		 * values. This method works even in the face of <code>null</code> keys
		 * and values.
		 * <p>
		 * This method is <code>final</code> because it is implemented correctly,
		 * as defined by the <code>Heap.Entry</code> interface.
		 * 
		 * @param other the other object.
		 * @return <code>true</code> if equal.
		 */
		@SuppressWarnings( "unchecked" )
		@Override
		public final boolean equals( final Object other )
		{
			if( other == null )
			{
				return ( false );
			}

			if( this == other )
			{
				return ( true );
			}

			if( Heap.Entry.class.isInstance( other ) == false )
			{
				return ( false );
			}

			Heap.Entry that = (Heap.Entry)other;

			// Use happier version to check for null.
			return ( objectEquals( this.key, that.getKey() ) && objectEquals( this.value, that.getValue() ) );
		}


		/**
		 * Hashcode inline with equals.
		 * <p>
		 * Hashcode is defined to be the hashcodes of the key and value objects (or
		 * 0 if these objects are <code>null</code>) XOR'ed with each other.
		 * <p>
		 * This method is <code>final</code> because it is implemented correctly,
		 * as defined by the <code>Heap.Entry</code> interface.
		 * 
		 * @return the hashcode.
		 */
		@Override
		public final int hashCode()
		{
			return ( objectHashCode( this.key ) ^ objectHashCode( this.value ) );
		}


		/**
		 * Return a string representation of this object... Only really useful for
		 * debugging.
		 * <p>
		 * This implementation is, however, smart enough to avoid blowing up on
		 * self-referential entries.
		 * 
		 * @return a string representation of this object.
		 */
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append( this.getKey() == this ? "[self-reference]"
					: String.valueOf( this.getKey() ) );
			sb.append( "->" );
			sb.append( this.getValue() == this ? "[ self-reference]"
					: String.valueOf( this.getValue() ) );
			return ( sb.toString() );
		}


	}


}
