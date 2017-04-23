package cd.frontend.semantic;

import cd.frontend.semantic.SymbolTable.Scope;

public class MethodSymbolTable extends SymbolTable {

	public MethodSymbolTable(String name) {
		super(name);
	}
	
	@Override
	public Scope getScope() {
		return Scope.METHOD;
	}
}
