package cd.transform.analysis;

import java.util.Set;

import cd.ToDoException;
import cd.ir.Ast;
import cd.ir.Ast.Assign;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.Var;
import cd.ir.BasicBlock;
import cd.ir.ControlFlowGraph;
import cd.ir.Symbol.VariableSymbol.Kind;
import cd.transform.analysis.ReachingDefsAnalysis.Def;
import cd.util.debug.AstOneLine;

/**
 * Computes the sets of reaching definitions for each basic block.
 */
public class ReachingDefsAnalysis extends DataFlowAnalysis<Set<Def>> {

	/**
	 * Perform reaching definitions analysis.
	 * 
	 * @param cfg
	 *            {@link ControlFlowGraph} of a {@link MethodDecl}
	 */
	public ReachingDefsAnalysis(ControlFlowGraph cfg) {
		super(cfg);
		
		throw new ToDoException();
	}
	
	@Override
	protected Set<Def> initialState() {
		throw new ToDoException();
	}
	
	@Override
	protected Set<Def> startState() {
		throw new ToDoException();
	}
	
	@Override
	protected Set<Def> transferFunction(BasicBlock block, Set<Def> inState) {
		throw new ToDoException();
	}
	
	@Override
	protected Set<Def> join(Set<Set<Def>> states) {
		throw new ToDoException();
	}
	
	/**
	 * Class representing a definition in the {@link Ast} of a method.
	 */
	public static class Def {
		public final Assign assign;
		public final String target;
		
		/**
		 * Create a {@link Def} from an {@link Assign} of the form <code>var = ...</code>
		 */
		public Def(Assign assign) {
			if (!(assign.left() instanceof Var) || ((Var) assign.left()).sym.kind == Kind.FIELD)
				throw new IllegalArgumentException("definitions must have (local) variable on LHS");
	
			this.assign = assign;
			this.target = ((Var) assign.left()).name;
		}
		
		@Override
		public String toString() {
			return AstOneLine.toString(assign);
		}
	}
}
