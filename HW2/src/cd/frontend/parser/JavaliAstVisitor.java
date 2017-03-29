package cd.frontend.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;

import cd.frontend.parser.JavaliParser.AdditiveExpressionContext;
import cd.frontend.parser.JavaliParser.AdditiveOperandContext;
import cd.frontend.parser.JavaliParser.AssignmentContext;
import cd.frontend.parser.JavaliParser.AtomContext;
import cd.frontend.parser.JavaliParser.BooleanLiteralContext;
import cd.frontend.parser.JavaliParser.CallStatementContext;
import cd.frontend.parser.JavaliParser.CastExpressionContext;
import cd.frontend.parser.JavaliParser.ClassDeclContext;
import cd.frontend.parser.JavaliParser.ComparativeExpressionContext;
import cd.frontend.parser.JavaliParser.ComparativeOperandContext;
import cd.frontend.parser.JavaliParser.DeclarationContext;
import cd.frontend.parser.JavaliParser.EqualityExpressionContext;
import cd.frontend.parser.JavaliParser.EqualityOperandContext;
import cd.frontend.parser.JavaliParser.ExpressionContext;
import cd.frontend.parser.JavaliParser.FormalParameterListContext;
import cd.frontend.parser.JavaliParser.IfStatementContext;
import cd.frontend.parser.JavaliParser.IntegerLiteralContext;
import cd.frontend.parser.JavaliParser.LogandExpressionContext;
import cd.frontend.parser.JavaliParser.LogiorExpressionContext;
import cd.frontend.parser.JavaliParser.MethodDeclarationContext;
import cd.frontend.parser.JavaliParser.ModifiedReferenceContext;
import cd.frontend.parser.JavaliParser.MultiplicativeExpressionContext;
import cd.frontend.parser.JavaliParser.MultiplicativeOperandContext;
import cd.frontend.parser.JavaliParser.ReadContext;
import cd.frontend.parser.JavaliParser.ReferenceModifierContext;
import cd.frontend.parser.JavaliParser.ReturnStatementContext;
import cd.frontend.parser.JavaliParser.StatementContext;
import cd.frontend.parser.JavaliParser.UnaryExpressionContext;
import cd.frontend.parser.JavaliParser.VariableDeclarationContext;
import cd.frontend.parser.JavaliParser.WhileStatementContext;
import cd.frontend.parser.JavaliParser.WriteContext;
import cd.frontend.parser.JavaliParser.WritelnContext;
import cd.ir.Ast;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.MethodCallExpr;
import cd.util.Pair;

public final class JavaliAstVisitor extends JavaliBaseVisitor<Ast> {
    public List<ClassDecl> classDecls = new ArrayList<>();

    @Override
    public Ast visitClassDecl(ClassDeclContext ctx) {
        TerminalNode name = ctx.Identifier(0);

        String superclass = "Object";
        if (ctx.Identifier(1) != null) {
            superclass = ctx.Identifier(1).getText();
        }

        ArrayList<Ast> declarations = new ArrayList<Ast>();
        for (DeclarationContext c : ctx.declaration()) {
            Ast ast = c.accept(this);
            if(ast instanceof Ast.Seq)
                declarations.addAll(((Ast.Seq)ast).rwChildren());
            else
                declarations.add(ast);
        }

        Ast.ClassDecl decl = new Ast.ClassDecl(name.getText(), 
                                               superclass,
                                               declarations);
        classDecls.add(decl);

        return decl;
    }

    @Override
    public Ast visitMethodDeclaration(MethodDeclarationContext ctx) {
        String returnType = ctx.returnType().getText();

        String name = ctx.Identifier().getText();

        FormalParameterListContext params = ctx.formalParameterList(); 
        ArrayList<Pair<String>> formalParams = new ArrayList<Pair<String>>();
        for(int i=0; i<params.Identifier().size(); i++){
            formalParams.add(new Pair<String>(params.type(i).getText(),
                                              params.Identifier(i).getText()));
        }

        ArrayList<Ast> declarations = new ArrayList<Ast>();
        for (VariableDeclarationContext c : ctx.variableDeclaration()) {
            declarations.addAll(((Ast.Seq)c.accept(this)).rwChildren());
        }

        ArrayList<Ast> statements = new ArrayList<Ast>();
        for (StatementContext c : ctx.statement()) {
            statements.add(c.accept(this));
        }
        
        return new Ast.MethodDecl(returnType,
                                  name,
                                  formalParams,
                                  new Ast.Seq(declarations),
                                  new Ast.Seq(statements));
    }

    @Override
    public Ast visitVariableDeclaration(VariableDeclarationContext ctx) {
        String type = ctx.type().getText();
        ArrayList<Ast> declarations = new ArrayList<Ast>();
        for(TerminalNode identifier : ctx.Identifier()){
            declarations.add(new Ast.VarDecl(type, identifier.getText()));
        }
        return new Ast.Seq(declarations);
    }

