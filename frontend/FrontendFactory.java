package wci.frontend;

import java.util.Arrays;
import wci.frontend.pascal.PascalParserTD;
import wci.frontend.pascal.PascalScanner;
import wci.frontend.SubsetC.SubsetCParserTD;
import wci.frontend.SubsetC.SubsetCScanner;
import java.lang.String;


/**
 * <h1>FrontendFactory</h1>
 *
 * <p>A factory class that creates parsers for specific source languages.</p>
 *
 */
public class FrontendFactory
{
    /**
     * Create a parser.
     * @param language the name of the source language (e.g., "SubsetC").
     * @param type the type of parser (e.g., "top-down").
     * @param source the source object.
     * @return the parser.
     * @throws Exception if an error occurred.
     */
    public static Parser createParser(String language, String type,
                                      Source source)
        throws Exception
    {
        if (language.equalsIgnoreCase("Pascal") &&
            type.equalsIgnoreCase("top-down"))
        {
            Scanner scanner = new PascalScanner(source);
            return new PascalParserTD(scanner);
        }
        else if (language.equalsIgnoreCase("SubsetC") &&
                type.equalsIgnoreCase("top-down"))
            {
                Scanner scanner = new SubsetCScanner(source);
                return new SubsetCParserTD(scanner);
            }

        else if (!language.equalsIgnoreCase("Pascal") && (!language.equalsIgnoreCase("SubsetC"))) {
            throw new Exception("Parser factory: Invalid language '" +
                                language + "'");
        }
        else {
            throw new Exception("Parser factory: Invalid type '" +
                                type + "'");
        }
    }
}
