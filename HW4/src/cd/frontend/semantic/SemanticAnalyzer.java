package cd.frontend.semantic;

import java.util.ArrayList;
import java.util.List;

import cd.Main;
import cd.frontend.semantic.SemanticFailure.Cause;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.MethodDecl;
import cd.ir.Symbol;
import cd.ir.Symbol.ClassSymbol;
import cd.ir.Symbol.MethodSymbol;
import cd.ir.Symbol.PrimitiveTypeSymbol;
import cd.ir.Symbol.TypeSymbol;
import cd.ir.Symbol.VariableSymbol;

public class SemanticAnalyzer {
	
	public final Main main;
	
	public SemanticAnalyzer(Main main) {
		this.main = main;
	}
	
	public void check(List<ClassDecl> classDecls) 
	throws SemanticFailure {
		{
			SymTable<TypeSymbol> typeSymbols = createSymbols(classDecls);
			checkInheritance(classDecls);
			checkStartPoint(typeSymbols);
			checkMethodBodies(typeSymbols, classDecls);
		}
	}

	/**
	 * Creates a symbol table with symbols for all built-in types, 
	 * as well as all classes and their fields and methods.  Also
	 * creates a corresponding array symbol for every type 
	 * (named {@code type[]}).
	 * @see SymbolCreator
	 */
	private SymTable<TypeSymbol> createSymbols(List<ClassDecl> classDecls) {
		
		// Start by creating a symbol for all built-in types.
		SymTable<TypeSymbol> typeSymbols = new SymTable<TypeSymbol>(null);
		
		typeSymbols.add(PrimitiveTypeSymbol.intType);
		typeSymbols.add(PrimitiveTypeSymbol.booleanType);
		typeSymbols.add(PrimitiveTypeSymbol.voidType);
		typeSymbols.add(ClassSymbol.objectType);
		
		// Add symbols for all declared classes.
		for (ClassDecl ast : classDecls) {
			// Check for classes named Object
			if (ast.name.equals(ClassSymbol.objectType.name))
				throw new SemanticFailure(Cause.OBJECT_CLASS_DEFINED);
			ast.sym = new ClassSymbol(ast);
			typeSymbols.add(ast.sym); 
		}
		
		// Create symbols for arrays of each type.
		for (Symbol sym : new ArrayList<Symbol>(typeSymbols.localSymbols())) {
			Symbol.ArrayTypeSymbol array = 
				new Symbol.ArrayTypeSymbol((TypeSymbol) sym);
			typeSymbols.add(array);
		}
		
		// For each class, create symbols for each method and field
		SymbolCreator sc = new SymbolCreator(main, typeSymbols);
		for (ClassDecl ast : classDecls)
			sc.createSymbols(ast);
		
		return typeSymbols;
	}
	
	/**
	 * Check for errors related to inheritance: 
	 * circular inheritance, invalid super
	 * classes, methods with different types, etc.
	 * Note that this must be run early because other code assumes
	 * that the inheritance is correct, for type checking etc.
	 * @see InheritanceChecker
	 */
	private void checkInheritance(List<ClassDecl> classDecls) {
		for (ClassDecl cd : classDecls)
			new InheritanceChecker().visit(cd, null);
	}

	/**
	 * Guarantee there is a class Main which defines a method main
	 * with no arguments.
	 */
	private void checkStartPoint(SymTable<TypeSymbol> typeSymbols) {
		Symbol mainClass = typeSymbols.get("Main");
		if (mainClass != null && mainClass instanceof ClassSymbol) {
			ClassSymbol cs = (ClassSymbol) mainClass;
			MethodSymbol mainMethod = cs.getMethod("main");
			if (mainMethod != null && mainMethod.parameters.size() == 0 &&
					mainMethod.returnType == PrimitiveTypeSymbol.voidType) {
				return; // found the main() method!
			}
		}
		throw new SemanticFailure(Cause.INVALID_START_POINT, "No Main class found");
	}
	
	/**
	 * Check the bodies of methods for errors, particularly type errors
	 * but also undefined identifiers and the like.
	 * @see TypeChecker
	 */
	private void checkMethodBodies(
			SymTable<TypeSymbol> typeSymbols,
			List<ClassDecl> classDecls) 
	{
		TypeChecker tc = new TypeChecker(typeSymbols);
		
		for (ClassDecl classd : classDecls) {
			
			SymTable<VariableSymbol> fldTable = new SymTable<VariableSymbol>(null);

			// add all fields of this class, or any of its super classes
			for (ClassSymbol p = classd.sym; p != null; p = p.superClass)
				for (VariableSymbol s : p.fields.values())
					if (!fldTable.contains(s.name))
						fldTable.add(s);
			
			// type check any method bodies and final locals
			for (MethodDecl md : classd.methods()) {
				
				boolean hasReturn = new ReturnCheckerVisitor().visit(md.body(), null);
				
				if (!md.returnType.equals("void") && !hasReturn) {
					
					throw new SemanticFailure(Cause.MISSING_RETURN, 
							"Method %s.%s is missing a return statement", 
							classd.name, 
							md.name);
					
				}
				
				SymTable<VariableSymbol> mthdTable = new SymTable<VariableSymbol>(fldTable);
				
				mthdTable.add(classd.sym.thisSymbol);
				
				for (VariableSymbol p : md.sym.parameters) {
					mthdTable.add(p);
				}
				
				for (VariableSymbol l : md.sym.locals.values()) {
					mthdTable.add(l);
				}
				
				tc.checkMethodDecl(md, mthdTable);
				
			}
		}
	}

}
