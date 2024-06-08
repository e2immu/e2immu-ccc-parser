/* Generated by: CongoCC Parser Generator. Do not edit.
* Generated Code for StringLiteral Token subclass
* by the ASTToken.java.ftl template
*/
package org.parsers.java.ast;

import org.parsers.java.*;
import static org.parsers.java.Token.TokenType.*;


public class StringLiteral extends Literal {

    /**
    * @return the literal string unescaped without the quotes
    */
    public String getString() {
        return removeEscapesAndQuotes(toString());
    }

    static public String removeEscapesAndQuotes(String content) {
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i < content.length() - 1; i++) {
            char ch = content.charAt(i);
            if (ch != '\\') {
                buf.append(ch);
            } else if (i < content.length() - 2) {
                char nextChar = content.charAt(++i);
                if (nextChar < '0' || nextChar > '7') {
                    switch(nextChar) {
                        case '\\' : 
                            buf.append('\\');
                            break;
                        case 'b' : 
                            buf.append('\b');
                            break;
                        case 't' : 
                            buf.append('\t');
                            break;
                        case 'n' : 
                            buf.append('\n');
                            break;
                        case 'f' : 
                            buf.append('\f');
                            break;
                        case 'r' : 
                            buf.append('\r');
                            break;
                        case '"' : 
                            buf.append('"');
                            break;
                        case '\'' : 
                            buf.append('\'');
                            break;
                    }
                } else {
                    // Deal with this legacy C handling of octals
                    int octal = nextChar - '0';
                    boolean possibly3digits = octal <= 3;
                    if (i < content.length() - 2) {
                        nextChar = content.charAt(i + 1);
                        if (nextChar >= '0' && nextChar <= '7') {
                            octal = octal * 8 + nextChar - '0';
                            i++;
                            if (possibly3digits && i < content.length() - 2) {
                                nextChar = content.charAt(i + 1);
                                if (nextChar >= '0' && nextChar <= '7') {
                                    octal = octal * 8 + nextChar - '0';
                                    i++;
                                }
                            }
                        }
                    }
                    buf.append((char) octal);
                }
            }
        }
        return buf.toString();
    }

    public StringLiteral(TokenType type, JavaLexer tokenSource, int beginOffset, int endOffset) {
        super(type, tokenSource, beginOffset, endOffset);
    }

}

