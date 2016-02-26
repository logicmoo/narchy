package nars.op.sys.prolog.fluents;

import nars.op.sys.prolog.io.IO;
import nars.op.sys.prolog.terms.PTerm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class implementes generic multiple tuples by key operations for use by
 * the PrologBlackBoard class implementing Linda operations on Prolog terms. It
 * uses the Queue class for keeping elements of type Term sharing the same key.
 * 
 * @see PrologBlackBoard
 * @see Queue
 * @see PTerm
 */
public class BlackBoard extends HashDict {

	/**
	 * creates a new BlackBoard
	 * 
	 * @see PTerm
	 */
	public BlackBoard() {
		super();
	}

	/**
	 * Removes the first Term having key k or the first enumerated key if k is
	 * null
	 */
	// synchronized
	private final PTerm pick(@Nullable String k) {
		if (k == null) {
			Iterator e = this.keySet().iterator();
			if (!e.hasNext())
				return null;
			k = (String) e.next();
			// IO.trace("$$Got key:"+k+this);
		}
		Queue Q = (Queue) get(k);
		if (Q == null)
			return null;
		PTerm T = (PTerm) Q.deq();
		if (Q.isEmpty()) {
			remove(k);
			// IO.trace("$$Removed key:"+k+this);
		}
		return T;
	}

	private final void addBack(String k, @NotNull ArrayList V) {
		for (int i = 0, vSize = V.size(); i < vSize; i++) {
			Object aV = V.get(i);
			// cannot be here if k==null
			add(k, (PTerm) aV);
		}
	}

	/**
	 * Removes the first matching Term or Clause from the blackboard, to be used
	 * by Linda in/1 operation in PrologBlackBoard
	 * 
	 * @see PrologBlackBoard#in()
	 */

	// synchronized
	@Nullable
	protected final PTerm take(String k, @NotNull PTerm pattern) {
		ArrayList V = new ArrayList();
		PTerm t;
		while (true) {
			t = pick(k);
			if (null == t)
				break;
			// IO.trace("$$After pick: t="+t+this);
			if (t.matches(pattern))
				break;
			else
				V.add(t);
		}
		addBack(k, V);
		return t;
	}

	/**
	 * Adds a Term or Clause to the the blackboard, to be used by Linda out/1
	 * operation
	 * 
	 * @see PrologBlackBoard
	 */
	// synchronized
	protected final void add(String k, PTerm value) {
		Queue Q = (Queue) get(k);
		if (Q == null) {
			Q = new Queue();
			put(k, Q);
		}
		if (!Q.enq(value))
			IO.error("Queue full, key:" + k);
		// IO.trace("$$Added key/val:"+k+"/"+value+"=>"+this);
	}

	/**
	 * This gives an enumeration view for the sequence of objects kept under key
	 * k.
	 */
	// synchronized
	public Iterator toEnumerationFor(String k) {
		Queue Q = (Queue) get(k);
		ArrayList V = (Q == null) ? new ArrayList() : Q.toVector();
		return V.iterator();
	}

	// synchronized
	@NotNull
	public Iterator toEnumeration() {
		return new BBoardEnumerator(this.keySet().iterator());
	}

}

/**
 * Generates an Iterator view of the blackboard
 * 
 * @see Iterator
 */

class BBoardEnumerator implements Iterator {
	BBoardEnumerator(Iterator EH) {
		EQ = null;
		this.EH = EH; // elements();
	}

	@Nullable
	private Iterator EQ;

	private final Iterator EH;

	// synchronized
	public boolean hasNext() {
		if ((EQ == null || !EQ.hasNext()) && EH.hasNext()) {
			EQ = ((Queue) EH.next()).toEnumeration();
		}
		return (EQ != null && EQ.hasNext());
	}

	// synchronized
	public Object next() {
		if (hasNext())
			return EQ.next();
		throw new NoSuchElementException("BBoardEnumerator");
	}

	public void remove() {
	}
}
