package cd.backend.codegen;

import java.util.TreeMap;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.VarDecl;
import cd.ir.Ast.Seq;

// FIXME: Maybe replace this with stuff from the symbols?
public class LiProgram{
    TreeMap<String, LiClass> classes = new TreeMap<String, LiClass>();
    
    public LiProgram(List<ClassDecl> decls){
        for(ClassDecl decl : decls){
            LiClass _class = new LiClass(this, decl);
            classes.put(_class.name, _class);
        }
        for(CLassDecl decl : decls){
            classes.get(decl.name).resolve(decl);
        }
    }

    String methodLocation(String _class, String method){
        LiClass liClass = classes.get(_class);
        if(liClass == null) throw new RuntimeException("Unknown class "+_class);

        LiMethod liMethod = liClass.methods.get(method);
        if(liMethod == null) throw new RuntimeException("Unknown method "+method+" on "+_class);

        return liMethod.location();
    }

    // FIXME: easy accessors for locations of resources.
}

class LiClass{
    public LiProgram program;
    public LiClass parent;
    public String name;
    public TreeMap<String, LiMethod> methods = new TreeMap<String, LiMethod>();
    public TreeMap<String, LiClassVariable> variables = new TreeMap<String, LiClassVariable>();
    public int typeID;

    public LiClass(LiProgram program, ClassDecl decl){
        this.program = program;
        this.name = decl.name;
    }

    public resolve(ClassDecl decl){
        if(decl.superClass != null){
            this.parent = program.classes.get(decl.superClass);
        }
        // Handle inheritance flattening.
        LiClass _parent = this.parent;
        while(_parent != null){
            for(LiMethod method : _parent.methods.values()){
                if(!methods.containsKey(method.name)){
                    methods.put(method.name, method);
                }
            }
            for(LiVariable variable : _parent.variables.values()){
                if(!variables.containsKey(variable.name)){
                    variables.put(variable.name, variable);
                }
            }
            _parent = _parent.parent;
        }
        // Inform children
        for(LiMethod method : methods){method.resolve();}
        for(LiVariable variable : variables){variable.resolve();}
        
        typeID = program.classes.values().position(this);
    }

    public size(){
        // 4 bytes header for the typeID, four bytes for each variable.
        return 4+variables.size()*4;
    }
}

class LiMethod{
    public LiClass _class;
    public String name;
    public TreeMap<String, LiLocalVariable> variables = new TreeMap<String, LiLocalVariable>();

    public LiMethod(LiClass _class, MethodDecl decl){
        this._class = _class;
        
        for(String arg : decl.argumentNames){
            VarDecl decl = new VarDecl("", arg);
            LiLocalVariable var = new LiLocalVariable(this, decl);
            variables.put(var.name, var);
        }

        for(Ast ast : decl.decls().rwChildren()){
            VarDecl decl = (VarDecl) ast;
            LiLocalVariable var = new LiLocalVariable(this, decl);
            variables.put(var.name, var);
        }
    }

    public String offset(){
        return name+"@"+_class.name;
    }

    public void resolve(){
        for(LiVariable var : variables){var.resolve();}
    }
}

abstract class LiVariable{
    public String name;
    public int offset;

    public abstract void resolve();
}

class LiLocalVariable extends LiVariable{
    public LiMethod method;

    public LiLocalVariable(LiMethod method, VarDecl decl){
        this.method = method;
        this.name = decl.name;
    }

    public void resolve(){
        offset = 4*method.variables.values().position(this);
        offset += 4*3; // 4 bytes each for ret, ebp, this.
    }
}

class LiClassVariable extends LiVariable{
    public LiClass _class;
    
    public LiClassVariable(LiClass _class, VarDecl decl){
        this._class = _class;
        this.name = decl.name;
    }

    public void resolve(){
        offset = 4*_class.variables.values().position(this);
    }
}
