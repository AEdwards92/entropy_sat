import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import org.math.plot.*;

import javax.swing.*;

public class Solver {
	protected ProblemInstance inst;
    private boolean exponential;
	protected double beta;
	private double probs[];
	private int flipHist[] = new int[8];
	protected List<Integer> assignment;
	static final int CUTOFF = 10000000;
	
	public Solver() {
		beta = 1.0;
        exponential = true;
		assignment = new ArrayList<Integer>();
	}

	public Solver(double cb) {
		beta = cb;
        exponential = true;
		assignment = new ArrayList<Integer>();
	}

	public Solver(ProblemInstance sat) {
		inst = sat;
        exponential = true;
		assignment = new ArrayList<Integer>();
		initialize();
	}

	public Solver(double cb, ProblemInstance sat) {
		inst = sat;
		beta = cb;
        exponential = true;
		assignment = new ArrayList<Integer>();
		initialize();
	}

	public Solver(double cb, ProblemInstance sat, boolean exp) {
		inst = sat;
		beta = cb;
        exponential = exp;
		assignment = new ArrayList<Integer>();
		initialize();
	}

	/*
	 * Initializer used by all constructors that pass in an existing
	 * ProblemInstance. Can also be used to reset a solver.
	 */

	public void initialize() {
		if (assignment == null) {
			System.err
					.println("Error: attempting to initalize with null assignment list");
			System.err.println("Terminating program");
			System.exit(1);
		}
		assignment.clear();

		if (inst == null) {
			System.err
					.println("Error: attempting to initalize with null instance");
			System.err.println("Terminating program");
			System.exit(1);
		}

		inst.resetClauses();

		probs = new double[ inst.getMaxOccur()];
		System.out.println(inst.getMaxOccur());
		for (int i = 0; i <  inst.getMaxOccur(); i++) {
			if (exponential) {
				probs[i] = Math.exp(-beta * i);
			} else {
				probs[i] = Math.pow(0.9 + i, -beta);
			}
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

	/*
	 * Returns whether or not the current assignment satisfies the
	 * ProblemInstance.
	 */
	private boolean satisfied() {
		boolean sat;
		for (int i = 0; i < inst.getNumClauses(); i++) {
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

	/*
	 * Uniform random sample method Clause c is assumed to be unsatisfied. We
	 * sample uniformly the set of 2^k possibilities, but we ensure that the
	 * resulting, random assignment fixes clause c.
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
	
	private void balintSample(Clause c) {
		int varBreakCnt[] = new int[c.size()];
		double Z = 0;
		double dist[] = new double[c.size()];
		
		for (int i = 0; i < c.size(); i++) {
			int v = c.getVar(i);
			List<Integer> neighbors = inst.signedOccurrenceMap.get(-v);
			if (neighbors == null)
				continue;
			neighbor_loop:
			for (int cid : neighbors) {
				Clause d = inst.getClause(cid);
		        if (!d.sat)
		        	continue;
				for (int u : d.getVars()) {
					if (u != -v && 
						((u < 0 && assignment.get(-u) == 0)
					  || (u > 0 && assignment.get(u)  == 1)))
						continue neighbor_loop;
				}
				varBreakCnt[i] += 1;
			}
		}

		for (int i = 0; i < c.size(); i++) {
//			probs[i] = Math.pow(Math.E, -beta * breakCnt[i]);
			dist[i] = probs[varBreakCnt[i]];
			Z += dist[i];
		}
		
		
		double r = Math.random() * Z;
		int flip;
		for (flip = 0; flip < c.size(); flip++) {
			r -= dist[flip];
			if (r <= 0)
				break;
		}
		assignment.set(Math.abs(c.getVar(flip)),
				1 - assignment.get(Math.abs(c.getVar(flip))));
	}

	
	private void sample(Clause c) {
		int breakCnt[] = new int[8];
		int v[] = {c.getVar(0), c.getVar(1), c.getVar(2)};
		double Z = 0;
		double dist[] = new double[8];

		for (int i = 1; i < 8; i++) {
			int s0 = (int) Math.pow(-1, i);
			int s1 = (int) Math.pow(-1, i/2);
			int s2 = (int) Math.pow(-1, i/4);
			List<Integer> neighbors  = new ArrayList<Integer>();
			if (s0 == -1)
				neighbors = Util.union(neighbors, inst.signedOccurrenceMap.get(s0 * v[0]));
			if (s1 == -1) 
				neighbors = Util.union(neighbors, inst.signedOccurrenceMap.get(s1 * v[1]));
			if (s2 == -1) 
				neighbors = Util.union(neighbors, inst.signedOccurrenceMap.get(s2 * v[2]));
			if (neighbors == null)
				continue;
			neighbor_loop:
				for (int cid : neighbors) {
					Clause d = inst.getClause(cid);
					if (!d.sat)
						continue;
					for (int u : d.getVars()) {
						if (s0 == -1 && u == -v[0])
							continue;
						if (s1 == -1 && u == -v[1])
							continue;
						if (s2 == -1 && u == -v[2])
							continue;
						if (s0 == -1 && u == v[0])
							continue neighbor_loop;
						if (s1 == -1 && u == v[1])
							continue neighbor_loop;
						if (s2 == -1 && u == v[2])
							continue neighbor_loop;
						if ((u < 0 && assignment.get(-u) == 0) 
								|| (u > 0 && assignment.get(u)  == 1))
							continue neighbor_loop;
					}
					breakCnt[i] += 1;
				}
		}

		for (int i = 1; i < 8; i++) {
			if (exponential) {
				dist[i] = Math.exp(-beta * breakCnt[i]);
			} else {
				dist[i] = Math.pow(0.9 + breakCnt[i], -beta);
			}
			Z += dist[i];
		}

		// Sample
		int flip;
		double r = Math.random() * Z;
		for (flip = 1; flip <= 7; flip++) {
			r -= dist[flip];
			if (r <= 0)
				break;
		}
		if ((flip & 1) == 1) {
			assignment.set(Math.abs(c.getVars().get(0)),
					1 - assignment.get(Math.abs(c.getVars().get(0))));
		}
		if ((flip & 2) == 2) {
			assignment.set(Math.abs(c.getVars().get(1)),
					1 - assignment.get(Math.abs(c.getVars().get(1))));
		}
		if ((flip & 4) == 4) {
			assignment.set(Math.abs(c.getVars().get(2)),
					1 - assignment.get(Math.abs(c.getVars().get(2))));
		}
		flipHist[flip] += 1;
	}

	private double entropy(double probs[]) {
		double h = 0;
		for (double p : probs) {
			if (p > 0)
				h += p * Math.log(1.0 / p) / Math.log(2);
		}
		return h;
	}

	public int solve() {
		List<Clause> unsetClauses;
		List<Clause> unsatClauses = new ArrayList<Clause>();
		int choice;
		int step;
		int numUnset = -1;
		int numUnsat = 0;
		Random randomGenerator = new Random();
		// Compute the number of unsatisfied clauses
		for (int i = 0; i < inst.getNumClauses(); i++) {
			Clause c = inst.getClause(i);
			c.sat = false;
			for (int v : c.getVars()) {
				if ((v < 0 && assignment.get(-v) == 0)
						|| (v > 0 && assignment.get(v) == 1)) {
					c.sat = true;
					break;
				}
			}
			if (!c.sat) {
				unsatClauses.add(c);
				numUnsat += 1;
			} else {
				inst.setClause(c);
			}
		}
		int minUnsat = inst.numClauses;
		for (step = 1; step < CUTOFF; step++) {
			unsetClauses = inst.getUnsetClauses();
			numUnset = unsetClauses.size();
			if (numUnsat == 0) {
				if (satisfied()) {
					System.out.println("All satisifed! Number unset: "
							+ numUnset);
					System.out.println("Total steps: " + step);
					break;
				} else {
					System.err
							.println("unsetClauses empty while problem not satisfied");
				}
			}
			if (numUnsat < minUnsat) {
				minUnsat = numUnsat;
			}
			if (step % 100000 == 0) {
				System.out.println("Number unset: " + numUnset);
				System.out.println("Number unsat: " + numUnsat);
				System.out.println("Min. unsat:   " + minUnsat);
				System.out.println("Total steps:  " + step);
				System.out.println("flipHist:     " + Util.arrayString(flipHist));
			}
			choice = randomGenerator.nextInt(numUnsat);
			Clause c = unsatClauses.get(choice);
			
			// Sample V(c)
			sample(c);

			inst.setClause(c);
			if (!c.sat) {
				c.sat = true;
				unsatClauses.remove(c);
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
						if ((u > 0) && (assignment.get(u) == 1)) {
							d.sat = true;
							break;
						} else if ((u < 0) && (assignment.get(-u) == 0)) {
							d.sat = true;
							break;
						}
					}
					if (!d.sat) {
						if (wasSat) {
							unsatClauses.add(d);
							numUnsat++;
						}
						if (d.isSet()) {
							inst.unsetClause(d);
						}
					} else if (!wasSat) {
						unsatClauses.remove(d);
						numUnsat--;
					}
				}
			}
		}
		if (!satisfied()) {
			System.out.println("Time out");
			System.out.println("Number unset: " + numUnset);
			System.out.println("Number unsat: " + numUnsat);
		}
		return step;
	}

	public int solve(ProblemInstance sat) {
		inst = sat;
		initialize();
		return solve();
	}
	
	private boolean unsatisfied(Clause c) {
		for (int v : c.getVars()) {
			if ((v < 0 && assignment.get(-v) == 0)
					|| (v > 0 && assignment.get(v) == 1)) {
				return false;
			}
		}
		return true;
	}

	public void setBeta(double b) {
		beta = b;
	}

	public static void main(String[] args) {
		// String[] suffixes = {"3_5", "4"};
		float movingAverage;
		String[] filenames = { "src/Files/krand/v10000c42000.cnf" };
		try {
			for (String name : filenames) {
				PrintWriter writer = new PrintWriter(name + ".out", "UTF-8");
				File child = new File(name);
				System.out.println("Filename: " + name);
				writer.println("Filename: " + name);
				ProblemInstance sat = DimacsParser.parseDimacsFile(child
						.toString());
//				for (int i = 1; i <= 20; i++) {
					Solver sol = new Solver(2.09, sat, false);
					movingAverage = 0;
					writer.println("Break penalty is " + (2.09));
					System.out.println("Break penalty is " + (2.09));
					for (int iteration = 0; iteration < 20; iteration++) {
						writer.print("\t\t");
						System.out.println("it = " + iteration);
						int steps = sol.solve();
						movingAverage = (steps + iteration * movingAverage)
								/ (iteration + 1);
						writer.println("Iteration " + iteration + ": " + steps);
						writer.flush();
						sol.initialize();
					}
					System.out.println();
					writer.println("Average: " + movingAverage);
					writer.println();
//				}
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
