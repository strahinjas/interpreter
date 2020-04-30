package interpreter.runtime;

import interpreter.Interpreter;

import java.util.LinkedHashMap;
import java.util.Map;

public class Environment
{
	private final Environment outer;

	private final Map<String, Object> values = new LinkedHashMap<>();

	public Environment()
	{
		this.outer = null;
	}

	public Environment(Environment outer)
	{
		this.outer = outer;
	}

	public Object get(String name)
	{
		if (values.containsKey(name))
		{
			return values.get(name);
		}

		if (values.containsKey(Interpreter.THIS))
		{
			RuntimeInstance instance = (RuntimeInstance) values.get(Interpreter.THIS);
			Object object = instance.get(name);

			if (object != null) return object;
		}

		if (outer != null)
		{
			return outer.get(name);
		}

		return null;
	}

	public void define(String name, Object value)
	{
		values.put(name, value);
	}

	public void assign(String name, Object value)
	{
		if (values.containsKey(name))
		{
			values.replace(name, value);
		}
		else if (values.containsKey(Interpreter.THIS))
		{
			RuntimeInstance instance = (RuntimeInstance) values.get(Interpreter.THIS);
			if (instance.set(name, value)) return;
		}

		if (outer != null)
		{
			outer.assign(name, value);
		}
	}
}
