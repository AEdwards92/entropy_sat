import java.util.List;

public class Clause {
	protected List<Integer> variables;
	boolean status;
	boolean sat;

	public Clause(List<Integer> vars) {
		variables = vars;
		status = false;
		sat = false;
	}
	
	public int getVar(int idx) {
		if (idx < 0 || idx >= variables.size()) {
			System.err.println("Invalid index");
			System.err.println("Returning 0");
			return 0;
		}
		return variables.get(idx);
	}

	public List<Integer> getVars() {
		return variables;
	}

	public int size() {
		return variables.size();
	}
	
	public boolean isSet() {
		return status;
	}

	public void unset() {
		status = false;
	}

	public void set() {
		status = true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Variables: [");
		for (int i = 0; i < variables.size(); i++) {
			sb.append(variables.get(i));
			if (i < variables.size() - 1) {
				sb.append(", ");
			}
		}
		sb.append("]\n");
		sb.append("Status: ");
		sb.append(status ? "set" : "unset");
		sb.append("\n");
		return sb.toString();
	}

}
