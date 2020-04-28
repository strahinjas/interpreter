package interpreter.ir;

import java.util.List;

public abstract class Statement
{
	public interface Visitor<R>
	{
		R visit(Assignment statement);
		R visit(Block statement);
		R visit(Call statement);
		R visit(Class statement);
		R visit(Constant statement);
		R visit(Control statement);
		R visit(Decrement statement);
		R visit(For statement);
		R visit(If statement);
		R visit(Increment statement);
		R visit(Method statement);
		R visit(Print statement);
		R visit(Program statement);
		R visit(Read statement);
		R visit(Return statement);
		R visit(Variable statement);
	}

	public int line;

	public Statement(int line)
	{
		this.line = line;
	}

	public abstract <R> R accept(Visitor<R> visitor);

	public static class Assignment extends Statement
	{
		public final Expression destination;
		public final Expression value;

		public Assignment(int line, Expression destination, Expression value)
		{
			super(line);
			this.destination = destination;
			this.value = value;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Block extends Statement
	{
		public final List<Statement> statements;

		public Block(int line, List<Statement> statements)
		{
			super(line);
			this.statements = statements;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Call extends Statement
	{
		public final Expression.Call expression;

		public Call(int line, Expression.Call expression)
		{
			super(line);
			this.expression = expression;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Class extends Statement
	{
		public enum Type { ABSTRACT, CONCRETE }

		public final String name;
		public final Class.Type type;
		public final List<String> fields;
		public final List<Statement.Method> methods;

		public Class(int line, String name, Class.Type type, List<String> fields, List<Statement.Method> methods)
		{
			super(line);
			this.name = name;
			this.type = type;
			this.fields = fields;
			this.methods = methods;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Constant extends Statement
	{
		public final String name;
		public final Object value;

		public Constant(int line, String name, Object value)
		{
			super(line);
			this.name = name;
			this.value = value;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Control extends Statement
	{
		public enum Type { BREAK, CONTINUE }

		public final Control.Type type;

		public Control(int line, Control.Type type)
		{
			super(line);
			this.type = type;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Decrement extends Statement
	{
		public final Expression number;

		public Decrement(int line, Expression number)
		{
			super(line);
			this.number = number;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class For extends Statement
	{
		public final Statement initializer;
		public final Expression condition;
		public final Statement increment;
		public final Statement body;

		public For(int line, Statement initializer, Expression condition, Statement increment, Statement body)
		{
			super(line);
			this.initializer = initializer;
			this.condition = condition;
			this.increment = increment;
			this.body = body;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class If extends Statement
	{
		public final Expression condition;
		public final Statement thenBranch;
		public final Statement elseBranch;

		public If(int line, Expression condition, Statement thenBranch, Statement elseBranch)
		{
			super(line);
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Increment extends Statement
	{
		public final Expression number;

		public Increment(int line, Expression number)
		{
			super(line);
			this.number = number;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Method extends Statement
	{
		public final String name;
		public final List<String> parameters;
		public final List<Statement> body;

		public Method(int line, String name, List<String> parameters, List<Statement> body)
		{
			super(line);
			this.name = name;
			this.parameters = parameters;
			this.body = body;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Print extends Statement
	{
		public final Expression expression;
		public final Integer width;

		public Print(int line, Expression expression, Integer width)
		{
			super(line);
			this.expression = expression;
			this.width = width;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Program extends Statement
	{
		public final List<Statement> statements;

		public Program(int line, List<Statement> statements)
		{
			super(line);
			this.statements = statements;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Read extends Statement
	{
		public final Expression destination;

		public Read(int line, Expression destination)
		{
			super(line);
			this.destination = destination;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Return extends Statement
	{
		public final Expression value;

		public Return(int line, Expression value)
		{
			super(line);
			this.value = value;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}

	public static class Variable extends Statement
	{
		public final String name;

		public Variable(int line, String name)
		{
			super(line);
			this.name = name;
		}

		@Override
		public <R> R accept(Visitor<R> visitor)
		{
			return visitor.visit(this);
		}
	}
}
