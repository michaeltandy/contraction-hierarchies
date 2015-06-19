/*
 * $Id: AbstractLinkedHeap.java,v 1.3.4.9 2008/11/22 22:32:19 fran Exp $
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

import java.lang.ref.WeakReference;


/**
 * Abstract linked heap. This class provides extra functionality for
 * linked-based heap implementations, i.e. where the heap structure is
 * maintained by having nodes point to parent/child/sibling nodes (c.f. a
 * typical binary heap implementation that is backed by an array).
 * <p>
 * The main things here are:
 * <ol>
 * <li>The node class contains a weak back-reference to the containing heap.
 * This solves the <code>holdsEntry</code> problem - basically, examining the
 * value of this reference will tell you the holding heap. I used a weak
 * reference here so that the holding help can still be garbage collected even
 * if some of the entries are still strongly reachable. This is a semi-minor
 * hack in the grand scheme of things: Dijkstra wouldn't like this, but it gets
 * the job done fairly cheaply. </li>
 * <li>The node class (and compare method) can deal with "infinite" values:
 * Setting the <code>is_infinite</code> flag on the node automatically makes
 * it smaller than any (finite) node. Generally, there should not be more than
 * one infinite node in any heap. If there are two, it's a good indication of a
 * serious programming error or concurrent modification. This is also a
 * semi-minor hack, I think; could be better, but we've all seen much worse.
 * </li>
 * </ol>
 * 
 * @param <K> the key type.
 * @param <V> the value type.
 * @author Fran Lattanzio
 * @version $Revision: 1.3.4.9 $ $Date: 2008/11/22 22:32:19 $
 */
public abstract class AbstractLinkedHeap<K, V>
	extends AbstractHeap<K, V>
	implements Heap<K, V>, Iterable<Heap.Entry<K, V>>
{


	/**
	 * Constructor.
	 * <p>
	 * Should be considered <code>private protected</code>. This constructor
	 * does nothing and is here only for access protection.
	 */
	protected AbstractLinkedHeap()
	{
		super();
	}


	/**
	 * Check for infinite flag.
	 * 
	 * @param node1 the first node.
	 * @param node2 the second node.
	 * @return integer just like {@link java.lang.Comparable#compareTo(Object)}.
	 * @throws ClassCastException If the keys of the nodes are not mutally
	 *         comparable.
	 * @throws NullPointerException If <code>node1</code> or <code>node2</code>
	 *         are <code>null</code>. This probably shouldn't happen.
	 */
	@Override
	protected int compare( final Entry<K, V> node1, final Entry<K, V> node2 )
		throws ClassCastException, NullPointerException
	{
		AbstractLinkedHeapEntry<K, V> e1 = (AbstractLinkedHeapEntry<K, V>)node1;
		AbstractLinkedHeapEntry<K, V> e2 = (AbstractLinkedHeapEntry<K, V>)node2;

		if( e1.is_infinite && e2.is_infinite )
		{
			// Probably shouldn't happen. A good indication of concurrent
			// modification... figure out if we should just toss an exception
			// here.
			return ( 0 );
		}
		else if( e1.is_infinite )
		{
			return ( -1 );
		}
		else if( e2.is_infinite )
		{
			return ( 1 );
		}
		else
		{
			return ( super.compare( node1, node2 ) );
		}
	}


	/**
	 * Abstract linked heap entry.
	 * 
	 * @param <K> the key type.
	 * @param <V> the value type.
	 * @author Fran Lattanzio
	 * @version $Revision: 1.3.4.9 $ $Date: 2008/11/22 22:32:19 $
	 */
	protected static abstract class AbstractLinkedHeapEntry<K, V>
		extends AbstractHeap.AbstractHeapEntry<K, V>
	{


		/**
		 * Infinite flag. Hack used for delete.
		 */
		protected transient volatile boolean is_infinite;

		/**
		 * Containing heap reference.
		 */
		private transient volatile HeapReference containing_ref;


		/**
		 * Constructor.
		 * <p>
		 * Should be considered <code>private protected</code>.
		 * 
		 * @param key the key.
		 * @param value the value.
		 * @param ref the heap reference.
		 */
		protected AbstractLinkedHeapEntry( final K key, final V value,
																				final HeapReference ref )
		{
			super( key, value );

			// Store ref.
			this.containing_ref = ref;
			this.is_infinite = false;
		}


		/**
		 * Is this node contained by the specified heap?
		 * 
		 * @param heap the heap for which to test membership.
		 * @return boolean true if this node is contained by the specified heap.
		 * @throws NullPointerException If <code>heap</code> is <code>null</code>.
		 *         Probably shouldn't happen.
		 */
		protected final boolean isContainedBy( final AbstractLinkedHeap<K, V> heap )
			throws NullPointerException
		{
			if( heap == null )
			{
				throw new NullPointerException();
			}

			if( this.containing_ref == null )
			{
				// Means that this node was orphaned from it's parent heap
				// via a clear or garbage collect.
				return ( false );
			}

			// Straight reference comparison.
			return ( this.containing_ref.getHeap() == heap );
		}


		/**
		 * Clear this object reference to the source heap.
		 */
		protected final void clearSourceReference()
		{
			this.containing_ref = null;
		}


	}


	/**
	 * Heap weak reference container.
	 * <p>
	 * The point of this class is to make sure that nodes are not deleted or have
	 * their key's decrease in the context of a heap of which they are not a
	 * member.
	 * <p>
	 * We use weak reference here to help the garbage collector. It also means
	 * that if an entry's containing heap is garbage collected, the node is
	 * considered "orphaned" and no longer a member of the heap.
	 * 
	 * @author Fran Lattanzio
	 * @version $Revision: 1.3.4.9 $ $Date: 2008/11/22 22:32:19 $
	 */
	@SuppressWarnings( "unchecked" )
	protected static final class HeapReference
		extends Object
	{


		/**
		 * A weak reference to a heap.
		 */
		private WeakReference<AbstractLinkedHeap> heap_ref;


		/**
		 * Constructor.
		 * 
		 * @param fh the heap to which this object's WeakReference should point.
		 */
		protected HeapReference( final AbstractLinkedHeap fh )
		{
			super();

			// Create stuff.
			this.heap_ref = new WeakReference<AbstractLinkedHeap>( fh );
		}


		/**
		 * Get the heap reference contained by this object.
		 * 
		 * @return FibonacciHeap the contained heap.
		 * @see #setHeap(AbstractLinkedHeap)
		 */
		protected final AbstractLinkedHeap getHeap()
		{
			return ( this.heap_ref.get() );
		}


		/**
		 * Set the heap reference contained by this object.
		 * 
		 * @param heap the new heap.
		 * @throws NullPointerException If <code>heap</code> is <code>null</code>.
		 * @see #getHeap()
		 */
		protected final void setHeap( final AbstractLinkedHeap heap )
			throws NullPointerException
		{
			if( heap == null )
			{
				throw new NullPointerException();
			}

			// Clear ref.
			this.clearHeap();

			// Create new reference object.
			this.heap_ref = new WeakReference<AbstractLinkedHeap>( heap );
		}


		/**
		 * Clear the reference.
		 */
		protected final void clearHeap()
		{
			this.heap_ref.clear();
		}


	}


}
