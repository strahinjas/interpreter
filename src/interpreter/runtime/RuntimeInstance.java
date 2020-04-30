package interpreter.runtime;

import java.util.HashMap;
import java.util.Map;

public class RuntimeInstance
{
	private final RuntimeClass runtimeClass;
	private final Map<String, Object> fields = new HashMap<>();

	public RuntimeInstance(RuntimeClass runtimeClass)
	{
		this.runtimeClass = runtimeClass;
		fields.putAll(runtimeClass.getFields());
	}

	public Object get(String member)
	{
		if (fields.containsKey(member)) return fields.get(member);

		RuntimeMethod method = runtimeClass.getMethod(member);
		if (method != null) method.bind(this);

		return method;
	}

	public boolean set(String field, Object value)
	{
		if (fields.containsKey(field))
		{
			fields.replace(field, value);
			return true;
		}
		return false;
	}
}
