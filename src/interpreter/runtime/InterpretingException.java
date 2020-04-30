package interpreter.runtime;

public class InterpretingException extends RuntimeException
{
	public InterpretingException(int line, String message)
	{
		super("Runtime error (line " + line + "): " + message);
	}
}
