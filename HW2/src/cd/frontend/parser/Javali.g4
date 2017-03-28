grammar Javali; // parser grammar, parses streams of tokens

@header {
	// Java header
	package cd.frontend.parser;
}



// PARSER RULES

//* // TODO: declare appropriate parser rules
//* // NOTE: Remove //* from the beginning of each line.
//* 
 unit
 	: classDecl + EOF
 	;

classDecl:
	 'class' Identifier ('extends' Identifier)? '{' (declaration)* '}';

methodDeclaration: ReturnType Identifier '(' formalParameterList ')' '{' (variableDeclaration)* (statement)* '}';

variableDeclaration :
	Type Identifier (',' Identifier)* ';'
;	

declaration : 
	variableDeclaration
	| methodDeclaration
	;

formalParameterList :
	Type Identifier (',' Type Identifier)*
;


statement:
	'if' '(' booleanExpression ')' 'then' '{' statement '}' 'else' '{' statement '}'
	| 'if' '(' booleanExpression ')' 'then' '{' statement '}'
	| 'while' '(' booleanExpression ')' '{' statement '}'
	| assignment 
	| write
	| 'return' (expression)? ';'
	;
	
assignment:
	Identifier '=' expression ';'
	;
	 

expression:
	expression '==' expression
	| expression '!=' expression
	| integerExpression ('<' | '<=' | '>' | '>=') integerExpression
	| booleanExpression 
	| integerExpression
	| newExpression
;

booleanExpression :
	'!' booleanExpression # NOT
	| booleanExpression '&&' booleanExpression # AND
	| booleanExpression '||' booleanExpression # OR
	| Boolean # BOOL
;

integerExpression : 
 	('+' | '-' ) integerExpression  # UNARY
	| integerExpression '*' integerExpression # MULT
	| integerExpression '/' integerExpression # DIV
	| integerExpression '+' integerExpression # ADD
	| integerExpression '-' integerExpression # SUB
	| Read	# READ
	| Identifier # IDENT
	| Integer # INT
;

newExpression : 
	'new' ( Identifier '(' ')' 
	| Identifier '[' expression ']'
	| PrimitiveType '[' expression ']' )
	;

write : 
	'write' '(' expression ')' ';'
	| 'writeln' '('')' ';'
	;


// LEXER RULES
// TODO: provide appropriate lexer rules for numbers and boolean literals



// Java(li) identifiers:
Identifier 
	:	Letter (Letter|Digit)*
	;
	
Read : 'read()';

fragment
Letter
	:	'A'..'Z'
	|	'a'..'z'
	;
	
//fragment
Digit
	:	'0'..'9'
	;
	
HexDigit :
	 Digit 
	 | 'a'..'f'
	 | 'A'..'F';
	 
Decimal : '0' | '1'..'9' (Digit)*;

Hex : ('0x' | '0X') (HexDigit)+ ;	

Integer : Hex | Decimal ;
	
Boolean:
	'true' | 'false';
	
Literal:
	Integer
	| Boolean
	| 'null';
	
AccessModifier : 'public' | 'private';	

// Types

PrimitiveType : 'boolean' | 'int' ;

Type : PrimitiveType | ReferenceType ;

ReferenceType : Identifier | ArrayType ;

ArrayType : Identifier '[' ']' | PrimitiveType '[' ']' ;

ReturnType : 'void' | Type;	

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
