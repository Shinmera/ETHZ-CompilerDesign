package cd.frontend.semantic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cd.frontend.semantic.SemanticFailure.Cause;
import cd.frontend.semantic.SymbolTable.Scope;
import cd.ir.Ast;
import cd.ir.AstVisitor;
import cd.ir.Ast.Assign;
import cd.ir.Ast.BinaryOp;
import cd.ir.Ast.BooleanConst;
import cd.ir.Ast.BuiltInRead;
import cd.ir.Ast.BuiltInWrite;
import cd.ir.Ast.Cast;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.Expr;
import cd.ir.Ast.Field;
import cd.ir.Ast.IfElse;
import cd.ir.Ast.Index;
import cd.ir.Ast.IntConst;
import cd.ir.Ast.MethodCall;
import cd.ir.Ast.MethodCallExpr;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.NewArray;
import cd.ir.Ast.NewObject;
import cd.ir.Ast.Nop;
import cd.ir.Ast.NullConst;
import cd.ir.Ast.ReturnStmt;
import cd.ir.Ast.Seq;
import cd.ir.Ast.UnaryOp;
import cd.ir.Ast.Var;
import cd.ir.Ast.VarDecl;
import cd.ir.Ast.WhileLoop;
import cd.ir.Ast.BinaryOp.BOp;
import cd.ir.Symbol.ArrayTypeSymbol;
import cd.ir.Symbol.ClassSymbol;
import cd.ir.Symbol.MethodSymbol;
import cd.ir.Symbol.PrimitiveTypeSymbol;
import cd.ir.Symbol.TypeSymbol;
import cd.ir.Symbol.VariableSymbol;
import cd.ir.Symbol.VariableSymbol.Kind;

/**
 * This Visitor is used for the second run to check the Ast with all Symbols and
 * information already added. All we need to do is to check for each symbol if
 * its type is valid.
 * 
 * @author dirkhuttig
 *
 */
public class SemanticCheckAstVisitor extends AstVisitor<TypeSymbol, SymbolTable> {

	private GlobalSymbolTable globalSymbols = new GlobalSymbolTable("global");

	public GlobalSymbolTable getSymbolTable() {
		return globalSymbols;
	}

	/**
	 * Constructor
	 * 
	 * @param table
	 */
	public SemanticCheckAstVisitor(GlobalSymbolTable table) {
		globalSymbols = table;
	}

	/**
	 * Visit class declaration of AST.
	 * 
	 * @param ast
	 * @param parent
	 */
	public TypeSymbol classDecl(ClassDecl ast, SymbolTable arg) {

		// Get ClassSymbol from SymbolTable
		ClassSymbol sym = globalSymbols.getClass(ast.name);

		// Get ClassSymbol
		ClassSymbol symbol = ast.sym;

		// Get SymbolTable
		ClassSymbolTable classSymbolTable = (ClassSymbolTable) globalSymbols.getChild(ast.name);

		// Visit children
		List<VarDecl> fields = ast.fields();
		for (VarDecl decl : fields) {
			decl.accept(this, classSymbolTable);
		}

		List<MethodDecl> methodDecls = ast.methods();
		for (MethodDecl decl : methodDecls) {
			decl.accept(this, classSymbolTable);
		}

		// Check for circular inheritance and overridden methods.
		ClassSymbol superclass = globalSymbols.getClass(symbol.superClass.name);

		ClassSymbolTable table;

		while (superclass != null && !superclass.name.equals("Object")) {

			// Check if method is overriding other method.
			for (Map.Entry<String, MethodSymbol> entry : classSymbolTable.getMethods().entrySet()) {

				MethodSymbol thisMethod = entry.getValue();
				MethodSymbol method = superclass.getMethod(entry.getValue().name);

				if (method != null) {

					// Check type, number of parameters, type of all parameters.
					if (!method.returnType.name.equals(thisMethod.returnType.name)
							|| method.parameters.size() != thisMethod.parameters.size()) {
						throw new SemanticFailure(Cause.INVALID_OVERRIDE);
					}
					for (int i = 0; i < method.parameters.size(); i++) {
						if (!method.parameters.get(i).type.name.equals(thisMethod.parameters.get(i).type.name)) {
							throw new SemanticFailure(Cause.INVALID_OVERRIDE);
						}
					}
				}
			}
			// Check for circular inheritance.
			if (superclass.name.equals(symbol.name)) {
				throw new SemanticFailure(Cause.CIRCULAR_INHERITANCE);
			}

			superclass = globalSymbols.getClass(superclass.name).superClass;
		}
		return null;
	}

