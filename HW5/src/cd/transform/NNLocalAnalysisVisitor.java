package cd.transform;

import java.util.HashMap;
import java.util.HashSet;
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

public class NNLocalAnalysisVisitor extends AstVisitor<VariableSymbol, Ast> {

	public Map<Stmt, Set<VariableSymbol>> stmtStates = new HashMap<Stmt, Set<VariableSymbol>>();
	public Map<BasicBlock, Set<VariableSymbol>> stmtBeforeConditionStates = new HashMap<BasicBlock, Set<VariableSymbol>>();
	public Set<VariableSymbol> state;
	public NonNullAnalysis analysis;
	Map<VariableSymbol, VariableSymbol> unresolved;

	/**
	 * Loop over the statements and do local analysis.
	 * 
	 * @param block
	 * @param analysis
	 */
	public NNLocalAnalysisVisitor(BasicBlock block, Set<VariableSymbol> state, NonNullAnalysis analysis) {

		this.state = state;
		this.analysis = analysis;

		// Statements to resolve in this block, e.g. x = y
		unresolved = analysis.toResolve.get(block);

		for (Stmt stmt : block.stmts) {

			// Add state before of current statement.
			stmtStates.put(stmt, new HashSet<VariableSymbol>(state));

			// Visit statement.
			stmt.accept(this, stmt);

			System.out.println("stmt: " + stmt.toString() + " state " + state.toString());
		}

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

		// Symbols
		VariableSymbol leftSym = ast.left().accept(this, parent);
		VariableSymbol rightSym = ast.right().accept(this, parent);

		if (leftSym != null) {

			// GEN: Get new and this statements.
			if (right instanceof ThisRef || right instanceof NewObject || right instanceof NewArray) {

				state.add(leftSym);
			}

			// TORESOLVE: Check if variable is contained in state.
			if (right instanceof Var || right instanceof Cast) {

				//if (unresolved.containsKey(right)) {
					if (state.contains(rightSym)) {
						state.add(leftSym);
					} else {
					//	state.remove(leftSym);
					}
				//}
			}

			// KILL
			if (right instanceof NullConst || right instanceof Field || right instanceof Index
					|| right instanceof BuiltInRead || right instanceof MethodCallExpr) {
				state.remove(leftSym);
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
			state.add(arg);
		}

		return null;
	}

	public VariableSymbol index(Index ast, Ast parent) {

		VariableSymbol var = ast.left().accept(this, parent);

		// null if "this"
		if (var != null) {
			state.add(var);
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
			state.add(receiver);
		}

		return null;
	}

	public VariableSymbol cast(Cast ast, Ast parent) {

		VariableSymbol var = ast.arg().accept(this, parent);
		if (var != null) {
			 //state.add(var);
		}

		return var;
	}

}
