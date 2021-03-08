
import com.hstairs.ppmajal.conditions.*;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.problem.RelState;

import java.util.*;

import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

public class RPG {

	public int levels;
	public Vector action_level;
	public Vector rel_state_level;
	public ComplexCondition goal;
	public boolean goal_reached;
	public Map<Predicate, Set<Predicate>> firstAchiever;
	public int goal_reached_at;
	private long cpu_time;
	private long spezzTime;
	private int numberOfActions;
	private long FPCTime;
	private Set relevantActions;
	private PDDLState init;
	private RelState fixPoint;

	public RPG() {
		super();
		levels = 0;
		action_level = new Vector();
		rel_state_level = new Vector();
		goal_reached = false;
		spezzTime = 0;
		FPCTime = 0;
		goal_reached_at = 0;
		relevantActions = new HashSet();
	}

	public RPG(PDDLState init) {
		super();
		levels = 0;
		action_level = new Vector();
		rel_state_level = new Vector();
		goal_reached = false;
		spezzTime = 0;
		FPCTime = 0;
		relevantActions = new HashSet();
		this.init = init;
		goal_reached_at = 0;
	}
//	    NumericPlanningGraph(RelState s, Conditions goal){
//	        super();
//	        levels=0;
//	        action_level = new ArrayList();
//	        rel_state_level = new ArrayList();
//	        rel_state_level.add(s);
//	        this.goal = goal;
//	        goal_reached = false;
//	    }
	
	
	public MultiValuedMap computeRelaxedPlan(PDDLState s, ComplexCondition goal, Set actions)
			throws CloneNotSupportedException {

		// System.out.println("Find relaxed plan");
		ArrayList ret = new ArrayList();

		this.goal = goal;
		RelState current = s.relaxState();
		rel_state_level.add(current.clone());

		// actions to be used for planning purposes. This list is a temporary collection
		// to do not compromise the input structure
		ArrayList acts = new ArrayList();
		acts.addAll(actions);
		long start = System.currentTimeMillis();
		ArrayList level = new ArrayList();
		levels = 0;
		
		
		ArrayList previousLevel = new ArrayList();
		
		while (true) {
			
			//System.out.println("CURRENT:"+current);
			if (current.satisfy(goal)) {
				goal_reached = true;
				this.goal_reached_at = levels;
				System.out.println("Goal reached at level " + levels);
				break;// goal reached!
			} else {

				
			//	System.out.println("Do sth");
				level = new ArrayList();
				for (Iterator it = acts.iterator(); it.hasNext();) {
					GroundAction gr = (GroundAction) it.next();
					if (gr.isApplicable(current)) {
						// if (gr.getPreconditions().isSatisfied(current)) {
						level.add(gr);
						if (gr.getNumericEffects() == null) {
							it.remove();
						}

					}
				}
			//	System.out.println("Level Size: " + level.size());
				
				//
				if (level.isEmpty() || level.equals(previousLevel)) {
					this.goal_reached = false;
					numberOfActions = Integer.MAX_VALUE;
				//	System.out.println("Relaxed plan computation: goal not reached");
					return null;// it means that the goal is unreacheable!
				}
				
				previousLevel = (ArrayList) level.clone();
				
				
				long start2 = System.currentTimeMillis();

				for (Object o : level) {
					GroundAction gr = (GroundAction) o;
					// System.out.println("Applying effect of:"+gr);
					gr.apply(current);
					
				}
				spezzTime += System.currentTimeMillis() - start2;
				this.action_level.add(level);
				this.rel_state_level.add(current.clone());
				levels++;
				// System.out.println("Level:"+levels);
			}
		}

		this.setFixPoint(current.clone());
		// System.out.println(current);
		this.goal_reached = true;
		System.out.println("Graphplan Building Time:" + (System.currentTimeMillis() - start));
		// System.out.println("Spezz Time:" + (spezzTime));
		// System.out.println("NumbersOfLevel" + (levels));
		long start2 = System.currentTimeMillis();
		MultiValuedMap ret1 = extractPlan(goal, levels);
		// System.out.println("estrazione:" + (System.currentTimeMillis() - start2));
		cpu_time = System.currentTimeMillis() - start;
		if (ret1 == null) {
			numberOfActions = Integer.MAX_VALUE;
			return ret1;
		}
		numberOfActions = ret1.size();

		// this considers also the interactions among actions. Inadmissible Heuristic
		// this.goal_reached_at = ret1.size();
		return ret1;

	}

	

