package interpreter.utilities;

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
		else if (outer != null)
		{
			outer.assign(name, value);
		}
	}
}
