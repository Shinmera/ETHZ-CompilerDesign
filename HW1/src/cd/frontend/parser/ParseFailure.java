package cd.frontend.parser;

public class ParseFailure extends RuntimeException {
	private static final long serialVersionUID = -949992757679367939L;
	public ParseFailure(int line, String string) {
		super(String.format("Parse failure on line %d: %s",
				line, string));
	}
}
