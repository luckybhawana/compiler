package wci.frontend.SubsetC.parsers;

import java.util.ArrayList;
import java.util.EnumSet;

import wci.frontend.*;
import wci.frontend.SubsetC.*;
import wci.intermediate.*;
import wci.intermediate.symtabimpl.*;

import static wci.frontend.SubsetC.SubsetCTokenType.*;
import static wci.frontend.SubsetC.SubsetCErrorCode.*;
import static wci.intermediate.symtabimpl.SymTabKeyImpl.*;
import static wci.intermediate.symtabimpl.DefinitionImpl.*;
import static wci.intermediate.symtabimpl.RoutineCodeImpl.*;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.*;
import static wci.intermediate.icodeimpl.ICodeKeyImpl.*;

/**
 * <h1>ProgramParser</h1>
 *
 * <p>Parse a Pascal program.</p>
 *
 * <p>Copyright (c) 2009 by Ronald Mak</p>
 * <p>For instructional purposes only.  No warranties.</p>
 */
public class ProgramParser extends DeclarationsParser
{
    /**
     * Constructor.
     * @param parent the parent parser.
     */
    public ProgramParser(SubsetCParserTD parent)
    {
        super(parent);
    }

    /**
     * Parse a program.
     * @param token the initial token.
     * @param parentId the symbol table entry of the parent routine's name.
     * @return null
     * @throws Exception if an error occurred.
     */
    public SymTabEntry parse(Token token, SymTabEntry parentId)
        throws Exception
    {

    	// Set up whole program symtab
        ICode iCode = ICodeFactory.createICode();
        SymTabEntry routineId = symTabStack.enterLocal("main".toLowerCase());       
        routineId.setDefinition(DefinitionImpl.PROGRAM);      
        symTabStack.setProgramId(routineId);      
        routineId.setAttribute(ROUTINE_SYMTAB, symTabStack.push());     
        routineId.setAttribute(ROUTINE_ICODE, iCode);
        routineId.setAttribute(ROUTINE_ROUTINES, new ArrayList<SymTabEntry>());
        SymTab symTab = (SymTab) routineId.getAttribute(ROUTINE_SYMTAB);
        symTabStack.push(symTab);

        // Parse the program.
        
        DeclarationsParser declarationParser = new DeclarationsParser(this);
        declarationParser.parse(token, routineId);
        
        ICodeNode callNode = ICodeFactory.createICodeNode(CALL);
        SymTabEntry pfId = symTabStack.lookup("main");
        ICode dumb = ICodeFactory.createICode();
        
        callNode.setAttribute(LINE, currentToken().getLineNumber());
        callNode.setAttribute(ID, pfId);
        dumb.setRoot(callNode);
        routineId.setAttribute(ROUTINE_ICODE, dumb);


        return routineId;
    }
}
