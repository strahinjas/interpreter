package interpreter.symbols;

import java.util.LinkedHashMap;
import java.util.Map;

public class Enviroment
{
	private Enviroment outer;

	private final Map<String, Symbol> symbols = new LinkedHashMap<>();

	public Enviroment(Enviroment outer)
	{
		this.outer = outer;
	}

	public boolean addSymbol(Symbol symbol)
	{
		if (symbols.containsKey(symbol.getName()))
		{
			return false;
		}
		else
		{
			symbols.put(symbol.getName(), symbol);
			return true;
		}
	}

	public Symbol findSymbol(String symbolName)
	{
		return symbols.get(symbolName);
	}

	public void removeSymbol(String symbolName)
	{
		symbols.remove(symbolName);
	}

	public Enviroment getOuter()
	{
		return outer;
	}

	public Map<String, Symbol> getSymbols()
	{
		return symbols;
	}

	public void accept(SymbolTableVisitor visitor)
	{
		visitor.visit(this);
	}
}
