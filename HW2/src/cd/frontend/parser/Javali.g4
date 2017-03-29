grammar Javali;

@header {
    package cd.frontend.parser;
}

// PARSER RULES
unit
    : classDecl+ EOF
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
    | writeln
    | returnStatement
    ;

ifStatement
    : 'if' '(' expression ')' '{' statement '}' ('else' '{' statement '}')?
    ;

whileStatement
    : 'while' '(' expression ')' '{' statement '}'
    ;
    
assignment
    : Identifier '=' expression ';'
    ;

write
    : 'write' '(' expression ')' ';'
    ;
    
writeln
    : 'writeln' '(' ')' ';'
    ;

returnStatement
    : 'return' (expression)? ';'
    ;

expression
    : logiorExpression
    ;

atom
    : booleanLiteral
    | integerLiteral
    | read
    | modifiedReference
    | '(' expression ')'
    ;

modifiedReference
    : (Identifier | THIS) referenceModifier*
    ;
    
referenceModifier
    : arrayModifier
    | fieldModifier
    | callModifier
    ;

arrayModifier
    : '[' expression ']'
    ;

fieldModifier
    : '.' Identifier
    ;

callModifier
    : '(' ')'
    | '(' expression (',' expression)* ')'
    ;

unaryExpression
    : (PLUS | MINUS | NOT)? atom
    ;

castExpression
    : ('(' referenceType ')')? unaryExpression
    ;

multiplicativeExpression
    : castExpression ((TIMES | DIVIDE | MODULUS) castExpression)*
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

comparativeExpression
    : additiveExpression ((LEQUAL | GEQUAL | LESS | GREATER) additiveExpression)*
    ;

equalityExpression
    : comparativeExpression ((EQUAL | NEQUAL) comparativeExpression)*
    ;

logandExpression
    : equalityExpression (AND equalityExpression)*
    ;

logiorExpression
    : logandExpression (OR logandExpression)*
    ;

read
    : 'read' '(' ')'
    ;

literal
    : integerLiteral
    | booleanLiteral
    | NULL
    ;
    
integerLiteral
    : HexLiteral
    | DecimalLiteral
    ;

booleanLiteral
    : FALSE
    | TRUE
    ;
    
type
    : referenceType
    | primitiveType
    ;

referenceType
    : arrayType
    | Identifier
    ;

primitiveType
    : BOOLEAN
    | INT
    ;

arrayType
    : (primitiveType|Identifier) '[' ']'
    ;

returnType
    : VOID
    | type
    ;

// LEXER RULES
TRUE: 'true';
FALSE: 'false';
NULL: 'null';
CLASS: 'class';
EXTENDS: 'extends';
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
THIS: 'this';
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
MODULUS: '%';
AND: '&&';
OR: '||';
NOT: '!';
EQUAL: '==';
NEQUAL: '!=';
GEQUAL: '>=';
LEQUAL: '<=';
LESS: '<';
GREATER: '>';
COMMA: ',';
SEMICOLON: ';';
ASSIGN: '=';
DOT: '.';

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
    
DecimalLiteral
    : '0'
    | '1'..'9' (Digit)*
    ;

HexLiteral
    : '0' ('x' | 'X') (HexDigit)+
    ;

Identifier 
    : Letter (Letter|Digit)*
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
