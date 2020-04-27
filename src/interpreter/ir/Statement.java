package interpreter.ir;

import java.util.List;

public abstract class Statement
{
	public interface Visitor<R>
	{
		R visit(Assignment statement);
		R visit(Block statement);
		R visit(Constant statement);
		R visit(Decrement statement);
		R visit(Increment statement);
		R visit(Print statement);
		R visit(Program statement);
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
		public final String name;
		public final Expression value;

		public Assignment(int line, String name, Expression value)
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

	public static class Decrement extends Statement
	{
		public final String name;

		public Decrement(int line, String name)
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

	public static class Increment extends Statement
	{
		public final String name;

		public Increment(int line, String name)
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
