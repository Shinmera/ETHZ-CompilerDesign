package cd.frontend.semantic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cd.Main;
import cd.ToDoException;
import cd.frontend.semantic.SemanticFailure.Cause;
import cd.frontend.semantic.SymbolTable.Scope;
import cd.ir.Ast.ClassDecl;
import cd.ir.Symbol;
import cd.ir.Symbol.ClassSymbol;
import cd.ir.Symbol.MethodSymbol;

public class SemanticAnalyzer {

	public final Main main;
	SemanticPopulatorAstVisitor populator;
	SemanticAstVisitor visitor;
	SemanticCheckAstVisitor checkVisitor;

	public SemanticAnalyzer(Main main) {
		this.main = main;
	}

	/**
	 * 
	 * 
	 * @param classDecls
	 * @throws SemanticFailure
	 */
	public void check(List<ClassDecl> classDecls) throws SemanticFailure {
		{
			/*
			 * We make three runs on the AST using three different visitors
			 * SemanticPopulatorAstVisitor, SemanticAstVisitor and
			 * SemanticCheckAstVisitor.
			 */

			/*
			 * First run : SemanticPopulatorAstVisitor
			 * 
			 * Collects type information: Traverses AST to create SymbolTables
			 * and add Symbols to the nodes and the SymbolTables.
			 * 
			 * For each class, type, variable etc. create new Symbol and add to
			 * corresponding symbol table.
			 * 
			 * In this run we can't know the type of variable expressions yet
			 * since the declared variables are no known at runtime yet.
			 */
			populator = new SemanticPopulatorAstVisitor();
			for (ClassDecl c : classDecls) {
				c.accept(populator, null);
			}

			/*
			 * Second run : SemanticAstVisitor
			 * 
			 * Since all the existing fields and classes and their types are
			 * saved in the corresponding Symbols and SymbolTables we can now
			 * collect type information for variable expressions.
			 * 
			 * We can also make some typechecks while traversing upwards.
			 */
			GlobalSymbolTable populatedTable = populator.getSymbolTable();
			visitor = new SemanticAstVisitor(populatedTable);

			for (ClassDecl c : classDecls) {
				c.accept(visitor, null);
			}

			/*
			 * Global checks before the third run.
			 * 
			 * - Is Object class defined? - Does Main class exist? - Does main
			 * method exist? Does it have return type void? - Do all extended
			 * classes exist?
			 */
			GlobalSymbolTable table = visitor.getSymbolTable();

			// Add Symbol for Object class
			ClassSymbol object = ClassSymbol.objectType;
			object.superClass = ClassSymbol.objectType;
			table.putClass("Object", object);

			// Check if class Main exists.
			Symbol main = table.getClass("Main");
			if (main == null) {
				throw new SemanticFailure(Cause.INVALID_START_POINT);
			}

			// Check if method main exists.
			ClassSymbolTable mainClassTable = (ClassSymbolTable) table.getChild("Main");
			MethodSymbol mainMethod = (MethodSymbol) mainClassTable.getMethod("main");
			if (mainMethod == null) {
				throw new SemanticFailure(Cause.INVALID_START_POINT);
			}
			// Check if main has return type void.
			if (!mainMethod.returnType.name.equals("void")) {
				throw new SemanticFailure(Cause.INVALID_START_POINT);
			}

			// Check if all superclasses exist.
			for (Map.Entry<String, ClassSymbol> entry : table.getClasses().entrySet()) {

				ClassSymbol superclass = entry.getValue().superClass;

				if (table.getClass(superclass.name) == null) {
					throw new SemanticFailure(Cause.NO_SUCH_TYPE);
				}
			}

			/*
			 * Third run : SemanticCheckAstVisitor
			 * 
			 * The Ast and SymbolTables contain all necessary information. We
			 * can now do all the remaining typechecks.
			 */
			checkVisitor = new SemanticCheckAstVisitor(table);
			for (ClassDecl c : classDecls) {
				c.accept(checkVisitor, null);
			}

		}
	}

}
