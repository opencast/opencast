grammar WorkflowCondition;

booleanExpression : booleanTerm ( OR booleanExpression )? ;
booleanTerm : booleanValue ( AND booleanTerm )? ;
booleanValue : ( NOT )* ( '(' booleanExpression ')' | relation | BOOL ) ;
relation : relationOperand COMPARISONOPERATOR relationOperand ;
relationOperand : atom ( NUMERICALOPERATOR atom )* | atom ;
atom : '(' relationOperand ')' | NUMBER | STRING | BOOL ;

AND: 'AND' ;
OR: 'OR' ;
NOT: 'NOT' ;
NUMBER: '-'? [0-9]+ ('.' [0-9]+)? ;
COMPARISONOPERATOR: '<=' | '<' | '==' | '>' | '!=' | '>=' ;
NUMERICALOPERATOR: '+' | '-' | '*' | '/' ;
BOOL: 'true' | 'false' ;
STRING : '\'' ( '\'\'' | ~['] )* '\'';
WS: [ \t\r\n]+ -> skip ;

