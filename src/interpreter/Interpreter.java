package interpreter;

import interpreter.ir.Expression;
import interpreter.ir.Statement;
import interpreter.runtime.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

public class Interpreter implements Expression.Visitor<Object>, Statement.Visitor<Void>
{
	public static final String THIS = "this";

	private final Environment universe = new Environment();
	private Environment environment = universe;

	private final Stack<Integer> line = new Stack<>();

	private final Scanner scanner = new Scanner(System.in);

	public Interpreter()
	{
		universe.define("chr", (RuntimeCallable) (interpreter, arguments) -> (char) (int) arguments.get(0));
		universe.define("ord", (RuntimeCallable) (interpreter, arguments) -> (int) (char) arguments.get(0));
		universe.define("len", (RuntimeCallable) (interpreter, arguments) ->
		{
			if (arguments.get(0) == null)
			{
				throw new InterpretingException(line.peek(), "Null pointer exception!");
			}
			return ((RuntimeArray) arguments.get(0)).length();
		});

		universe.define("null", null);
		universe.define("eol", System.lineSeparator());
	}

	public void interpret(Statement.Program program)
	{
		program.accept(this);
	}

	public void execute(List<Statement> statements, Environment environment)
	{
		Environment previous = this.environment;
		this.environment = environment;

		try
		{
			for (Statement statement : statements) execute(statement);
		}
		finally
		{
			this.environment = previous;
		}
	}

	public Environment getEnvironment()
	{
		return environment;
	}

	//////////////////////////////////////
	//////////// HELPER METHODS //////////
	//////////////////////////////////////

	private Object evaluate(Expression expression)
	{
		line.push(expression.line);

		Object object = expression.accept(this);

		line.pop();
		return object;
	}

	private void execute(Statement statement)
	{
		line.push(statement.line);

		statement.accept(this);

		line.pop();
	}

	private boolean areEqual(Object a, Object b)
	{
		if (a == null && b == null) return true;
		if (a == null) return false;

		return a.equals(b);
	}

	private boolean isTrue(Object object)
	{
		if (object == null) return false;
		if (object instanceof Boolean) return (boolean) object;

		return true;
	}

	private void assign(Expression destination, Object value)
	{
		if (destination instanceof Expression.Index)
		{
			Expression.Index expression = (Expression.Index) destination;
			RuntimeArray array = (RuntimeArray) evaluate(expression.array);
			int index = (int) evaluate(expression.index);

			if (array == null)
			{
				throw new InterpretingException(expression.line, "Null pointer exception!");
			}

			array.set(index, value);
		}
		else if (destination instanceof Expression.Property)
		{
			Expression.Property expression = (Expression.Property) destination;
			RuntimeInstance instance = (RuntimeInstance) evaluate(expression.object);

			if (instance == null)
			{
				throw new InterpretingException(expression.line, "Null pointer exception!");
			}

			instance.set(expression.name, value);
		}
		else if (destination instanceof Expression.Variable)
		{
			Expression.Variable expression = (Expression.Variable) destination;

			environment.assign(expression.name, value);
		}
		else
		{
			throw new InterpretingException(destination.line, "Invalid expression on the left side of an assignment.");
		}
	}

	private Object getInitializer(Statement.Declaration.Type type)
	{
		Object initializer = null;

		switch (type)
		{
		case INTEGER:
			initializer = 0;
			break;
		case CHARACTER:
			initializer = '\0';
			break;
		case BOOLEAN:
			initializer = false;
			break;
		}

		return initializer;
	}

	//////////////////////////////////////
	///////////// EXPRESSIONS ////////////
	//////////////////////////////////////

	@Override
	public Object visit(Expression.Binary expression)
	{
		Object left  = evaluate(expression.left);
		Object right = evaluate(expression.right);

		Object result;

		switch (expression.operation)
		{
		case ADDITION:
			result = (int) left + (int) right;
			break;
		case SUBTRACTION:
			result = (int) left - (int) right;
			break;
		case MULTIPLICATION:
			result = (int) left * (int) right;
			break;
		case DIVISION:
			if ((int) right == 0)
			{
				throw new InterpretingException(expression.line, "Division by zero!");
			}

			result = (int) left / (int) right;
			break;
		case MODULUS:
			result = (int) left % (int) right;
			break;
		case EQUAL:
			result = areEqual(left, right);
			break;
		case NOT_EQUAL:
			result = !areEqual(left, right);
			break;
		case GREATER:
			result = (int) left > (int) right;
			break;
		case GREATER_EQUAL:
			result = (int) left >= (int) right;
			break;
		case LESS:
			result = (int) left < (int) right;
			break;
		case LESS_EQUAL:
			result = (int) left <= (int) right;
			break;
		default:
			throw new InterpretingException(expression.line, "Unrecognized binary operation.");
		}

		return result;
	}

