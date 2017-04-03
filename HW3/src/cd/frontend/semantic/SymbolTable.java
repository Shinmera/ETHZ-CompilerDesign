package cd.frontend.semantic;

import java.util.HashMap;
import java.util.Map;

import cd.ir.Ast;
import cd.ir.Symbol;

/**
 * Keeps track of all symbols using key-value pairs. Keys are identifiers (names) and values are
 * instances of the class Symbol (attributes).
 * 
 * Use one symbol table per scope (global, class, method).
 * Symbol table stores separate Map for each name space (classes, methods, fields).
 * 
 * @author dirkhuttig
 *
 */
public class SymbolTable {

	// TODO nested symbol tables.
	
	// Optional
	private String name;
	
	private HashMap<String, Symbol> classes;
	private HashMap<String, Symbol> methods;
	private HashMap<String, Symbol> fields;
	
	private SymbolTable parent;

	/**
	 * Constructor 
	 * 
	 * @param name
	 */
	public SymbolTable(String name) {
		this.name = name;
		classes = new HashMap<String, Symbol>();
		methods = new HashMap<String, Symbol>();
		fields = new HashMap<String, Symbol>();
	}
	
	/**
	 * Add class Symbol to classes.
	 * 
	 * @param key
	 * @param value
	 */
	public void putClass(String key, Symbol value) {
		classes.put(key, value);
	}
	
	/**
	 * Get class Symbol by name.
	 * 
	 * @param key
	 * @return
	 */
	public Symbol getClass(String key) {
		return classes.get(key);
	}
		
	/**
	 * Method declaration: bind key to value.
	 * 
	 * @param key
	 * @param value
	 */
	public void putMethod(String key, Symbol value) {
		methods.put(key, value);
	}
	
	/**
	 * Method lookup: get value by key.
	 * 
	 * @param key
	 * @return
	 */
	public Symbol getMethod(String key) {
		
		return methods.get(key);
	}
	
	/**
	 * Add field Symbol to fields (declaration).
	 * 
	 * @param key
	 * @param value
	 */
	public void putField(String key, Symbol value) {
		fields.put(key, value);
	}
	
	/**
	 * Get field Symbol by name (lookup).
	 * 
	 * @param key
	 * @return
	 */
	public Symbol getField(String key) {
		return fields.get(key);
	}
	
	/**
	 * remember current state of table.
	 */
	public void beginScope() {
		
	}
	
	/**
	 * restore table to state at most recent scope that has not been ended.
	 */
	public void endScope() {
		
	}
	
}
