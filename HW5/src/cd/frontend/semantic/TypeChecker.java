package cd.frontend.semantic;

import cd.frontend.semantic.SemanticFailure.Cause;
import cd.ir.Ast;
import cd.ir.Ast.Assign;
import cd.ir.Ast.BinaryOp;
import cd.ir.Ast.BooleanConst;
import cd.ir.Ast.BuiltInRead;
import cd.ir.Ast.BuiltInWrite;
import cd.ir.Ast.BuiltInWriteln;
import cd.ir.Ast.Cast;
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
import cd.ir.Ast.NullConst;
import cd.ir.Ast.ReturnStmt;
import cd.ir.Ast.ThisRef;
import cd.ir.Ast.UnaryOp;
import cd.ir.Ast.Var;
import cd.ir.Ast.WhileLoop;
import cd.ir.AstVisitor;
import cd.ir.ExprVisitor;
import cd.ir.Symbol.ArrayTypeSymbol;
import cd.ir.Symbol.ClassSymbol;
import cd.ir.Symbol.MethodSymbol;
import cd.ir.Symbol.PrimitiveTypeSymbol;
import cd.ir.Symbol.TypeSymbol;
import cd.ir.Symbol.VariableSymbol;
import cd.util.debug.AstOneLine;

public class TypeChecker {

	final private SymTable<TypeSymbol> typeSymbols;
	
	public TypeChecker(SymTable<TypeSymbol> typeSymbols) {
		this.typeSymbols = typeSymbols;
	}

	public void checkMethodDecl(MethodDecl method, SymTable<VariableSymbol> locals) {
		new MethodDeclVisitor(method, locals).visit(method.body(), null);
	}
	

	/**
	 * Checks whether two expressions have the same type
	 * and throws an exception otherwise.
	 * @return The common type of the two expression.
	 */
	private TypeSymbol checkTypesEqual(Expr leftExpr, Expr rightExpr, SymTable<VariableSymbol> locals) {
		final TypeSymbol leftType = typeExpr(leftExpr, locals);
		final TypeSymbol rightType = typeExpr(rightExpr, locals);

		if (leftType != rightType) {
			throw new SemanticFailure(
						Cause.TYPE_ERROR, 
						"Expected operand types to be equal but found %s, %s",
						leftType, 
						rightType);
		}
	
		return leftType;
	}
	
	private void checkTypeIsInt(TypeSymbol type) {
		if (type != PrimitiveTypeSymbol.intType) {
			throw new SemanticFailure(
					Cause.TYPE_ERROR, 
					"Expected %s for operands but found type %s", 
					PrimitiveTypeSymbol.intType, 
					type);	
		}
	}
	
	private ClassSymbol asClassSymbol(TypeSymbol type) {
		if (type instanceof ClassSymbol)
			return (ClassSymbol) type;
		throw new SemanticFailure(
				Cause.TYPE_ERROR, 
				"A class type was required, but %s was found", type);
	}

	private ArrayTypeSymbol asArraySymbol(TypeSymbol type) {
		if (type instanceof ArrayTypeSymbol)
			return (ArrayTypeSymbol) type;
		throw new SemanticFailure(
				Cause.TYPE_ERROR, 
				"An array type was required, but %s was found", type);
	}
	
	private TypeSymbol typeExpr(Expr expr, SymTable<VariableSymbol> locals) {
		return new TypingVisitor().visit(expr, locals);
	}

	private void checkType(Expr ast, TypeSymbol expected, SymTable<VariableSymbol> locals) {
		TypeSymbol actual = typeExpr(ast, locals);
		if (!expected.isSuperTypeOf(actual))
			throw new SemanticFailure(
					Cause.TYPE_ERROR,
					"Expected %s but type was %s",
					expected,
					actual);
	}
		
	private class MethodDeclVisitor extends AstVisitor<Void, Void> {
		
		private MethodDecl method;
		private SymTable<VariableSymbol> locals;

		public MethodDeclVisitor(MethodDecl method, SymTable<VariableSymbol> locals) {
			this.method = method;
			this.locals = locals;
		}

		@Override
		protected Void dfltExpr(Expr ast, Void arg) {
			throw new RuntimeException("Should not get here");
		}
		
		@Override
		public Void assign(Assign ast, Void arg) {
			TypeSymbol lhs = typeLhs(ast.left(), locals);
			TypeSymbol rhs = typeExpr(ast.right(), locals);
			if (!lhs.isSuperTypeOf(rhs))
				throw new SemanticFailure(
						Cause.TYPE_ERROR,
						"Type %s cannot be assigned to %s",
						rhs, lhs);
			return null;
		}

		@Override
		public Void builtInWrite(BuiltInWrite ast, Void arg) {
			checkType(ast.arg(), PrimitiveTypeSymbol.intType, locals);
			return null;
		}
		
		@Override
		public Void builtInWriteln(BuiltInWriteln ast, Void arg) {
			return null;
		}
		