	/**
	 * Visit method declaration of AST.
	 * 
	 * @param ast
	 * @param parent
	 *            : the enclosing scope (parent table).
	 */
	public TypeSymbol methodDecl(MethodDecl ast, SymbolTable parent) {

		// Create new MethodSymbol and add information
		MethodSymbol sym = ast.sym;

		// Get method symbol table.
		MethodSymbolTable methodSymbolTable = (MethodSymbolTable) parent.getChild(ast.name);

		/*
		 * Visit declarations
		 */
		Seq decls = ast.decls();
		decls.accept(this, methodSymbolTable);

		/*
		 * Visit body
		 */
		Seq body = ast.body();
		body.accept(this, methodSymbolTable);

		// Typecheck formal parameters
		List<VariableSymbol> parameters = sym.parameters;
		for (VariableSymbol var : parameters) {
			if (var.type.isClassType()) {

				// Check if declared
				if (!isClassDeclared(var.type.name)) {
					throw new SemanticFailure(Cause.NO_SUCH_TYPE);
				}
			}
		}

		List<String> names = ast.argumentNames;
		for (int i = 0; i < names.size(); i++) {

			// Check if parameters declared in body.
			if (sym.locals.get(names.get(i)) != null) {
				throw new SemanticFailure(Cause.DOUBLE_DECLARATION);
			}
		}

		// Check if return type exists.
		if (ast.sym.returnType.isClassType()) {
			if (!isClassDeclared(ast.sym.returnType.name)) {
				throw new SemanticFailure(Cause.NO_SUCH_TYPE);
			}
		}

		if (!ast.sym.returnType.name.equals("void")) {
			// Check if return statement exists.
			List<ReturnStmt> returnStmts = ast.body().childrenOfType(Ast.ReturnStmt.class);
			if (returnStmts.isEmpty()) {
				throw new SemanticFailure(Cause.MISSING_RETURN);
			}
		}
		return null;
	}

	/**
	 * Visit variable declaration of AST.
	 * 
	 * @param ast
	 * @param parent
	 */
	public TypeSymbol varDecl(VarDecl ast, SymbolTable parent) {

		VariableSymbol sym = ast.sym;

		// Check if class exists.
		if (sym.type.isClassType()) {
			if (!isClassDeclared(sym.type.name)) {
				throw new SemanticFailure(Cause.NO_SUCH_TYPE);
			}
		}

		return null;
	}

	/**
	 * Visit seq (delarations + body).
	 */
	public TypeSymbol seq(Seq ast, SymbolTable parent) {

		List<Ast> children = ast.rwChildren();
		for (Ast child : children) {
			child.accept(this, parent);
		}

		return null;
	}

	/*************
	 * Statements
	 *************/

	public TypeSymbol assign(Assign ast, SymbolTable parent) {

		// Visit left and right.
		Expr left = ast.left();
		Expr right = ast.right();
		left.accept(this, parent);
		right.accept(this, parent);

		// Only assignable: variables, fields, array-indexing.
		if (!(left instanceof Var) && !(left instanceof Field) && !(left instanceof Index)) {
			throw new SemanticFailure(Cause.NOT_ASSIGNABLE);
		}

		// Get the raw TypeSymbol if left is array type.
		TypeSymbol leftType = left.type;
		TypeSymbol rightType = right.type;
		if (leftType.isArrayType()) {
			leftType = ((ArrayTypeSymbol) leftType).getElementType();
			if (right.type.isArrayType()) {
				rightType = ((ArrayTypeSymbol) rightType).getElementType();
			}
		} else {
			if (right.type.isArrayType()) {
				throw new SemanticFailure(Cause.TYPE_ERROR);
			}
		}

		// Right must be subtype of left.
		if (!isSubTypeOf(rightType, leftType)) {
			throw new SemanticFailure(Cause.TYPE_ERROR);
		}
		return null;
	}

	public TypeSymbol builtInWrite(BuiltInWrite ast, SymbolTable parent) {

		Expr arg = ast.arg();
		arg.accept(this, parent);

		if (!arg.type.toString().equals("int")) {
			throw new SemanticFailure(Cause.TYPE_ERROR);
		}

		return null;
	}

