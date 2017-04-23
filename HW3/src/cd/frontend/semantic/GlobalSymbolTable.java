package cd.frontend.semantic;

import cd.frontend.semantic.SymbolTable.Scope;
import cd.ir.Symbol.VariableSymbol.Kind;

public class GlobalSymbolTable extends SymbolTable {

	public GlobalSymbolTable(String name) {
		super(name);
	}
	
	@Override
	public Scope getScope() {
		return Scope.GLOBAL;
	}

}
