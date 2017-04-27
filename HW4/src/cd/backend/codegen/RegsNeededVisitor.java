package cd.backend.codegen;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.HashMap;
import java.util.Map;

import cd.ir.Ast;
import cd.ir.Ast.Assign;
import cd.ir.Ast.BinaryOp;
import cd.ir.Ast.BooleanConst;
import cd.ir.Ast.BuiltInRead;
import cd.ir.Ast.Cast;
import cd.ir.Ast.Expr;
import cd.ir.Ast.Field;
import cd.ir.Ast.Index;
import cd.ir.Ast.IntConst;
import cd.ir.Ast.MethodCallExpr;
import cd.ir.Ast.NewArray;
import cd.ir.Ast.NewObject;
import cd.ir.Ast.NullConst;
import cd.ir.Ast.ThisRef;
import cd.ir.Ast.UnaryOp;
import cd.ir.Ast.Var;
import cd.ir.AstVisitor;

/** 
 * Determines the maximum number of registers 
 * required to execute one subtree. */
public class RegsNeededVisitor extends AstVisitor<Integer, Void> {
	
	public int calc(Ast ast) {
		return visit(ast, null);
	}
	
	private Map<Ast,Integer> memo = new HashMap<Ast, Integer>();
	
	/** 
	 * Override visit() so as to memorize the results and avoid
	 * unnecessary computation
	 */
	@Override
	public Integer visit(Ast ast, Void arg) {
		if (memo.containsKey(ast))
			return memo.get(ast);
		Integer res = ast.accept(this, null);
		memo.put(ast, res);
		return res;		
	}
	
	@Override
	protected Integer dflt(Ast ast, Void arg) {
		// For a non-expression, it suffices to find the
		// maximum registers used by any individual expression.
		int maxReg = 0;
		for (Ast a : ast.children()) {
			maxReg = Math.max(calc(a), maxReg);
		}
		return maxReg;
	}

	@Override
	protected Integer dfltExpr(Expr ast, Void arg) {
		throw new RuntimeException("Should never be used");
	}

	@Override
	public Integer binaryOp(BinaryOp ast, Void arg) {
		int left = calc(ast.left());
		int right = calc(ast.right());
		int ifLeftFirst = max(left, right+1);
		int ifRightFirst = max(left+1, right);
		int overall = min(ifLeftFirst, ifRightFirst);
		return overall;
	}

	@Override
	public Integer assign(Assign ast, Void arg) {
		return max(calc(ast.left()) + 1, calc(ast.right()));
	}
	
	@Override
	public Integer booleanConst(BooleanConst ast, Void arg) {
		return 1;
	}

	@Override
	public Integer builtInRead(BuiltInRead ast, Void arg) {
		return 1;
	}
	
	@Override
	public Integer cast(Cast ast, Void arg) {
		return calc(ast.arg());
	}

	@Override
	public Integer index(Index ast, Void arg) {
		return max(calc(ast.left()), calc(ast.right()) + 1);
	}
	
	@Override
	public Integer field(Field ast, Void arg) {
		return calc(ast.arg());
	}

	@Override
	public Integer intConst(IntConst ast, Void arg) {
		return 1;
	}

	@Override
	public Integer newArray(NewArray ast, Void arg) {
		return calc(ast.arg());
	}

	@Override
	public Integer newObject(NewObject ast, Void arg) {
		return 1;
	}

	@Override
	public Integer nullConst(NullConst ast, Void arg) {
		return 1;
	}

	@Override
	public Integer thisRef(ThisRef ast, Void arg) {
		return 1;
	}
	
	@Override
	public Integer methodCall(MethodCallExpr ast, Void arg) {
		int max = 1;
		for (Expr argex : ast.allArguments()) {
			int needed = calc(argex);
			if (needed > max)
				max = needed;
		}
		return max;
	}

	@Override
	public Integer unaryOp(UnaryOp ast, Void arg) {
		return calc(ast.arg());
	}

	@Override
	public Integer var(Var ast, Void arg) {
		return 1;
	}		
}