	public TypeSymbol builtInWriteln(Ast.BuiltInWriteln ast, SymbolTable parent) {

		// No effect.

		return null;
	}

	public TypeSymbol ifElse(IfElse ast, SymbolTable parent) {

		Expr condition = ast.condition();
		condition.accept(this, parent);

		Ast then = ast.then();
		then.accept(this, parent);

		Ast otherwise = ast.otherwise();
		otherwise.accept(this, parent);

		return null;
	}

	public TypeSymbol returnStmt(ReturnStmt ast, SymbolTable parent) {

		Expr arg = ast.arg();
		arg.accept(this, parent);

		// Check if type of return statement is subtype of return type of method.
		MethodSymbol methodSymbol = parent.getParent().getMethod(parent.name);
		if (!isSubTypeOf(arg.type, methodSymbol.returnType)) {
			throw new SemanticFailure(Cause.TYPE_ERROR);
		}

		return null;
	}

	public TypeSymbol methodCall(MethodCall ast, SymbolTable parent) {

		MethodCallExpr methodCallExpr = ast.getMethodCallExpr();
		methodCallExpr.accept(this, parent);

		return null;
	}

	public TypeSymbol nop(Nop ast, SymbolTable parent) {

		// No effect.

		return null;
	}

	public TypeSymbol whileLoop(WhileLoop ast, SymbolTable parent) {

		Expr condition = ast.condition();
		condition.accept(this, parent);

		Ast body = ast.body();
		body.accept(this, parent);

		return null;
	}

	/*************
	 * Expressions
	 *************/

	public TypeSymbol binaryOp(BinaryOp ast, SymbolTable parent) {

		Expr left = ast.left();
		left.accept(this, parent);

		Expr right = ast.right();
		right.accept(this, parent);

		BOp op = ast.operator;

		// Check if left and right are of the same type.
		if (!isSubTypeOf(left.type, right.type)) {
			throw new SemanticFailure(Cause.TYPE_ERROR);
		}

		// * / % + -
		if (op.isIntToIntOperation()) {
			if (!(left.type.toString().equals("int") && right.type.toString().equals("int"))) {
				throw new SemanticFailure(Cause.TYPE_ERROR);
			}
		}

		// && ||
		if (op.isBooleanOperation()) {
			if (!(left.type.toString().equals("boolean") && right.type.toString().equals("boolean"))) {
				throw new SemanticFailure(Cause.TYPE_ERROR);
			}
		}

		// < <= >= >
		if (op.isIntToBooleanOperation()) {
			if (!(left.type.toString().equals("boolean") && right.type.toString().equals("int"))) {
				throw new SemanticFailure(Cause.TYPE_ERROR);
			}
		}

		// == !=
		if (op.isEqualityOperation()) {
			if (!(isSubTypeOf(left.type, right.type) || isSubTypeOf(right.type, left.type))) {
				throw new SemanticFailure(Cause.TYPE_ERROR);
			}
		}

		return null;
	}

	// (typeName)arg
	public TypeSymbol cast(Cast ast, SymbolTable parent) {

		// Get typeSymbol
		TypeSymbol sym = SemanticAstVisitor.getTypeSymbol(ast.type.name);

		TypeSymbol castToType = SemanticAstVisitor.getTypeSymbol(ast.typeName);

		if (!isTypeDeclared(castToType)) {
			throw new SemanticFailure(Cause.NO_SUCH_TYPE);
		}

		if (!isSubTypeOf(castToType, ast.arg().type) && !isSubTypeOf(ast.arg().type, castToType)) {
			throw new SemanticFailure(Cause.TYPE_ERROR);
		}

		return sym;
	}

	public TypeSymbol intConst(IntConst ast, SymbolTable parent) {
		return null;
	}

	public TypeSymbol booleanConst(BooleanConst ast, SymbolTable parent) {
		return null;
	}

	public TypeSymbol nullConst(NullConst ast, SymbolTable parent) {
		return null;
	}