	public int computeUntilFixedPoint(PDDLState s, Set actions) {

		RelState current = s.relaxState();
		ArrayList acts = new ArrayList(100000);
		acts.addAll(actions);
		long start = System.currentTimeMillis();
		HashSet level = new HashSet(10000);
		numberOfActions = 0;
		while (true) {
			boolean newActions = false;
			for (Iterator it = acts.iterator(); it.hasNext();) {
				GroundAction gr = (GroundAction) it.next();
				if (gr.getPreconditions().can_be_true(current)) {
					newActions = true;
					level.add(gr);
					this.numberOfActions++;
					it.remove();
				}
			}
			if (!newActions) {
				break;// it means that the goal is unreacheable!
			}
			long start2 = System.currentTimeMillis();

			for (Object o : level) {
				GroundAction gr = (GroundAction) o;
				// System.out.println(gr.getName());
				gr.apply(current);
			}
			spezzTime += System.currentTimeMillis() - start2;
			levels++;
		}

		fixPoint = current.clone();
		FPCTime = System.currentTimeMillis() - start;
		System.out.println("Graphplan Building Time:" + (System.currentTimeMillis() - start));
		System.out.println("NumbersOfLevel" + (levels));

		return numberOfActions;
	}

	public Set computeActionsUntilFixedPoint(PDDLState s, Set actions) {

		RelState current = s.relaxState();
		Set acts = new HashSet();
		acts.addAll(actions);
		long start = System.currentTimeMillis();
		HashSet level = new HashSet(10000);
		numberOfActions = 0;
		while (true) {

			boolean newActions = false;
			for (Iterator it = acts.iterator(); it.hasNext();) {
				GroundAction gr = (GroundAction) it.next();
				if (gr.getPreconditions().can_be_true(current)) {
					newActions = true;
					level.add(gr);
					this.numberOfActions++;
					it.remove();
				}
			}
			if (!newActions) {
				break;// it means that the goal is unreacheable!
			}
			long start2 = System.currentTimeMillis();

			for (Object o : level) {
				GroundAction gr = (GroundAction) o;
				// System.out.println(gr.getName());
				gr.apply(current);
			}
			spezzTime += System.currentTimeMillis() - start2;
			levels++;
		}
		fixPoint = current.clone();
		FPCTime = System.currentTimeMillis() - start;
		System.out.println("Graphplan Building Time:" + (System.currentTimeMillis() - start));
		System.out.println("Levels Number: " + (levels));
		relevantActions = level;
		return level;
	}

	// The following function computes reacheability for the propositional part of
	// the problem. The numeric part is also considered but there just for the
	// purpose of identifying a
	// the relevant set of actions
	public Set reacheability(PDDLState s, Set actions) {

		RelState current = s.relaxState();
		Set acts = new HashSet();
		acts.addAll(actions);
		long start = System.currentTimeMillis();
		LinkedHashSet level = new LinkedHashSet();
		numberOfActions = 0;
		levels = 0;
		while (true) {
			boolean newActions = false;
			for (Iterator it = acts.iterator(); it.hasNext();) {
				GroundAction gr = (GroundAction) it.next();
				if (gr.getPreconditions().can_be_true(current)) {
					newActions = true;
					level.add(gr);
					this.numberOfActions++;
					it.remove();
				}
			}
			// storing information about the fact that the goal has been reached. Pay
			// attention that for the numeric case having a goal that is not reached does
			// not imply that
			// the task is not solvable (using this procedure).
			if (!goal_reached && current.satisfy(goal)) {
				goal_reached = true;
				goal_reached_at = levels;
				// break;
			}
			// System.out.println(level.size());
			this.action_level.add(level.clone());
			if (!newActions) {
				break;// fixpoint reached for the propositional part. For the numeric this is not a
						// fixpoint, and seems hard to define a fixpoint for the numeric case
			}
			long start2 = System.currentTimeMillis();
			for (Object o : level) {
				GroundAction gr = (GroundAction) o;
				// System.out.println(gr.getName());
				gr.apply(current);
			}
			spezzTime += System.currentTimeMillis() - start2;
			levels++;
			// System.out.println("Level:"+levels);
		}
		System.out.println("Total Number of Levels Developped:" + levels);
		fixPoint = current.clone();
		FPCTime = System.currentTimeMillis() - start;
		relevantActions = level;
		this.cpu_time = System.currentTimeMillis() - start;
		return level;
	}

