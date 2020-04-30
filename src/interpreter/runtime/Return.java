package interpreter.runtime;

public class Return extends RuntimeException
{
	public final Object value;

	public Return()
	{
		value = null;
	}

	public Return(Object value)
	{
		this.value = value;
	}
}
