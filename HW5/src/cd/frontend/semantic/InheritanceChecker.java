package cd.frontend.semantic;

import java.util.HashSet;
import java.util.Set;

import cd.frontend.semantic.SemanticFailure.Cause;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.MethodDecl;
import cd.ir.AstVisitor;
import cd.ir.Symbol.ClassSymbol;
import cd.ir.Symbol.MethodSymbol;
import cd.ir.Symbol.VariableSymbol;
import cd.util.Pair;

public class InheritanceChecker extends AstVisitor<Void, Void> {
	
	ClassSymbol classSym;

	@Override
	public Void classDecl(ClassDecl ast, Void arg) {
		classSym = ast.sym;
		
		// check for cycles in the inheritance hierarchy:
		Set<ClassSymbol> supers = new HashSet<ClassSymbol>();
		ClassSymbol sc = classSym.superClass;
		supers.add(classSym);
		while (sc != null) {
			if (supers.contains(sc))
				throw new SemanticFailure(
						Cause.CIRCULAR_INHERITANCE,
						"Class %s has %s as a superclass twice", 
						ast.name, sc.name);
			supers.add(sc);
			sc = sc.superClass;
		}
		
		this.visitChildren(ast, null);
		
		return null;
	}

	@Override
	public Void methodDecl(MethodDecl ast, Void arg) {
		
		// check that methods overridden from a parent class agree
		// on number/type of parameters
		MethodSymbol sym = ast.sym;
		MethodSymbol superSym = classSym.superClass.getMethod(ast.name);
		sym.overrides = superSym;	
		if (superSym != null) {
			if (superSym.parameters.size() != sym.parameters.size())
				throw new SemanticFailure(
						Cause.INVALID_OVERRIDE,
						"Overridden method %s has %d parameters, " +
						"but original has %d",
						ast.name, sym.parameters.size(),
						superSym.parameters.size());
			for (Pair<VariableSymbol> pair : Pair.zip(sym.parameters, superSym.parameters))
				if (pair.a.type != pair.b.type)
					throw new SemanticFailure(
							Cause.INVALID_OVERRIDE,
							"Method parameter %s has type %s, but " +
							"corresponding base class parameter %s has type %s",
							pair.a.name, pair.a.type, pair.b.name, pair.b.type);
			if (superSym.returnType != sym.returnType)
				throw new SemanticFailure(
						Cause.INVALID_OVERRIDE,
						"Overridden method %s has return type %s," +
						"but its superclass has %s",
						ast.name, sym.returnType, superSym.returnType);
		}
		
		return null;
	}		

}
