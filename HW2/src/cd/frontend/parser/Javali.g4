grammar Javali;

@header {
	package cd.frontend.parser;
}

// PARSER RULES
unit
 	: classDecl + EOF
 	;

classDecl
	: 'class' Identifier ('extends' Identifier)? '{' (declaration)* '}'
	;

methodDeclaration
	: returnType Identifier '(' formalParameterList ')' '{' (variableDeclaration)* (statement)* '}'
	;

variableDeclaration 
	: type Identifier (',' Identifier)* ';'
	;

declaration
	: variableDeclaration
	| methodDeclaration
	;

formalParameterList
	:
	| type Identifier (',' type Identifier)*
	;

statement
	: ifStatement
	| whileStatement
	| assignment 
	| write
	| returnStatement
	;

ifStatement
    : 'if' '(' booleanExpression ')' '{' statement '}' ('else' '{' statement '}')?
    ;

whileStatement
    : 'while' '(' booleanExpression ')' '{' statement '}'
    ;
	
assignment
	: Identifier '=' expression ';'
	;

returnStatement
    : 'return' (expression)? ';'
    ;

expression
	: expression '==' expression
	| expression '!=' expression
	| integerExpression ('<=' | '<' | '>=' | '>') integerExpression
	| booleanExpression 
	| integerExpression
	| newExpression
	;

booleanExpression
	: '!' booleanExpression
	| booleanExpression '&&' booleanExpression
	| booleanExpression '||' booleanExpression
	| BooleanLiteral
	;

integerExpression
	: ('+' | '-') integerExpression 
	| integerExpression '*' integerExpression
	| integerExpression '/' integerExpression
	| integerExpression '+' integerExpression
	| integerExpression '-' integerExpression
	| read
	| Identifier
	| IntegerLiteral
	;

newExpression 
	: 'new' ( Identifier '(' ')' 
	| Identifier '[' expression ']'
	| primitiveType '[' expression ']' )
	;

write
	: 'write' '(' expression ')' ';'
	| 'writeln' '(' ')' ';'
	;

read
    : 'read' '(' ')'
    ;

literal
	: IntegerLiteral
	| BooleanLiteral
	| 'null'
	;
	
accessModifier
	: 'public'
	| 'private'
	;
	
type
	: arrayType
	| primitiveType
	| Identifier
	;

primitiveType
	: 'boolean'
	| 'int'
	;

arrayType
	: (primitiveType|Identifier) '[' ']'
	;

returnType
	: 'void'
	| type
	;

// LEXER RULES
TRUE: 'true';
FALSE: 'false';
NULL: 'null';
CLASS: 'class';
EXTENDS: 'extends';
PUBLIC: 'public';
PRIVATE: 'private';
INT: 'int';
BOOLEAN: 'boolean';
VOID: 'void';
NEW: 'new';
WRITE: 'write';
WRITELN: 'writeln';
READ: 'read';
RETURN: 'return';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACKET: '[';
RBRACKET: ']';
PLUS: '+';
MINUS: '-';
TIMES: '*';
DIVIDE: '/';
AND: '&&';
OR: '||';
NOT: '!';
EQUALS: '==';
NEQUALS: '!=';
GEQUALS: '>=';
LEQUALS: '<=';
LESS: '<';
GREATER: '>';
COMMA: ',';
SEMICOLON: ';';
ASSIGN: '=';

Identifier 
	: Letter (Letter|Digit)*
	;

fragment
Letter
	: 'A'..'Z'
	| 'a'..'z'
	;

fragment
Digit
	: '0'..'9'
	;

fragment
HexDigit
	: Digit 
	| 'a'..'f'
	| 'A'..'F'
	;
	
Decimal
	: '0'
	| '1'..'9' (Digit)*
	;

Hex
	: '0' [xX] (HexDigit)+
	;	

IntegerLiteral
	: Hex
	| Decimal
	;
	
BooleanLiteral
	: TRUE
	| FALSE
	;

// comments and white space does not produce tokens:
COMMENT
	: '/*' .*? '*/' -> skip
	;

LINE_COMMENT
	: '//' ~('\n'|'\r')* -> skip
	;

WS
	: (' '|'\r'|'\t'|'\n') -> skip
	;