	public TypeSymbol field(Field ast, SymbolTable parent) {

		// arg is the object (class instance).
		Expr arg = ast.arg();
		arg.accept(this, parent);

		// arg of non-class type?
		if (!arg.type.isClassType()) {
			throw new SemanticFailure(Cause.TYPE_ERROR);
		}

		ClassSymbol classSymbol = globalSymbols.getClass(arg.type.toString());
		VariableSymbol sym = classSymbol.getField(ast.fieldName);

		// Check if field is declared (in static class type and in superclass).
		if (sym == null) {

			while (classSymbol.superClass != null && !classSymbol.superClass.name.equals("Object")
					&& classSymbol.superClass.getField(ast.fieldName) == null) {
				classSymbol = classSymbol.superClass;
				sym = globalSymbols.getClass(classSymbol.name).getField(ast.fieldName);
			}
		}
		if (sym == null) {
			throw new SemanticFailure(Cause.NO_SUCH_FIELD);
		}

		ast.sym = sym;
		ast.type = sym.type;

		return null;
	}

	// Array index access.
	public TypeSymbol index(Index ast, SymbolTable parent) {
		return null;
	}

	public TypeSymbol newObject(NewObject ast, SymbolTable parent) {
		return null;
	}

	public TypeSymbol newArray(NewArray ast, SymbolTable parent) {
		return null;
	}

	public TypeSymbol unaryOp(UnaryOp ast, SymbolTable parent) {

		Expr arg = ast.arg();
		arg.accept(this, parent);

		return null;
	}

	// Variable in Expression
	public TypeSymbol var(Var ast, SymbolTable parent) {

		// TODO

		return null;
	}

	public TypeSymbol builtInRead(BuiltInRead ast, SymbolTable parent) {

		return null;
	}

	// Method call : i0 = foo(); a.foo();
	public TypeSymbol methodCall(MethodCallExpr ast, SymbolTable parent) {

		ClassSymbol classSymbol;

		// 1. Find MethodSymbol: check if method declaration exists
		if (ast.receiver().type == null) {

			// Search in this class
			String className = parent.getParent().getName();
			classSymbol = globalSymbols.getClass(className);

		} else {
			// Search in receiver class
			classSymbol = globalSymbols.getClass(ast.receiver().type.name);
		}

		if (classSymbol == null) {
			throw new SemanticFailure(Cause.TYPE_ERROR);
		}

		MethodSymbol methodSymbol = classSymbol.getMethod(ast.methodName);
		// Search in superclasses
		while (classSymbol.superClass != null && !classSymbol.superClass.name.equals("Object")
				&& methodSymbol == null) {

			classSymbol = classSymbol.superClass;
			methodSymbol = globalSymbols.getClass(classSymbol.name).getMethod(ast.methodName);
		}

		if (methodSymbol == null) {
			// Method not found.
			throw new SemanticFailure(Cause.NO_SUCH_METHOD);
		}

		// 2. Set type of call = returnType of declaration
		ast.sym = methodSymbol;
		ast.type = methodSymbol.returnType;

		// Check arguments against formal parameters.
		List<Expr> arguments = ast.argumentsWithoutReceiver();
		if (arguments.size() != methodSymbol.parameters.size()) {
			throw new SemanticFailure(Cause.WRONG_NUMBER_OF_ARGUMENTS);
		}
		for (int i = 0; i < arguments.size(); i++) {
			if ( !isSubTypeOf(arguments.get(i).type, methodSymbol.parameters.get(i).type)) {
				throw new SemanticFailure(Cause.TYPE_ERROR);
			}
		}
		return null;
	}

	/**
	 * Check if type is subtype of superType.
	 * 
	 * @param type
	 * @param superType
	 * @return
	 */
	public boolean isSubTypeOf(TypeSymbol type, TypeSymbol superType) {

		// Equal types
		if (type.name.equals(superType.name)) {
			return true;
		}

		// Iterate over superclasses.
		if (type.isClassType()) {

			ClassSymbol classSymbol = globalSymbols.getClass(type.name);
			while (classSymbol.superClass != null && !classSymbol.superClass.name.equals("Object")) {

				classSymbol = classSymbol.superClass;
				if (classSymbol.name.equals(superType.name)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check if class is declared.
	 * 
	 * @param className
	 * @return
	 */
	public boolean isClassDeclared(String className) {

		for (Map.Entry<String, ClassSymbol> entry : globalSymbols.getClasses().entrySet()) {
			if (entry.getValue().name.equals(SemanticAstVisitor.getClassName(className))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if type has been declared.
	 * 
	 * @param type
	 * @return
	 */
	public boolean isTypeDeclared(TypeSymbol type) {

		if (!type.isClassType()) {
			return true;
		}
		if (isClassDeclared(type.name)) {
			return true;
		}
		return false;
	}

}
