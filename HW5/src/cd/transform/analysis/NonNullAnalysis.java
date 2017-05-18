package cd.transform.analysis;

import java.util.Set;

import cd.ToDoException;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.Stmt;
import cd.ir.BasicBlock;
import cd.ir.Symbol.VariableSymbol;

/**
 * A data-flow analysis that determines if a variable is guaranteed to be non-<code>null</code> at a
 * given point in the program. The state of this analysis represents the set of
 * non-<code>null</code> variables.
 */
public class NonNullAnalysis extends DataFlowAnalysis<Set<VariableSymbol>> {
	
	public NonNullAnalysis(MethodDecl method) {
		super(method.cfg);
		if(method.cfg == null)
			throw new IllegalArgumentException("method is missing CFG");
		
		throw new ToDoException();
	}
	
	@Override
	protected Set<VariableSymbol> initialState() {
		throw new ToDoException();
	}
	
	@Override
	protected Set<VariableSymbol> startState() {
		throw new ToDoException();
	}
	
	@Override
	protected Set<VariableSymbol> transferFunction(BasicBlock block, Set<VariableSymbol> inState) {
		throw new ToDoException();
	}
	
	@Override
	protected Set<VariableSymbol> join(Set<Set<VariableSymbol>> states) {
		throw new ToDoException();
	}
	
	/**
	 * Returns the set of variables that are guaranteed to be non-<code>null</code> before
	 * the given statement.
	 */
	public Set<VariableSymbol> nonNullBefore(Stmt stmt) {
		throw new ToDoException();
	}
	
	/**
	 * Returns the set of variables that are guaranteed to be non-<code>null</code> before
	 * the condition of the given basic block.
	 */
	public Set<VariableSymbol> nonNullBeforeCondition(BasicBlock block) {
		throw new ToDoException();
	}
}
