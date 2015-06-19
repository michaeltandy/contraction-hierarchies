/*
 * $Id: FibonacciHeap.java,v 1.6.4.12 2008/11/22 22:32:19 fran Exp $
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
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;


/**
 * A Fibonacci heap implementation.
 * <p>
 * A Fibonacci heap is a node-disjoint set of trees (i.e. a forest), each of
 * which of satisfy the heap property: The key of a child node is never greater
 * than the key of its parent. Clearly, this means that the smallest node is at
 * the root of any tree, and that the minimum of the heap as a whole must be
 * among the roots of the forest. Fibonacci heaps have a relaxed structure - the
 * trees have no prescribed shape (allowing for an abitrary number of children)
 * nor are there restrictions on the number of trees in the forest. This
 * flexibility allows Fibonacci heaps to do work in a lazy manner. We postpone
 * doing work on the heap structure until it is convenient (or easy) to do.
 * <p>
 * Two basic operations determine the structure of a Fibonacci heap:
 * <ul>
 * <li> <b>Linking:</b><br>
 * Linking reduces the number of trees in the forest by linking trees whose
 * roots have the same degree (here, degree means the number of direct
 * children). The linking is performed in such a way that the degree of any
 * given node never exceeds <code>O(log n)</code>. </li>
 * <li> <b>Cutting:</b><br>
 * Cutting creates an additional tree in the forst by removing a child from a
 * node. Any non-root node can have only one child cut from it before the node
 * itself needs to be cut. This ensures that the size of subtree, whose parent
 * has degree <code>d</code>, is at least <code>F<sub>d+2</sub></code>,
 * where <code>F<sub>i</sub></code> is the <code>i<sup>th</sup></code>
 * Fibonacci number. </li>
 * </ul>
 * <p>
 * All methods/algorithms of this class are based on the code in <i>Introduction
 * to Algorithms</i> by Cormen et al (refered to as CLRS hereafter). Generally,
 * I use the same variable names and such with those in the text. You should
 * refer to the text if you want a better/deeper understanding of a particular
 * procedure's purpose or correctness.
 * <p>
 * The collection-view methods of this class are backed by iterators over the
 * heap structure which are <i>fail-fast</i>: If the heap is structurally
 * modified at any time after the iterator is created, the iterator throws a
 * <code>ConcurrentModificationException</code>. Thus, in the face of
 * concurrent modification, the iterator fails quickly and cleanly, rather than
 * risking arbitrary, non-deterministic behavior at an undetermined time in the
 * future. The collection-views returned by this class do not support the
 * <code>remove()</code> operation. This is, unfortunately, not possible using
 * stateless iterators. (By stateless, I simply mean that the iterator holds a
 * reference to a single entry in the heap, and asks the heap itself for the
 * "successor" to its current entry to find the next entry.) As long as the heap
 * structure remains the same, this function will work across multiple calls.
 * However, the several of the operations may result in substantial modification
 * to the heap structure, depending on the state of the heap. At this point, the
 * semantics of the "successor" call (with no context as to which nodes have
 * already been iterated over and which have not) are no longer well-defined.
 * Alternatively, the iterators could support a single remove operation, after
 * which they would fail with a <code>ConcurrentModificationException</code>.
 * However, this is almost certainly more annoying, and thus this is not done.
 * <p>
 * This class is not synchronized (by choice). You must ensure sequential access
 * externally, or you may damage instances of this class. Damage may be subtle
 * and difficult to detect, or it may be pronounced. You can use
 * {@link org.teneighty.heap.Heaps#synchronizedHeap(Heap)} to obtain
 * synchronized instances of this class.
 * <p>
 * The serialization mechanism of this class warrants some discussion. The full
 * heap structure is not serialized to the stream. It would fairly stupid to do
 * this. Instead, this class serializes only the key/value pairs, and restores
 * the heap completely "flat". In other words, any internal balancing that was
 * performed by the serialized instance will be forgotten and not restored by
 * any deserialized versions. Of course, a deserialized version will exhibit the
 * same external behavior and amortized time bounds, despite the fact the the
 * internal structure may be vastly different. Note that the worst-case time
 * bounds for a deserialized version may be worse, but a thorough
 * discussion/investigation is beyond the scope of these comments.
 * 
 * @param <K> the key type.
 * @param <V> the value type.
 * @author Fran Lattanzio
 * @version $Revision: 1.6.4.12 $ $Date: 2008/11/22 22:32:19 $
 * @see "Cormen, T. H.; Leiserson C. E.; Rivest R. L.; &amp; Stein, C (2001)
 *      <i>Introduction to Algorithms</i>. MIT Press."
 */
