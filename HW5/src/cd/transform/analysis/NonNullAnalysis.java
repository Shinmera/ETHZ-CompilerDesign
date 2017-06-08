package cd.transform.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cd.ToDoException;
import cd.ir.Ast.Assign;
import cd.ir.Ast.BuiltInRead;
import cd.ir.Ast.Expr;
import cd.ir.Ast.Field;
import cd.ir.Ast.Index;
import cd.ir.Ast.MethodCallExpr;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.NewArray;
import cd.ir.Ast.NewObject;
import cd.ir.Ast.NullConst;
import cd.ir.Ast.Stmt;
import cd.ir.Ast.ThisRef;
import cd.ir.Ast.Var;
import cd.ir.BasicBlock;
import cd.ir.Symbol.VariableSymbol;
import cd.transform.NNLocalAnalysisVisitor;
import cd.transform.NonNullAnalysisVisitor;
import cd.transform.analysis.ReachingDefsAnalysis.Def;

/**
 * A data-flow analysis that determines if a variable is guaranteed to be
 * non-<code>null</code> at a given point in the program. The state of this
 * analysis represents the set of non-<code>null</code> variables.
 */
public class NonNullAnalysis extends DataFlowAnalysis<Set<VariableSymbol>> {

	// Before-sets for local analysis.
	private Map<Stmt, Set<VariableSymbol>> stmtStates = new HashMap<Stmt, Set<VariableSymbol>>();
	private Map<BasicBlock, Set<VariableSymbol>> stmtBeforeConditionStates = new HashMap<BasicBlock, Set<VariableSymbol>>();

	// gen and kill sets
	public Map<BasicBlock, Set<VariableSymbol>> gen = new HashMap<BasicBlock, Set<VariableSymbol>>();
	public Map<BasicBlock, Set<VariableSymbol>> kill = new HashMap<BasicBlock, Set<VariableSymbol>>();
	public Map<BasicBlock, Set<VariableSymbol>> undefined = new HashMap<BasicBlock, Set<VariableSymbol>>();

	// Statements with unresolved variables: we don't know yet if they are in
	// gen or in kill,
	// e.g x = y.
	public Map<BasicBlock, Map<Stmt, Map<VariableSymbol, VariableSymbol>>> toResolve = new HashMap<BasicBlock, Map<Stmt, Map<VariableSymbol, VariableSymbol>>>();

	public NonNullAnalysis(MethodDecl method) {
		super(method.cfg);
		if (method.cfg == null)
			throw new IllegalArgumentException("method is missing CFG");

		for (BasicBlock block : cfg.allBlocks) {

			// Visit statements of each block and generate gen, kill and
			// toResolve sets.
			NonNullAnalysisVisitor visitor = new NonNullAnalysisVisitor(block, this);

			// Add to gen set of this block.
			Set<VariableSymbol> gen = new HashSet<VariableSymbol>();
			gen.addAll(visitor.gen.values());
			this.gen.put(block, gen);

			// Add to kill set of this block.
			Set<VariableSymbol> kill = new HashSet<VariableSymbol>();
			kill.addAll(visitor.kill.values());
			this.kill.put(block, kill);

			// Add to toResolve set of this block.
			if (visitor.unresolvedStmtsInBlock.get(block) != null) {
				this.toResolve.put(block, visitor.unresolvedStmtsInBlock.get(block));
			}

			// Get all unresolved variables from all precedessors.
			Set<VariableSymbol> undefined = getUndefinedInPrecedessors(block, new HashSet<VariableSymbol>(),
					new HashSet<BasicBlock>(), true);

			this.undefined.put(block, undefined);
		}

		// Do the global and local analysis.
		iterate();
	}

	/**
	 * Recursively loop through all precedessors and get their unresolved
	 * variables.
	 * 
	 * @param block
	 * @param undefined
	 * @param visited
	 * @param first
	 * @return
	 */
	protected Set<VariableSymbol> getUndefinedInPrecedessors(BasicBlock block, Set<VariableSymbol> undefined,
			Set<BasicBlock> visited, boolean first) {

		visited.add(block);

		if (!first) {
			if (toResolve.get(block) != null) {
				for (Map.Entry<Stmt, Map<VariableSymbol, VariableSymbol>> unresEntry : toResolve.get(block)
						.entrySet()) {
					for (Map.Entry<VariableSymbol, VariableSymbol> entry : unresEntry.getValue().entrySet()) {
						undefined.add(entry.getKey());
						undefined.add(entry.getValue());
					}
				}
			}
		}

		for (BasicBlock precedessor : block.predecessors) {

			if (visited.contains(precedessor)) {
				break;
			}

			undefined.addAll(getUndefinedInPrecedessors(precedessor, undefined, visited, false));
		}

		return undefined;
	}

	/**
	 * Local per-block analysis. This function is called on each block in the
	 * global analysis.
	 * 
	 * @param block
	 * @return
	 */
	private Set<VariableSymbol> doLocalBlockAnalysis(BasicBlock block, Set<VariableSymbol> out) {

		// in-state of the block
		Set<VariableSymbol> state = inStateOf(block);

		// Local analysis.
		NNLocalAnalysisVisitor visitor = new NNLocalAnalysisVisitor(block, state, out, this);

		// Set of variables that are guaranteed to be non-null before
		// each statement.
		stmtStates.putAll(visitor.stmtStates);

		// The latest state is the one before the condition statement.
		stmtBeforeConditionStates.put(block, visitor.state);

		// Update statements with unresolved variables in this block.
		Map<Stmt, Map<VariableSymbol, VariableSymbol>> unresolved = visitor.unresolved;
		toResolve.put(block, unresolved);

		// Add the latest state to the out-set of this block.
		return out;
	}

