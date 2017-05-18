package cd.transform.analysis;

import java.util.Map;
import java.util.Set;

import cd.ToDoException;
import cd.ir.BasicBlock;
import cd.ir.ControlFlowGraph;

/**
 * The abstract superclass of all data-flow analyses. This class provides a framework to
 * implement concrete analyses by providing {@link #initialState()},
 * {@link #startState()}, {@link #transferFunction(BasicBlock, Object)}, and
 * {@link #join(Set)} methods.
 * 
 * @param <State>
 *            The type of states the analysis computes, specified by a concrete subclass.
 *            Typically, this is a set or map type.
 */
public abstract class DataFlowAnalysis<State> {

	protected final ControlFlowGraph cfg;
	private Map<BasicBlock, State> inStates;
	private Map<BasicBlock, State> outStates;
	
	public DataFlowAnalysis(ControlFlowGraph cfg) {
		this.cfg = cfg;
	}
	
	/**
	 * Returns the in-state of basic block <code>block</code>.
	 */
	public State inStateOf(BasicBlock block) {
		return inStates.get(block);
	}
	
	/**
	 * Returns the out-state of basic block <code>block</code>.
	 */
	public State outStateOf(BasicBlock block) {
		return outStates.get(block);
	}
	
	/**
	 * Do forward flow fixed-point iteration until out-states do not change anymore.
	 * Subclasses should call this method in their constructor after the required
	 * initialization.
	 */
	protected void iterate() {
		throw new ToDoException();
	}
	
	/**
	 * Returns the initial state for all blocks except the {@link ControlFlowGraph#start start}
	 * block.
	 */
	protected abstract State initialState();
	
	/**
	 * Returns the initial state for the {@link ControlFlowGraph#start start} block.
	 */
	protected abstract State startState();
	
	/**
	 * Calculates the out-state for a basic block <code>block</code> and an in-state
	 * <code>inState</code>
	 */
	protected abstract State transferFunction(BasicBlock block, State inState);

	/**
	 * Merges together several out-states and returns the in-state for the transfer function.
	 */
	protected abstract State join(Set<State> states);
}
