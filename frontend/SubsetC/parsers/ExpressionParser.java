package wci.frontend.SubsetC.parsers;

import java.util.EnumSet;
import java.util.HashMap;

import wci.frontend.*;
import wci.frontend.SubsetC.*;
import wci.intermediate.*;
import wci.intermediate.icodeimpl.*;
import wci.intermediate.symtabimpl.*;
import wci.intermediate.typeimpl.*;

import static wci.frontend.SubsetC.SubsetCTokenType.*;
import static wci.frontend.SubsetC.SubsetCTokenType.NOT;
import static wci.frontend.SubsetC.SubsetCErrorCode.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.*;
import static wci.intermediate.icodeimpl.ICodeKeyImpl.*;
import static wci.intermediate.symtabimpl.SymTabKeyImpl.*;
import static wci.intermediate.symtabimpl.DefinitionImpl.*;

/**
 * <h1>ExpressionParser</h1>
 *
 * <p>Parse a SubsetC expression.</p>
 *
 * <p>Copyright (c) 2009 by Ronald Mak</p>
 * <p>For instructional purposes only.  No warranties.</p>
 */
public class ExpressionParser extends StatementParser
{
	
	// Synchronization set for starting an expression.
    static final EnumSet<SubsetCTokenType> EXPR_START_SET =
        EnumSet.of(PLUS, MINUS, IDENTIFIER, INTEGER, FLOAT, STRING,
                   SubsetCTokenType.NOT, LEFT_PAREN);

    /**
     * Constructor.
     * @param parent the parent parser.
     */
    public ExpressionParser(SubsetCParserTD parent)
    {
        super(parent);
    }

 
    /**
     * Parse an expression.
     * @param token the initial token.
     * @return the root node of the generated parse tree.
     * @throws Exception if an error occurred.
     */
    public ICodeNode parse(Token token)
        throws Exception
    {
        return parseExpression(token);
    }

    // Set of relational operators.
    private static final EnumSet<SubsetCTokenType> REL_OPS =
        EnumSet.of(EQUALS,NOT_EQUALS, LESS_THAN, LESS_EQUALS,
                   GREATER_THAN, GREATER_EQUALS);

    // Map relational operator tokens to node types.
    private static final HashMap<SubsetCTokenType, ICodeNodeType>
        REL_OPS_MAP = new HashMap<SubsetCTokenType, ICodeNodeType>();
    static {
        REL_OPS_MAP.put(EQUALS, EQ);
        //REL_OPS_MAP.put(IS_EQUALS, EQ);
        REL_OPS_MAP.put(NOT_EQUALS, NE);
        REL_OPS_MAP.put(LESS_THAN, LT);
        REL_OPS_MAP.put(LESS_EQUALS, LE);
        REL_OPS_MAP.put(GREATER_THAN, GT);
        REL_OPS_MAP.put(GREATER_EQUALS, GE);
    };

    /**
     * Parse an expression.
     * @param token the initial token.
     * @return the root of the generated parse subtree.
     * @throws Exception if an error occurred.
     */
    private ICodeNode parseExpression(Token token)
        throws Exception
    {
        // Parse a simple expression and make the root of its tree
        // the root node.
        ICodeNode rootNode = parseSimpleExpression(token);
        
        TypeSpec resultType = rootNode != null ? rootNode.getTypeSpec() : Predefined.undefinedType;

        token = currentToken();
        
        TokenType tokenType = token.getType();
        // Look for a relational operator.
        if (REL_OPS.contains(tokenType)) {

            // Create a new operator node and adopt the current tree
            // as its first child.
            ICodeNodeType nodeType = REL_OPS_MAP.get(tokenType);
            ICodeNode opNode = ICodeFactory.createICodeNode(nodeType);
            opNode.addChild(rootNode);

            token = nextToken();  // consume the operator

            // Parse the second simple expression.  The operator node adopts
            // the simple expression's tree as its second child.
            ICodeNode simExprNode = parseSimpleExpression(token);
            opNode.addChild(simExprNode);
            
            // The operator node becomes the new root node.
            rootNode = opNode;
            
            TypeSpec simExprType = simExprNode != null ? simExprNode.getTypeSpec() : Predefined.undefinedType;
            
            if(TypeChecker.areComparisonCompatible(resultType, simExprType))
            {
            	resultType = Predefined.booleanType;
            }
            else
            {
            	errorHandler.flag(token, INCOMPATIBLE_TYPES, this);
            	resultType = Predefined.undefinedType;
            }
        }
        
        if(rootNode != null)
        {
        	rootNode.setTypeSpec(resultType);
        }
        
        return rootNode;
    }