	@Override
	public Object visit(Expression.Call expression)
	{
		RuntimeCallable callee = (RuntimeCallable) evaluate(expression.callee);
		List<Object> arguments = new ArrayList<>();

		for (Expression argument : expression.arguments)
		{
			arguments.add(evaluate(argument));
		}

		return callee.call(this, arguments);
	}

	@Override
	public Object visit(Expression.Group expression)
	{
		return evaluate(expression.expression);
	}

	@Override
	public Object visit(Expression.Index expression)
	{
		RuntimeArray array = (RuntimeArray) evaluate(expression.array);
		int index = (int) evaluate(expression.index);

		return array.get(index);
	}

	@Override
	public Object visit(Expression.Literal expression)
	{
		return expression.value;
	}

	@Override
	public Object visit(Expression.Logical expression)
	{
		Object left = evaluate(expression.left);

		Object result;

		switch (expression.operation)
		{
		case AND:
			if (!isTrue(left)) result = left;
			else result = evaluate(expression.right);
			break;
		case OR:
			if (isTrue(left)) result = left;
			else result = evaluate(expression.right);
			break;
		default:
			throw new InterpretingException(expression.line, "Unrecognized logical operation.");
		}

		return result;
	}

	@Override
	public Object visit(Expression.New expression)
	{
		if (expression.size == null)
		{
			return new RuntimeInstance((RuntimeClass) environment.get(expression.type));
		}
		else
		{
			int size = (int) evaluate(expression.size);
			return new RuntimeArray(size);
		}
	}

	@Override
	public Object visit(Expression.Property expression)
	{
		RuntimeInstance instance = (RuntimeInstance) evaluate(expression.object);

		if (instance == null)
		{
			throw new InterpretingException(expression.line, "Null pointer exception!");
		}

		return instance.get(expression.name);
	}

	@Override
	public Object visit(Expression.Unary expression)
	{
		int right = (int) evaluate(expression.right);

		switch (expression.operation)
		{
		case NEGATION:
			right = -right;
			break;
		default:
			throw new InterpretingException(expression.line, "Unrecognized unary operation.");
		}

		return right;
	}

	@Override
	public Object visit(Expression.Variable expression)
	{
		return environment.get(expression.name);
	}

	//////////////////////////////////////
	///////////// STATEMENTS /////////////
	//////////////////////////////////////

	@Override
	public Void visit(Statement.Assignment statement)
	{
		assign(statement.destination, evaluate(statement.value));
		return null;
	}

	@Override
	public Void visit(Statement.Block statement)
	{
		for (Statement stmt : statement.statements) execute(stmt);
		return null;
	}

	@Override
	public Void visit(Statement.Call statement)
	{
		evaluate(statement.expression);
		return null;
	}

	@Override
	public Void visit(Statement.Class statement)
	{
		Map<String, Object> fields = new HashMap<>();
		Map<String, RuntimeMethod> methods = new HashMap<>();

		for (Statement.Class.Field field : statement.fields)
		{
			fields.put(field.name, getInitializer(field.type));
		}

		for (Statement.Method method : statement.methods)
		{
			RuntimeMethod runtimeMethod = new RuntimeMethod(method);
			methods.put(method.name, runtimeMethod);
		}

		RuntimeClass superClass = null;

		if (statement.superClass != null)
		{
			superClass = (RuntimeClass) environment.get(statement.superClass);
		}

		RuntimeClass runtimeClass = new RuntimeClass(statement.name, superClass, fields, methods);
		environment.define(statement.name, runtimeClass);
		return null;
	}

	@Override
	public Void visit(Statement.Constant statement)
	{
		environment.define(statement.name, statement.value);
		return null;
	}

	@Override
	public Void visit(Statement.Control statement)
	{
		switch (statement.type)
		{
		case BREAK:
			throw new Break();
		case CONTINUE:
			throw new Continue();
		default:
			throw new InterpretingException(statement.line, "Unrecognized control statement.");
		}
	}

