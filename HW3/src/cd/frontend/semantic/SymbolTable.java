package cd.frontend.semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cd.ir.Ast;
import cd.ir.Symbol;
import cd.ir.Symbol.ClassSymbol;
import cd.ir.Symbol.MethodSymbol;
import cd.ir.Symbol.VariableSymbol;
import cd.ir.Symbol.VariableSymbol.Kind;

/**
 * Keeps track of all symbols using key-value pairs. Keys are identifiers
 * (names) and values are instances of the class Symbol (attributes).
 * 
 * Use one symbol table per scope (global, class, method). Symbol table stores
 * separate Map for each name space (classes, methods, fields).
 * 
 * @author dirkhuttig
 *
 */
public class SymbolTable {

	// TODO nested symbol tables.

	// Optional
	protected String name;
	//protected Symbol sym;

	protected HashMap<String, ClassSymbol> classes;
	protected HashMap<String, MethodSymbol> methods;
	protected HashMap<String, VariableSymbol> fields;

	// Enclosing scope.
	protected SymbolTable parent;
	protected HashMap<String, SymbolTable> children;
	
	protected static enum Scope {GLOBAL, CLASS, METHOD};

	/**
	 * Constructor
	 * 
	 * @param name
	 */
	public SymbolTable(String name) {
		this.name = name;
		classes = new HashMap<String, ClassSymbol>();
		methods = new HashMap<String, MethodSymbol>();
		fields = new HashMap<String, VariableSymbol>();
		children = new HashMap<String, SymbolTable>();
		parent = null;
	}
	
	public String getName() {
		return name;
	}
	/*
	public Symbol getSymbol() {
		return sym;
	}
	public void setSymbol(Symbol symbol) {
		sym = symbol;
	}
	*/
	public HashMap<String, VariableSymbol> getFields() {
		return fields;
	}

	public HashMap<String, MethodSymbol> getMethods() {
		return methods;
	}

	public HashMap<String, ClassSymbol> getClasses() {
		return classes;
	}

	/**
	 * Set enclosing symbol table.
	 * 
	 * @param parent
	 */
	public void setParent(SymbolTable parent) {
		this.parent = parent;
	}

	/**
	 * Get enclosing symbol table.
	 * 
	 * @return
	 */
	public SymbolTable getParent() {
		return parent;
	}

	/**
	 * Add children.
	 * 
	 * @param child
	 */
	public void addChild(SymbolTable child) {
		//children.add(child);
		children.put(child.name, child);
	}

	/**
	 * Get children.
	 * 
	 * @return
	 */
	public HashMap<String, SymbolTable> getChildren() {
		return children;
	}

	/**
	 * Get child by name. 
	 * 
	 * @param name
	 * @return
	 */
	public SymbolTable getChild(String name) {
		return children.get(name);
	}
	
	/**
	 * Get child by SymbolTable.
	 * 
	 * @param child
	 * @return
	 */
	public SymbolTable getChild(SymbolTable child){
		return children.get(child.name);
	}
	
	/**
	 * Add class Symbol to classes.
	 * 
	 * @param key
	 * @param value
	 */
	public void putClass(String key, ClassSymbol value) {
		classes.put(key, value);
	}

	/**
	 * Get class Symbol by name.
	 * 
	 * @param key
	 * @return
	 */
	public ClassSymbol getClass(String key) {
		return classes.get(key);
	}

	/**
	 * Method declaration: bind key to value.
	 * 
	 * @param key
	 * @param value
	 */
	public void putMethod(String key, MethodSymbol value) {
		methods.put(key, value);
	}

	/**
	 * Method lookup: get value by key.
	 * 
	 * TODO Get method based on arguments.
	 * 
	 * @param key
	 * @return
	 */
	public MethodSymbol getMethod(String key) {

		return methods.get(key);
	}

	/**
	 * Add field Symbol to fields (declaration).
	 * 
	 * @param key
	 * @param value
	 */
	public void putField(String key, VariableSymbol value) {
		fields.put(key, value);
	}

	/**
	 * Get field Symbol by name (lookup).
	 * 
	 * TODO Get field based on type.
	 * 
	 * @param key
	 * @return
	 */
	public VariableSymbol getField(String key) {
		return fields.get(key);
	}
	
	public Scope getScope() {
		return Scope.GLOBAL;
	}
	
	/**
	 * remember current state of table.
	 */
	public void beginScope() {
		// TODO
	}

	/**
	 * restore table to state at most recent scope that has not been ended.
	 */
	public void endScope() {
		// TODO
	}

}
