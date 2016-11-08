import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.List;
import java.util.LinkedList;

public class DimacsParser {
	public static ProblemInstance parseDimacsFile(String fileName) {
		ProblemInstance sat = new ProblemInstance();
		try {
			Scanner in = new Scanner(new FileInputStream(fileName));
			String problemLine = in.nextLine();
			while (problemLine.startsWith("c")) {
				problemLine = in.nextLine();
			}
			String[] params = problemLine.split(" ");
			if (!params[0].equals("p")) {
				System.err.println("ERROR: File missing problem line!");
				System.err.println("       Returning empty SAT instance!");
				in.close();
				return sat;
			}
			if (!params[1].equals("cnf")) {
				System.err.println("ERROR: Parsing a non-CNF Dimacs file!");
				System.err.println("       Returning empty SAT instance!");
				in.close();
				return sat;
			}

			sat.setNumVars(Integer.parseInt(params[2]));
			sat.setNumClauses(Integer.parseInt(params[3]));

			String currentLine;
			String[] tokens;
			List<Integer> currentClause = new LinkedList<Integer>();
			// int lineID = 0;
			while (in.hasNext()) {
				currentLine = in.nextLine();
				// lineID++;
				// System.err.println("line #" + lineID);
				tokens = currentLine.split("\\s+");
				if (tokens[0].equals("c")) {
					if (tokens.length == 3) {
						sat.setSymbolMapping(Integer.parseInt(tokens[1]),
								tokens[2]);
					}
				} else {
					for (int i = 0; i < tokens.length; i++) {
						Integer literal = Integer.parseInt(tokens[i]);
						if (literal == 0) {
							sat.addClause(currentClause);
							currentClause = new LinkedList<Integer>();
						} else {
							currentClause.add(literal);
						}
					}
				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			System.err.println("ERROR: File not found:" + fileName);
			System.err.println("       Returning empty SAT instance!");
		}
		return sat;
	}
}
