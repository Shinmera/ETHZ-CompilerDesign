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
public class SemanticPopulatorAstVisitor extends AstVisitor<TypeSymbol, SymbolTable> {

	// We have to build a tree of symbol tables.
	private GlobalSymbolTable globalSymbols = new GlobalSymbolTable("global");

	public GlobalSymbolTable getSymbolTable() {
		return globalSymbols;
	}

	/**
	 * Visit class declaration of AST.
	 * 
	 * @param ast
	 * @param parent
	 */
	public TypeSymbol classDecl(ClassDecl ast, SymbolTable arg) {

		// "Object" is reserved.
		if (ast.name.equals("Object")) {
			throw new SemanticFailure(Cause.OBJECT_CLASS_DEFINED);
		}

		// Create new ClassSymbol and add information
		ClassSymbol sym = new ClassSymbol(ast.name);

		// Create new symbol table
		ClassSymbolTable classSymbolTable = new ClassSymbolTable(ast.name);
		// Link class symbol table to global symbol table.
		classSymbolTable.setParent(globalSymbols);
		globalSymbols.addChild(classSymbolTable);

		// Superclass
		sym.superClass = new ClassSymbol(ast.superClass);

		// Visit fields
		List<VarDecl> fields = ast.fields();
		for (VarDecl decl : fields) {
			decl.accept(this, classSymbolTable);
		}
		// Add fields
		sym.fields.putAll(classSymbolTable.getFields());

		// Visit methods.
		List<MethodDecl> methodDecls = ast.methods();
		for (MethodDecl decl : methodDecls) {
			decl.accept(this, classSymbolTable);
		}
		// Add methods.
		sym.methods.putAll(classSymbolTable.getMethods());

		// Add class symbol to AST.
		ast.sym = sym;

		// Check if symbol exists
		ClassSymbol symbol = globalSymbols.getClass(ast.name);

		if (symbol == null) {

			// Add class symbol to global symbol table.
			symbol = ast.sym;
			globalSymbols.putClass(ast.name, symbol);

		} else {

			// error: symbol exists.
			throw new SemanticFailure(Cause.DOUBLE_DECLARATION);
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
		MethodSymbol sym = new MethodSymbol(ast);

		// Create new symbol table.
		MethodSymbolTable methodSymbolTable = new MethodSymbolTable(ast.name);

		// Check if symbol already exists in scope.
		MethodSymbol symbol = parent.getMethod(ast.name);
		if (symbol != null) {
			throw new SemanticFailure(Cause.DOUBLE_DECLARATION);
		}

		// Link method symbol table to parent class symbol table.
		methodSymbolTable.setParent(parent);
		parent.addChild(methodSymbolTable);

		// Add parameters to symbol
		List<String> types = ast.argumentTypes;
		List<String> names = ast.argumentNames;
		for (int i = 0; i < names.size(); i++) {

			// Check for double declaration within parameters.
			for (int j = 0; j < sym.parameters.size(); j++) {
				if (sym.parameters.get(j).name.equals(names.get(i))) {
					throw new SemanticFailure(Cause.DOUBLE_DECLARATION);
				}
			}
			sym.parameters.add(new VariableSymbol(names.get(i), getTypeSymbol(types.get(i)), Kind.PARAM));
		}

		ast.sym = sym;
		parent.putMethod(ast.name, ast.sym);

		/*
		 * Visit declarations
		 */
		Seq decls = ast.decls();
		decls.accept(this, methodSymbolTable);

		// Add locals to symbol
		sym.locals.putAll(methodSymbolTable.getFields());

		/*
		 * Visit body
		 */
		Seq body = ast.body();
		body.accept(this, methodSymbolTable);

		// Add return type to symbol
		sym.returnType = getTypeSymbol(ast.returnType);

		// Add class symbol to AST.
		ast.sym = sym;

		// Check if symbol already exists in scope.
		symbol = parent.getMethod(ast.name);

		// Add symbol to symbol table.
		symbol = ast.sym;
		parent.putMethod(ast.name, symbol);

		return null;
	}

	/**
	 * Visit variable declaration of AST.
	 * 
	 * @param ast
	 * @param parent
	 */
	public TypeSymbol varDecl(VarDecl ast, SymbolTable parent) {

		// Get scope: class or method?
		Kind kind = Kind.FIELD;
		if (parent.getScope() == Scope.METHOD) {
			kind = Kind.LOCAL;
		}

		// Create new VariableSymbol and add information
		VariableSymbol sym = new VariableSymbol(ast.name, getTypeSymbol(ast.type), kind);

		// Add class variable symbol to AST.
		ast.sym = sym;

		// Check if symbol exists in scope.
		VariableSymbol symbol = parent.getField(ast.name);
		if (symbol == null) {

			// Add symbol to symbol table.
			// symbol = ast.sym;
			parent.putField(ast.name, ast.sym);

		} else {

			// error: symbol exists.
			throw new SemanticFailure(Cause.DOUBLE_DECLARATION);
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

	// TODO Expressions have to return something.. Maybe the type?

	public TypeSymbol binaryOp(BinaryOp ast, SymbolTable parent) {

		Expr left = ast.left();
		left.accept(this, parent);

		Expr right = ast.right();
		right.accept(this, parent);

		BOp op = ast.operator;

		if (op.isIntToIntOperation()) {
			ast.type = PrimitiveTypeSymbol.intType;
		} else {
			ast.type = PrimitiveTypeSymbol.booleanType;
		}

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

		ast.type = PrimitiveTypeSymbol.intType;

		return null;
	}

	public TypeSymbol booleanConst(BooleanConst ast, SymbolTable parent) {

		ast.type = PrimitiveTypeSymbol.booleanType;

		return null;
	}

	public TypeSymbol nullConst(NullConst ast, SymbolTable parent) {

		ast.type = ClassSymbol.nullType;

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

		// Type is equal to the type of array (left).
		ast.type = left.type;

		return null;
	}

	public TypeSymbol newObject(NewObject ast, SymbolTable parent) {

		ast.type = getTypeSymbol(ast.typeName);

		return null;
	}

	// typeName [arg]
	public TypeSymbol newArray(NewArray ast, SymbolTable parent) {

		Expr arg = ast.arg();
		arg.accept(this, parent);

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

		// We define the type of the variable in the next step.

		return null;
	}

	public TypeSymbol builtInRead(BuiltInRead ast, SymbolTable parent) {

		ast.type = PrimitiveTypeSymbol.intType;

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
