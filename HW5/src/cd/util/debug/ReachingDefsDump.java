package cd.util.debug;

import static cd.util.debug.DumpUtils.classComparator;
import static cd.util.debug.DumpUtils.methodComparator;
import static cd.util.debug.DumpUtils.sortedStrings;
import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.List;

import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.MethodDecl;
import cd.ir.BasicBlock;
import cd.transform.analysis.ReachingDefsAnalysis;

/**
 * For each basic block in the program, dumps the definitions that reach the end of that
 * block. Everything is dumped in a well-defined (alphabetic) order, so that two dumps can
 * simply be string-compared.
 */
public class ReachingDefsDump {
	
	public static String toString(List<ClassDecl> astRoots) {
		StringBuilder dump = new StringBuilder();
		sort(astRoots, classComparator);
		for(ClassDecl clazz : astRoots) {
			List<MethodDecl> methods = new ArrayList<>(clazz.methods());
			sort(methods, methodComparator);
			for(MethodDecl method : methods) {
				dump.append(clazz.name).append(".").append(method.name).append("\n");
				
				ReachingDefsAnalysis analysis = new ReachingDefsAnalysis(method.cfg);
				for(BasicBlock block : method.cfg.allBlocks) {
					dump.append("  BB").append(block.index).append(" out:\n");
					for(String string : sortedStrings(analysis.outStateOf(block)))
						dump.append("    ").append(string).append("\n");
				}
				dump.append("\n\n");
			}
		}
		return dump.toString();
	}
}
