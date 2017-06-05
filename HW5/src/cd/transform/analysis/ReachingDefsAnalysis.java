package cd.transform.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cd.ToDoException;
import cd.ir.Ast;
import cd.ir.Ast.Assign;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.Stmt;
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
	private Map<BasicBlock, Set<Def>> gen = new HashMap<BasicBlock, Set<Def>>();
	private Map<BasicBlock, Set<Def>> kill = new HashMap<BasicBlock, Set<Def>>();

	/**
	 * Perform reaching definitions analysis.
	 * 
	 * @param cfg
	 *            {@link ControlFlowGraph} of a {@link MethodDecl}
	 */
	public ReachingDefsAnalysis(ControlFlowGraph cfg) {
		super(cfg);
		// Figure out gens.
		for (BasicBlock block : cfg.allBlocks) {

			// Get all assignment statements.
			Map<String, Def> defs = new HashMap<String, Def>();
			for (Stmt stmt : block.stmts) {
				if (stmt instanceof Assign && ((Assign) stmt).left() instanceof Var) {
					Def def = new Def((Assign) stmt);

					// Replaces all previous definitions if they exist.
					defs.put(def.target, def);
				}
			}

			Set<Def> gen = new HashSet<Def>();
			gen.addAll(defs.values());
			this.gen.put(block, gen);
		}
		// Figure out kills.

		// TODO Kills from own block? e.g. x = 1; x = 2;

		for (BasicBlock block : cfg.allBlocks) {
			Set<Def> kill = new HashSet<Def>();
			for (Def def : gen.get(block)) {

				// Check if definition with same target in other blocks exist.
				for (BasicBlock otherBlock : cfg.allBlocks) {
					if (otherBlock != block) {
						for (Def otherDef : gen.get(otherBlock)) {
							if (def.target.equals(otherDef.target)) {
								kill.add(otherDef);
							}
						}
					}
				}
			}
			this.kill.put(block, kill);
		}

		iterate();
	}

	@Override
	protected Set<Def> initialState() {
		return new HashSet<Def>();
	}

	@Override
	protected Set<Def> startState() {
		return new HashSet<Def>();
	}

	@Override
	protected Set<Def> transferFunction(BasicBlock block, Set<Def> inState) {
		Set<Def> out = new HashSet<Def>();
		out.addAll(inState);
		out.removeAll(kill.get(block));
		out.addAll(gen.get(block));
		return out;
	}

	@Override
	protected Set<Def> join(Set<Set<Def>> states) {
		Set<Def> joined = new HashSet<Def>();
		for (Set<Def> state : states) {
			joined.addAll(state);
		}
		return joined;
	}

	/**
	 * Class representing a definition in the {@link Ast} of a method.
	 */
	public static class Def {
		public final Assign assign;
		public final String target;

		/**
		 * Create a {@link Def} from an {@link Assign} of the form
		 * <code>var = ...</code>
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
