package cd.frontend.semantic;

import java.util.ArrayList;
import java.util.List;

import cd.ir.Ast;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.VarDecl;
import cd.ir.AstVisitor;
import cd.ir.Symbol;
import cd.ir.Symbol.ClassSymbol;

/**
 * Visits all nodes and add/lookup Symbols to SymbolTable.
 * 
 * @author dirkhuttig
 */
public class SemanticAstVisitor extends AstVisitor<SymbolTable, Void> {

	GlobalSymbolTable globalSymbols;
	/**
	 * Visit class declaration of AST.
	 */
	public SymbolTable classDecl(ClassDecl ast, Void arg) {

		// Add ClassSymbol.
		ast.sym = new ClassSymbol(ast.name);
		
		// Check if symbol exists
		Symbol symbol = globalSymbols.getClass(ast.name);
		if (symbol == null){
			 symbol = ast.sym;
			 globalSymbols.putClass(ast.name, symbol);
		} else {
			// error: symbol exists.
		}
				
		// Visit children
		// TODO 
		List<MethodDecl> methodDecls = ast.methods();
		
		// TODO
		List<VarDecl> fields = ast.fields();
		
		return null;
	}

	/**
	 * Visit class declaration of AST.
	 * 
	 * @param ast
	 * @param arg
	 */
	public void methodDecl(Ast ast, Void arg) {

	}
	
	// ...

}
