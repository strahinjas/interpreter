package interpreter;

public class InterpretingException extends RuntimeException
{
	InterpretingException(int line, String message)
	{
		super("Runtime error (line " + line + "): " + message);
	}
}
