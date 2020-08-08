package interpreter;

import interpreter.ast.Program;
import interpreter.ast.SyntaxNode;
import interpreter.lexer.Yylex;
import interpreter.parser.Parser;
import interpreter.runtime.InterpretingException;
import interpreter.symbols.SymbolTable;
import java_cup.runtime.Symbol;

import java.io.*;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		if (args.length < 2 || args.length > 3)
		{
			System.err.println("Wrong number of arguments!");
			System.err.println("Program should be called with two or three arguments: input_file(.mj) to_interpret(true/false) [output_file(.ir)].");
			return;
		}

		String inputFileName = args[0];

		if (!inputFileName.endsWith(".mj"))
		{
			System.err.println("Invalid MicroJava source code provided.");
			System.err.println("Input file should have (.mj) extension.");
			return;
		}

		boolean toInterpret = Boolean.parseBoolean(args[1]);

		String outputFileName;

		if (args.length == 2)
		{
			outputFileName = inputFileName.replace(".mj", ".ir");
		}
		else
		{
			outputFileName = args[2];
		}

		if (!outputFileName.endsWith(".ir"))
		{
			System.err.println("Invalid output file provided.");
			System.err.println("Output file should have (.ir) extension.");
			return;
		}

		Reader reader = null;

		try
		{
			File sourceFile = new File(inputFileName);

			if (!sourceFile.exists())
			{
				System.err.println("Provided input file could not be found.");
				return;
			}

			System.out.println("Interpreting source file: " + sourceFile.getAbsolutePath());

			reader = new BufferedReader(new FileReader(sourceFile));
			Yylex lexer = new Yylex(reader);
			Parser parser = new Parser(lexer);

			System.out.println("========================= Syntax Analysis ===========================");

			Symbol symbol = parser.parse();
			SyntaxNode root = (SyntaxNode) symbol.value;

			if (!(root instanceof Program))
			{
				System.err.println("Syntax error! Interpretation cannot continue!");
				return;
			}

			Program program = (Program) root;

			System.out.println(program.toString(""));

			if (!parser.isSyntacticallyCorrect())
			{
				System.err.println("Syntax error! Interpretation cannot continue!");
				return;
			}

			System.out.println("========================= Semantic Analysis =========================");

			SymbolTable symbolTable = new SymbolTable();
			SemanticAnalyzer analyzer = new SemanticAnalyzer(symbolTable);

			analyzer.analyze(program);

			symbolTable.dump();

			if (!analyzer.isSemanticallyCorrect())
			{
				System.err.println("Semantic error! Interpretation cannot continue!");
				return;
			}

			System.out.println("================== Intermediate Code Generation =====================");
			System.out.println();

			IntermediateCodeGenerator generator = new IntermediateCodeGenerator(symbolTable);

			generator.generate(program);

			System.out.println("Intermediate code successfully generated.");
			System.out.println("Writing intermediate code to file '" + outputFileName + "'...");

			generator.writeIRFile(outputFileName);

			System.out.println("Finished writing IR file.");
			System.out.println();

			if (toInterpret)
			{
				System.out.println("========================= Interpretation ============================");
				System.out.println();

				Interpreter interpreter = new Interpreter();

				try
				{
					interpreter.interpret(generator.getIntermediateCode());

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
			else
			{
				System.out.println("Interpretation omitted.");
				System.out.println("Generated IR file can be interpreted afterwards by passing it as a parameter to the Interpreter application.");
			}
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException exception)
				{
					System.err.println(exception.getMessage());
				}
			}
		}
	}
}
