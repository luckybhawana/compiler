package wci.frontend.SubsetC.parsers;

import java.util.ArrayList;
import java.util.EnumSet;

import wci.frontend.*;
import wci.frontend.SubsetC.SubsetCTokenType;
import wci.frontend.SubsetC.*;
import wci.intermediate.*;
import wci.intermediate.symtabimpl.*;
import wci.intermediate.typeimpl.*;

import static wci.frontend.SubsetC.SubsetCTokenType.*;
import static wci.frontend.SubsetC.SubsetCErrorCode.*;
import static wci.intermediate.symtabimpl.SymTabKeyImpl.*;
import static wci.intermediate.symtabimpl.DefinitionImpl.*;
import static wci.intermediate.symtabimpl.RoutineCodeImpl.*;

/**
 * <h1>DeclaredRoutineParser</h1>
 *
 * <p>Parse a main program routine or a declared procedure or function.</p>
 *
 * <p>Copyright (c) 2009 by Ronald Mak</p>
 * <p>For instructional purposes only.  No warranties.</p>
 */
public class DeclaredRoutineParser extends DeclarationsParser
{
    /**
     * Constructor.
     * @param parent the parent parser.
     */
    public DeclaredRoutineParser(SubsetCParserTD parent)
    {
        super(parent);
    }

    private static int dummyCounter = 0;  // counter for dummy routine names

    /**
     * Parse a standard subroutine declaration.
     * @param token the initial token.
     * @param parentId the symbol table entry of the parent routine's name.
     * @return the symbol table entry of the declared routine's name.
     * @throws Exception if an error occurred.
     */
    public SymTabEntry parse(Token token, SymTabEntry parentId, Token type, Token identifier)
        throws Exception
    {
        Definition routineDefn = null;
        String dummyName = "DummyProgramName".toLowerCase();
        SymTabEntry routineId = null;
        TokenType routineType = token.getType();

        //initialize
        if(identifier.getText() == "main")
        {
        	routineDefn = DefinitionImpl.PROGRAM;
        }
        else if((SubsetCTokenType) type.getType() == VOID)
        {
        	routineDefn = DefinitionImpl.PROCEDURE;
        }
        else
        {
        	routineDefn = DefinitionImpl.FUNCTION;
        }
        
        

        // Parse the routine name.
        routineId = parseRoutineName(identifier, dummyName);
        routineId.setDefinition(routineDefn);

        token = currentToken();

        // Create new intermediate code for the routine.
        ICode iCode = ICodeFactory.createICode();
        routineId.setAttribute(ROUTINE_ICODE, iCode);
        routineId.setAttribute(ROUTINE_ROUTINES, new ArrayList<SymTabEntry>());

        // Push the routine's new symbol table onto the stack.
        // If it was forwarded, push its existing symbol table.
        if (routineId.getAttribute(ROUTINE_CODE) == FORWARD) {
            SymTab symTab = (SymTab) routineId.getAttribute(ROUTINE_SYMTAB);
            symTabStack.push(symTab);
        }
        else {
            routineId.setAttribute(ROUTINE_SYMTAB, symTabStack.push());
        }
        
        ArrayList<SymTabEntry> subroutines = (ArrayList<SymTabEntry>) parentId.getAttribute(ROUTINE_ROUTINES);
        subroutines.add(routineId);
        
        parseHeader(token, routineId);
        
        
        // Program: Set the program identifier in the symbol table stack.
        if (routineId.getDefinition() == DefinitionImpl.FUNCTION) {
            //symTabStack.setProgramId(routineId);
        	
        	VariableDeclarationsParser variableDeclarationsParser = new VariableDeclarationsParser(this);
        	variableDeclarationsParser.setDefinition(DefinitionImpl.FUNCTION);
        	TypeSpec typeSpec = variableDeclarationsParser.parseTypeSpec(type);
        	
        	token = currentToken();
        	
        	if(typeSpec != null)
        	{
        		TypeForm form = typeSpec.getForm();
        		if((form == TypeFormImpl.ARRAY) || (form == TypeFormImpl.RECORD))
        		{
        			errorHandler.flag(token, INVALID_TYPE, this);
        		}
        	}
        	else
        	{
        		typeSpec = Predefined.undefinedType;
        	}
        	
        	routineId.setTypeSpec(typeSpec);
        	token = currentToken();
        }
        
        token = currentToken();
        
            routineId.setAttribute(ROUTINE_CODE, DECLARED);
            
            BlockParser blockParser = new BlockParser(this);
            ICodeNode rootNode = blockParser.parse(token, routineId);
            iCode.setRoot(rootNode);

        // Pop the routine's symbol table off the stack.
        symTabStack.pop();

        return routineId;
    }

