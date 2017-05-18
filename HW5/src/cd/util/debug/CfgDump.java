package cd.util.debug;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.Stmt;
import cd.ir.BasicBlock;

public class CfgDump {
	
	public static void toString(
			MethodDecl mdecl,			
			String phase, 
			File filename, 
			boolean dumpDominators) 
	{
		if (filename == null) return;
		FileWriter fw;
		try {
			fw = new FileWriter(new File(filename.getAbsolutePath()+phase+".dot"));
			fw.write(toString(mdecl, dumpDominators));
			fw.close();
		} catch (IOException e) {
		}
	}

	public static void toString(
			List<? extends ClassDecl> astRoots, 
			String phase, 
			File filename, 
			boolean dumpDominators) 
	{
		if (filename == null) return;
		FileWriter fw;
		try {
			fw = new FileWriter(new File(filename.getAbsolutePath()+phase+".dot"));
			fw.write(toString(astRoots, dumpDominators));
			fw.close();
		} catch (IOException e) {
		}
	}

	public static String toString(
			List<? extends ClassDecl> astRoots, 
			boolean dumpDominators) 
	{
		return new CfgDump().dump(astRoots, dumpDominators);
	}
	
	public static String toString(
			MethodDecl mdecl,
			boolean dumpDominators) 
	{
		return new CfgDump().dump(mdecl, dumpDominators);
	}
	
	private int indent = 0;
	private final StringBuilder sb = new StringBuilder();
	
	private void append(String format, Object... args){
		String out = String.format(format, args);
		if (out.startsWith("}") || out.startsWith("]")) indent -= 2;
		for (int i = 0; i < indent; i++) sb.append(" ");
		if (out.endsWith("{") || out.endsWith("[")) indent += 2;
		sb.append(String.format(format, args));
		sb.append("\n");
	}

	private String dump(
			MethodDecl mdecl,
			boolean dumpDominators)
	{
		sb.setLength(0);
		append("digraph G {");
		append("graph [ rankdir = \"LR\" ];");
		
		dumpBlocks(dumpDominators, mdecl, "");
		
		append("}");
		
		return sb.toString();		
	}

	private String dump(
			List<? extends ClassDecl> astRoots, 
			boolean dumpDominators) 
	{
		sb.setLength(0);
		append("digraph G {");
		append("graph [ rankdir = \"LR\" ];");
		
		int sgcntr = 0, mcntr = 0;
		for (ClassDecl cdecl : astRoots) {
			//append("subgraph cluster_%d {", sgcntr++);
			//append("label = \"%s\";", cdecl.name);
			
			for (MethodDecl mdecl : cdecl.methods()) {
				append("subgraph cluster_%d {", sgcntr++);
				append("label = \"%s.%s\"", cdecl.name, mdecl.name);
				String m = String.format("M%d_", mcntr++);
				
				dumpBlocks(dumpDominators, mdecl, m);
				
				append("}");
			}
			
			//append("}");
		}
		
		append("}");
		
		return sb.toString();
	}

	private void dumpBlocks(boolean dumpDominators, MethodDecl mdecl, String m) {
		for (BasicBlock blk : mdecl.cfg.allBlocks) {
			append("%sBB%d [", m, blk.index);
			append("shape=\"record\"");
			
			// If we are not just dumping dominators, then build up 
			// a label with all the instructions in the block.
			StringBuilder blklbl = new StringBuilder();					
			blklbl.append(String.format("BB%d", blk.index));
			if (!dumpDominators || true) {
				
				for(Stmt stmt : blk.stmts)
					blklbl.append("|").append(AstOneLine.toString(stmt));
				if(blk.condition != null)
					blklbl.append("|If: " + AstOneLine.toString(blk.condition));
			} 
			String[] replacements = new String[] {
					"<", "\\<",
					">", "\\>",
					"@", "\\@",
					"||", "\\|\\|",
			};
			String blklbls = blklbl.toString();
			for (int i = 0; i < replacements.length; i += 2)
				blklbls = blklbls.replace(replacements[i], replacements[i+1]);
			append("label=\"%s\"", blklbls);
			append("];");
			
			for (int idx = 0; idx < blk.successors.size(); idx++) {
				BasicBlock sblk = blk.successors.get(idx);			
				String edgelbl = (idx == 0 ? "" : " [label=\"False\"]");
				append("%sBB%d -> %sBB%d%s;", 
						m, blk.index, m, sblk.index, edgelbl);
			}
		}
	}
	
}
