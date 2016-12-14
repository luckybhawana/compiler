package wci.backend;

import wci.intermediate.ICode;
import wci.intermediate.SymTabStack;
import wci.message.*;

/**
 * <h1>Backend</h1>
 *
 * <p>The framework class that represents the back end component.</p>
 *
 * <p>Copyright (c) 2009 by Ronald Mak</p>
 * <p>For instructional purposes only.  No warranties.</p>
 */
public abstract class Backend implements MessageProducer
{
	protected static SymTabStack symTabStack;
    protected static MessageHandler messageHandler;  // message handler delegate

    static {
        messageHandler = new MessageHandler();
    }

   // protected SymTab symTab;  // symbol table
    protected ICode iCode;    // intermediate code

    /**
     * Getter.
     * @return the intermediate code generated by this parser.
     */
    public ICode getICode()
    {
        return iCode;
    }

    /**
     * Getter.
     * @return the symbol table generated by this parser.
     */
    public SymTabStack getSymTab()
    {
        return symTabStack;
    }

    /**
     * Getter.
     * @return the message handler.
     */
    public MessageHandler getMessageHandler()
    {
        return messageHandler;
    }

    /**
     * Process the intermediate code and the symbol table generated by the
     * parser.  To be implemented by a compiler or an interpreter subclass.
     * @param iCode the intermediate code.
     * @param symTab the symbol table.
     * @throws Exception if an error occurred.
     */
    public abstract void process(ICode iCode, SymTabStack symTabStack)
        throws Exception;

    /**
     * Notify listeners after setting the message.
     * @param message the message to set.
     */
    public void sendMessage(Message message)
    {
        messageHandler.sendMessage(message);
    }

    /**
     * Add a parser message listener.
     * @param listener the message listener to add.
     */
    public void addMessageListener(MessageListener listener)
    {
        messageHandler.addListener(listener);
    }

    /**
     * Remove a parser message listener.
     * @param listener the message listener to remove.
     */
    public void removeMessageListener(MessageListener listener)
    {
        messageHandler.removeListener(listener);
    }
}
