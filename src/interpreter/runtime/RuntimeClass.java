package interpreter.runtime;

import java.util.Map;

public class RuntimeClass
{
	private final String name;
	private final RuntimeClass superClass;
	private final Map<String, Object> fields;
	private final Map<String, RuntimeMethod> methods;

	public RuntimeClass(String name,
						RuntimeClass superClass,
						Map<String, Object> fields,
						Map<String, RuntimeMethod> methods)
	{
		this.name = name;
		this.superClass = superClass;
		this.fields = fields;
		this.methods = methods;
	}

	public RuntimeMethod getMethod(String name)
	{
		if (methods.containsKey(name)) return methods.get(name);

		if (superClass != null) return superClass.getMethod(name);

		return null;
	}

	public Map<String, Object> getFields()
	{
		return fields;
	}

	public Map<String, RuntimeMethod> getMethods()
	{
		return methods;
	}
}
