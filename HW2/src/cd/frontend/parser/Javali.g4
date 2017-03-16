grammar Javali; // parser grammar, parses streams of tokens

@header {
	// Java header
	package cd.frontend.parser;
}



// PARSER RULES

//* // TODO: declare appropriate parser rules
//* // NOTE: Remove //* from the beginning of each line.
//* 
//* unit
//* 	: classDecl+ EOF
//* 	;

        


// LEXER RULES
// TODO: provide appropriate lexer rules for numbers and boolean literals



// Java(li) identifiers:
Identifier 
	:	Letter (Letter|Digit)*
	;

fragment
Letter
	:	'A'..'Z'
	|	'a'..'z'
	;

// comments and white space does not produce tokens:
COMMENT
	:	'/*' .*? '*/' -> skip
	;

LINE_COMMENT
	:	'//' ~('\n'|'\r')* -> skip
	;

WS
	:	(' '|'\r'|'\t'|'\n') -> skip
	;