    // Set of additive operators.
    private static final EnumSet<SubsetCTokenType> ADD_OPS =
        EnumSet.of(PLUS, MINUS, SubsetCTokenType.OR);

    // Map additive operator tokens to node types.
    private static final HashMap<SubsetCTokenType, ICodeNodeTypeImpl>
        ADD_OPS_OPS_MAP = new HashMap<SubsetCTokenType, ICodeNodeTypeImpl>();
    static {
        ADD_OPS_OPS_MAP.put(PLUS, ADD);
        ADD_OPS_OPS_MAP.put(MINUS, SUBTRACT);
        ADD_OPS_OPS_MAP.put(SubsetCTokenType.OR, ICodeNodeTypeImpl.OR);
    };

    /**
     * Parse a simple expression.
     * @param token the initial token.
     * @return the root of the generated parse subtree.
     * @throws Exception if an error occurred.
     */
    private ICodeNode parseSimpleExpression(Token token)
        throws Exception
    {
        TokenType signToken = null;  // type of leading sign (if any)
        TokenType signType = null;

        // Look for a leading + or - sign.
        TokenType tokenType = token.getType();
        if ((tokenType == PLUS) || (tokenType == MINUS)) {
            signType = tokenType;
            signToken = (TokenType) token; // changed becoz of error
            token = nextToken();  // consume the + or -
        }

        // Parse a term and make the root of its tree the root node.
        ICodeNode rootNode = parseTerm(token);
        
        TypeSpec resultType = rootNode != null ? rootNode.getTypeSpec()
                : Predefined.undefinedType;
        // Type check: Leading sign.
        if ((signType != null) && (!TypeChecker.isIntegerOrReal(resultType))) {
        	errorHandler.flag((Token) signToken, INCOMPATIBLE_TYPES, this); //added (Token)
        }


        // Was there a leading - sign?
        if (signType == MINUS) {

            // Create a NEGATE node and adopt the current tree
            // as its child. The NEGATE node becomes the new root node.
            ICodeNode negateNode = ICodeFactory.createICodeNode(NEGATE);
            negateNode.addChild(rootNode);
            negateNode.setTypeSpec(rootNode.getTypeSpec());
            rootNode = negateNode;
        }

        token = currentToken();
        tokenType = token.getType();
        // Loop over additive operators.
        while (ADD_OPS.contains(tokenType)) {
        	TokenType operator = tokenType;

            // Create a new operator node and adopt the current tree
            // as its first child.
            ICodeNodeType nodeType = ADD_OPS_OPS_MAP.get(tokenType);
            ICodeNode opNode = ICodeFactory.createICodeNode(nodeType);
            opNode.addChild(rootNode);

            token = nextToken();  // consume the operator

            // Parse another term.  The operator node adopts
            // the term's tree as its second child.
            ICodeNode termNode = parseTerm(token);
            opNode.addChild(termNode);
            
            TypeSpec termType = termNode != null ? termNode.getTypeSpec() : Predefined.undefinedType;

            // The operator node becomes the new root node.
            rootNode = opNode;

            switch ((SubsetCTokenType) operator)
            {
            case PLUS:
            case MINUS:
            {
            	if(TypeChecker.areBothInteger(resultType, termType))
            	{
            		resultType = Predefined.integerType;
            	}
            	else if (TypeChecker.isAtLeastOneReal(resultType, termType))
            	{
            		resultType = Predefined.realType;
            	}
            	else
            	{
            		errorHandler.flag(token, INCOMPATIBLE_TYPES, this);
            	}
            	break;
            }
            
            case OR:
            {
            	if (TypeChecker.areBothBoolean(resultType, termType)) {
                    resultType = Predefined.booleanType;
                }
                else {
                    errorHandler.flag(token, INCOMPATIBLE_TYPES, this);
                }

                break;
            }                 
          }
            
           rootNode.setTypeSpec(resultType);
           
           token = currentToken();
           tokenType = token.getType();
            
        }

        return rootNode;
    }

    // Set of multiplicative operators.
    private static final EnumSet<SubsetCTokenType> MULT_OPS =
        EnumSet.of(STAR, SLASH, DIV, SubsetCTokenType.MOD, SubsetCTokenType.AND);