		@Override
		public Void ifElse(IfElse ast, Void arg) {
			checkType((Expr)ast.condition(), PrimitiveTypeSymbol.booleanType, locals);
			visit(ast.then(), arg);
			if (ast.otherwise() != null)
				visit(ast.otherwise(), arg);
			return null;
		}

		@Override
		public Void methodCall(MethodCall ast, Void arg) {
			typeExpr(ast.getMethodCallExpr(), locals);
			
			return null;
			
		}

		@Override
		public Void whileLoop(WhileLoop ast, Void arg) {
			checkType((Expr)ast.condition(), PrimitiveTypeSymbol.booleanType, locals);
			return visit(ast.body(), arg);
		}
		
		@Override
		public Void returnStmt(ReturnStmt ast, Void arg) {
			boolean hasArg = ast.arg() != null;
			if (hasArg && method.sym.returnType == PrimitiveTypeSymbol.voidType) {
				// void m() { return m(); }
				throw new SemanticFailure(
						Cause.TYPE_ERROR,
						"Return statement of method with void return type should "
						+ "not have arguments.");
			} else if (!hasArg) {
				// X m() { return; }
				if (method.sym.returnType != PrimitiveTypeSymbol.voidType) {
					throw new SemanticFailure(	
							Cause.TYPE_ERROR,
							"Return statement has no arguments. Expected %s but type was %s",
							method.sym.returnType,
							PrimitiveTypeSymbol.voidType);
				}
			} else {
				// X m() { return y; }
				checkType(ast.arg(), method.sym.returnType, locals);
			}
			
			return null;
		}
		
	}

	private class TypingVisitor extends ExprVisitor<TypeSymbol, SymTable<VariableSymbol>> {
		
		@Override
		public TypeSymbol visit(Expr ast, SymTable<VariableSymbol> arg) {
			ast.type = super.visit(ast, arg);
			return ast.type;
		}
 
		@Override
		public TypeSymbol binaryOp(BinaryOp ast, SymTable<VariableSymbol> locals) {
			switch (ast.operator) {
			case B_TIMES:
			case B_DIV:
			case B_MOD:
			case B_PLUS:
			case B_MINUS:
				TypeSymbol type = checkTypesEqual(ast.left(), ast.right(), locals);
				checkTypeIsInt(type);
				return type;
			
			case B_AND:
			case B_OR:
	        	checkType(ast.left(), PrimitiveTypeSymbol.booleanType, locals);
    			checkType(ast.right(), PrimitiveTypeSymbol.booleanType, locals);
    			return PrimitiveTypeSymbol.booleanType;
	        	
			case B_EQUAL:
			case B_NOT_EQUAL:
	        	TypeSymbol left = typeExpr(ast.left(), locals);
	        	TypeSymbol right = typeExpr(ast.right(), locals);
	        	if (left.isSuperTypeOf(right) || right.isSuperTypeOf(left))
	        		return PrimitiveTypeSymbol.booleanType;
	        	throw new SemanticFailure(
	        			Cause.TYPE_ERROR,
	        			"Types %s and %s could never be equal",
	        			left, right);
	        	
			case B_LESS_THAN:
			case B_LESS_OR_EQUAL:
			case B_GREATER_THAN:
			case B_GREATER_OR_EQUAL:
				checkTypeIsInt(checkTypesEqual(ast.left(), ast.right(), locals));
    			return PrimitiveTypeSymbol.booleanType;
			
			}
			throw new RuntimeException("Unhandled operator "+ast.operator);
		}

		@Override
		public TypeSymbol booleanConst(BooleanConst ast, SymTable<VariableSymbol> arg) {
			return PrimitiveTypeSymbol.booleanType;
		}

		@Override
		public TypeSymbol builtInRead(BuiltInRead ast, SymTable<VariableSymbol> arg) {
			return PrimitiveTypeSymbol.intType;
		}

		@Override
		public TypeSymbol cast(Cast ast, SymTable<VariableSymbol> locals) {
			TypeSymbol argType = typeExpr(ast.arg(), locals);
			ast.type = typeSymbols.getType(ast.typeName);
			
			if (argType.isSuperTypeOf(ast.type) || ast.type.isSuperTypeOf(argType))
				return ast.type;
			
			throw new SemanticFailure(
					Cause.TYPE_ERROR,
					"Types %s and %s in cast are completely unrelated.",
					argType, ast.type);
		}

		@Override
		protected TypeSymbol dfltExpr(Expr ast, SymTable<VariableSymbol> arg) {
			throw new RuntimeException("Unhandled type");
		}

		@Override
		public TypeSymbol field(Field ast, SymTable<VariableSymbol> locals) {
			ClassSymbol argType = asClassSymbol(typeExpr(ast.arg(), locals));  // class of the receiver of the field access
			ast.sym = argType.getField(ast.fieldName);
			if (ast.sym == null)
				throw new SemanticFailure(
						Cause.NO_SUCH_FIELD,
						"Type %s has no field %s",
						argType, ast.fieldName);
			return ast.sym.type;
		}

