package cd.transform;

import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.IfElse;
import cd.ir.Ast.WhileLoop;
import cd.ir.Ast.ReturnStmt;
import cd.ir.Ast.Seq;
import cd.ir.Ast.Stmt;
import cd.ir.Ast;
import cd.ir.AstVisitor;
import cd.ir.ControlFlowGraph;
import cd.ir.BasicBlock;

public class CfgBuilder extends AstVisitor<BasicBlock, BasicBlock>{
	
    ControlFlowGraph cfg;
	
    public void build(MethodDecl mdecl){
        cfg = mdecl.cfg = new ControlFlowGraph();
        cfg.start = cfg.newBlock(); // Note: Use newBlock() to create new basic blocks
        cfg.end = cfg.newBlock(); // unique exit block to which all blocks that end with a return stmt. lead

        BasicBlock lastBlock = visit(mdecl, cfg.start);
        if(!cfg.end.predecessors.contains(lastBlock)){
            cfg.connect(lastBlock, cfg.end);
        }
		
        // CFG and AST are not synchronized, only use CFG from now on
        mdecl.setBody(null);
    }

    @Override
    public BasicBlock seq(Seq ast, BasicBlock arg){
        for(Ast child : ast.children())
            arg = visit(child, arg);
        return arg;
    }

    @Override
    public BasicBlock ifElse(IfElse ast, BasicBlock block){
        cfg.terminateInCondition(block, ast.condition());
        
        visit(ast.then(), block.trueSuccessor());
        if(ast.otherwise() != null)
            visit(ast.otherwise(), block.falseSuccessor());
        
        BasicBlock rest = cfg.newBlock();
        cfg.connect(block.trueSuccessor(), rest);
        cfg.connect(block.falseSuccessor(), rest);
        return rest;
    }
    
    @Override
    public BasicBlock whileLoop(WhileLoop ast, BasicBlock block){
        BasicBlock body = cfg.newBlock();

        block.condition = ast.condition();
        cfg.connect(block, body);
        visit(ast.body(), body);

        BasicBlock rest = cfg.newBlock();
        cfg.connect(block, rest);
        return rest;
    }

    @Override
    public BasicBlock returnStmt(ReturnStmt ast, BasicBlock block){
        cfg.connect(block, cfg.end);
        return block;
    }

    @Override
    public BasicBlock dfltStmt(Stmt stmt, BasicBlock block){
        block.stmts.add(stmt);
        return block;
    }
}
