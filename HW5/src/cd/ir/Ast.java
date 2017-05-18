package cd.ir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cd.ir.Symbol.ClassSymbol;
import cd.ir.Symbol.MethodSymbol;
import cd.ir.Symbol.TypeSymbol;
import cd.ir.Symbol.VariableSymbol;
import cd.util.Pair;
import cd.util.debug.AstOneLine;

public abstract class Ast {

	/**
	 * The list of children AST nodes.  Typically, this list is of a fixed size: its contents
	 * can also be accessed using the various accessors defined on the Ast subtypes.
	 * 
	 * <p><b>Note:</b> this list may contain null pointers! 
	 */
	public final List<Ast> rwChildren;
	
	protected Ast(int fixedCount) {
		if (fixedCount == -1)
			this.rwChildren = new ArrayList<Ast>();
		else
			this.rwChildren = Arrays.asList(new Ast[fixedCount]);
	}
	
	/** 
	 * Returns a copy of the list of children for this node.  The result will 
	 * never contain null pointers.
	 */
	public List<Ast> children() {
		ArrayList<Ast> result = new ArrayList<Ast>();
		for (Ast n : rwChildren) {
			if (n != null) result.add(n);
		}
		return result;
	}
	
	/** 
	 * Returns a new list containing all children AST nodes 
	 * that are of the given type.
	 */
	public <A> List<A> childrenOfType(Class<A> C) {
		List<A> res = new ArrayList<A>();
		for (Ast c : children()) {
			if (C.isInstance(c))
				res.add(C.cast(c));
		}
		return res;
	}
	
	
	/** Accept method for the pattern Visitor. */
	public abstract <R,A> R accept(AstVisitor<R, A> visitor, A arg);
	
	
	/** Makes a deep clone of this AST node. */
	public abstract Ast deepCopy();
	
	/** Convenient debugging printout */
	@Override
    public String toString() {
		return String.format(
				"(%s)@%x",
				AstOneLine.toString(this),
				System.identityHashCode(this));
	}
	
	// _________________________________________________________________
	// Expressions

	/** Base class for all expressions */
	public static abstract class Expr extends Ast {
		
		protected Expr(int fixedCount) {
			super(fixedCount);
		}
		
		/** Type that this expression will evaluate to (computed in semantic phase). */
		public TypeSymbol type;
		
		@Override
        public <R,A> R accept(AstVisitor<R, A> visitor, A arg) {
			return this.accept((ExprVisitor<R,A>)visitor, arg);
		}
		public abstract <R,A> R accept(ExprVisitor<R, A> visitor, A arg);
		
		/** Copies any non-AST fields. */
		protected <E extends Expr> E postCopy(E item) {
			{
				item.type = type;
			}
			return item;
		}
	}	
	
	/** Base class used for exprs with left/right operands.  
	 *  We use this for all expressions that take strictly two operands, 
	 *  such as binary operators or array indexing. */
	public static abstract class LeftRightExpr extends Expr {
		
		public LeftRightExpr(Expr left, Expr right) {
			super(2);
			assert left != null;
			assert right != null;
			setLeft(left); 
			setRight(right);
		}
		
		public Expr left() { return (Expr) this.rwChildren.get(0); }
		public void setLeft(Expr node) { this.rwChildren.set(0, node); }

		public Expr right() { return (Expr) this.rwChildren.get(1); }
		public void setRight(Expr node) { this.rwChildren.set(1, node); }
	}
	
	/** Base class used for expressions with a single argument */
	public static abstract class ArgExpr extends Expr {
		
		public ArgExpr(Expr arg) {
			super(1);
			assert arg != null;
			setArg(arg);
		}
		
		public Expr arg() { return (Expr) this.rwChildren.get(0); }
		public void setArg(Expr node) { this.rwChildren.set(0, node); }
		
	}
	
	/** Base class used for things with no arguments */
	protected static abstract class LeafExpr extends Expr {
		public LeafExpr() {
			super(0);
		}
	}
	
	/** Represents {@code this}, the current object */
	public static class ThisRef extends LeafExpr {
		
		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.thisRef(this, arg);
		}
		
