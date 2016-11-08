import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class ProblemInstance {
	// summary variables
	protected int maxClauseID;
	protected int maxVarID;
	protected int numClauses;
	protected int numVars;
    protected int maxOccur;
	protected HashMap<Integer, String> symbolTable;
	protected Map<Integer, Clause> clauseStore;
	protected List<Clause> unsetClauses;
	protected Map<Integer, List<Integer>> occurrenceMap = null;
	protected Map<Integer, List<Integer>> signedOccurrenceMap = null;

	public ProblemInstance() {
		symbolTable = new HashMap<Integer, String>();
		maxClauseID = -1;
		maxVarID = 0;
        maxOccur = 0;
		clauseStore = new HashMap<Integer, Clause>();
		unsetClauses = new ArrayList<Clause>();
		occurrenceMap = new HashMap<Integer, List<Integer>>();
		signedOccurrenceMap = new HashMap<Integer, List<Integer>>();
	}

	public void setSymbolMapping(int id, String symbol) {
		symbolTable.put(id, symbol);
	}

	public String getSymbolForLiteral(int literal) {
		if (Integer.signum(literal) == 1) {
			String symbol = symbolTable.get(literal);
			if (symbol == null)
				return literal + "";
			return symbol;
		} else {
			String symbol = symbolTable.get(-literal);
			if (symbol == null)
				return literal + "";
			return "-" + symbol;
		}
	}

	public void setNumClauses(int nc) {
		numClauses = nc;
	}

	public int getNumClauses() {
		return numClauses;
	}

	public void setNumVars(int nv) {
		numVars = nv;
	}

	public int getNumVars() {
		return numVars;
	}

	public Clause getClause(int c) {
		return clauseStore.get(c);
	}

	public void setClause(Clause c) {
		c.set();
		unsetClauses.remove(c);
	}

	public List<Clause> getUnsetClauses() {
		return unsetClauses;
	}
	
	public int getMaxOccur() {
		return maxOccur;
	}
	
	public void unsetClause(Clause d) {
		d.unset();
		unsetClauses.add(d);
	}

	public void resetClauses() {
		for (Clause c : clauseStore.values()) {
			if (c.isSet()) {
				c.unset();
				c.sat = false;
				unsetClauses.add(c);
			}
		}
	}

	public int addClause(List<Integer> clause) {
		maxClauseID++;
		Clause c = new Clause(clause);
		clauseStore.put(maxClauseID, c);
		unsetClauses.add(c);
		for (int literal : clause) {
			int key = Math.abs(literal);
			List<Integer> occurrences = occurrenceMap.get(key);
			List<Integer> signedOccurrences = signedOccurrenceMap.get(literal);
			if (occurrences == null) {
				occurrences = new ArrayList<Integer>();
				occurrenceMap.put(key, occurrences);
			}
			if (signedOccurrences == null) {
				signedOccurrences = new ArrayList<Integer>();
				signedOccurrenceMap.put(literal, signedOccurrences);
			}
			occurrences.add(maxClauseID);
			signedOccurrences.add(maxClauseID);
            if (occurrences.size() > maxOccur)
                maxOccur = occurrences.size();
			if (literal < 0)
				literal = -literal;
			if (literal > maxVarID)
				maxVarID = literal;
		}
		return maxClauseID;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Clause c : clauseStore.values()) {
			sb.append(c.toString());
		}
		return sb.toString();
	}
}