public class FibonacciHeap<K, V>
	extends AbstractLinkedHeap<K, V>
	implements Heap<K, V>, Iterable<Heap.Entry<K, V>>, Serializable
{


	/**
	 * Serialization ID.
	 */
	private static final long serialVersionUID = 9802348L;


	/**
	 * The minimum entry of this heap.
	 */
	private FibonacciHeapEntry<K, V> minimum;

	/**
	 * The size of this heap.
	 */
	private int size;

	/**
	 * The mod count.
	 */
	private volatile int mod_count;

	/**
	 * Comparator.
	 */
	private Comparator<? super K> comp;

	/**
	 * The heap reference.
	 */
	private HeapReference source_heap;


	/**
	 * Constructor.
	 * <p>
	 * The nodes of this heap will be ordered by their keys' <i>natural ordering</i>.
	 * <p>
	 * The keys of all nodes inserted into the heap must implement the
	 * <code>Comparable</code> interface. Furthermore, all such keys must be
	 * <i>mutually comparable</i>:<code>k1.compareTo(k2)</code> must not throw
	 * a <code>ClassCastException</code> for any elements <code>k1</code> and
	 * <code>k2</code> in the heap.
	 */
	public FibonacciHeap()
	{
		this( null );
	}


	/**
	 * Constructor.
	 * <p>
	 * The keys of all nodes inserted into the heap must be <i>mutually comparable</i>
	 * by the given <code>Comparator</code>:
	 * <code>comparator.compare(k1,k2)</code> must not throw a
	 * <code>ClassCastException</code> for any keys <code>k1</code> and
	 * <code>k2</code> in the heap.
	 * 
	 * @param comp the comparator to use. A <code>null</code> means the keys'
	 *        natural ordering will be used.
	 */
	public FibonacciHeap( final Comparator<? super K> comp )
	{
		super();

		// Null min.
		this.minimum = null;
		this.size = 0;
		this.mod_count = 0;
		this.comp = comp;
		this.source_heap = new HeapReference( this );
	}


	/**
	 * Get the size.
	 * 
	 * @return the size.
	 */
	public int getSize()
	{
		return ( this.size );
	}


	/**
	 * Get the the Comparator.
	 * <p>
	 * If this method returns <code>null</code>, then this heap uses the keys'
	 * <i>natural ordering</i>.
	 * 
	 * @return the Comparator or <code>null</code>.
	 */
	public Comparator<? super K> getComparator()
	{
		return ( this.comp );
	}


	/**
	 * Insert the given key/value pair into this heap, returning the entry in
	 * which the new pair is stored.
	 * <p>
	 * Code basically identical to CLRS.
	 * 
	 * @param key the key to insert.
	 * @param value the value.
	 * @return the newly created and inserted Entry.
	 * @throws ClassCastException If the key of <code>node</code> is not
	 *         mutually comparable with the keys of other nodes already in this
	 *         heap.
	 * @throws NullPointerException If <code>node</code> is <code>null</code>.
	 */
	public Entry<K, V> insert( final K key, final V value )
		throws ClassCastException, NullPointerException
	{
		FibonacciHeapEntry<K, V> node = new FibonacciHeapEntry<K, V>( key, value, this.source_heap );

		// Do some node housekeeping.
		node.degree = 0;
		node.marked = false;
		node.left = node.right = node;
		node.parent = null;
		node.child = null;

		// Connect to root node.
		if( this.minimum == null )
		{
			this.minimum = node;
		}
		else
		{
			// Check for key compatibility before inserting.
			// May throw class cast...
			int cmp = compare( node, this.minimum );

			// Insert into root list.
			this.minimum.right.left = node;
			node.right = this.minimum.right;
			this.minimum.right = node;
			node.left = this.minimum;

			// We have a new winner...
			if( cmp < 0 )
			{
				this.minimum = node;
			}
		}

		// Inc size
		this.size += 1;

		// Inc mod cout.
		this.mod_count += 1;

		// Return the new node.
		return ( node );
	}


	/**
	 * Remove and return the minimum entry in this heap.
	 * <p>
	 * Code based on CLRS.
	 * 
	 * @return the entry with the smallest key.
	 * @throws NoSuchElementException If this heap is empty.
	 */
	public Entry<K, V> extractMinimum()
		throws NoSuchElementException
	{
		if( this.isEmpty() )
		{
			throw new NoSuchElementException();
		}

		// References that will be needed. See CLRS.
		FibonacciHeapEntry<K, V> t;
		FibonacciHeapEntry<K, V> w;
		FibonacciHeapEntry<K, V> z = this.minimum;

		if( z.child != null )
		{
			// Remove parent references for all of z's children.
			w = z.child;
			t = w;

			do
			{
				t.parent = null;
				t = t.right;
			}
			while( t != w );

			// Add the children to the root list.
			this.minimum.left.right = w.right;
			w.right.left = this.minimum.left;
			this.minimum.left = w;
			w.right = this.minimum;
		}

		// Remove z from the root list.
		z.left.right = z.right;
		z.right.left = z.left;

		if( z == z.right )
		{
			// We hope the heap is now empty...
			this.minimum = null;
		}
		else
		{
			// We have some work to do.
			this.minimum = z.right;
			this.consolidate();
		}

		// Dec size, inc mod.
		this.size -= 1;
		this.mod_count += 1;

		// Clear old heap reference.
		z.clearSourceReference();

		// Return old minimum.
		return ( z );
	}


	/**
	 * Get the minimum node of this heap.
	 * <p>
	 * The returned node is not removed.
	 * 
	 * @return the minimum node.
	 * @throws NoSuchElementException If the heap is empty.
	 */
	public Entry<K, V> getMinimum()
		throws NoSuchElementException
	{
		if( this.isEmpty() )
		{
			throw new NoSuchElementException();
		}

		// Return it.
		return ( this.minimum );
	}


	/**
	 * Run the consolidate operation.
	 * <p>
	 * Based on CLRS code, but with minor mods...
	 */
	@SuppressWarnings( "unchecked" )
	private void consolidate()
	{
		// Create the auxiliary array.
		int dn = (int)Math.floor( Math.log( this.size ) / Math.log( 2 ) ) + 2;
		FibonacciHeapEntry[] a = new FibonacciHeapEntry[ dn ];

		// Iterating node - node at which to stop iterating...
		FibonacciHeapEntry<K, V> iter = this.minimum;

		// The node we're on now; w from CLRS.
		FibonacciHeapEntry<K, V> w = iter;

		// x and y from CLRS code.
		FibonacciHeapEntry<K, V> x;
		FibonacciHeapEntry<K, V> y;

		// temp ref.
		FibonacciHeapEntry<K, V> temp;

		// d from CLRS code.
		int d;

		do
		{
			x = w;
			d = x.degree;

			if( a[ d ] != x )
			{
				while( a[ d ] != null )
				{
					// y has same degree as x... This much we know.
					y = a[ d ];

					if( compare( y, x ) < 0 )
					{
						// Swap x and y.
						temp = x;
						x = y;
						y = temp;
					}

					// Make y a child of x.
					this.link( y, x );
					iter = x;
					w = x;
					a[ d ] = null;
					d += 1;
				}

				a[ d ] = x;
			}

			// Next node.
			w = w.right;
		}
		while( w != iter );

		// Reset... we need to iterate over the root list again.
		this.minimum = iter;
		w = iter;

		// Find the new minimum in the root list (if we don't already have it).
		do
		{
			if( compare( w, this.minimum ) < 0 )
			{
				// Found a new minimum node.
				this.minimum = w;
			}

			// Next.
			w = w.right;
		}
		while( w != iter );

	}


	/**
	 * Link <code>y</code> to <code>x</code>, by removing <code>y</code>
	 * from the root list and making it a child of <code>x</code>.
	 * 
	 * @param y the new child node.
	 * @param x the new parent node.
	 */
	private void link( final FibonacciHeapEntry<K, V> y,
			final FibonacciHeapEntry<K, V> x )
	{
		// Remove y from the root list.
		y.left.right = y.right;
		y.right.left = y.left;

		if( x.child == null )
		{
			// x is all alone in the world.
			y.right = y;
			y.left = y;
			x.child = y;
		}
		else
		{
			// Concat into child list of x.
			y.right = x.child.right;
			y.left = x.child;
			x.child.right.left = y;
			x.child.right = y;
		}

		// Some housekeeping for the nodes.
		y.parent = x;
		x.degree += 1;
		y.marked = false;
	}


	/**
	 * Delete the specified entry.
	 * <p>
	 * This class can always cheaply determine of <code>e</code> is not a member
	 * of this heap (in <code>O(1)</code> time).
	 * 
	 * @param e entry to delete.
	 * @throws IllegalArgumentException If <code>e</code> is not in this heap.
	 * @throws NullPointerException If <code>e</code> is <code>null</code>.
	 */
	public void delete( final Heap.Entry<K, V> e )
		throws IllegalArgumentException, NullPointerException
	{
		// Check and cast.
		if( this.holdsEntry( e ) == false )
		{
			throw new IllegalArgumentException();
		}

		// Narrow.
		FibonacciHeapEntry<K, V> entry = (FibonacciHeapEntry<K, V>)e;

		// Make it infinitely small.
		entry.is_infinite = true;

		// Percolate the top,
		this.decreaseKeyImpl( entry );

		// Remove.
		this.extractMinimum();

		// Reset entry state.
		entry.is_infinite = false;
	}


	/**
	 * Decrease the key of the given element.
	 * <p>
	 * This class can always cheaply determine of <code>e</code> is not a member
	 * of this heap (in <code>O(1)</code> time, thanks to reference magic).
	 * 
	 * @param e the entry for which to decrease the key.
	 * @param k the new key.
	 * @throws IllegalArgumentException If <code>k</code> is larger than
	 *         <code>e</code>'s current key or <code>k</code> is not a member
	 *         of this heap.
	 * @throws ClassCastException If the new key is not mutually comparable with
	 *         other keys in the heap.
	 */
	public void decreaseKey( final Heap.Entry<K, V> e, final K k )
		throws IllegalArgumentException, ClassCastException
	{
		// Check and cast.
		if( this.holdsEntry( e ) == false )
		{
			throw new IllegalArgumentException();
		}

		// x from CLRS.
		FibonacciHeapEntry<K, V> x = (FibonacciHeapEntry<K, V>)e;

		// Check key... May throw class cast as well.
		if( this.compareKeys( k, x.getKey() ) > 0 )
		{
			throw new IllegalArgumentException();
		}

		// Store the new key value.
		x.setKey( k );

		// Restore the heap structure.
		this.decreaseKeyImpl( x );
	}


	/**
	 * Decrease key implementation. Basically, we restore the heap structure by
	 * cutting <code>x</code> from it's parent (if necessary) and making it's
	 * parent a child of <code>x</code>.
	 * 
	 * @param x the whose key has just been decreased and needs to be percolated
	 *        toward the top of the heap.
	 */
	private void decreaseKeyImpl( final FibonacciHeapEntry<K, V> x )
	{
		// Get x's parent.
		FibonacciHeapEntry<K, V> y = x.parent;

		// If x has a lower key than it's parent (and assuming x was not already in
		// the root list)
		// then we have work to do.
		if( y != null && compare( x, y ) < 0 )
		{
			this.cut( x, y );
			this.cascadingCut( y );
		}

		// See if the new node is smaller.
		if( compare( x, this.minimum ) < 0 )
		{
			this.minimum = x;
		}

		this.mod_count += 1;
	}


	/**
	 * Does this heap hold the specified entry?
	 * 
	 * @param e entry to check.
	 * @throws NullPointerException If <code>e</code> is <code>null</code>.
	 * @return <code>true</code> if this heap holds <code>e</code>;
	 *         <code>false</code> otherwise.
	 */
	public boolean holdsEntry( final Heap.Entry<K, V> e )
		throws NullPointerException
	{
		if( e == null )
		{
			throw new NullPointerException();
		}

		// Obvious check.
		if( e.getClass().equals( FibonacciHeapEntry.class ) == false )
		{
			return ( false );
		}

		// Narrow.
		FibonacciHeapEntry<K, V> entry = (FibonacciHeapEntry<K, V>)e;

		// Use reference trickery.
		if( entry.isContainedBy( this ) == false )
		{
			return ( false );
		}

		// Yup.
		return ( true );
	}


	/**
	 * Remove <code>x</code> from the child list of <code>y</code> and add
	 * <code>x</code> to the root list.
	 * 
	 * @param x the node to cut.
	 * @param y the node from which to cut <code>x</code>.
	 */
	private void cut( final FibonacciHeapEntry<K, V> x,
			final FibonacciHeapEntry<K, V> y )
	{
		if( x.right == x )
		{
			// Last child.
			y.child = null;
		}
		else
		{
			// Next yutz over.
			y.child = x.right;
		}

		// Remove x from the child list.
		x.left.right = x.right;
		x.right.left = x.left;

		// y has one less child.
		y.degree -= 1;

		// Add x to the root list.
		this.minimum.right.left = x;
		x.right = this.minimum.right;
		this.minimum.right = x;
		x.left = this.minimum;
		x.parent = null;

		// Unmark x, since it has just been cut.
		x.marked = false;
	}


	/**
	 * Perform a cascading cut across <code>y</code>, by cutting <code>y</code>
	 * from it's parent and then performing <code>cascadingCut()</code> on
	 * <code>y</code>'s parent.
	 * 
	 * @param y the node on which to perform a cascading cut.
	 */
	private void cascadingCut( final FibonacciHeapEntry<K, V> y )
	{
		FibonacciHeapEntry<K, V> z = y.parent;

		if( z != null )
		{
			if( y.marked == false )
			{
				// Simply mark y.
				y.marked = true;
			}
			else
			{
				// Otherwise, cut y and recursively cascade on z.
				this.cut( y, z );
				this.cascadingCut( z );
			}
		}
	}


	/**
	 * Union with another heap.
	 * <p>
	 * This operation takes <code>O(1)</code> time.
	 * 
	 * @param other the other heap.
	 * @throws NullPointerException If <code>other</code> is <code>null</code>.
	 * @throws ClassCastException If the keys of the nodes are not mutally
	 *         comparable.
	 * @throws IllegalArgumentException If you attempt to union a heap with
	 *         itself.
	 */
	@SuppressWarnings( "unchecked" )
	public void union( final Heap<K, V> other )
		throws ClassCastException, NullPointerException, IllegalArgumentException
	{
		if( other == null )
		{
			throw new NullPointerException();
		}

		if( this == other )
		{
			throw new IllegalArgumentException();
		}

		if( other.isEmpty() )
		{
			return;
		}

		if( other.getClass().equals( FibonacciHeap.class ) )
		{
			// Get other root.
			FibonacciHeap<K, V> that = (FibonacciHeap<K, V>)other;

			try
			{
				int cmp = 0;
				if( this.minimum != null && that.minimum != null )
				{
					// May throw class cast.
					cmp = compare( that.minimum, this.minimum );
				}

				// Cat root list of other heap together with this one's.
				this.minimum.left.right = that.minimum.right;
				that.minimum.right.left = this.minimum.left;
				this.minimum.left = that.minimum;
				that.minimum.right = this.minimum;

				if( cmp < 0 )
				{
					// Point to new min.
					this.minimum = that.minimum;
				}

				// Update stuff.
				this.size += that.size;
				this.mod_count += 1;

				// Change that heap's heap reference to point to this heap.
				// Thus, all child of that become children of this.
				that.source_heap.setHeap( this );
				that.source_heap = new HeapReference( that );
			}
			finally
			{
				// Actually clear the other heap. Always done!
				that.clear();
			}
		}
		else
		{
			throw new ClassCastException();
		}
	}


	/**
	 * Clear this heap.
	 * <p>
	 * In addition to actually clearing this heap via <code>clearImpl()</code>,
	 * this method clears the source heap ref, thus "orphaning" all contained
	 * nodes in <code>O(1)</code> time.
	 */
	public void clear()
	{
		// Clear lame fields.
		this.minimum = null;
		this.size = 0;
		this.mod_count += 1;

		// Clear the heap ref that all the existing nodes have been using.
		// All contained nodes now have null containing heap.
		this.source_heap.clearHeap();

		// Recreate the reference object.
		this.source_heap = new HeapReference( this );
	}


	/**
	 * Get an iterator over this heap entry set.
	 * 
	 * @return an iterator.
	 */
	public Iterator<Heap.Entry<K, V>> iterator()
	{
		return ( new EntryIterator() );
	}


	/**
	 * Serialize the object to the specified output stream.
	 * <p>
	 * This method takes time <code>O(n)</code> where <code>n</code> is the
	 * size this heap.
	 * 
	 * @param out the stream to which to serialize this object.
	 * @throws IOException If this object cannot be serialized.
	 */
	private void writeObject( final ObjectOutputStream out )
		throws IOException
	{
		// write comparator and size.
		out.writeObject( this.comp );
		out.writeInt( this.size );

		// Write out all key/value pairs.
		Iterator<Heap.Entry<K, V>> it = new EntryIterator();
		Heap.Entry<K, V> et = null;
		while( it.hasNext() )
		{
			try
			{
				et = it.next();

				// May result in NotSerializableExceptions, but we there's not a whole
				// helluva lot we can do about that.
				out.writeObject( et.getKey() );
				out.writeObject( et.getValue() );
			}
			catch( final ConcurrentModificationException cme )
			{
				// User's fault.
				throw (IOException)new IOException( "Heap structure changed during serialization" ).initCause( cme );
			}
		}
	}


	/**
	 * Deserialize the restore this object from the specified stream.
	 * <p>
	 * This method takes time <code>O(n)</code> where <code>n</code> is the
	 * size this heap.
	 * 
	 * @param in the stream from which to read data.
	 * @throws IOException If this object cannot properly read from the specified
	 *         stream.
	 * @throws ClassNotFoundException If deserialization tries to classload an
	 *         undefined class.
	 */
	@SuppressWarnings( "unchecked" )
	private void readObject( final ObjectInputStream in )
		throws IOException, ClassNotFoundException
	{
		// get comparator and size.
		this.comp = (Comparator<? super K>)in.readObject();		
		int rsize = in.readInt();

		// Create new ref object.
		this.source_heap = new HeapReference( this );

		// Read and insert all the keys and values.
		K key;
		V value;
		for( int index = 0; index < rsize; index++ )
		{
			key = (K)in.readObject();
			value = (V)in.readObject();
			this.insert( key, value );
		}
	}


	/**
	 * Entry iterator class.
	 * <p>
	 * This iterator does not support the <code>remove()</code> operation. Any
	 * call to <code>remove()</code> will fail with a
	 * <code>UnsupportedOperationException</code>.
	 * 
	 * @author Fran Lattanzio
	 * @version $Revision: 1.6.4.12 $ $Date: 2008/11/22 22:32:19 $
	 */
	private class EntryIterator
		extends Object
		implements Iterator<Heap.Entry<K, V>>
	{


		/**
		 * The next entry.
		 */
		private FibonacciHeapEntry<K, V> next;

		/**
		 * The mod count.
		 */
		private int my_mod_count;


		/**
		 * Constructor.
		 */
		EntryIterator()
		{
			super();

			// Start at min.
			this.next = FibonacciHeap.this.minimum;

			// Copy mod count.
			this.my_mod_count = FibonacciHeap.this.mod_count;
		}


		/**
		 * Does this iterator have another object?
		 * 
		 * @return <code>true</code> if there's another object; <code>false</code>
		 *         otherwise.
		 * @throws ConcurrentModificationException If concurrent modification
		 *         occurs.
		 */
		public boolean hasNext()
		{
			if( this.my_mod_count != FibonacciHeap.this.mod_count )
			{
				throw new ConcurrentModificationException();
			}

			return ( this.next != null );
		}


		/**
		 * Get the next object from this iterator.
		 * 
		 * @return the next object.
		 * @throws NoSuchElementException If the iterator has no more elements.
		 * @throws ConcurrentModificationException If concurrent modification
		 *         occurs.
		 */
		public Heap.Entry<K, V> next()
			throws NoSuchElementException, ConcurrentModificationException
		{
			if( this.hasNext() == false )
			{
				throw new NoSuchElementException();
			}

			// Get the next node.
			FibonacciHeapEntry<K, V> n = this.next;
			this.next = this.getSuccessor( this.next );
			return ( n );
		}


		/**
		 * Not supported.
		 * 
		 * @throws UnsupportedOperationException Always.
		 */
		public void remove()
			throws UnsupportedOperationException
		{
			throw new UnsupportedOperationException();
		}


		/**
		 * Return the successor entry to the specified entry.
		 * 
		 * @param entry the given entry.
		 * @return the successor entry.
		 */
		private FibonacciHeapEntry<K, V> getSuccessor(
				FibonacciHeapEntry<K, V> entry )
		{
			if( entry.child != null )
			{
				return ( entry.child );
			}

			// The first entry.
			FibonacciHeapEntry<K, V> first;

			do
			{
				first = ( entry.parent == null ) ? FibonacciHeap.this.minimum
						: entry.parent.child;

				// Look for siblings.
				if( entry.right != first )
				{
					return ( entry.right );
				}

				// Look at entry parent.
				entry = entry.parent;
			}
			while( entry != null );

			// Reached the root node, no more sucessors.
			return ( null );
		}


	}


	/**
	 * Fibonacci heap entry.
	 * 
	 * @param <K> the key type.
	 * @param <V> the value type.
	 * @author Fran Lattanzio
	 * @version $Revision: 1.6.4.12 $ $Date: 2008/11/22 22:32:19 $
	 */
	private static final class FibonacciHeapEntry<K, V>
		extends AbstractLinkedHeap.AbstractLinkedHeapEntry<K, V>
		implements Heap.Entry<K, V>, Serializable
	{


		/**
		 * Serial version.
		 */
		private static final long serialVersionUID = 2348L;


		/**
		 * Is this node marked?
		 */
		transient boolean marked;

		/**
		 * The node degree - number of children.
		 */
		transient int degree;

		/**
		 * Parent node.
		 */
		transient FibonacciHeapEntry<K, V> parent;

		/**
		 * Child node.
		 */
		transient FibonacciHeapEntry<K, V> child;

		/**
		 * Left sibling node.
		 */
		transient FibonacciHeapEntry<K, V> left;

		/**
		 * Right sibling node.
		 */
		transient FibonacciHeapEntry<K, V> right;


		/**
		 * Constructor.
		 * 
		 * @param key the key.
		 * @param value the value.
		 * @param source_ref a wrapped weak reference to the creating heap.
		 */
		FibonacciHeapEntry( final K key, final V value,
																final HeapReference source_ref )
		{
			super( key, value, source_ref );
		}


	}


}