	// The following function computes reacheability for the propositional part of
	// the problem. The numeric part is also considered but there just for the
	// purpose of identifying a
	// the relevant set of actions. As before but it stops when the goal is reached
	// in the relaxed state.
	public Set reacheabilityTillGoal(PDDLState s, Condition goal, Set actions) {

		RelState current = s.relaxState();
		Set acts = new HashSet();
		acts.addAll(actions);
		long start = System.currentTimeMillis();
		HashSet level = new HashSet(10000);
		numberOfActions = 0;
		levels = 0;

		while (true) {
			if (current.satisfy(goal)) {
				goal_reached = true;
				break;
			}
			boolean newActions = false;
			for (Iterator it = acts.iterator(); it.hasNext();) {
				GroundAction gr = (GroundAction) it.next();

				if (gr.getPreconditions() == null || gr.getPreconditions().can_be_true(current)) {
					newActions = true;
					level.add(gr);
					this.numberOfActions++;
					it.remove();
				}
			}
			// System.out.println("Here:"+action_level.size());
			// System.out.println(level.size());
			this.action_level.add(level.clone());

			// THIS IS A BUG: IT SHOULD CONSIDER THE FACT THAT THE SATISFACTION OF AT LEAST
			// A CONDITION IS SATISFIED!
			if (!newActions) {
				System.out.println("No new actions applicable and goal is not reacheable...");
				break;// it means that the goal is unreacheable!
			}
			long start2 = System.currentTimeMillis();

			for (Object o : level) {
				GroundAction gr = (GroundAction) o;
				// System.out.println(gr.getName());
				gr.apply(current);
			}
			spezzTime += System.currentTimeMillis() - start2;
			levels++;
		}
		fixPoint = current.clone();
		FPCTime = System.currentTimeMillis() - start;
		relevantActions = level;
		this.cpu_time = System.currentTimeMillis() - start;
		return level;
	}

	private MultiValuedMap extractPlan(ComplexCondition goal, int levels) {
		ComplexCondition AG[] = new ComplexCondition[levels + 1];
		AG[levels] = goal;
		//MultiValuedMap rel_plan = new MultiValuedMap();
		MultiValuedMap<Integer, GroundAction> rel_plan = new ArrayListValuedHashMap<>();

		for (int t = levels - 1; t >= 0; t--) {
			RelState tem = (RelState) rel_state_level.get(t);
			RelState s = tem.clone();
			AG[t] = new AndCond();
			Map visited = new HashMap();
			// System.out.println("Prima aggiunta proposizioni"+AG[t + 1].isSatisfied(s));
			for (Object o : AG[t + 1].sons) {
				if (o instanceof Predicate) {
					Predicate p = (Predicate) o;
					// System.out.println("Livello in esame:" + t);
					// System.out.println(p.isSatisfied((RelState) this.rel_state_level.get(t )));
					if ((!((p.can_be_true((RelState) rel_state_level.get(t)))))) {
						// if (!((p.isSatisfied(s)))) {
						if (visited.get(p) == null) {
							// System.out.println("Searching for the support for:" + p);
							// GroundAction gr = searchForAndRemove((Collection) this.action_level.get(t),
							// p);
							GroundAction gr = this.searchSupporter((Collection) this.action_level.get(t), p);
							if (gr == null) {
								System.out.println("Error!! No supporter for " + p + " level " + t);
								System.out.println(rel_plan);
								System.out.println("Requisiti livello successivo:" + AG[t + 1]);
								System.exit(-1);
							}
							AG[t].sons.addAll(gr.getPreconditions().sons);
							// System.out.println("AG[t]" + AG[t]);
							// AG[t].sons.addAll(gr.getNumeric().sons);
							// if (gr.getName().equalsIgnoreCase("tp")){
							// System.out.println("prima applicazione tp" +s);
							// }

							for (Object o1 : gr.getAddList().sons) {
								visited.put(o1, true);
							}
							gr.apply(s);
							// if (gr.getName().equalsIgnoreCase("tp-lr")){
							// System.out.println("dopo applicazione tp" +s);
							// }
							// AG[t].sons.remove(o);
							rel_plan.put(t,gr);
						}

					} else {
						AG[t].sons.add(p);
					}

				}
			}
//	            for(Object o:rel_plan){
//	                GroundAction gr = (GroundAction)o;
//	                gr.apply(s);
//	            }

			Iterator it = AG[t + 1].sons.iterator();
			while (it.hasNext()) {
				Object o = it.next();
				if (o instanceof Comparison) {
					Comparison comp = (Comparison) o;
//	                    while(true){
					for (GroundAction gr : (Collection<GroundAction>) this.action_level.get(t)) {
						if (comp.can_be_true(s)) {
							AG[t].sons.add(comp);
							break;
						}
						gr.generateAffectedNumFluents();
//	                            System.out.println("Condizione non soddisfatta:" + comp);
//	                            System.out.println("Piano correntemente trovato:");
//	                            for (int z = 0; z < rel_plan.length; z++) {
//	                               if (rel_plan[z] != null) {
//	                                  System.out.println(rel_plan[z]);
//	                               }
//	                            }
						if (comp.involve(gr.getNumericFluentAffected())) {
							// System.out.println(gr.getNumericFluentAffected());
							gr.apply(s);
							AG[t].sons.addAll(gr.getPreconditions().sons);
							rel_plan.put(t,gr);
						}

					}
					// !comp.isSatisfied(s)
//	                    }
				}
			}

		}

		return rel_plan;

	}

