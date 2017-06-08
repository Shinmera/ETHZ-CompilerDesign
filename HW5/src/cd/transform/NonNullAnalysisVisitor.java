package cd.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cd.ir.Ast;
import cd.ir.AstVisitor;
import cd.ir.BasicBlock;
import cd.ir.Ast.Assign;
import cd.ir.Ast.BuiltInRead;
import cd.ir.Ast.Cast;
import cd.ir.Ast.Expr;
import cd.ir.Ast.Field;
import cd.ir.Ast.Index;
import cd.ir.Ast.MethodCallExpr;
import cd.ir.Ast.NewArray;
import cd.ir.Ast.NewObject;
import cd.ir.Ast.NullConst;
import cd.ir.Ast.Stmt;
import cd.ir.Ast.ThisRef;
import cd.ir.Ast.Var;
import cd.ir.Symbol.VariableSymbol;
import cd.transform.analysis.NonNullAnalysis;

public class NonNullAnalysisVisitor extends AstVisitor<VariableSymbol, Ast> {

	public Map<String, VariableSymbol> gen = new HashMap<String, VariableSymbol>();
	public Map<String, VariableSymbol> kill = new HashMap<String, VariableSymbol>();
	public Map<VariableSymbol, VariableSymbol> toResolve = new HashMap<VariableSymbol, VariableSymbol>();
	public Map<Stmt, Map<VariableSymbol, VariableSymbol>> unresolvedStmts = new HashMap<Stmt, Map<VariableSymbol, VariableSymbol>>();
	public Map<BasicBlock, Map<Stmt, Map<VariableSymbol, VariableSymbol>>> unresolvedStmtsInBlock = new HashMap<BasicBlock, Map<Stmt, Map<VariableSymbol, VariableSymbol>>>();
	private NonNullAnalysis analysis;

	/**
	 * For each block loop over statements.
	 * 
	 * @param block
	 * @param analysis
	 */
	public NonNullAnalysisVisitor(BasicBlock block, NonNullAnalysis analysis) {
		this.analysis = analysis;
		unresolvedStmts = analysis.toResolve.get(block);
		if (unresolvedStmts == null) {
			unresolvedStmts = new HashMap<Stmt, Map<VariableSymbol, VariableSymbol>>();
		}

		// Loop over statements.
		for (Stmt stmt : block.stmts) {

			stmt.accept(this, stmt);

		}
		unresolvedStmtsInBlock.put(block, unresolvedStmts);
	}

	/*************
	 * Statements
	 *************/

	/**
	 * Visit assignment statement and check if left is null, non-null or to
	 * resolve.
	 */
	public VariableSymbol assign(Assign ast, Ast parent) {

		Ast right = ast.right();

		VariableSymbol leftSym = ast.left().accept(this, parent);
		VariableSymbol rightSym = ast.right().accept(this, parent);

		if (leftSym != null) {

			// GEN: Get new and this statements.
			if (right instanceof ThisRef || right instanceof NewObject || right instanceof NewArray) {
				gen.put(leftSym.name, leftSym);
			}

			// TORESOLVE
			if (right instanceof Var || right instanceof Cast) {

				toResolve.put(leftSym, rightSym);
				HashMap<VariableSymbol, VariableSymbol> tr = new HashMap<VariableSymbol, VariableSymbol>();
				tr.put(leftSym, rightSym);
				unresolvedStmts.put((Stmt) parent, tr);
			}

			// KILL
			if (right instanceof NullConst || right instanceof Field || right instanceof Index
					|| right instanceof BuiltInRead || right instanceof MethodCallExpr) {
				kill.put(leftSym.name, leftSym);
				gen.remove(leftSym.name, leftSym);
			}
		}

		return null;
	}

	/*************
	 * Expressions
	 *************/

	public VariableSymbol var(Var ast, Ast parent) {

		return ast.sym;
	}

	/**
	 * Gen
	 */

	public VariableSymbol newObject(NewObject ast, Ast parent) {

		return null;
	}

	// typeName [arg]
	public VariableSymbol newArray(NewArray ast, Ast parent) {

		return null;
	}

	public VariableSymbol thisRef(ThisRef ast, Ast parent) {

		return null;
	}

	public VariableSymbol nullConst(NullConst ast, Ast parent) {

		return null;
	}

	/**
	 * The argument of a field access is supposed to be non-null.
	 */
	public VariableSymbol field(Field ast, Ast parent) {

		VariableSymbol arg = ast.arg().accept(this, parent);
		// null if "this"
		if (arg != null) {
			gen.put(arg.name, arg);
		}

		// TODO in local analysis.

		return null;
	}

	/**
	 * 
	 */
	public VariableSymbol index(Index ast, Ast parent) {

		VariableSymbol var = ast.left().accept(this, parent);
		if (var != null) {
			gen.put(var.name, var);
		}
		return null;
	}

	public VariableSymbol builtInRead(BuiltInRead ast, Ast parent) {

		return null;
	}

	/**
	 * The receiver of a method call is supposed to be non-null. e.g. in a.b()
	 * the variable a is non-null.
	 */
	public VariableSymbol methodCall(MethodCallExpr ast, Ast parent) {

		VariableSymbol receiver = ast.receiver().accept(this, parent);
		// null if "this"
		if (receiver != null) {
			gen.put(receiver.name, receiver);
		}

		return null;
	}

	public VariableSymbol cast(Cast ast, Ast parent) {

		VariableSymbol var = ast.arg().accept(this, parent);
		return var;
	}

}
