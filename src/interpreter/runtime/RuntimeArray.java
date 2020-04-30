package interpreter.runtime;

public class RuntimeArray
{
	private final Object[] values;

	public RuntimeArray(int size)
	{
		values = new Object[size];
	}

	public Object get(int index)
	{
		return values[index];
	}

	public void set(int index, Object value)
	{
		values[index] = value;
	}

	public int length()
	{
		return values.length;
	}
}