    // Map multiplicative operator tokens to node types.
    private static final HashMap<SubsetCTokenType, ICodeNodeType>
        MULT_OPS_OPS_MAP = new HashMap<SubsetCTokenType, ICodeNodeType>();
    static {
        MULT_OPS_OPS_MAP.put(STAR, MULTIPLY);
        MULT_OPS_OPS_MAP.put(SLASH, FLOAT_DIVIDE);
        MULT_OPS_OPS_MAP.put(DIV, INTEGER_DIVIDE);
        MULT_OPS_OPS_MAP.put(SubsetCTokenType.MOD, ICodeNodeTypeImpl.MOD);
        MULT_OPS_OPS_MAP.put(SubsetCTokenType.AND, ICodeNodeTypeImpl.AND);
    };

    /**
     * Parse a term.
     * @param token the initial token.
     * @return the root of the generated parse subtree.
     * @throws Exception if an error occurred.
     */
    private ICodeNode parseTerm(Token token)
        throws Exception
    {
        // Parse a factor and make its node the root node.
        ICodeNode rootNode = parseFactor(token);
        
        TypeSpec resultType = rootNode != null ? rootNode.getTypeSpec()
                : Predefined.undefinedType;


        token = currentToken();
        TokenType tokenType = token.getType();
        // Loop over multiplicative operators.
        while (MULT_OPS.contains(tokenType)) {
        	
        	TokenType operator = tokenType;

            // Create a new operator node and adopt the current tree
            // as its first child.
            ICodeNodeType nodeType = MULT_OPS_OPS_MAP.get(tokenType);
            ICodeNode opNode = ICodeFactory.createICodeNode(nodeType);
            opNode.addChild(rootNode);

            token = nextToken();  // consume the operator

         // Parse another factor.  The operator node adopts
            // the term's tree as its second child.
            ICodeNode factorNode = parseFactor(token);
            opNode.addChild(factorNode);
            TypeSpec factorType = factorNode != null ? factorNode.getTypeSpec()
                                                     : Predefined.undefinedType;

            // The operator node becomes the new root node.
            rootNode = opNode;

            // Determine the result type.
            switch ((SubsetCTokenType) operator) {

                case STAR: {
                    // Both operands integer ==> integer result.
                    if (TypeChecker.areBothInteger(resultType, factorType)) {
                        resultType = Predefined.integerType;
                    }

                    // Both real operands or one real and one integer operand
                    // ==> real result.
                    else if (TypeChecker.isAtLeastOneReal(resultType,
                                                          factorType)) {
                        resultType = Predefined.realType;
                    }

                    else {
                        errorHandler.flag(token, INCOMPATIBLE_TYPES, this);
                    }

                    break;
                }

                case SLASH: {
                    // All integer and real operand combinations
                    // ==> real result.
                    if (TypeChecker.areBothInteger(resultType, factorType) ||
                        TypeChecker.isAtLeastOneReal(resultType, factorType))
                    {
                        resultType = Predefined.realType;
                    }
                    else {
                        errorHandler.flag(token, INCOMPATIBLE_TYPES, this);
                    }

                    break;
                }

                case AND: {
                    // Both operands boolean ==> boolean result.
                    if (TypeChecker.areBothBoolean(resultType, factorType)) {
                        resultType = Predefined.booleanType;
                    }
                    else {
                        errorHandler.flag(token, INCOMPATIBLE_TYPES, this);
                    }

                    break;
                }
            }
            
            rootNode.setTypeSpec(resultType);
            
            token = currentToken();
            tokenType = token.getType();
        }

        return rootNode;
    }

