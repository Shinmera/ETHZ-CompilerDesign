package cd.frontend.semantic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cd.Main;
import cd.frontend.semantic.SemanticFailure.Cause;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.VarDecl;
import cd.ir.AstVisitor;
import cd.ir.Symbol;
import cd.ir.Symbol.ClassSymbol;
import cd.ir.Symbol.MethodSymbol;
import cd.ir.Symbol.PrimitiveTypeSymbol;
import cd.ir.Symbol.TypeSymbol;
import cd.ir.Symbol.VariableSymbol;
import cd.ir.Symbol.VariableSymbol.Kind;

/**
 * A helper class for {@link SemanticAnalyzer} which iterates through
 * a class definition and creates symbols for all of its fields and
 * methods.  For each method, we also create symbols for parameters
 * and local variables.
 */
public class SymbolCreator extends Object {
	
	final Main main;
	final SymTable<TypeSymbol> typesTable;
	
	public SymbolCreator(Main main, SymTable<TypeSymbol> typesTable) {
		this.main = main;
		this.typesTable = typesTable;
	}

	public void createSymbols(ClassDecl cd) {
		// lookup the super class.  the grammar guarantees that this
		// will refer to a class, if the lookup succeeds.		
		cd.sym.superClass = (ClassSymbol) typesTable.getType(cd.superClass);
		new ClassSymbolCreator(cd.sym).visitChildren(cd, null); 
	}

	/**
	 * Useful method which adds a symbol to a map, checking to see 
	 * that there is not already an entry with the same name.
	 * If a symbol with the same name exists, throws an exception.
	 */
	public <S extends Symbol> void add(Map<String,S> map, S sym) {
		if (map.containsKey(sym.name))
			throw new SemanticFailure(
					Cause.DOUBLE_DECLARATION,
					"Symbol '%s' was declared twice in the same scope", 
					sym.name);
		map.put(sym.name, sym);
	}
	
	/**
	 * Creates symbols for all fields/constants/methods in a class.
	 * Uses {@link MethodSymbolCreator} to create symbols for all 
	 * parameters and local variables to each method as well.
	 * Checks for duplicate members.
	 */
	public class ClassSymbolCreator extends AstVisitor<Void, Void> {
		
		final ClassSymbol classSym; 
		
		public ClassSymbolCreator(ClassSymbol classSym) {
			this.classSym = classSym;
		}

		@Override
		public Void varDecl(VarDecl ast, Void arg) {
			ast.sym = new VariableSymbol(
					ast.name, typesTable.getType(ast.type), Kind.FIELD);
			add(classSym.fields, ast.sym);
			return null;
		}

		@Override
		public Void methodDecl(MethodDecl ast, Void arg) {
			
			ast.sym = new MethodSymbol(ast);
			
			add(classSym.methods, ast.sym);

			// create return type symbol
			if (ast.returnType.equals("void")) {
				ast.sym.returnType = PrimitiveTypeSymbol.voidType;
			} else {
				ast.sym.returnType = typesTable.getType(ast.returnType);
			}
			
			// create symbols for each parameter
			Set<String> pnames = new HashSet<String>();
			for (int i = 0; i < ast.argumentNames.size(); i++) {
				String argumentName = ast.argumentNames.get(i);
				String argumentType = ast.argumentTypes.get(i);
				if (pnames.contains(argumentName)) 
					throw new SemanticFailure(
							Cause.DOUBLE_DECLARATION,
							"Method '%s' has two parameters named '%s'",
							ast.sym, argumentName);
				pnames.add(argumentName);
				VariableSymbol vs = new VariableSymbol(
						argumentName, typesTable.getType(argumentType));
				ast.sym.parameters.add(vs);
			}
			
			// create symbols for the local variables
			new MethodSymbolCreator(ast.sym).visitChildren(ast.decls(), null);

			return null;
		}
		
	}

	public class MethodSymbolCreator extends AstVisitor<Void, Void> {
		
		final MethodSymbol methodSym;
		
		public MethodSymbolCreator(MethodSymbol methodSym) {
			this.methodSym = methodSym;
		}
		
		@Override
		public Void methodDecl(MethodDecl ast, Void arg) {
			throw new SemanticFailure(
					Cause.NESTED_METHODS_UNSUPPORTED,
					"Nested methods are not supported.");
		}
		
		@Override
		public Void varDecl(VarDecl ast, Void arg) {
			ast.sym = new VariableSymbol(
					ast.name, typesTable.getType(ast.type), Kind.LOCAL);
			add(methodSym.locals, ast.sym);
			return null;
		}

	}
}
