package cd.frontend.semantic;

/** 
 * Thrown by the semantic checker when a semantic error is detected
 * in the user's program. */
public class SemanticFailure extends RuntimeException {
	private static final long serialVersionUID = 5375946759285719123L;
	
	public enum Cause {
		/**
		 * Caused by a nested method being found.  Those are not 
		 * supported.
		 */
		NESTED_METHODS_UNSUPPORTED,
		
		/** 
		 * Caused by an assignment to either a final field, {@code this},
		 * or some other kind of expression which cannot be assigned to.
		 * <b>Not</b> used for type errors in assignments, which fall
		 * under {@link #TYPE_ERROR}. */
		NOT_ASSIGNABLE,

		/** The value of a final field is not a compile-time constant */
		NOT_A_CONSTANT_EXPR,

		/** Two variables, fields, methods, or classes with the same name 
		 *  were declared in the same scope */
		DOUBLE_DECLARATION,

		/** A field was accessed that does not exist */
		NO_SUCH_FIELD,
		
		/** A method was called that does not exist */
		NO_SUCH_METHOD,
		
		/** 
		 * A variable or other identifier was used in a method
		 * body which has no corresponding declaration */
		NO_SUCH_VARIABLE,
		
		/** 
		 * A method with a return type is missing a return statement among one of its paths */
		MISSING_RETURN,
		
		/** 
		 * Can occur in many contents:
		 * <ul>
		 * <li> Assignment to a variable from an expression of wrong type
		 * <li> A parameter in a method invocation had the wrong type
		 * <li> A condition of a while loop or if statement was not boolean
		 * <li> A non-array was indexed (i.e., a[i] where a is not an array)
		 * <li> The index was not an int (i.e., a[i] where i is not an int)
		 * <li> Arithmetic operators (+,-,etc) used with non-int type
		 * <li> Boolean operators (!,&&,||,etc) used with non-boolean type
		 * <li> Method or field accessed on non-object type
		 * <li> {@code write()} is used with non-int argument
		 * <li> An invalid cast was detected (i.e., a cast to a type that
		 * is not a super- or sub-type of the original type).
		 * <li> The size of an array in a new expression was not an
		 * int (i.e., new A[i] where i is not an int)
		 * </ul>
		 */
		TYPE_ERROR,
		
		/**
		 * A class is its own super class
		 */
		CIRCULAR_INHERITANCE,
		
		/**
		 * One of the following:
		 * <ul>
		 * <li>No class {@code Main}
		 * <li>Class {@code Main} does not have a method {@code main()}
		 * <li>The method {@code main} has > 0 parameters.
		 * <li>The method {@code main} has a non-void return type.
		 * </ul>
		 */
		INVALID_START_POINT,
		
		/**
		 * A class {@code Object} was defined.  This class is implicitly
		 * defined and cannot be defined explicitly.
		 */
		OBJECT_CLASS_DEFINED,
		
		/**
		 * A type name was found for which no class declaration exists.
		 * This can occur in many contexts:
		 * <ul>
		 * <li>The extends clause on a class declaration.
		 * <li>A cast.
		 * <li>A new expression.
		 * </ul>
		 */
		NO_SUCH_TYPE,
		
		/**
		 * The parameters of an overridden method have different types
		 * from the base method, there is a different 
		 * number of parameters, or the return value is different.
		 */
		INVALID_OVERRIDE,
		
		/** A method was called with the wrong number of arguments */
		WRONG_NUMBER_OF_ARGUMENTS,
		
		/** 
		 * Indicates the use of a local variable that may not have been
		 * initialized (ACD only).
		 */
		POSSIBLY_UNINITIALIZED,
	}
	
	public final Cause cause;
	
	public SemanticFailure(Cause cause) {
		super(cause.name());
		this.cause = cause;
	}
	
	public SemanticFailure(Cause cause, String format, Object... args) {
		super(String.format(format, args));
		this.cause = cause;
	}
	
}