    /**
     * Parse a routine's name.
     * @param token the current token.
     * @param routineDefn how the routine is defined.
     * @param dummyName a dummy name in case of parsing problem.
     * @return the symbol table entry of the declared routine's name.
     * @throws Exception if an error occurred.
     */
    private SymTabEntry parseRoutineName(Token token, String dummyName)
        throws Exception
    {
        SymTabEntry routineId = null;
        
        // Parse the routine name identifier.
        if (token.getType() == IDENTIFIER) {
            String routineName = token.getText().toLowerCase();
            routineId = symTabStack.lookupLocal(routineName);

            // Not already defined locally: Enter into the local symbol table.
            if (routineId == null) {
                routineId = symTabStack.enterLocal(routineName);
            }

            // If already defined, it should be a forward definition.
            else if (routineId.getAttribute(ROUTINE_CODE) != FORWARD) {
                routineId = null;
                errorHandler.flag(token, IDENTIFIER_REDEFINED, this);
            }

            //token = nextToken();  // consume routine name identifier
        }
        else {
            errorHandler.flag(token, MISSING_IDENTIFIER, this);
        }

        // If necessary, create a dummy routine name symbol table entry.
        if (routineId == null) {
            routineId = symTabStack.enterLocal(dummyName);
        }

        return routineId;
    }

    /**
     * Parse a routine's formal parameter list and the function return type.
     * @param token the current token.
     * @param routineId the symbol table entry of the declared routine's name.
     * @throws Exception if an error occurred.
     */
    private void parseHeader(Token token, SymTabEntry routineId)
        throws Exception
    {
        // Parse the routine's formal parameters.
        parseFormalParameters(token, routineId);
        
    }

    // Synchronization set for a formal parameter sublist.
    private static final EnumSet<SubsetCTokenType> PARAMETER_SET =
        IDENTIFIER_SET.clone();
    static {        
        PARAMETER_SET.add(RIGHT_PAREN);
    }

    // Synchronization set for the opening left parenthesis.
    private static final EnumSet<SubsetCTokenType> LEFT_PAREN_SET =
        EnumSet.of(LEFT_PAREN);  

    // Synchronization set for the closing right parenthesis.
    private static final EnumSet<SubsetCTokenType> RIGHT_PAREN_SET =
        LEFT_PAREN_SET.clone();
    static {
        RIGHT_PAREN_SET.remove(LEFT_PAREN);
        RIGHT_PAREN_SET.add(RIGHT_PAREN);
    }

    /**
     * Parse a routine's formal parameter list.
     * @param token the current token.
     * @param routineId the symbol table entry of the declared routine's name.
     * @throws Exception if an error occurred.
     */
    protected void parseFormalParameters(Token token, SymTabEntry routineId)
        throws Exception
    {
        // Parse the formal parameters if there is an opening left parenthesis.
        token = synchronize(LEFT_PAREN_SET);
        if (token.getType() == LEFT_PAREN) {
            token = nextToken();  // consume (

            ArrayList<SymTabEntry> parms = new ArrayList<SymTabEntry>();

            token = synchronize(PARAMETER_SET);
            TokenType tokenType = token.getType();

            // Loop to parse sublists of formal parameter declarations.
            while (IDENTIFIER_SET.contains(tokenType)) {
                parms.add(parseParmSublist(token, routineId));
                token = currentToken();
                tokenType = token.getType();
                
                if((SubsetCTokenType) tokenType == COMMA)
                {
                	token = nextToken();
                	tokenType = token.getType();
                	
                	if(!IDENTIFIER_SET.contains(tokenType))
                	{
                		errorHandler.flag(token, MISSING_IDENTIFIER, this);
                	}
                }
            }
            // Closing right parenthesis.
            if (token.getType() == RIGHT_PAREN) {
                token = nextToken();  // consume )
            }
            else {
                errorHandler.flag(token, MISSING_RIGHT_PAREN, this);
            }

            routineId.setAttribute(ROUTINE_PARMS, parms);
        }
    }

    // Synchronization set to follow a formal parameter identifier.
    private static final EnumSet<SubsetCTokenType> PARAMETER_FOLLOW_SET =
        EnumSet.of(COLON, RIGHT_PAREN, SEMICOLON);
   
    // Synchronization set for the , token.
    private static final EnumSet<SubsetCTokenType> COMMA_SET =
        EnumSet.of(COMMA, COLON, IDENTIFIER, RIGHT_PAREN, SEMICOLON);
   

    /**
     * Parse a sublist of formal parameter declarations.
     * @param token the current token.
     * @param routineId the symbol table entry of the declared routine's name.
     * @return the sublist of symbol table entries for the parm identifiers.
     * @throws Exception if an error occurred.
     */
    private SymTabEntry parseParmSublist(Token token,
                                                    SymTabEntry routineId)
        throws Exception
    {
        boolean isProgram = routineId.getDefinition() == DefinitionImpl.PROGRAM;
        Definition parmDefn = isProgram ? PROGRAM_PARM : null;
        TokenType tokenType = token.getType();

        // VAR or value parameter?
      
            if (!isProgram) {
                parmDefn = VAR_PARM;
            }
            
        // Parse the parameter sublist and its type specification.
        VariableDeclarationsParser variableDeclarationsParser =
            new VariableDeclarationsParser(this);
        variableDeclarationsParser.setDefinition(parmDefn);
        ArrayList<SymTabEntry> sublist = new ArrayList<SymTabEntry>();
            return variableDeclarationsParser.parseParam(token, routineId);
    }
}