	@Override
	public Void visit(Statement.Declaration statement)
	{
		environment.define(statement.name, getInitializer(statement.type));
		return null;
	}

	@Override
	public Void visit(Statement.Decrement statement)
	{
		int value = (int) evaluate(statement.number) - 1;
		assign(statement.number, value);
		return null;
	}

	@Override
	public Void visit(Statement.For statement)
	{
		if (statement.initializer != null)
		{
			execute(statement.initializer);
		}

		while (isTrue(evaluate(statement.condition)))
		{
			try
			{
				execute(statement.body);
			}
			catch (Break aBreak)
			{
				break;
			}
			catch (Continue aContinue)
			{

			}

			if (statement.increment != null)
			{
				execute(statement.increment);
			}
		}

		return null;
	}

	@Override
	public Void visit(Statement.If statement)
	{
		if (isTrue(evaluate(statement.condition)))
		{
			execute(statement.thenBranch);
		}
		else if (statement.elseBranch != null)
		{
			execute(statement.elseBranch);
		}
		return null;
	}

	@Override
	public Void visit(Statement.Increment statement)
	{
		int value = (int) evaluate(statement.number) + 1;
		assign(statement.number, value);
		return null;
	}

	@Override
	public Void visit(Statement.Method statement)
	{
		RuntimeMethod method = new RuntimeMethod(statement);
		environment.define(statement.name, method);
		return null;
	}

	@Override
	public Void visit(Statement.Print statement)
	{
		Object value  = evaluate(statement.expression);
		String output = value.toString();

		Integer width = statement.width;

		if (width != null)
		{
			for (int i = 0; i < width - output.length(); i++) System.out.print(' ');
		}

		System.out.print(output);
		return null;
	}

	@Override
	public Void visit(Statement.Program statement)
	{
		for (Statement stmt : statement.statements) execute(stmt);
		return null;
	}

	@Override
	public Void visit(Statement.Read statement)
	{
		switch (statement.type)
		{
		case INTEGER:
			assign(statement.destination, scanner.nextInt());
			break;
		case CHARACTER:
			assign(statement.destination, scanner.next().charAt(0));
			break;
		case BOOLEAN:
			assign(statement.destination, scanner.nextBoolean());
			break;
		default:
			throw new InterpretingException(statement.line, "Variable in read statement must be of primitive type.");
		}
		return null;
	}

	@Override
	public Void visit(Statement.Return statement)
	{
		if (statement.value == null)
		{
			throw new Return();
		}
		else
		{
			throw new Return(evaluate(statement.value));
		}
	}

	//////////////////////////////////////
	/////// STANDALONE APPLICATION ///////
	//////////////////////////////////////

	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.err.println("Wrong number of arguments!");
			System.err.println("Program should be called with exactly one argument: input_file(.ir).");
			return;
		}

		String inputFileName = args[0];

		if (!inputFileName.endsWith(".ir"))
		{
			System.err.println("Invalid intermediate code provided.");
			System.err.println("Input file should have (.ir) extension.");
			return;
		}

		ObjectInputStream inputStream = null;

		try
		{
			File inputFile = new File(inputFileName);

			if (!inputFile.exists())
			{
				System.err.println("Provided input file could not be found.");
				return;
			}

			System.out.println("Reading intermediate code from file '" + inputFileName + "'...");

			inputStream = new ObjectInputStream(new FileInputStream(inputFile));
			Statement.Program intermediateCode = (Statement.Program) inputStream.readObject();

			System.out.println("Finished reading IR file.");
			System.out.println("Interpreting intermediate code...");
			System.out.println();

			Interpreter interpreter = new Interpreter();

			try
			{
				interpreter.interpret(intermediateCode);

				System.out.println();
				System.out.println("Interpretation finished successfully!");
			}
			catch (InterpretingException exception)
			{
				System.err.println();
				System.err.println(exception.getMessage());
				System.err.println("Interpretation aborted with an error!");
			}
		}
		catch (IOException | ClassNotFoundException exception)
		{
			System.err.println(exception.getMessage());
		}
		finally
		{
			if (inputStream != null)
			{
				try
				{
					inputStream.close();
				}
				catch (IOException exception)
				{
					System.err.println(exception.getMessage());
				}
			}
		}
	}
}
