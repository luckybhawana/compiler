package wci.frontend.SubsetC.parsers;

import java.util.EnumSet;
import java.util.ArrayList;

import wci.frontend.*;
import wci.frontend.SubsetC.*;
import wci.intermediate.*;
import wci.intermediate.symtabimpl.*;
import wci.intermediate.typeimpl.*;

import static wci.frontend.SubsetC.SubsetCTokenType.*;
import static wci.frontend.SubsetC.SubsetCErrorCode.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.*;
import static wci.intermediate.icodeimpl.ICodeKeyImpl.*;
import static wci.intermediate.symtabimpl.SymTabKeyImpl.*;

/**
 * <h1>AssignmentStatementParser</h1>
 *
 * <p>Parse a Pascal assignment statement.</p>
 *
 * <p>Copyright (c) 2009 by Ronald Mak</p>
 * <p>For instructional purposes only.  No warranties.</p>
 */
public class AssignmentStatementParser extends StatementParser
{
	
	private boolean isFunctionTarget = false;
	
	private static final EnumSet<SubsetCTokenType> ASSIGNMENT_SET =
			ExpressionParser.EXPR_START_SET.clone();
	static{
		ASSIGNMENT_SET.add(ASSIGNMENT);
		ASSIGNMENT_SET.addAll(StatementParser.STMT_FOLLOW_SET);
	}
    /**
     * Constructor.
     * @param parent the parent parser.
     */
    public AssignmentStatementParser(SubsetCParserTD parent)
    {
        super(parent);
    }
    
    /**
     * Parse an assignment statement.
     * @param token the initial token.
     * @return the root node of the generated parse tree.
     * @throws Exception if an error occurred.
     */
    public ICodeNode parse(Token token)
        throws Exception
    {
        // Create the ASSIGN node.
        ICodeNode assignNode = ICodeFactory.createICodeNode(ASSIGN);

        // Parse the target variable.
        VariableParser variableParser = new VariableParser(this);
        ICodeNode targetNode = variableParser.parse(token);

    
        return parse(assignNode, targetNode);
    }

    /**
     * Parse an assignment statement.
     * @param token the initial token.
     * @return the root node of the generated parse tree.
     * @throws Exception if an error occurred.
     */
    public ICodeNode parse(String functionName, Token token)
        throws Exception
    {
        // Create the ASSIGN node.
        ICodeNode assignNode = ICodeFactory.createICodeNode(ASSIGN);
        
        // Parse the target variable.
        VariableParser variableParser = new VariableParser(this);
        ICodeNode targetNode = variableParser.parseFunctionNameTarget(functionName, token);
        
        return parse(assignNode, targetNode);
    }
    
    private ICodeNode parse(ICodeNode assignNode, ICodeNode targetNode) 
            throws Exception
        {
            Token token = currentToken();

            TypeSpec targetType = targetNode != null ? targetNode.getTypeSpec()
                                                   : Predefined.undefinedType;
            
            // The ASSIGN node adopts the variable node as its first child.
            assignNode.addChild(targetNode);
          
            if (!isFunctionTarget) {
                // Synchronize on the := token.
                token = synchronize(ASSIGNMENT_SET);
                if (token.getType() == ASSIGNMENT) {
                    token = nextToken();  // consume the :=
                }
                else {
                    errorHandler.flag(token, MISSING_ASSIGNMENT, this);
                }
            }

            // Parse the expression.  The ASSIGN node adopts the expression's
            // node as its second child.
            ExpressionParser expressionParser = new ExpressionParser(this);
            
            ICodeNode exprNode = expressionParser.parse(token);
            assignNode.addChild(exprNode);
            
            // Type check: Assignment compatible?
            TypeSpec exprType = exprNode != null ? exprNode.getTypeSpec()
                                                 : Predefined.undefinedType;
            
            if (!TypeChecker.areAssignmentCompatible(targetType, exprType)) {
                errorHandler.flag(token, INCOMPATIBLE_TYPES, this);
            }

            assignNode.setTypeSpec(targetType);


            token = currentToken();
            if (token.getType() != SEMICOLON) {
                errorHandler.flag(token, MISSING_SEMICOLON, this);
            }
            token = nextToken();

            return assignNode;
        }

    /**
     * Parse an assignment to a function name.
     * @param token Token
     * @return ICodeNode
     * @throws Exception
     */
    public ICodeNode parseFunctionNameAssignment(Token token)
        throws Exception
    {
        isFunctionTarget = true;
        
        SymTabEntry routineId = symTabStack.getProgramId();
        
        ArrayList<SymTabEntry> routineIds = (ArrayList<SymTabEntry>) routineId.getAttribute(ROUTINE_ROUTINES);
        
        return parse(routineIds.get(routineIds.size() - 1).getName(),token);
    }

}
