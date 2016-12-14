package wci.frontend.SubsetC.parsers;

import java.util.EnumSet;

import wci.frontend.*;
import wci.frontend.SubsetC.*;
import wci.intermediate.*;

import static wci.frontend.SubsetC.SubsetCTokenType.*;
import static wci.frontend.SubsetC.SubsetCErrorCode.*;
import static wci.intermediate.symtabimpl.SymTabKeyImpl.*;
import static wci.intermediate.symtabimpl.DefinitionImpl.VARIABLE;

/**
 * <h1>DeclarationsParser</h1>
 *
 * <p>Parse Pascal declarations.</p>
 *
 * <p>Copyright (c) 2009 by Ronald Mak</p>
 * <p>For instructional purposes only.  No warranties.</p>
 */
public class DeclarationsParser extends SubsetCParserTD
{
	private Token type;
	private Token identifier;
   
    static  protected final EnumSet<SubsetCTokenType> IDENTIFIER_SET =
            EnumSet.of(INT,FLOAT, STRING);
   
    
    static protected final EnumSet<SubsetCTokenType> FUNCTION_SET = IDENTIFIER_SET.clone();
    
    static{
    	FUNCTION_SET.add(VOID);
    }
    
    static protected final EnumSet<SubsetCTokenType> DECLARATION_IDENTIFIER_SET = EnumSet.of(IDENTIFIER);
	
    
	 /**
     * Constructor.
     * @param parent the parent parser.
     */
    public DeclarationsParser(SubsetCParserTD parent)
    {
        super(parent);
    }
	
    static final EnumSet<SubsetCTokenType> DECLARATION_START_SET =
        EnumSet.of(CONST, TYPE, VAR, PROCEDURE, FUNCTION);

   static final EnumSet<SubsetCTokenType> TYPE_START_SET =
        DECLARATION_START_SET.clone();
    static {
        TYPE_START_SET.remove(CONST);
    }

    static final EnumSet<SubsetCTokenType> VAR_START_SET =
    		 EnumSet.of(VAR);
    static {
        VAR_START_SET.remove(TYPE);
    }

    /**
     * Parse declarations.
     * To be overridden by the specialized declarations parser subclasses.
     * @param token the initial token.
     * @throws Exception if an error occurred.
     */
    public SymTabEntry parse(Token token, SymTabEntry parentId)
        throws Exception
    {
	
    	token = synchronize(FUNCTION_SET);

        while (FUNCTION_SET.contains(token.getType())) {
        	
        	type = token;
        	token = nextToken();
        	identifier = token;
        	token = nextToken();
        	TokenType tokenType = token.getType();
        	
        	if((SubsetCTokenType) type.getType() == VOID){
        		
            DeclaredRoutineParser routineParser =
                new DeclaredRoutineParser(this);
            routineParser.parse(token, parentId, type, identifier);
        	}
        	else
        	{
        		switch((SubsetCTokenType) tokenType)
        		{
        		case LEFT_PAREN:
        			
        			DeclaredRoutineParser routineParser = new DeclaredRoutineParser(this);
        			routineParser.parse(token, parentId, type, identifier);
        			break;
        		default:
        		
        			VariableDeclarationsParser variableDeclarationsParser = new VariableDeclarationsParser(this);
        			variableDeclarationsParser.setDefinition(VARIABLE);
        			variableDeclarationsParser.parse(token,null, type, identifier);

        		}
        		
        	}
            // Look for one or more semicolons after a definition.
            token = currentToken();
            
        }
        return null;
    }
}
       