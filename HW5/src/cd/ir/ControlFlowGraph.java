package cd.ir;

import java.util.ArrayList;
import java.util.List;

import cd.ir.Ast.Expr;

/** Represents the control flow graph of a single method. */
public class ControlFlowGraph {
	public BasicBlock start, end;
	public final List<BasicBlock> allBlocks = new ArrayList<BasicBlock>();
	
	public int count() {
		return allBlocks.size();
	}
	
	public BasicBlock newBlock() {
		BasicBlock blk = new BasicBlock(count());
		allBlocks.add(blk);
		return blk;
	}
	
	/**
	 * Given a list of basic blocks that do not yet have successors,
	 * merges their control flows into a single successor and returns
	 * the new successor.
	 */
	public BasicBlock join(BasicBlock... pred) {
		BasicBlock result = newBlock();
		for (BasicBlock p : pred) {
			assert p.condition == null;
			assert p.successors.size() == 0;
			p.successors.add(result);
			result.predecessors.add(p);
		}
		return result;
	}
	
	/** 
	 * Terminates {@code blk} so that it evaluates {@code cond},
	 * and creates two new basic blocks, one for the case where
	 * the result is true, and one for the case where the result is
	 * false.
	 */
	public void terminateInCondition(BasicBlock blk, Expr cond) {
		assert blk.condition == null;
		assert blk.successors.size() == 0;
		blk.condition = cond;
		blk.successors.add(newBlock());
		blk.successors.add(newBlock());
		blk.trueSuccessor().predecessors.add(blk);
		blk.falseSuccessor().predecessors.add(blk);
	}

	public void connect(BasicBlock from, BasicBlock to) {
		to.predecessors.add(from);
		from.successors.add(to);
	}
}