	private ArrayList[] extractPlan_new() {
		ComplexCondition AG;
		AG = this.goal;
		ArrayList rel_plan[] = new ArrayList[levels];

		for (int t = levels - 1; t >= 0; t--) {
			RelState s = (RelState) rel_state_level.get(t);
			// AG[t] = AG[t+1].clone();
			Iterator it = AG.sons.iterator();
			// for (Iterator it=AG.sons.iterator();it.hasNext()){
			while (it.hasNext()) {
				Object o = it.next();
				if (o instanceof Predicate) {
					Predicate p = (Predicate) o;
					// System.out.println("Livello in esame:" + t);
					// System.out.println(p.isSatisfied((RelState) this.rel_state_level.get(t )));
					if (!((p.can_be_true((RelState) this.rel_state_level.get(t))))) {
						// System.out.println("Searching for the support for:" + p);
						GroundAction gr = searchForAndRemove((HashSet) this.action_level.get(t), p);
						if (gr == null) {

							System.out.println("Error!! No supporter for " + p + " level " + t);
							System.exit(-1);
						}
						// AG[t].sons.addAll(gr.getPreconditions().sons);
						// AG[t].sons.addAll(gr.getNumeric().sons);
						gr.apply(s);
						if (rel_plan[t] == null) {
							rel_plan[t] = new ArrayList();
						}
						rel_plan[t].add(gr);
						// AG.sons.remove(o);
						it.remove();
					}

				}

			}
//	            while (!AG.isSatisfied(s)) {
////	                GroundAction gr = bestSupport((HashSet) this.action_level.get(t), AG, s);
//	                AG.sons.addAll(gr.getPreconditions().sons);
//	                //AG[t].sons.addAll(gr.getNumeric().sons);
//	                gr.apply(s);
//	                rel_plan[t].add(gr);
//	            }
		}

		return rel_plan;

	}

	private GroundAction searchForAndRemove(Collection get, Predicate p) {
		Iterator it = get.iterator();
		while (it.hasNext()) {
			GroundAction gr = (GroundAction) it.next();
			if (gr.getAddList().sons.contains(p)) {
				it.remove();
				return gr;
			}
		}
		return null;
	}

	private GroundAction bestSupport(HashSet get, Condition conditions, RelState s) {

		float bestDistance = 0;
		GroundAction ret = null;
		//
		AndCond c;
		if (conditions instanceof AndCond) {
			c = (AndCond) conditions;
			for (Object o : c.sons) {
				if (o instanceof Comparison) {
					Comparison comp = (Comparison) o;
					bestDistance += comp.satisfactionDistance(s);
				}

			}
			Iterator it = get.iterator();
			while (it.hasNext()) {
				GroundAction gr = (GroundAction) it.next();
//	                if (gr.getName().contains("tp"))
//	                   System.out.println("tp");
//	                if (gr.getName().contains("comm"))
//	                   System.out.println("comm");
				Float distance2 = new Float(0);
				RelState s1 = s.clone();
				gr.apply(s1);
				for (Object o : c.sons) {
					if (o instanceof Comparison) {
						Comparison comp = (Comparison) o;
						distance2 += comp.satisfactionDistance(s1);
					}

				}

				if (distance2 < bestDistance) {
//	                    System.out.println("Distanza dopo.." + distance2);
					it.remove();
					return gr;

				}

			}
////	          System.out.println("Comparison:" + comp);
//	            System.out.println("Distanza prima.." + distance);
		} else {
			System.out.println("Coniditions not supported");
		}
		return ret;
	}

