package cd.util.debug;

import static cd.util.debug.DumpUtils.classComparator;
import static cd.util.debug.DumpUtils.methodComparator;
import static cd.util.debug.DumpUtils.sortedStrings;
import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.List;

import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.Stmt;
import cd.ir.BasicBlock;
import cd.transform.analysis.NonNullAnalysis;

/**
 * Dumps the sets of variables that are reported to be non-<code>null</code> by the
 * {@link NonNullAnalysis}. This is done for each basic block, statement and basic block
 * condition in the program. For statements and conditions, the set
 * <strong>before</strong> the statement/condition is dumped (see
 * {@link NonNullAnalysis#isNonNullBefore(cd.ir.Symbol.VariableSymbol, Stmt)},
 * {@link NonNullAnalysis#nonNullBeforeCondition(BasicBlock)}) and for basic block, the
 * set <strong>after</strong> the block (the block's out-state, see
 * {@link NonNullAnalysis#outStateOf(BasicBlock)}) is dumped. Everything is dumped in a
 * well-defined (alphabetic) order, so that two dumps can simply be string-compared.
 */
public class NonNullDump {
	
	public static String toString(List<ClassDecl> astRoots) {
		StringBuilder dump = new StringBuilder();
		sort(astRoots, classComparator);
		for(ClassDecl clazz : astRoots) {
			List<MethodDecl> methods = new ArrayList<>(clazz.methods());
			sort(methods, methodComparator);
			for(MethodDecl method : methods) {
				dump.append(clazz.name).append(".").append(method.name).append("\n");
				
				NonNullAnalysis analysis = new NonNullAnalysis(method);
				for(BasicBlock block : method.cfg.allBlocks) {
					dump.append("  BB").append(block.index).append(" out: ")
					    .append(sortedStrings(analysis.outStateOf(block))).append("\n");
					for(Stmt stmt : block.stmts)
						dump.append("    ").append(AstOneLine.toString(stmt)).append(": ")
						    .append(sortedStrings(analysis.nonNullBefore(stmt))).append("\n");
					if(block.condition != null)
						dump.append("    ").append(AstOneLine.toString(block.condition)).append(": ")
							.append(sortedStrings(analysis.nonNullBeforeCondition(block))).append("\n");
				}
				dump.append("\n\n");
			}
		}
		return dump.toString();
	}
}
