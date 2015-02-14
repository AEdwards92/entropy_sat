import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;
import org.math.plot.*;
import javax.swing.*;

public class Solver {
	/* Holds sat problem information
	 */
	protected ProblemInstance inst;
	
	/* parameter for sampling distribution, which makes new
	 * clause assignments exponentially less likely as a
	 * function of the number of other clauses they break.
	 */
	protected double breakCost;
	
	/* A boolean that is set upon solver instantiation.
	 * Indicates whether to use the above sampling method
	 * or to use a uniform random sample.
	 */
	private boolean balint;
	
	/* Partial assignment maintained but never directly used
	 * when sampling a clause, the assignment is updated
	 * the individual variables are never accessed except
	 * via a clause
	 */
	protected List<Integer> assignment;
	
	/* The various constructors for the solver
	 */
	
	public Solver() {
		breakCost = 1.0;
		balint = false;
		assignment = new LinkedList<Integer>();
	}
	
	public Solver(double cb) {
		breakCost = cb;
		balint = true;
		assignment = new LinkedList<Integer>();
	}
	
	public Solver(ProblemInstance sat) {
		inst = sat;
		balint = false;
		assignment = new LinkedList<Integer>();
		initialize();
	}

	public Solver(double cb, ProblemInstance sat) {
		inst = sat;
		breakCost = cb;
		balint = true;
		assignment = new LinkedList<Integer>();
		initialize();
	}

	/* Initializer used by all constructors that pass
	 * in an existing ProblemInstance.
	 */
	
	private void initialize() {
		if (inst == null) {
			System.err.println("Error: attempting to initalize with null instance");
			System.err.println("Terminating program");
			System.exit(1);
		}
		// add dummy item to use 1-based indexing
		assignment.add(-1);
		for (int i = 1; i <= inst.numVars; i++) {
			if (Math.random() < 0.5) {
				assignment.add(0);
			} else {
				assignment.add(1);
			}
		}
	}
	
	
	/* Returns whether or not the current assignment
	 * satisfies the ProblemInstance.
	 */
	private boolean satisfied() {
		boolean sat;
		for(int i = 0; i < inst.getNumClauses(); i++) {
			sat = false;
			Clause c = inst.getClause(i);
			for (int v : c.getVars()) {
				if ((v < 0 && assignment.get(-v) == 0)
				 || (v > 0 && assignment.get(v) == 1)) {
					sat = true;
					break;
				} 
			}
			if (!sat) {
				return false;
			}
		}
		return true;
	}
	