		@Override
		public TypeSymbol index(Index ast, SymTable<VariableSymbol> locals) {
			ArrayTypeSymbol argType = asArraySymbol(typeExpr(ast.left(), locals));
			checkType(ast.right(), PrimitiveTypeSymbol.intType, locals);
			return argType.elementType;
		}

		@Override
		public TypeSymbol intConst(IntConst ast, SymTable<VariableSymbol> arg) {
			return PrimitiveTypeSymbol.intType;
		}

		@Override
		public TypeSymbol newArray(NewArray ast, SymTable<VariableSymbol> locals) {
			checkType(ast.arg(), PrimitiveTypeSymbol.intType, locals);
			return typeSymbols.getType(ast.typeName);
		}

		@Override
		public TypeSymbol newObject(NewObject ast, SymTable<VariableSymbol> arg) {
			return typeSymbols.getType(ast.typeName);
		}

		@Override
		public TypeSymbol nullConst(NullConst ast, SymTable<VariableSymbol> arg) {
			return ClassSymbol.nullType;
		}

		@Override
		public TypeSymbol thisRef(ThisRef ast, SymTable<VariableSymbol> locals) {
			VariableSymbol vsym = locals.get("this");
			return vsym.type;
		}

		@Override
		public TypeSymbol unaryOp(UnaryOp ast, SymTable<VariableSymbol> locals) {
			
			switch (ast.operator) {
			case U_PLUS:
			case U_MINUS:
			{
				TypeSymbol type = typeExpr(ast.arg(), locals);
				checkTypeIsInt(type);			
				return type;
			}
			
			case U_BOOL_NOT:
				checkType(ast.arg(), PrimitiveTypeSymbol.booleanType, locals);
				return PrimitiveTypeSymbol.booleanType;			
			}
			throw new RuntimeException("Unknown unary op "+ast.operator);
		}

		@Override
		public TypeSymbol var(Var ast, SymTable<VariableSymbol> locals) {
			if (!locals.contains(ast.name))
				throw new SemanticFailure(
						Cause.NO_SUCH_VARIABLE,
						"No variable %s was found",
						ast.name);
			ast.setSymbol(locals.get(ast.name));
			return ast.sym.type;
		}
		
		@Override
		public TypeSymbol methodCall(MethodCallExpr ast, SymTable<VariableSymbol> locals) {

			ClassSymbol rcvrType = asClassSymbol(typeExpr(ast.receiver(), locals));			
			MethodSymbol mthd = rcvrType.getMethod(ast.methodName);
			if (mthd == null)
				throw new SemanticFailure(
						Cause.NO_SUCH_METHOD,
						"Class %s has no method %s()",
						rcvrType.name, ast.methodName);		
			
			ast.sym = mthd;
			
			// Check that the number of arguments is correct.
			if (ast.argumentsWithoutReceiver().size() != mthd.parameters.size())
				throw new SemanticFailure(
						Cause.WRONG_NUMBER_OF_ARGUMENTS,
						"Method %s() takes %d arguments, but was invoked with %d",
						ast.methodName,
						mthd.parameters.size(),
						ast.argumentsWithoutReceiver().size());
									
			// Check that the arguments are of correct type.
			int i = 0;
			for (Ast argAst : ast.argumentsWithoutReceiver())
				checkType((Expr)argAst, mthd.parameters.get(i++).type, locals);
			
			return ast.sym.returnType;
			
		}

	}
	
	/**
	 * Checks an expr as the left-hand-side of an assignment,
	 * returning the type of value that may be assigned there.
	 * May fail if the expression is not a valid LHS (for example,
	 * a "final" field). */
	private TypeSymbol typeLhs(Expr expr, SymTable<VariableSymbol> locals) {
		return new LValueVisitor().visit(expr, locals);
	}
	
	/**
	 * @see TypeChecker#typeLhs(Expr, SymTable)
	 */
	private class LValueVisitor extends ExprVisitor<TypeSymbol, SymTable<VariableSymbol>> {
		/** Fields, array-indexing, and vars can be on the LHS: */
		
		@Override
		public TypeSymbol field(Field ast, SymTable<VariableSymbol> locals) {
			return typeExpr(ast, locals);
		}

		@Override
		public TypeSymbol index(Index ast, SymTable<VariableSymbol> locals) {
			return typeExpr(ast, locals);
		}

		@Override
		public TypeSymbol var(Var ast, SymTable<VariableSymbol> locals) {
			return typeExpr(ast, locals);
		}
		
		/** Any other kind of expression is not a value lvalue */
		@Override
		protected TypeSymbol dfltExpr(Expr ast, SymTable<VariableSymbol> locals) {
			throw new SemanticFailure(
					Cause.NOT_ASSIGNABLE,
					"'%s' is not a valid lvalue", 
					AstOneLine.toString(ast));
		}
	}
}

