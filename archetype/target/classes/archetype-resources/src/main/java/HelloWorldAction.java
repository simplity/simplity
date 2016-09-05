#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import ${groupId}.kernel.value.Value;
import ${groupId}.service.ServiceContext;
import ${groupId}.tp.LogicInterface;

/**
 * This is how we say "Hello World"
 *
 * @author James Bond
 *
 */
public class HelloWorldAction implements LogicInterface {

	@Override
	public Value execute(ServiceContext ctx) {
		ctx.setValue("hello", Value.newTextValue("Helloooo World"));
		return Value.VALUE_TRUE;
	}
}
