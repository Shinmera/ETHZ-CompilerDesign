package cd.frontend.semantic;

import java.util.ArrayList;
import java.util.List;

import cd.frontend.semantic.SemanticFailure.Cause;
import cd.frontend.semantic.SymbolTable.Scope;
import cd.ir.Ast;
import cd.ir.Ast.Assign;
import cd.ir.Ast.BinaryOp;
import cd.ir.Ast.BinaryOp.BOp;
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
import cd.ir.AstVisitor;
import cd.ir.Symbol;
import cd.ir.Symbol.ArrayTypeSymbol;
import cd.ir.Symbol.ClassSymbol;
import cd.ir.Symbol.MethodSymbol;
import cd.ir.Symbol.PrimitiveTypeSymbol;
import cd.ir.Symbol.TypeSymbol;
import cd.ir.Symbol.VariableSymbol;
import cd.ir.Symbol.VariableSymbol.Kind;

/**
 * Visits all nodes and add/lookup Symbols to SymbolTable.
 * 
 * @author dirkhuttig
 */
public class SemanticAstVisitor extends AstVisitor<TypeSymbol, SymbolTable> {

	// We have to build a tree of symbol tables.
	private GlobalSymbolTable globalSymbols = new GlobalSymbolTable("global");

	public GlobalSymbolTable getSymbolTable() {
		return globalSymbols;
	}

	/**
	 * Constructor
	 * 
	 * @param table
	 */
	public SemanticAstVisitor(GlobalSymbolTable table) {
		globalSymbols = table;
	}

	/**
	 * Visit class declaration of AST.
	 * 
	 * @param ast
	 * @param parent
	 */
	public TypeSymbol classDecl(ClassDecl ast, SymbolTable arg) {

		// Create new ClassSymbol and add information
		ClassSymbol sym = globalSymbols.getClass(ast.name);

		// Create new symbol table
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

		// Add class symbol to AST.
		ast.sym = sym;

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

		// Create new symbol table.
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

		// Add class symbol to AST.
		ast.sym = sym;

		return null;
	}

	/**
	 * Visit variable declaration of AST.
	 * 
	 * @param ast
	 * @param parent
	 */
	public TypeSymbol varDecl(VarDecl ast, SymbolTable parent) {

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

		Expr left = ast.left();
		Expr right = ast.right();
		left.accept(this, parent);
		right.accept(this, parent);

		return null;
	}

	public TypeSymbol builtInWrite(BuiltInWrite ast, SymbolTable parent) {

		Expr arg = ast.arg();
		arg.accept(this, parent);

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

		if (!condition.type.name.equals(PrimitiveTypeSymbol.booleanType.name)) {
			throw new SemanticFailure(Cause.TYPE_ERROR);
		}

		return null;
	}

	public TypeSymbol returnStmt(ReturnStmt ast, SymbolTable parent) {

		Expr arg = ast.arg();
		arg.accept(this, parent);
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

		return null;
	}

	public TypeSymbol cast(Cast ast, SymbolTable parent) {

		Expr arg = ast.arg();
		arg.accept(this, parent);

		// Add typeSymbol
		ast.type = getTypeSymbol(ast.typeName);

		return null;
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

	// e.g. expr.f
	public TypeSymbol field(Field ast, SymbolTable parent) {

		Expr arg = ast.arg();
		arg.accept(this, parent);

		return null;
	}

	// Array index access.
	public TypeSymbol index(Index ast, SymbolTable parent) {

		// left is array, right is index.
		Expr left = ast.left();
		left.accept(this, parent);

		Expr right = ast.right();
		right.accept(this, parent);

		if (!right.type.name.equals("int")) {
			throw new SemanticFailure(Cause.TYPE_ERROR);
		}

		// Type is equal to the type of array (left).
		ast.type = left.type;

		return null;
	}

	public TypeSymbol newObject(NewObject ast, SymbolTable parent) {

		return null;
	}

	// typeName [arg]
	public TypeSymbol newArray(NewArray ast, SymbolTable parent) {

		Expr arg = ast.arg();
		arg.accept(this, parent);

		// arg must be of type int.
		if (!arg.type.name.equals("int")) {
			throw new SemanticFailure(Cause.TYPE_ERROR);
		}

		ast.type = getTypeSymbol(ast.typeName);

		return null;
	}

	public TypeSymbol unaryOp(UnaryOp ast, SymbolTable parent) {

		Expr arg = ast.arg();
		arg.accept(this, parent);

		ast.type = arg.type;

		return null;
	}

	// Variable in Expression
	public TypeSymbol var(Var ast, SymbolTable parent) {

		boolean found = false;

		VariableSymbol var = parent.getField(ast.name);

		// Variable not declared in method.
		if (var == null) {
			if (parent.parent != null) {

				// Search in formalParameters.
				MethodSymbol method = parent.parent.getMethod(parent.getName());

				List<VariableSymbol> parameters = method.parameters;
				for (VariableSymbol parameter : parameters) {
					if (parameter.name.equals(ast.name)) {
						found = true;
						var = parameter;
					}
				}

				// Variable not declared in this context. Search in parent of
				// parent (class).
				if (!found) {

					var = parent.getParent().getField(ast.name);
					if (var != null) {
						found = true;
					}
				}

				// Check if field is declared (in static class type and in
				// superclass).
				ClassSymbol classSymbol = globalSymbols.getClass(parent.getParent().name);

				if (var == null) {
					while (classSymbol.superClass != null && !classSymbol.superClass.name.equals("Object")
							&& classSymbol.superClass.getField(ast.name) == null) {
						classSymbol = classSymbol.superClass;
						var = globalSymbols.getClass(classSymbol.name).getField(ast.name);
						if (var != null) {
							found = true;
						}
					}
				}
			}
		} else {
			found = true;
		}

		if (!found) {
			throw new SemanticFailure(Cause.NO_SUCH_VARIABLE);
		}

		ast.type = var.type;

		return null;
	}

	public TypeSymbol builtInRead(BuiltInRead ast, SymbolTable parent) {

		return null;
	}

	// Method call : i0 = foo();
	public TypeSymbol methodCall(MethodCallExpr ast, SymbolTable parent) {

		ast.receiver().accept(this, parent);

		List<Expr> arguments = ast.argumentsWithoutReceiver();
		for (Expr expr : arguments) {
			expr.accept(this, parent);
		}

		return null;
	}

	/**
	 * Get the type symbol based on the name of the type.
	 * 
	 * @param typeString
	 * @return
	 */
	public static TypeSymbol getTypeSymbol(String typeString) {

		TypeSymbol type;
		boolean isArray = false;

		// Check if array
		if (typeString.indexOf("[") != -1) {
			typeString = typeString.substring(0, typeString.indexOf("["));
			isArray = true;
		}

		// PrimitiveType
		if (typeString.equals("int") || typeString.equals("boolean") || typeString.equals("void")) {
			type = new PrimitiveTypeSymbol(typeString);

		}
		// ClassType
		else {
			type = new ClassSymbol(typeString);
		}

		// ArrayType
		if (isArray) {
			type = new ArrayTypeSymbol(type);
		}

		return type;
	}

	public static String getClassNameByType(TypeSymbol type) {

		if (!type.isClassType()) {
			return null;
		}

		if (!type.isArrayType()) {
			return type.name;
		}

		return type.name.substring(0, type.name.indexOf("["));
	}

	public static String getClassName(String className) {
		if (className.indexOf("[") != -1) {
			return className.substring(0, className.indexOf("["));
		}
		return className;
	}

}