		@Override
		public ThisRef deepCopy() {
			return postCopy(new ThisRef());
		}
		
	}
	
	/** A binary operation combining a left and right operand, 
	 *  such as "1+2" or "3*4" */
	public static class BinaryOp extends LeftRightExpr {
				
	    public static enum BOp {
	    	
	        B_TIMES("*"),
	        B_DIV("/"),
	        B_MOD("%"),
	        B_PLUS("+"),
	        B_MINUS("-"),
	        B_AND("&&"),
	        B_OR("||"),
	        B_EQUAL("=="),
	        B_NOT_EQUAL("!="),
	        B_LESS_THAN("<"),
	        B_LESS_OR_EQUAL("<="),
	        B_GREATER_THAN(">"),
	        B_GREATER_OR_EQUAL(">=");
	        
	        public String repr;
			private BOp(String repr) { this.repr = repr; }
			
			/**
			 * Note that this method ignores short-circuit evaluation of boolean 
			 * AND/OR.
			 * 
			 * @return <code>true</code> iff <code>A op B == B op A</code> for this
			 * operator.
			 */
			public boolean isCommutative() {
		        switch(this) {
		        case B_PLUS:
		        case B_TIMES:
		        case B_AND:
		        case B_OR:
		        case B_EQUAL:
		        case B_NOT_EQUAL:
		            return true;
		        default:
		            return false;
		        }
			}
	    };
	    
	    public BOp operator;
	    
	    public BinaryOp(Expr left, BOp operator, Expr right) {
	    	super(left, right);
	    	this.operator = operator;
	    }

		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.binaryOp(this, arg);
		}
		
		@Override
		public BinaryOp deepCopy() {
			return postCopy(new BinaryOp(left(), operator, right()));
		}

	}
	
	/** A Cast from one type to another: {@code (typeName)arg} */
	public static class Cast extends ArgExpr {
		
		public String typeName;
		
		public Cast(Expr arg, String typeName) {
			super(arg);
			this.typeName = typeName;
		}

		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.cast(this, arg);
		}
		
		@Override
		public Cast deepCopy() {
			return postCopy(new Cast(arg(), typeName));
		}

		@Override
		protected <E extends Expr> E postCopy(E item) {
			((Cast)item).type = type;
			return super.postCopy(item);
		}
		
	}

	public static class IntConst extends LeafExpr {
		
		public final int value;
		public IntConst(int value) {
			this.value = value;
		}
		
		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.intConst(this, arg);
		}
		
		@Override
		public IntConst deepCopy() {
			return postCopy(new IntConst(value));
		}
		
	}

	public static class BooleanConst extends LeafExpr {
		
		public final boolean value;
		public BooleanConst(boolean value) {
			this.value = value;
		}
		
		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.booleanConst(this, arg);
		}
		
		@Override
		public BooleanConst deepCopy() {
			return postCopy(new BooleanConst(value));
		}
		
	}
	
	public static class NullConst extends LeafExpr {
		
		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.nullConst(this, arg);
		}
		
		@Override
		public NullConst deepCopy() {
			return postCopy(new NullConst());
		}
		
	}
	
	public static class Field extends ArgExpr {
		
		public final String fieldName;
		
		public VariableSymbol sym;
		
		public Field(Expr arg, String fieldName) {
			super(arg);
			assert arg != null && fieldName != null;
			this.fieldName = fieldName;
		}
		
		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.field(this, arg);
		}
		
		@Override
		public Field deepCopy() {
			return postCopy(new Field(arg(), fieldName));
		}
		
		@Override
		protected <E extends Expr> E postCopy(E item) {
			((Field)item).sym = sym;
			return super.postCopy(item);
		}
		
	}
	
	public static class Index extends LeftRightExpr {
		
		public Index(Expr array, Expr index) {
			super(array, index);			
		}

		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.index(this, arg);
		}
		
		@Override
		public Index deepCopy() {
			return postCopy(new Index(left(), right()));
		}
		
	}
	
	public static class NewObject extends LeafExpr {
		
		/** Name of the type to be created */
		public String typeName;
		
		public NewObject(String typeName) {
			this.typeName = typeName;
		}

		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.newObject(this, arg);
		}
		
		@Override
		public NewObject deepCopy() {
			return postCopy(new NewObject(typeName));
		}
		
	}
	
	public static class NewArray extends ArgExpr {
		
		/** Name of the type to be created: must be an array type */
		public String typeName;
		
		public NewArray(String typeName, Expr capacity) {
			super(capacity);
			this.typeName = typeName;
		}

		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.newArray(this, arg);
		}
		
		@Override
		public NewArray deepCopy() {
			return postCopy(new NewArray(typeName, arg()));
		}
		
	}
	
	public static class UnaryOp extends ArgExpr {
		
	    public static enum UOp {
	        U_PLUS("+"),
	        U_MINUS("-"),
	        U_BOOL_NOT("!");
	        public String repr;
			private UOp(String repr) { this.repr = repr; }
	    };
	    
	    public final UOp operator;
	    
	    public UnaryOp(UOp operator, Expr arg) {
			super(arg);
			this.operator = operator;
		}
	    
		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.unaryOp(this, arg);
		}
		
		@Override
		public UnaryOp deepCopy() {
			return postCopy(new UnaryOp(operator, arg()));
		}
		
	}
	
	public static class Var extends LeafExpr {
		
		public String name;
		
		public VariableSymbol sym;
		
		/** 
		 * Use this constructor to build an instance of this AST
		 * in the parser.  
		 */
		public Var(String name) {
			this.name = name;
		}
		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.var(this, arg);
		}

		/**
		 * Use this static function to build an instance after the
		 * semantic phase; it fills in the {@link #type} and {@link #sym}
		 * fields.
		 */
		public static Var withSym(VariableSymbol sym) {
			Var v = new Var(sym.name);
			v.sym = sym;
			v.type = sym.type;
			return v;
		}
		
		@Override
		public Var deepCopy() {
			return postCopy(new Var(name));
		}
		
		@Override
		protected <E extends Expr> E postCopy(E item) {
			((Var)item).sym = sym;
			return super.postCopy(item);
		}
		
		public void setSymbol(VariableSymbol variableSymbol) {
			sym = variableSymbol;
			name = sym.toString();
		}
		
	}
	
	public static class BuiltInRead extends LeafExpr {
		
		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.builtInRead(this, arg);
		}
		
		@Override
		public BuiltInRead deepCopy() {
			return postCopy(new BuiltInRead());
		}
		
	}
		
	public static class MethodCallExpr extends Expr {
		
		public String methodName;
		
		public MethodSymbol sym;
		
		public MethodCallExpr(Expr rcvr, String methodName, List<Expr> arguments) {
			super(-1);
			assert rcvr != null && methodName != null && arguments != null;
			this.methodName = methodName;
			this.rwChildren.add(rcvr);
			this.rwChildren.addAll(arguments);
		}
		
		/** Returns the receiver of the method call.  
		 *  i.e., for a method call {@code a.b(c,d)} returns {@code a}. */
		public Expr receiver() { return (Expr) this.rwChildren.get(0); }

		/** Changes the receiver of the method call.  
		 *  i.e., for a method call {@code a.b(c,d)} changes {@code a}. */
		public void setReceiver(Expr rcvr) { this.rwChildren.set(0, rcvr); }
		
		/** Returns all arguments to the method, <b>including the receiver.</b> 
		 * 	i.e, for a method call {@code a.b(c,d)} returns {@code [a, c, d]} */
		public List<Expr> allArguments() 
		{ 
			ArrayList<Expr> result = new ArrayList<Expr>();
			for (Ast chi : this.rwChildren)
				result.add((Expr) chi);
			return Collections.unmodifiableList(result);
		}
		
		/** Returns all arguments to the method, without the receiver.
		 * 	i.e, for a method call {@code a.b(c,d)} returns {@code [c, d]} */
		public List<Expr> argumentsWithoutReceiver()
		{
			ArrayList<Expr> result = new ArrayList<Expr>();
			for (int i = 1; i < this.rwChildren.size(); i++)
				result.add((Expr) this.rwChildren.get(i));
			return Collections.unmodifiableList(result);
		}
		

		@Override
		public <R, A> R accept(ExprVisitor<R, A> visitor, A arg) {
			return visitor.methodCall(this, arg);
		}

		public List<Expr> deepCopyArguments() {
			
			ArrayList<Expr> result = new ArrayList<Expr>();
			
			for (final Expr expr : argumentsWithoutReceiver()) {
				result.add((Expr) expr.deepCopy());
			}
			
			return result;
			
		}
		
		@Override
		public MethodCallExpr deepCopy() {
			return postCopy(new MethodCallExpr((Expr) receiver().deepCopy(), methodName, deepCopyArguments()));
		}
		
	}
	
	// _________________________________________________________________
	// Statements	
	
	/** Interface for all statements */
	public static abstract class Stmt extends Ast {
		protected Stmt(int fixedCount) {
			super(fixedCount);
		} 
	}

	/** Represents an empty statement: has no effect. */
	public static class Nop extends Stmt {
		
		public Nop() {
			super(0);
		}
		
		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.nop(this, arg);
		}

		@Override
		public Ast deepCopy() {
			return new Nop();
		}
		
	}
	
	/** An assignment from {@code right()} to the location 
	 *  represented by {@code left()}.
	 */
	public static class Assign extends Stmt {
		
		public Assign(Expr left, Expr right) {
			super(2);
			assert left != null && right != null;
			setLeft(left);
			setRight(right);
		}

		public Expr left() { return (Expr) this.rwChildren.get(0); }
		public void setLeft(Expr node) { this.rwChildren.set(0, node); }

		public Expr right() { return (Expr) this.rwChildren.get(1); }
		public void setRight(Expr node) { this.rwChildren.set(1, node); }

		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.assign(this, arg);
		}

		@Override
		public Ast deepCopy() {
			return new Assign((Expr) left().deepCopy(), (Expr) right().deepCopy());
		}
		
	}
	
	public static class IfElse extends Stmt {
		
		public IfElse(Expr cond, Ast then, Ast otherwise) {
			super(3);
			assert cond != null && then != null && otherwise != null;
			setCondition(cond);
			setThen(then);
			setOtherwise(otherwise);
		}
		
		public Expr condition() { return (Expr) this.rwChildren.get(0); }
		public void setCondition(Expr node) { this.rwChildren.set(0, node); }

		public Ast then() { return this.rwChildren.get(1); }
		public void setThen(Ast node) { this.rwChildren.set(1, node); }

		public Ast otherwise() { return this.rwChildren.get(2); }
		public void setOtherwise(Ast node) { this.rwChildren.set(2, node); }

		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.ifElse(this, arg);
		}

		@Override
		public Ast deepCopy() {
			return new IfElse((Expr) condition().deepCopy(), then().deepCopy(), otherwise().deepCopy());
		}
		
	}
	

	public static class ReturnStmt extends Stmt {
		
		public ReturnStmt(Expr arg) {
			super(1);
			setArg(arg);
		}

		public Expr arg() { return (Expr) this.rwChildren.get(0); }
		public void setArg(Expr node) { this.rwChildren.set(0, node); }
		
		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.returnStmt(this, arg);
		}

		@Override
		public Ast deepCopy() {
			return new ReturnStmt(arg() != null ? (Expr) arg().deepCopy() : null);
		}
	
	}
	
	public static class BuiltInWrite extends Stmt {
		
		public BuiltInWrite(Expr arg) {
			super(1);
			assert arg != null;
			setArg(arg);
		}

		public Expr arg() { return (Expr) this.rwChildren.get(0); }
		public void setArg(Expr node) { this.rwChildren.set(0, node); }
		
		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.builtInWrite(this, arg);
		}

		@Override
		public Ast deepCopy() {
			return new BuiltInWrite((Expr) arg().deepCopy());
		}
		
	}
	
	public static class BuiltInWriteln extends Stmt {
		
		public BuiltInWriteln() {
			super(0);
		}
		
		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.builtInWriteln(this, arg);
		}

		@Override
		public Ast deepCopy() {
			return new BuiltInWriteln();
		}
		
	}
	
	public static class MethodCall extends Stmt {
		
		public MethodCall(MethodCallExpr mce) {
			super(1);
			this.rwChildren.set(0, mce);
		}
		
		public MethodCallExpr getMethodCallExpr() {
			return (MethodCallExpr)this.rwChildren.get(0);
		}
		
		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.methodCall(this, arg);
		}

		@Override
		public Ast deepCopy() {
			return new MethodCall(this.getMethodCallExpr().deepCopy());
		}
		
	}	

	public static class WhileLoop extends Stmt {
		
		public WhileLoop(Expr condition, Ast body) {
			super(2);
			assert condition != null && body != null;
			setCondition(condition);
			setBody(body);
		}
		
		public Expr condition() { return (Expr) this.rwChildren.get(0); }
		public void setCondition(Expr cond) { this.rwChildren.set(0, cond); }
		
		public Ast body() { return this.rwChildren.get(1); }
		public void setBody(Ast body) { this.rwChildren.set(1, body); }

		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.whileLoop(this, arg);
		}

		@Override
		public Ast deepCopy() {
			return new WhileLoop((Expr) condition().deepCopy(), body().deepCopy());
		}
	}
		
	// _________________________________________________________________
	// Declarations
	
	/** Interface for all declarations */
	public static abstract class Decl extends Ast {
		protected Decl(int fixedCount) {
			super(fixedCount);
		} 
	}
	
	public static class VarDecl extends Decl {
		
		public String type;
		public String name;
		public VariableSymbol sym;
		
		public VarDecl(String type, String name) {
			this(0, type, name);
		}
		
		protected VarDecl(int num, String type, String name) {
			super(num);
			this.type = type;
			this.name = name;
		}

		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.varDecl(this, arg);
		}

		@Override
		public Ast deepCopy() {
			return new VarDecl(type, name);
		}		
		
	}
	
	/** Used in {@link MethodDecl} to group together declarations
	 *  and method bodies. */
	public static class Seq extends Decl {
		
		public Seq(List<Ast> nodes) {
			super(-1);
			if (nodes != null) this.rwChildren.addAll(nodes);
		}
		
		/** Grant access to the raw list of children for seq nodes */
		public List<Ast> rwChildren() {
			return this.rwChildren;
		}

		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.seq(this, arg);
		}

		@Override
		public Ast deepCopy() {
			
			List<Ast> result = new ArrayList<Ast>();
				
			for (final Ast ast : this.rwChildren) {
				result.add(ast.deepCopy());
			}
			
			return new Seq(result);
			
		}
	}
	
	public static class MethodDecl extends Decl {
		
		public String returnType;
		public String name;
		public List<String> argumentTypes;
		public List<String> argumentNames;
		public MethodSymbol sym;
		public ControlFlowGraph cfg;
		
		public MethodDecl(
				String returnType, 
				String name,
				List<Pair<String>> formalParams,
				Seq decls,
				Seq body) {
			this(returnType, name, Pair.unzipA(formalParams), Pair.unzipB(formalParams), decls, body);
		}
		public MethodDecl(
				String returnType, 
				String name,
				List<String> argumentTypes, 
				List<String> argumentNames,
				Seq decls,
				Seq body) {
			super(2);
			this.returnType = returnType;
			this.name = name;
			this.argumentTypes = argumentTypes;
			this.argumentNames = argumentNames;
			setDecls(decls);
			setBody(body);
		}
		
		public Seq decls() { return (Seq) this.rwChildren.get(0); }
		public void setDecls(Seq decls) { this.rwChildren.set(0, decls); }
		
		public Seq body() { return (Seq) this.rwChildren.get(1); }
		public void setBody(Seq body) { this.rwChildren.set(1, body); }

		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.methodDecl(this, arg);
		}
		
		@Override
		public Ast deepCopy() {
			return new MethodDecl(
					returnType, 
					name, 
					Collections.unmodifiableList(argumentTypes), 
					Collections.unmodifiableList(argumentNames), 
					(Seq) decls().deepCopy(),
					(Seq) body().deepCopy());
		}
		
	}
	
	public static class ClassDecl extends Decl {
		
		public String name;
		public String superClass;
		public ClassSymbol sym;
		
		public ClassDecl(
				String name, 
				String superClass, 
				List<? extends Ast> members) {
			super(-1);
			this.name = name;
			this.superClass = superClass;
			this.rwChildren.addAll(members);
		}
		
		public List<Ast> members() { 
			return Collections.unmodifiableList(this.rwChildren); 
		}
		
		public List<VarDecl> fields() { 
			return childrenOfType(VarDecl.class); 
		} // includes constants!

		public List<MethodDecl> methods() { 
			return childrenOfType(MethodDecl.class); 
		}

		@Override
		public <R, A> R accept(AstVisitor<R, A> visitor, A arg) {
			return visitor.classDecl(this, arg);
		}

		@Override
		public Ast deepCopy() {
			
			List<Ast> result = new ArrayList<Ast>();
			
			for (final Ast ast : members()) {
				result.add(ast.deepCopy());
			}
			
			return new ClassDecl(name, superClass, result);
		}
		
	}
}
