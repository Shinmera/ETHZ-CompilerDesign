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

	// Unresolved variables: we don't know yet if they are in gen or in kill,
	// e.g x = y.
	public Map<BasicBlock, Map<VariableSymbol, VariableSymbol>> toResolve = new HashMap<BasicBlock, Map<VariableSymbol, VariableSymbol>>();

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
			this.toResolve.put(block, visitor.toResolve);	

		}

		// Do the global and local analysis.
		iterate();

		// doLocalAnalysis();
	}

	/**
	 * Do local statement analysis within each block.
	 */
	private void doLocalAnalysis() {
		for (BasicBlock block : cfg.allBlocks) {

			// in-state of the block
			Set<VariableSymbol> state = inStateOf(block);

			// Visit all nodes and do local analysis.
			NNLocalAnalysisVisitor visitor = new NNLocalAnalysisVisitor(block, state, this);

			// set of variables that are guaranteed to be non-null before
			// each statement.
			stmtStates.putAll(visitor.stmtStates);

			// The latest state is the one before the condition statement.
			stmtBeforeConditionStates.put(block, visitor.state);
		}

	}

	/**
	 * Local per-block analysis. This function is called on each block in the
	 * global analysis.
	 * 
	 * @param block
	 * @return
	 */
	private Set<VariableSymbol> doLocalBlockAnalysis(BasicBlock block) {
		// in-state of the block
		Set<VariableSymbol> state = inStateOf(block);

		NNLocalAnalysisVisitor visitor = new NNLocalAnalysisVisitor(block, state, this);

		// Set of variables that are guaranteed to be non-null before
		// each statement.
		stmtStates.putAll(visitor.stmtStates);

		// The latest state is the one before the condition statement.
		stmtBeforeConditionStates.put(block, visitor.state);

		// Add the latest state to the out-set of this block.
		return visitor.state;
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

		// KILL
		out.removeAll(kill.get(block));

		// Get unresolved variable symbols (not in gen or kill).
		Map<VariableSymbol, VariableSymbol> unresolved = toResolve.get(block);

		// Kill set of this block
		Set<VariableSymbol> KillOfBlock = kill.get(block);

		// Gen set of this block
		Set<VariableSymbol> GenOfBlock = gen.get(block);

		// Check unresolved variable symbols.
		for (Map.Entry<VariableSymbol, VariableSymbol> entry : unresolved.entrySet()) {

			// Check if unresolved variable is initialized in other block. We
			// check if it is initialized in this block in the local analysis.
			if (out.contains(entry.getValue())) {
				out.add(entry.getKey());
				unresolved.remove(entry);
			}
			// TODO Does this work?
			else {
				if (KillOfBlock.contains(entry.getValue())) {
					out.remove(entry.getKey());
					unresolved.remove(entry);
				}
			}
		}

		// GEN
		out.addAll(gen.get(block));

		// Do local analysis for this block.
		out.addAll(doLocalBlockAnalysis(block));

		System.out.println("Block " + block.toString() + " Kill: " + kill.get(block).toString() + " Gen: "
				+ gen.get(block).toString() + " Out: " + out.toString());
		return out;
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