    @Override
    public Ast visitReturnStatement(ReturnStatementContext ctx) {
        return new Ast.ReturnStmt((ctx.expression() != null)
                                  ? (Ast.Expr)ctx.expression().accept(this)
                                  : null);
    }

    @Override
    public Ast visitWrite(WriteContext ctx) {
        return new Ast.BuiltInWrite((Ast.Expr)ctx.expression().accept(this));
    }
    
    public Ast visitWriteln(WritelnContext ctx) {
        return new Ast.BuiltInWriteln();
    }

    @Override
    public Ast visitBooleanLiteral(BooleanLiteralContext ctx) {
        return new Ast.BooleanConst((ctx.TRUE() != null)? true : false);
    }

    @Override
    public Ast visitRead(ReadContext ctx) {
        return new Ast.BuiltInRead();
    }

    @Override
    public Ast visitAssignment(AssignmentContext ctx) {
        return new Ast.Assign((Ast.Expr)ctx.modifiedReference().accept(this),
                              (Ast.Expr)ctx.expression().accept(this));
    }

    @Override
    public Ast visitCallStatement(CallStatementContext ctx) {
        return new Ast.MethodCall((MethodCallExpr)ctx.modifiedReference().accept(this));
    }

    @Override
    public Ast visitIfStatement(IfStatementContext ctx) {
        return new Ast.IfElse((Ast.Expr)ctx.expression().accept(this),
                              ctx.statement(0).accept(this),
                              ctx.statement(1).accept(this));
    }

    @Override
    public Ast visitWhileStatement(WhileStatementContext ctx) {
        return new Ast.WhileLoop((Ast.Expr)ctx.expression().accept(this),
                                  ctx.statement().accept(this));
    }

    @Override
    public Ast visitIntegerLiteral(IntegerLiteralContext ctx) {
        return new Ast.IntConst((ctx.DecimalLiteral() != null)
                ? Integer.parseInt(ctx.DecimalLiteral().getText(), 10)
                : Integer.parseInt(ctx.HexLiteral().getText(), 16));
    }

    @Override
    public Ast visitUnaryExpression(UnaryExpressionContext ctx) {
        if(ctx.NOT() != null)
            return new Ast.UnaryOp(Ast.UnaryOp.UOp.U_BOOL_NOT, (Ast.Expr)ctx.atom().accept(this));
        if(ctx.PLUS() != null)
            return new Ast.UnaryOp(Ast.UnaryOp.UOp.U_PLUS, (Ast.Expr)ctx.atom().accept(this));
        if(ctx.MINUS() != null)
            return new Ast.UnaryOp(Ast.UnaryOp.UOp.U_MINUS, (Ast.Expr)ctx.atom().accept(this));
        return ctx.atom().accept(this);
    }
    
    @Override
    public Ast visitCastExpression(CastExpressionContext ctx) {
        if(ctx.referenceType() != null)
            return new Ast.Cast((Ast.Expr)ctx.unaryExpression().accept(this),
                                ctx.referenceType().getText());
        return ctx.unaryExpression().accept(this);
    }

    @Override
    public Ast visitModifiedReference(ModifiedReferenceContext ctx) {
        Ast.Expr left = (ctx.THIS() != null)
                        ? new Ast.ThisRef()
                        : new Ast.Var(ctx.Identifier().getText());
        for(int i=0; i<ctx.referenceModifier().size(); i++){
            ReferenceModifierContext mod = ctx.referenceModifier(i);
            if(mod.arrayModifier() != null){
                left = new Ast.Index(left, (Ast.Expr)mod.arrayModifier().expression().accept(this));
            }else if(mod.fieldModifier() != null){
                // Stupid, stupid.
                if(i+1<ctx.referenceModifier().size() && ctx.referenceModifier(i+1).callModifier() != null){
                    ArrayList<Ast.Expr> args = new ArrayList<Ast.Expr>();
                    for(ExpressionContext expr : ctx.referenceModifier(i+1).callModifier().expression()){
                        args.add((Ast.Expr)expr.accept(this));
                    }
                    left = new Ast.MethodCallExpr(left, mod.fieldModifier().Identifier().getText(), args);
                    i++;
                }else{
                    left = new Ast.Field(left, mod.fieldModifier().Identifier().getText());
                }
            // This can only be called if the call is at first place.
            }else if(mod.callModifier() != null){
                ArrayList<Ast.Expr> args = new ArrayList<Ast.Expr>();
                for(ExpressionContext expr : mod.callModifier().expression()){
                    args.add((Ast.Expr)expr.accept(this));
                }
                left = new Ast.MethodCallExpr(new Ast.ThisRef(), ctx.Identifier().getText(), args);
            }
        }
        return left;
    }

