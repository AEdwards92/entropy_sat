import java.util.List;

public class Clause {
	protected List<Integer> variables;
	boolean status;
	public Clause(List<Integer> vars) {
		variables = vars;
		status = false;
	}
	
	public List<Integer> getVars() {
		return variables;
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
	
	@Override public String toString() {
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
