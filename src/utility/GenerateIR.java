package utility;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Metaprogramming technique to generate
 * intermediate representation tree classes
 */
public class GenerateIR
{
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.err.println("Output directory missing!");
			return;
		}

		String outputDirectory = args[0];

		try
		{
			defineIRClass(outputDirectory, "Expression", Arrays.asList(
					"Binary   : Expression left, Binary.Operation operation, Expression right",
					"Call     : Expression callee, List<Expression> arguments",
					"Group    : Expression expression",
					"Index    : Expression array, Expression index",
					"Literal  : Object value",
					"Logical  : Expression left, Logical.Operation operation, Expression right",
					"New      : String type, Expression size",
					"Property : Expression object, String name",
					"Unary    : Unary.Operation operation, Expression right",
					"Variable : String name"
			));

			defineIRClass(outputDirectory, "Statement", Arrays.asList(
					"Assignment  : Expression destination, Expression value",
					"Block       : List<Statement> statements",
					"Call        : Expression.Call expression",
					"Class       : String name, String superClass, List<Class.Field> fields, List<Statement.Method> methods",
					"Constant    : String name, Object value",
					"Control     : Control.Type type",
					"Declaration : Declaration.Type type, String name",
					"Decrement   : Expression number",
					"For         : Statement initializer, Expression condition, Statement increment, Statement body",
					"If          : Expression condition, Statement thenBranch, Statement elseBranch",
					"Increment   : Expression number",
					"Method      : boolean isVoid, String name, List<String> parameters, List<Statement> body",
					"Print       : Expression expression, Integer width",
					"Program     : List<Statement> statements",
					"Read        : Declaration.Type type, Expression destination",
					"Return      : Expression value"
			));
		}
		catch (IOException exception)
		{
			System.err.println(exception.getMessage());
		}
	}

	private static void defineIRClass(String outputDirectory, String className, List<String> subClasses)
	throws IOException
	{
		String fileName = outputDirectory + "/" + className + ".java";
		PrintWriter writer = new PrintWriter(fileName, StandardCharsets.UTF_8);

		writer.println("package interpreter.ir;");
		writer.println();
		writer.println("import java.io.Serializable;");
		writer.println("import java.util.List;");
		writer.println();
		writer.println("public abstract class " + className + " implements Serializable");
		writer.println("{");

		defineVisitor(writer, className, subClasses);

		writer.println();
		writer.println("\tpublic int line;");

		writer.println();
		writer.println("\tpublic " + className + "(int line)");
		writer.println("\t{");
		writer.println("\t\tthis.line = line;");
		writer.println("\t}");

		writer.println();
		writer.println("\tpublic abstract <R> R accept(Visitor<R> visitor);");

		for (String subClass : subClasses)
		{
			writer.println();

			String subClassName = subClass.split(":")[0].trim();
			String fields = subClass.split(":")[1].trim();

			defineSubClass(writer, className, subClassName, fields);
		}

		writer.println("}");
		writer.close();
	}

	private static void defineVisitor(PrintWriter writer, String className, List<String> subClasses)
	{
		writer.println("\tpublic interface Visitor<R>");
		writer.println("\t{");

		for (String subClass : subClasses)
		{
			String subClassName = subClass.split(":")[0].trim();

			writer.println("\t\tR visit(" + subClassName + " " + className.toLowerCase() + ");");
		}

		writer.println("\t}");
	}

	private static void defineSubClass(PrintWriter writer, String baseClassName, String className, String fieldList)
	{
		writer.println("\tpublic static final class " + className + " extends " + baseClassName);
		writer.println("\t{");

		// enums
		switch (className)
		{
		case "Binary":
			writer.println("\t\tpublic enum Operation");
			writer.println("\t\t{");
			writer.println("\t\t\tADDITION,");
			writer.println("\t\t\tSUBTRACTION,");
			writer.println("\t\t\tMULTIPLICATION,");
			writer.println("\t\t\tDIVISION,");
			writer.println("\t\t\tMODULUS,");
			writer.println("\t\t\tEQUAL,");
			writer.println("\t\t\tNOT_EQUAL,");
			writer.println("\t\t\tGREATER,");
			writer.println("\t\t\tGREATER_EQUAL,");
			writer.println("\t\t\tLESS,");
			writer.println("\t\t\tLESS_EQUAL");
			writer.println("\t\t}");
			writer.println();
			break;
		case "Logical":
			writer.println("\t\tpublic enum Operation { AND, OR }");
			writer.println();
			break;
		case "Unary":
			writer.println("\t\tpublic enum Operation { NEGATION }");
			writer.println();
			break;
		case "Class":
			writer.println("\t\tpublic static final class Field implements Serializable");
			writer.println("\t\t{");
			writer.println("\t\t\tpublic final String name;");
			writer.println("\t\t\tpublic final Declaration.Type type;");
			writer.println();
			writer.println("\t\t\tpublic Field(String name, Declaration.Type type)");
			writer.println("\t\t\t{");
			writer.println("\t\t\t\tthis.name = name;");
			writer.println("\t\t\t\tthis.type = type;");
			writer.println("\t\t\t}");
			writer.println("\t\t}");
			writer.println();
			break;
		case "Control":
			writer.println("\t\tpublic enum Type { BREAK, CONTINUE }");
			writer.println();
			break;
		case "Declaration":
			writer.println("\t\tpublic enum Type");
			writer.println("\t\t{");
			writer.println("\t\t\tINTEGER,");
			writer.println("\t\t\tCHARACTER,");
			writer.println("\t\t\tBOOLEAN,");
			writer.println("\t\t\tREFERENCE");
			writer.println("\t\t}");
			writer.println();
			break;
		}

		String[] fields = fieldList.split(", ");

		// fields
		for (String field : fields)
		{
			writer.println("\t\tpublic final " + field + ";");
		}
		writer.println();

		// constructor
		writer.println("\t\tpublic " + className + "(int line, " + fieldList + ")");
		writer.println("\t\t{");
		writer.println("\t\t\tsuper(line);");

		for (String field : fields)
		{
			String fieldName = field.split(" ")[1].trim();

			writer.println("\t\t\tthis." + fieldName + " = " + fieldName + ";");
		}

		writer.println("\t\t}");
		writer.println();

		// accept method
		writer.println("\t\t@Override");
		writer.println("\t\tpublic <R> R accept(Visitor<R> visitor)");
		writer.println("\t\t{");
		writer.println("\t\t\treturn visitor.visit(this);");
		writer.println("\t\t}");

		writer.println("\t}");
	}
}