    @Override
    public Ast visitLogandExpression(LogandExpressionContext ctx) {
        Ast.Expr left = (Ast.Expr)ctx.equalityExpression(0).accept(this);
        for(int i=1; i<ctx.equalityExpression().size(); i++){
            EqualityExpressionContext node = ctx.equalityExpression(i);
            left = new Ast.BinaryOp(left,
                                    Ast.BinaryOp.BOp.B_AND,
                                    (Ast.Expr)node.accept(this));
        }
        return left;
    }

    @Override
    public Ast visitLogiorExpression(LogiorExpressionContext ctx) {
        Ast.Expr left = (Ast.Expr)ctx.logandExpression(0).accept(this);
        for(int i=1; i<ctx.logandExpression().size(); i++){
            LogandExpressionContext node = ctx.logandExpression(i);
            left = new Ast.BinaryOp(left,
                                    Ast.BinaryOp.BOp.B_OR,
                                    (Ast.Expr)node.accept(this));
        }
        return left;
    }

    @Override
    public Ast visitMultiplicativeExpression(MultiplicativeExpressionContext ctx) {
        Ast.Expr left = (Ast.Expr)ctx.castExpression(0).accept(this);
        for(int i=1; i<ctx.castExpression().size(); i++){
            CastExpressionContext node = ctx.castExpression(i);
            MultiplicativeOperandContext op = ctx.multiplicativeOperand(i-1);
            left = new Ast.BinaryOp(left,
                                    (op.TIMES() != null)? Ast.BinaryOp.BOp.B_TIMES:
                                    (op.DIVIDE() != null)? Ast.BinaryOp.BOp.B_DIV:
                                    (op.MODULUS() != null)? Ast.BinaryOp.BOp.B_MOD:
                                    null,
                                    (Ast.Expr)node.accept(this));
        }
        return left;
    }

    @Override
    public Ast visitAdditiveExpression(AdditiveExpressionContext ctx) {
        Ast.Expr left = (Ast.Expr)ctx.multiplicativeExpression(0).accept(this);
        for(int i=1; i<ctx.multiplicativeExpression().size(); i++){
            MultiplicativeExpressionContext node = ctx.multiplicativeExpression(i);
            AdditiveOperandContext op = ctx.additiveOperand(i-1);
            left = new Ast.BinaryOp(left,
                                    (op.PLUS() != null)? Ast.BinaryOp.BOp.B_PLUS:
                                    (op.MINUS() != null)? Ast.BinaryOp.BOp.B_MINUS:
                                    null,
                                    (Ast.Expr)node.accept(this));
        }
        return left;
    }

    @Override
    public Ast visitComparativeExpression(ComparativeExpressionContext ctx) {
        Ast.Expr left = (Ast.Expr)ctx.additiveExpression(0).accept(this);
        for(int i=1; i<ctx.additiveExpression().size(); i++){
            AdditiveExpressionContext node = ctx.additiveExpression(i);
            ComparativeOperandContext op = ctx.comparativeOperand(i-1);
            left = new Ast.BinaryOp(left,
                                    (op.LESS() != null)? Ast.BinaryOp.BOp.B_LESS_THAN:
                                    (op.GREATER() != null)? Ast.BinaryOp.BOp.B_GREATER_THAN:
                                    (op.LEQUAL() != null)? Ast.BinaryOp.BOp.B_LESS_OR_EQUAL:
                                    (op.GEQUAL() != null)? Ast.BinaryOp.BOp.B_GREATER_OR_EQUAL:
                                    null,
                                    (Ast.Expr)node.accept(this));
        }
        return left;
    }

    @Override
    public Ast visitEqualityExpression(EqualityExpressionContext ctx) {
        Ast.Expr left = (Ast.Expr)ctx.comparativeExpression(0).accept(this);
        for(int i=1; i<ctx.comparativeExpression().size(); i++){
            ComparativeExpressionContext node = ctx.comparativeExpression(i);
            EqualityOperandContext op = ctx.equalityOperand(i-1);
            left = new Ast.BinaryOp(left,
                                    (op.EQUAL() != null)? Ast.BinaryOp.BOp.B_EQUAL:
                                    (op.NEQUAL() != null)? Ast.BinaryOp.BOp.B_NOT_EQUAL:
                                    null,
                                    (Ast.Expr)node.accept(this));
        }
        return left;
    }

    @Override
    public Ast visitAtom(AtomContext ctx) {
        // No idea why this is necessary, but it was one hell of a piss show
        // to figure out that I had to do this.
        if(ctx.expression() != null)
            return ctx.expression().accept(this);
        else return super.visitAtom(ctx);
    }

}
