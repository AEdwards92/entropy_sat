import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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
	protected double beta;
	
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
	
	/* A counter to keep track of how many clauses are currently
	 * not satisfied.
	 */
	private int numUnsat;
	
	/* The various constructors for the solver
	 */
	
	public Solver() {
		beta = 1.0;
		balint = false;
		assignment = new LinkedList<Integer>();
	}
	
	public Solver(double cb) {
		beta = cb;
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
		beta = cb;
		balint = true;
		assignment = new LinkedList<Integer>();
		initialize();
	}

	/* Initializer used by all constructors that pass
	 * in an existing ProblemInstance. Can also be used
	 * to reset a solver
	 */
	
	public void initialize() {
		if (assignment == null) {
			System.err.println("Error: attempting to initalize with null assignment list");
			System.err.println("Terminating program");
			System.exit(1);
		}
		assignment.clear();
		
		if (inst == null) {
			System.err.println("Error: attempting to initalize with null instance");
			System.err.println("Terminating program");
			System.exit(1);
		}
		
		inst.resetClauses();
		
		// add dummy item to use 1-based indexing
		assignment.add(-1);
		for (int i = 1; i <= inst.numVars; i++) {
			if (Math.random() < 0.5) {
				assignment.add(0);
			} else {
				assignment.add(1);
			}
		}
		// Compute the number of unsatisfied clauses
		numUnsat = 0;
		for(int i = 0; i < inst.getNumClauses(); i++) {
			Clause c = inst.getClause(i);
			for (int v : c.getVars()) {
				if ((v < 0 && assignment.get(-v) == 0)
				 || (v > 0 && assignment.get(v) == 1)) {
					c.sat = true;
					break;
				} 
			}
			if (!c.sat) {
				numUnsat += 1;
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
			probs[i] = Math.pow(Math.E, -beta * breakCnt[i]);
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
		for (step = 1; ; step++) {
			unsetClauses = inst.getUnsetClauses();
			numUnset = unsetClauses.size();
			if (numUnsat == 0) {
				if (satisfied()) {
					System.out.println("All satisifed! Number unset: " + numUnset);
					break;
				} else {
					System.err.println("unsetClauses empty while problem not satisfied");
				}
			}
			if (step % 50000 == 0) {
				System.out.println("numUnset = " + numUnset);
				System.out.println("numUnsat = " + numUnsat);
			}
			choice = randomGenerator.nextInt(numUnset);
			Clause c = unsetClauses.get(choice);
			// Sample V(c), make sure the resulting assignment
			// satisfies c.
			if (balint)
				balintSample(c);
			else
				unifSample(c);
			
			inst.setClause(c);
			if (!c.sat) {
				c.sat = true;
				numUnsat--;
			}
			
			// Check neighboring clauses of c which are unsatisfied
			// by current assignment, unset them if need be
			for (int v : c.getVars()) {
				List<Integer> neighborClauseIDs = inst.occurrenceMap.get(Math.abs(v));
				for (int cid : neighborClauseIDs) {
					Clause d = inst.getClause(cid);
					boolean wasSat = d.sat;
					d.sat = false;
					for (int u : d.getVars()) {
						if ((u > 0) && (assignment.get(u) == 1) ) {
							d.sat = true;
							break;
						} else if ((u < 0) && (assignment.get(-u) == 0)) {
							d.sat = true;
							break;
						}
					}
					if (!d.sat) {
						if (wasSat) {
							numUnsat++;
						}
						if (d.isSet()) {
							inst.unsetClause(d);
						}
					} else if (!wasSat) {
						numUnsat--;
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
	
	public void setBeta(double b) {
		beta = b;
	}
	
	public static void main(String[] args) {
		//		String[] suffixes = {"3_5", "4"};
		float movingAverage;
		String[] filenames = {"src/Files/krand/v10000c40000.cnf"};
		try {
			for (String name : filenames) {
				PrintWriter writer = new PrintWriter(name + ".out", "UTF-8");
				File child = new File(name);
				System.out.println("Filename: " + name);
				writer.println("Filename: " + name);
				ProblemInstance sat = DimacsParser.parseDimacsFile(child.toString());
				Solver sol = new Solver(1.75, sat);
				movingAverage = 0;
				writer.println("Break penalty is 1.75");
				for (int iteration = 0; iteration < 10; iteration++) {
					writer.print("\t\t");
					System.out.println("it = " + iteration);
					int steps = sol.solve();
					movingAverage = (steps + iteration * movingAverage) / (iteration + 1);
					writer.println("Iteration " + iteration + ": " + steps);
					writer.flush();
					sol.initialize();
				}
				System.out.println();
				writer.println("Average: " + movingAverage);
				writer.flush();
				writer.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


}
