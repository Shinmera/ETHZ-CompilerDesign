package cd.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Symbol {
	
    public final String name;
	
    public static abstract class TypeSymbol extends Symbol {
		
        public TypeSymbol(String name) {
            super(name);
        }

        public abstract boolean isReferenceType();
		
        @Override
        public String toString() {
            return name;
        }
		
        public abstract TypeSymbol getSuperType();
		
        public boolean isSuperTypeOf(TypeSymbol sub) {
            // "void" is not a subtype of any type not even itself
            if(this == PrimitiveTypeSymbol.voidType || sub == PrimitiveTypeSymbol.voidType)
                return false;
            
            if (sub == this)
                return true;
			
            if (this instanceof PrimitiveTypeSymbol || sub instanceof PrimitiveTypeSymbol)
                return false; // no hierarchy with primitive types
			
            if (sub == ClassSymbol.nullType && this.isReferenceType())
                return true;
			
            TypeSymbol curr = sub;
            while (curr != null) {
                if (curr == this)
                    return true;
                curr = curr.getSuperType();
            }
            return false;
        }
		
    }
	
    public static class PrimitiveTypeSymbol extends TypeSymbol {
		
        /** Symbols for the built-in primitive types */
        public static final PrimitiveTypeSymbol intType = new PrimitiveTypeSymbol("int");
        public static final PrimitiveTypeSymbol voidType = new PrimitiveTypeSymbol("void");
        public static final PrimitiveTypeSymbol booleanType = new PrimitiveTypeSymbol("boolean");

        public PrimitiveTypeSymbol(String name) {
            super(name);
        }		
		
        @Override
        public boolean isReferenceType() {
            return false;
        }
		
        @Override
        public TypeSymbol getSuperType() {
            throw new RuntimeException("should not call this on PrimitiveTypeSymbol");
        }
    }
	
    public static class ArrayTypeSymbol extends TypeSymbol {
        public final TypeSymbol elementType;
		
        public ArrayTypeSymbol(TypeSymbol elementType) {
            super(elementType.name+"[]");
            this.elementType = elementType;
        }
		
        @Override
        public boolean isReferenceType() {
            return true;
        }
		
        @Override
        public TypeSymbol getSuperType() {
            return ClassSymbol.objectType;
        }
		
    }
	
    public static class ClassSymbol extends TypeSymbol {
        public final Ast.ClassDecl ast;
        public ClassSymbol superClass;
        public final VariableSymbol thisSymbol =
            new VariableSymbol("this", this);
        public final Map<String, VariableSymbol> fields = 
            new HashMap<String, VariableSymbol>();
        public final Map<String, MethodSymbol> methods =
            new HashMap<String, MethodSymbol>();

        public List<VariableSymbol> effectiveFields = new ArrayList<VariableSymbol>();
        public List<MethodSymbol> effectiveMethods = new ArrayList<MethodSymbol>();
        private boolean finalized = false;

        /** Symbols for the built-in Object and null types */
        public static final ClassSymbol nullType = new ClassSymbol("<null>");
        public static final ClassSymbol objectType = new ClassSymbol("Object"); 
		
        public ClassSymbol(Ast.ClassDecl ast) {
            super(ast.name);
            this.ast = ast;
        }
		
        /** Used to create the default {@code Object} 
         *  and {@code <null>} types */
        public ClassSymbol(String name) {
            super(name);
            this.ast = null;
        }
		
        @Override
        public boolean isReferenceType() {
            return true;
        }
		
        @Override
        public TypeSymbol getSuperType() {
            return superClass;
        }
		
        public VariableSymbol getField(String name) {
            VariableSymbol fsym = fields.get(name);
            if (fsym == null && superClass != null)
                return superClass.getField(name);
            return fsym;
        }
		
        public MethodSymbol getMethod(String name) {
            MethodSymbol msym = methods.get(name);
            if (msym == null && superClass != null)
                return superClass.getMethod(name);
            return msym;
        }

        public void finalizeInheritance(){
            if(finalized) return;
            // Flattening the inheritance like this ensures that
            // the order of the methods in each class is preserved.
            // This is important as we need the offset in the VTABLE
            // to a particular method name to be fixed for all
            // subclasses in the hierarchy.
            if(superClass != null){
                superClass.finalizeInheritance();
                effectiveFields.addAll(superClass.effectiveFields);
                effectiveMethods.addAll(superClass.effectiveMethods);
            }
            // Compute effective fields
            for(VariableSymbol field : fields.values()){
                field.parent = this;
                // Handle overriding
                for(int i=0; i<effectiveFields.size(); i++){
                    if(effectiveFields.get(i).name.equals(field.name)){
                        effectiveFields.set(i, field);
                        break;
                    }
                }
                if(!effectiveFields.contains(field)){
                    effectiveFields.add(field);
                }
            }
            // Compute effective methods
            for(MethodSymbol method : methods.values()){
                method.parent = this;
                method.finalizeInheritance();
                // Handle overriding
                for(int i=0; i<effectiveMethods.size(); i++){
                    if(effectiveMethods.get(i).name.equals(method.name)){
                        effectiveMethods.set(i, method);
                        break;
                    }
                }
                if(!effectiveMethods.contains(method)){
                    effectiveMethods.add(method);
                }
            }
            
            finalized = true;
        }
    }

    public static class MethodSymbol extends Symbol {
		
        public final Ast.MethodDecl ast;
        public final Map<String, VariableSymbol> locals =
            new HashMap<String, VariableSymbol>();
        public final List<VariableSymbol> parameters =
            new ArrayList<VariableSymbol>();
		
        public TypeSymbol returnType;
        public ClassSymbol parent;
		
        public MethodSymbol(Ast.MethodDecl ast) {
            super(ast.name);
            this.ast = ast;
        }
		
        @Override
        public String toString() {
            return name + "(...)";
        }

        public int getPosition(){
            for(int i=0; i<parent.effectiveMethods.size(); i++){
                if(parent.effectiveMethods.get(i) == this){
                    return i;
                }
            }
            return -1;
        }

        public void finalizeInheritance(){
            for(VariableSymbol var : locals.values()){
                var.parent = this;
            }
            for(VariableSymbol var : parameters){
                var.parent = this;
            }
        }

        public String getLabel(){
            return parent.name+"."+name;
        }
    }
	
    public static class VariableSymbol extends Symbol {
		
        public static enum Kind { PARAM, LOCAL, FIELD };
        public final TypeSymbol type;
        public final Kind kind;
        public Object parent;
		
        public VariableSymbol(String name, TypeSymbol type) {
            this(name, type, Kind.PARAM);
        }

        public VariableSymbol(String name, TypeSymbol type, Kind kind) {
            super(name);
            this.type = type;
            this.kind = kind;		
        }
		
        @Override
        public String toString() {
            return name;
        }

        public int getPosition(){
            switch(kind){
            case FIELD:{
                ClassSymbol symbol = (ClassSymbol)parent;
                for(int i=0; i<symbol.effectiveFields.size(); i++){
                    if(symbol.effectiveFields.get(i) == this){
                        return i;
                    }
                }
                return -1;}
                
            case PARAM:
            case LOCAL:{
                MethodSymbol symbol = (MethodSymbol)parent;
                int i=0;
                for(VariableSymbol var : symbol.parameters){
                    if(var == this){
                        return i;
                    }
                    i++;
                }
                for(VariableSymbol var : symbol.locals.values()){
                    if(var == this){
                        return i;
                    }
                    i++;
                }
                return -1;}
            default:
                return -1;
            }
        }
    }

    protected Symbol(String name) {
        this.name = name;
    }

}
