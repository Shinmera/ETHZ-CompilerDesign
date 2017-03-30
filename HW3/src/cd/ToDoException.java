package cd;

/** TAs insert this to mark code that students need to write */
public class ToDoException extends RuntimeException {
	private static final long serialVersionUID = 4054810321239901944L;

	public ToDoException() {	
	}
	
	public ToDoException(String message) {
		super(message);
	}

}
