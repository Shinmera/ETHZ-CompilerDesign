package cd.frontend.semantic;

import cd.frontend.semantic.SymbolTable.Scope;

public class ClassSymbolTable extends SymbolTable{

	public ClassSymbolTable(String name) {
		super(name);
	}
	
	@Override
	public Scope getScope() {
		return Scope.CLASS;
	}
	
}