	/* Uniform random sample method
	 * Clause c is assumed to be unsatisfied.
	 * We sample uniformly the set of 2^k possibilities,
	 * but we ensure that the resulting, random assignment
	 * fixes clause c.
	 */
	private void unifSample(Clause c) {
		boolean unsat = true;
		do {
			for (int v : c.getVars()) {
				if (Math.random() < 0.5) {
					assignment.set(Math.abs(v), 0);
					if (unsat && v < 0) {
						unsat = false;
					}
				} else {
					assignment.set(Math.abs(v), 1);
					if (unsat && v > 0) {
						unsat = false;
					}
				}
			} 
		} while (unsat);
	}
	
	
	/* Samples all seven possible new clause assignments
	 * based on the following distribution:
	 * 		Pr[c = τ] = e^{-C*Break(τ, σ)}/Z
	 * where
	 * 	c is the current clause assignment
	 * 	τ is the new clause assignment
	 * 	σ is the current partial assignment
	 * 	C is the break penalty (default 1)
	 * 	Break(τ, σ) is the number of clauses that will break
	 * 				if we use τ instead of c.
	 * 	Z is a normalizing factor
	 */
	private void balintSample(Clause c) {
		// Current implementation is rigid and only works
		// for 3SAT, for performance purposes.
		int breakCnt[] = new int[8];
		int varBreakCnt[] = new int[3];
		double Z = 0;
		double probs[] = new double[8];
		

		for (int i = 0; i < 3; i++) {
			int v = c.getVars().get(i);
			List<Integer> neighbors = inst.signedOccurrenceMap.get(-v);
			if (neighbors == null)
				continue;
			neighbor_loop:
			for (int cid : neighbors) {
				Clause d = inst.getClause(cid);
//				if (!d.isSet())
//					continue neighbor_loop;
				for (int u : d.getVars()) {
					if (u != -v && 
						((u < 0 && assignment.get(-u) == 0)
					  || (u > 0 && assignment.get(u) == 1)))
						continue neighbor_loop;
				}
				varBreakCnt[i] += 1;
			}
		}
		
		for (int i = 1; i <= 7; i++) {
			breakCnt[i] =        (1 & i) * varBreakCnt[0]
						+ ((2 & i) >> 1) * varBreakCnt[1]
						+ ((4 & i) >> 2) * varBreakCnt[2];
			probs[i] = Math.pow(Math.E, -breakCost * breakCnt[i]);
			Z += probs[i];
		}

		double r = Math.random();
		int flip;
		for (flip = 1; flip <= 7; flip++) {
			r -= (probs[flip] / Z);
			if (r <= 0)
				break;
		}
		if ((flip & 1) == 1) {
			assignment.set(Math.abs(c.getVars().get(0)), 
					       Math.abs(1 - assignment.get(Math.abs(c.getVars().get(0)))));
		}
		if ((flip & 2) == 2) {
			assignment.set(Math.abs(c.getVars().get(1)), 
					       Math.abs(1 - assignment.get(Math.abs(c.getVars().get(1)))));
		}
		if ((flip & 4) == 4) {
			assignment.set(Math.abs(c.getVars().get(2)), 
					       Math.abs(1 - assignment.get(Math.abs(c.getVars().get(2)))));
		}
	}
	
	
	public int solve() {
		List<Clause> unsetClauses;
		int numUnset;
		int choice;
		int step;
		Random randomGenerator = new Random();
		for (step = 0; ; step++) {
			unsetClauses = inst.getUnsetClauses();
			numUnset = unsetClauses.size();
			if (numUnset == 0) {
//				System.err.println("Step: " + step);
				if (satisfied()) {
					break;
				} else {
					System.err.println("unsetClauses empty while problem not satisfied");
				}
			}
			choice = randomGenerator.nextInt(numUnset);
			Clause c = unsetClauses.get(choice);
			boolean unsat;
			// Sample V(c), make sure the resulting assignment
			// satisfies c.
			if (balint)
				balintSample(c);
			else
				unifSample(c);
			
			inst.setClause(c);
			
			// Check neighboring clauses of c which are set, and if they
			// are unsatasified by current assignment, unset them
			for (int v : c.getVars()) {
				List<Integer> neighborClauseIDs = inst.occurrenceMap.get(Math.abs(v));
				for (int cid : neighborClauseIDs) {
					Clause d = inst.getClause(cid);
					if (d.isSet()) {
						unsat = true;
						for (int u : d.getVars()) {
							if ((u > 0) && (assignment.get(u) == 1) ) {
								unsat = false;
								break;
							} else if ((u < 0) && (assignment.get(-u) == 0)) {
								unsat = false;
								break;
							}
						}
						if (unsat) {
							inst.unsetClause(d);
						}
					}
				}
			}
		}
		return step;
	}
	
	public int solve(ProblemInstance sat) {
		inst = sat;
		initialize();
		return solve();
	}
	
	public static void main(String[] args) {
		String fileName = "src/Files/random_ksat.dimacs";
		float movingAverage;
		double stepArray[][] = new double[10][100];
		double unifSteps[] = new double[100];
		Plot2DPanel plot = new Plot2DPanel();
		
		for (int i = 0; i < 20; i++) {
			movingAverage = 0;
			System.out.println("Break penalty is " + (1.0 + (0.1 *i)));
			for (int iteration = 0; iteration < 5; iteration++) {
				ProblemInstance sat = DimacsParser.parseDimacsFile(fileName);
				Solver sol = new Solver(1.0 + (0.1) * i, sat);
				int steps = sol.solve();
				movingAverage = (steps + iteration * movingAverage) / (iteration + 1);
				stepArray[i][iteration] = steps;
				//System.out.println("Steps: " + steps);
			}
			System.out.print("\tAverage for this round: " + movingAverage);
			System.out.println();
			System.out.println();
			plot.addHistogramPlot("Balint Hist: C_B = " + (1.0 + (0.1 *i)), stepArray[i], 50);
		}
//		for (int iteration = 0; iteration < 1; iteration++) {
//			System.out.println("Iteration 1");
//			ProblemInstance sat = DimacsParser.parseDimacsFile(fileName);
//			Solver sol = new Solver(sat);
//			int steps = sol.solve();
//			unifSteps[iteration] = steps;
//			System.out.println("Steps: " + steps);
//		}
//		plot.addHistogramPlot("Uniform Hist", unifSteps, 50);
        // put the PlotPanel in a JFrame like a JPanel
        JFrame frame = new JFrame("a plot panel");
        frame.setSize(600, 600);
        frame.setContentPane(plot);
        frame.setVisible(true);
	}
	
}