	@Override
	protected Set<VariableSymbol> initialState() {

		return new HashSet<VariableSymbol>();
	}

	@Override
	protected Set<VariableSymbol> startState() {

		return new HashSet<VariableSymbol>();
	}

	@Override
	protected Set<VariableSymbol> transferFunction(BasicBlock block, Set<VariableSymbol> inState) {

		Set<VariableSymbol> out = new HashSet<VariableSymbol>();
		out.addAll(inState);

		// Remove all variables in the kill set.
		out.removeAll(kill.get(block));

		// Kill set of this block
		Set<VariableSymbol> KillOfBlock = kill.get(block);

		// Get unresolved variable symbols (not in gen or kill).
		Map<Stmt, Map<VariableSymbol, VariableSymbol>> unresolved = new HashMap<Stmt, Map<VariableSymbol, VariableSymbol>>(
				toResolve.get(block));
		if (unresolved != null) {

			// Check statements with unresolved variable symbols by looping
			// through precedessors
			// and checking if the are in gen. They must not occur in any kill
			// set on the way up before they occur in a gen set.
			for (Map.Entry<Stmt, Map<VariableSymbol, VariableSymbol>> entry : unresolved.entrySet()) {

				Map<VariableSymbol, VariableSymbol> entryVal = entry.getValue();
				for (Map.Entry<VariableSymbol, VariableSymbol> stmtEntry : entryVal.entrySet()) {
					boolean instantiated = checkIfInstantiatedInPredecessor(block, stmtEntry.getValue(),
							new HashSet<BasicBlock>(), true);

					if (instantiated) {
						out.add(stmtEntry.getKey());
						out.add(stmtEntry.getValue());

						toResolve.get(block).remove(entry.getKey());

					} else {
						KillOfBlock.add(stmtEntry.getKey());
					}

					// Check if unresolved variable is initialized in other
					// block.
					// We check if it is initialized in this block in the local
					// analysis.
					if (out.contains(stmtEntry.getValue())) {
						out.add(stmtEntry.getKey());

						// toResolve.get(block).remove(entry.getKey());
					} else {
						if (KillOfBlock.contains(entry.getValue())) {
							out.remove(entry.getKey());
							// toResolve.get(block).remove(entry.getKey());
						}
					}
				}

			}
		}

		kill.put(block, KillOfBlock);

		Set<VariableSymbol> unresolvedInOthers = this.undefined.get(block);

		// Checki if there are any unresolved variables from precedessors and
		// update the out set.
		for (VariableSymbol var : unresolvedInOthers) {
			boolean instantiated = checkIfInstantiatedInPredecessor(block, var, new HashSet<BasicBlock>(), true);

			if (instantiated) {
				out.add(var);
				undefined.remove(var);
			}
		}

		// GEN
		out.addAll(new HashSet(gen.get(block)));

		// Do local analysis for this block.
		out = doLocalBlockAnalysis(block, out);

		return out;
	}

	/**
	 * Checks if in all paths to the precedessors there exists one block with
	 * where the VariableSymbol is in the gen set but not in the kill set.
	 * 
	 * @param block
	 * @param unresolved
	 * @param visited
	 * @param first
	 * @return
	 */
	protected boolean checkIfInstantiatedInPredecessor(BasicBlock block, VariableSymbol unresolved,
			Set<BasicBlock> visited, boolean first) {

		visited.add(block);

		if (kill.get(block).contains(unresolved)) {
			return false;
		}
		if (!first) {
			if (gen.get(block).contains(unresolved)) {
				return true;
			}
		}

		boolean allTrue = false;
		for (BasicBlock precedessor : block.predecessors) {

			if (visited.contains(precedessor)) {
				return true;
				// break;
			}
			allTrue = checkIfInstantiatedInPredecessor(precedessor, unresolved, visited, false);
			if (!allTrue) {
				break;
			}
		}

		return allTrue;
	}

	@Override
	protected Set<VariableSymbol> join(Set<Set<VariableSymbol>> states) {
		Set<VariableSymbol> out = new HashSet<VariableSymbol>();

		// Compute intersection.
		if (0 < states.size()) {

			out.addAll((Set<VariableSymbol>) states.toArray()[0]);

			for (Set<VariableSymbol> set : states) {
				out.retainAll(set);
			}
		}
		return out;
	}

	/**
	 * Returns the set of variables that are guaranteed to be
	 * non-<code>null</code> before the given statement.
	 */
	public Set<VariableSymbol> nonNullBefore(Stmt stmt) {
		return stmtStates.get(stmt);
	}

	/**
	 * Returns the set of variables that are guaranteed to be
	 * non-<code>null</code> before the condition of the given basic block.
	 */
	public Set<VariableSymbol> nonNullBeforeCondition(BasicBlock block) {
		return stmtBeforeConditionStates.get(block);
	}

}
