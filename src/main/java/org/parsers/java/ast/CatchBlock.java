/* Generated by: CongoCC Parser Generator. Do not edit.
* Generated Code for CatchBlock AST Node type
* by the ASTNode.java.ftl template
*/
package org.parsers.java.ast;

import org.parsers.java.*;
import java.util.*;
import static org.parsers.java.Token.TokenType.*;


public class CatchBlock extends BaseNode {

    public CodeBlock getBlock() {
        return firstChildOfType(CodeBlock.class);
    }

}

