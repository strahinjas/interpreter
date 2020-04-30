package interpreter.runtime;

import interpreter.Interpreter;
import interpreter.ir.Statement;

import java.util.List;

public class RuntimeMethod implements RuntimeCallable
{
	private final Statement.Method method;

	private RuntimeInstance thisInstance;

	public RuntimeMethod(Statement.Method method)
	{
		this.method = method;
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments)
	{
		Environment environment = new Environment(interpreter.getEnvironment());

		if (thisInstance != null)
		{
			environment.define(Interpreter.THIS, thisInstance);
		}

		for (int i = 0; i < arguments.size(); i++)
		{
			environment.define(method.parameters.get(i), arguments.get(i));
		}

		try
		{
			interpreter.execute(method.body, environment);
		}
		catch (Return aReturn)
		{
			return aReturn.value;
		}

		if (!method.isVoid)
		{
			throw new InterpretingException(method.line, "Missing return statement in method '" + method.name + "'");
		}

		return null;
	}

	public void bind(RuntimeInstance instance)
	{
		thisInstance = instance;
	}
}