    /**
     * Parse a factor.
     * @param token the initial token.
     * @return the root of the generated parse subtree.
     * @throws Exception if an error occurred.
     */
    private ICodeNode parseFactor(Token token)
        throws Exception
    {
        TokenType tokenType = token.getType();
        ICodeNode rootNode = null;

        switch ((SubsetCTokenType) tokenType) {

        case IDENTIFIER: {
            return parseIdentifier(token);
        }

        case INTEGER: {
            // Create an INTEGER_CONSTANT node as the root node.
            rootNode = ICodeFactory.createICodeNode(INTEGER_CONSTANT);
            rootNode.setAttribute(VALUE, token.getValue());

            token = nextToken();  // consume the number
            rootNode.setTypeSpec(Predefined.integerType);
            break;
        }

        case FLOAT: {
            // Create an REAL_CONSTANT node as the root node.
            rootNode = ICodeFactory.createICodeNode(REAL_CONSTANT);
            rootNode.setAttribute(VALUE, token.getValue());

            token = nextToken();  // consume the number
            rootNode.setTypeSpec(Predefined.realType);
            break;
        }

        case STRING: {
            String value = (String) token.getValue();

            // Create a STRING_CONSTANT node as the root node.
            rootNode = ICodeFactory.createICodeNode(STRING_CONSTANT);
            rootNode.setAttribute(VALUE, value);

            TypeSpec resultType = value.length() == 1
                                    ? Predefined.charType
                                    : TypeFactory.createStringType(value);

            token = nextToken();  // consume the string

            rootNode.setTypeSpec(resultType);

            break;
        }

        case NOT: {
            token = nextToken();  // consume the NOT

            // Create a NOT node as the root node.
            rootNode = ICodeFactory.createICodeNode(ICodeNodeTypeImpl.NOT);

            // Parse the factor.  The NOT node adopts the
            // factor node as its child.
            ICodeNode factorNode = parseFactor(token);
            rootNode.addChild(factorNode);

            // Type check: The factor must be boolean.
            TypeSpec factorType = factorNode != null
                                      ? factorNode.getTypeSpec()
                                      : Predefined.undefinedType;
            if (!TypeChecker.isBoolean(factorType)) {
                errorHandler.flag(token, INCOMPATIBLE_TYPES, this);
            }

            rootNode.setTypeSpec(Predefined.booleanType);

            break;
        }

        case LEFT_PAREN: {
            token = nextToken();      // consume the (

            // Parse an expression and make its node the root node.
            rootNode = parseExpression(token);
            TypeSpec resultType = rootNode != null
                                      ? rootNode.getTypeSpec()
                                      : Predefined.undefinedType;

            // Look for the matching ) token.
            token = currentToken();
            
            if (token.getType() == RIGHT_PAREN) {
                token = nextToken();  // consume the )
            }
            else {
                errorHandler.flag(token, MISSING_RIGHT_PAREN, this);
            }

            rootNode.setTypeSpec(resultType);

            break;
        }

        default: {
            errorHandler.flag(token, UNEXPECTED_TOKEN, this);
            break;
        }
    }

        return rootNode;
    }


/**
 * Parse an identifier.
 * @param token the current token.
 * @return the root node of the generated parse tree.
 * @throws Exception if an error occurred.
 */
private ICodeNode parseIdentifier(Token token)
    throws Exception
{
    ICodeNode rootNode = null;

    // Look up the identifier in the symbol table stack.
    String name = token.getText().toLowerCase();
    SymTabEntry id = symTabStack.lookup(name);

    // Undefined.
    if (id == null) {
        errorHandler.flag(token, IDENTIFIER_UNDEFINED, this);
        id = symTabStack.enterLocal(name);
        id.setDefinition(UNDEFINED);
        id.setTypeSpec(Predefined.undefinedType);
    }

    Definition defnCode = id.getDefinition();

    switch ((DefinitionImpl) defnCode) {

        case CONSTANT: {
            Object value = id.getAttribute(CONSTANT_VALUE);
            TypeSpec type = id.getTypeSpec();

            if (value instanceof Integer) {
                rootNode = ICodeFactory.createICodeNode(INTEGER_CONSTANT);
                rootNode.setAttribute(VALUE, value);
            }
            else if (value instanceof Float) {
                rootNode = ICodeFactory.createICodeNode(REAL_CONSTANT);
                rootNode.setAttribute(VALUE, value);
            }
            else if (value instanceof String) {
                rootNode = ICodeFactory.createICodeNode(STRING_CONSTANT);
                rootNode.setAttribute(VALUE, value);
            }

            id.appendLineNumber(token.getLineNumber());
            token = nextToken();  // consume the constant identifier

            if (rootNode != null) {
                rootNode.setTypeSpec(type);
            }

            break;
        }

        case FUNCTION: {
            CallParser callParser = new CallParser(this);
            rootNode = callParser.parse(token);
            break;
        }

        default: {
            VariableParser variableParser = new VariableParser(this);
            rootNode = variableParser.parse(token, id);
            break;
        }
    }

    return rootNode;
}
}