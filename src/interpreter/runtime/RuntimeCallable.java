package interpreter.runtime;

import interpreter.Interpreter;

import java.util.List;

public interface RuntimeCallable
{
	Object call(Interpreter interpreter, List<Object> arguments);
}