	/**
	 * @return the cpu_time
	 */
	public long getCpu_time() {
		return cpu_time;
	}

	/**
	 * @return the timeForNumbers
	 */
	public long getTimeForNumbers() {
		return spezzTime;
	}

	/**
	 * @return the numberOfActions
	 */
	public int getNumberOfActions() {
		return numberOfActions;
	}

	private GroundAction searchSupporter(Collection hashSet, Predicate p) {
		Iterator it = hashSet.iterator();
		while (it.hasNext()) {
			GroundAction gr = (GroundAction) it.next();
			if (gr.getAddList().sons.contains(p)) {
				return gr;
			}
		}
		return null; 
	}

	public Set findFirstLevelActions() {

		Set actions = new HashSet();
		if (this.init == null) {
			System.out.println("Initial state unknown");
		}
		// System.out.println("The number of relevant actions is:
		// "+relevantActions.size());
		for (GroundAction gr : (Set<GroundAction>) this.relevantActions) {
			// System.out.println(gr);
			if (gr.isApplicable(init)) {
				actions.add(gr);
			}
		}
		return actions;
	}

	/**
	 * @return the fixPoint
	 */
	public RelState getFixPoint() {
		return fixPoint;
	}

	/**
	 * @param fixPoint the fixPoint to set
	 */
	public void setFixPoint(RelState fixPoint) {
		this.fixPoint = fixPoint;
	}

	public RelState computeStateBound(PDDLState init, ComplexCondition goals, Set actions) {

		this.goal = goals;
		RelState current = init.relaxState();
		rel_state_level.add(current.clone());

		// actions to be used for planning purposes. This list is a temporary collection
		// to do not compromise the input structure
		ArrayList acts = new ArrayList();
		acts.addAll(actions);
		long start = System.currentTimeMillis();
		ArrayList level = new ArrayList();
		while (true) {
			if (current.satisfy(goal)) {
				goal_reached = true;
				break;// goal reached!
			} else {
				level = new ArrayList();
				for (Iterator it = acts.iterator(); it.hasNext();) {
					GroundAction gr = (GroundAction) it.next();
					if (gr.isApplicable(current)) {
						// if (gr.getPreconditions().isSatisfied(current)) {
						level.add(gr);
						it.remove();
					}
				}
				if (level.isEmpty()) {
					this.goal_reached = false;
					numberOfActions = Integer.MAX_VALUE;
					return null;// it means that the goal is unreacheable!
				}
				long start2 = System.currentTimeMillis();

				for (Object o : level) {
					GroundAction gr = (GroundAction) o;
					gr.apply(current);
				}
				spezzTime += System.currentTimeMillis() - start2;
				this.action_level.add(level);
				this.rel_state_level.add(current.clone());
				levels++;
			}
		}

		this.goal_reached = true;
		// System.out.println("Graphplan Building Time:" + (System.currentTimeMillis() -
		// start));
		// long start2 = System.currentTimeMillis();

		cpu_time = System.currentTimeMillis() - start;

		return current;

	}

	private void addPredicatesPrecondition(HashSet lmOfP, Map<Predicate, Set<Predicate>> ret, GroundAction gr) {

		if (gr.getPreconditions() == null) {
			return;
		}

		for (Object o : gr.getPreconditions().sons) {
			if (o instanceof Predicate) {
				lmOfP.addAll(ret.get(o));

			}

		}

	}

	private void intersectPredicatesPrecondition(HashSet lmOfP, Map<Predicate, Set<Predicate>> ret, GroundAction gr) {
		if (gr.getPreconditions() == null) {
			lmOfP.clear();
		} else {
			HashSet preconditions = new HashSet();
			for (Object o : gr.getPreconditions().sons) {
				if (o instanceof Predicate) {
					preconditions.addAll(ret.get(o));
				}
			}

			lmOfP.retainAll(preconditions);

		}
	}

	private void setFA(Predicate p, GroundAction gr, Map<Predicate, Set<Predicate>> FA) {

		HashSet set = new HashSet();
		for (Object o : gr.getPreconditions().sons) {
			if (o instanceof Predicate) {
				set.add(o);
			}
		}

		FA.put(p, set);

	}

}
