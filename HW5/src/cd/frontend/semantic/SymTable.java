package cd.frontend.semantic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cd.frontend.semantic.SemanticFailure.Cause;
import cd.ir.Symbol;

/** 
 * A simple symbol table, with a pointer to the enclosing scope. 
 * Used by {@link TypeChecker} to store the various scopes for
 * local, parameter, and field lookup. */
public class SymTable<S extends Symbol> {
	
	private final Map<String, S> map = new HashMap<String, S>();
	
	private final SymTable<S> parent;
	
	public SymTable(SymTable<S> parent) {
		this.parent = parent;
	}
	
	public void add(S sym) {
		// check that the symbol is not already declared *at this level*
		if (containsLocally(sym.name))
			throw new SemanticFailure(Cause.DOUBLE_DECLARATION);
		map.put(sym.name, sym);
	}

	public List<S> allSymbols() {
		List<S> result = new ArrayList<S>();
		SymTable<S> st = this;
		while (st != null) {
			for (S sym : st.map.values())
				result.add(sym);
			st = st.parent;
		}
		return result;
	}

	public Collection<S> localSymbols() {
		return this.map.values();
	}
	
	/**
	 * True if there is a declaration with the given name at any
	 * level in the symbol table
	 */
	public boolean contains(String name) {
		return get(name) != null;
	}

	/** 
	 * True if there is a declaration at THIS level in the symbol
	 * table; may return {@code false} even if a declaration exists
	 * in some enclosing scope
	 */
	public boolean containsLocally(String name) {
		return this.map.containsKey(name);
	}

	/** 
	 * Base method: returns {@code null} if no symbol by that
	 * name can be found, in this table or in its parents */
	public S get(String name) {
		S res = map.get(name);
		if (res != null)
			return res;
		if (parent == null)
			return null;
		return parent.get(name);
	}
	
	/**
	 * Finds the symbol with the given name, or fails with a 
	 * NO_SUCH_TYPE error.  Only really makes sense to use this
	 * if S == TypeSymbol, but we don't strictly forbid it... */
	public S getType(String name) {
		S res = get(name);
		if (res == null)
			throw new SemanticFailure(
					Cause.NO_SUCH_TYPE,
					"No type '%s' was found", name);
		return res;
	}
}