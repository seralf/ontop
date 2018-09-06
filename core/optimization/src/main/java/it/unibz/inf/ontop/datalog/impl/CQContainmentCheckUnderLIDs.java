package it.unibz.inf.ontop.datalog.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unibz.inf.ontop.datalog.CQIE;
import it.unibz.inf.ontop.datalog.CQContainmentCheck;
import it.unibz.inf.ontop.datalog.DatalogFactory;
import it.unibz.inf.ontop.datalog.LinearInclusionDependencies;
import it.unibz.inf.ontop.model.atom.AtomPredicate;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.model.term.functionsymbol.Predicate;
import it.unibz.inf.ontop.substitution.Substitution;
import it.unibz.inf.ontop.substitution.SubstitutionBuilder;
import it.unibz.inf.ontop.substitution.impl.SubstitutionUtilities;
import it.unibz.inf.ontop.substitution.impl.UnifierUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CQContainmentCheckUnderLIDs implements CQContainmentCheck {

	private static final Logger LOGGER = LoggerFactory.getLogger(CQContainmentCheckUnderLIDs.class);
	
	private final Map<CQIE,IndexedCQ> indexedCQcache = new HashMap<>();
	
	private final LinearInclusionDependencies dependencies;
	private final DatalogFactory datalogFactory;
	private final UnifierUtilities unifierUtilities;
	private final SubstitutionUtilities substitutionUtilities;
	private final TermFactory termFactory;

	/***
	 * Constructs a CQC utility using the given query. If Sigma is not null and
	 * not empty, then it will also be used to verify containment w.r.t.\ Sigma.
	 * @param datalogFactory
	 * @param unifierUtilities
	 * @param substitutionUtilities
	 * @param termFactory
	 */
	public CQContainmentCheckUnderLIDs(DatalogFactory datalogFactory, UnifierUtilities unifierUtilities,
									   SubstitutionUtilities substitutionUtilities, TermFactory termFactory) {
		this.datalogFactory = datalogFactory;
		this.unifierUtilities = unifierUtilities;
		this.substitutionUtilities = substitutionUtilities;
		this.termFactory = termFactory;
		dependencies = null;
	}

	/**
	 * *@param sigma
	 * A set of ABox dependencies
	 */
	public CQContainmentCheckUnderLIDs(LinearInclusionDependencies dependencies, DatalogFactory datalogFactory,
									   UnifierUtilities unifierUtilities, SubstitutionUtilities substitutionUtilities,
									   TermFactory termFactory) {
		this.dependencies = dependencies;
		this.datalogFactory = datalogFactory;
		this.unifierUtilities = unifierUtilities;
		this.substitutionUtilities = substitutionUtilities;
		this.termFactory = termFactory;
	}
	
	
	/**
	 * This method is used to chase foreign key constraint rule in which the rule
	 * has only one atom in the body.
	 * 
	 * IMPORTANT: each rule is applied only ONCE to each atom
	 * 
	 * @param atoms
	 * @return set of atoms
	 */
	private Set<Function> chaseAtoms(Collection<Function> atoms) {

		Set<Function> derivedAtoms = new HashSet<>();
		for (Function fact : atoms) {
			derivedAtoms.add(fact);
			for (CQIE rule : dependencies.getRules((AtomPredicate) fact.getFunctionSymbol())) {
				rule = datalogFactory.getFreshCQIECopy(rule);
				Function ruleBody = rule.getBody().get(0);
				Substitution theta = unifierUtilities.getMGU(ruleBody, fact);
				if (theta != null && !theta.isEmpty()) {
					Function ruleHead = rule.getHead();
					Function newFact = (Function)ruleHead.clone();
					// unify to get fact is needed because the dependencies are not necessarily full
					// (in other words, they may contain existentials in the head)
					substitutionUtilities.applySubstitution(newFact, theta);
					derivedAtoms.add(newFact);
				}
			}
		}
		return derivedAtoms;
	}
	
	public final class IndexedCQ {
		
		private final Function head;
		/***
		 * An index of all the facts obtained by freezing this query.
		 */
		private final Map<Predicate, List<Function>> factMap;

		/***
		 * Computes a query in which all terms have been replaced by
		 * ValueConstants that have the no type and have the same 'name' as the
		 * original variable.
		 * 
		 * This new query can be used for query containment checking.
		 * 
		 */
		
		public IndexedCQ(Function head, Collection<Function> body) { 
			
			this.head = head;

			this.factMap = new HashMap<>(body.size() * 2);
			for (Function atom : body) 
				// not boolean, not algebra, not arithmetic, not datatype
				if (atom != null && atom.isDataFunction()) {
					Predicate pred = atom.getFunctionSymbol();
					List<Function> facts = factMap.get(pred);
					if (facts == null) {
						facts = new LinkedList<Function>();
						factMap.put(pred, facts);
					}
					facts.add(atom);
				}
		}
		
		private Substitution computeHomomorphism(CQIE query) {
			SubstitutionBuilder sb = new  SubstitutionBuilder(termFactory);

			// get the substitution for the head first 
			// it will ensure that all answer variables are mapped either to constants or
			//       to answer variables in the base (but not to the labelled nulls generated by the chase)
			boolean headResult = HomomorphismUtilities.extendHomomorphism(sb, query.getHead(), head);
			if (!headResult)
				return null;
			
			Substitution sub = HomomorphismUtilities.computeHomomorphism(sb, query.getBody(), factMap);
			
			return sub;
		}	
	}

	
	/***
	 * True if the first query is contained in the second query
	 *    (in other words, the first query is more specific, it has fewer answers)
	 * 
	 * @param q1
	 * @param q2
	 * @return true if the first query is contained in the second query
	 */
	@Override	
	public boolean isContainedIn(CQIE q1, CQIE q2) {

		if (!q2.getHead().getFunctionSymbol().equals(q1.getHead().getFunctionSymbol()))
			return false;
		
		return (computeHomomorphsim(q1, q2) != null);
	}
	
	@Override
	public Substitution computeHomomorphsim(CQIE q1, CQIE q2) {

        IndexedCQ indexedQ1 = indexedCQcache.get(q1);
        if (indexedQ1 == null) {
        	Collection<Function> q1body = q1.getBody();
        	if (dependencies != null)
        		q1body = chaseAtoms(q1body);
        	
        	indexedQ1 = new IndexedCQ(q1.getHead(), q1body);
    		indexedCQcache.put(q1, indexedQ1);
        }
           
        // just to speed up the check in case there can be no match
        for (Function q2atom : q2.getBody()) 
			if (!indexedQ1.factMap.containsKey(q2atom.getFunctionSymbol())) { 
				// in particular, !q2atom.isDataFunction() 
				return null;
			}
				
		return indexedQ1.computeHomomorphism(q2);
	}	

	static int redundantCounter = 0;
	public static int twoAtomQs = 0;
	public static int oneAtomQs = 0;
	
	public CQIE removeRedundantAtoms(CQIE query) {
		List<Function> databaseAtoms = new ArrayList<>(query.getBody().size());
		
		Set<Term> groundTerms = new HashSet<>();
		for (Function atom : query.getBody())
			// non-database atom
			if (!(atom.getFunctionSymbol() instanceof AtomPredicate)) {
				collectVariables(groundTerms, atom);
			}
			else {
				databaseAtoms.add(atom);
			}

		if (databaseAtoms.size() < 2) {
			oneAtomQs++;
			return query;
		}
		
		collectVariables(groundTerms, query.getHead());
		
		CQIE db = datalogFactory.getCQIE(query.getHead(), databaseAtoms);
		
		for (int i = 0; i < databaseAtoms.size(); i++) {
			Function atomToBeRemoved = databaseAtoms.get(i);
			if (checkRedundant(db, groundTerms, atomToBeRemoved)) {
				LOGGER.warn("  REDUNDANT " + ++redundantCounter + ": " + atomToBeRemoved + " IN " + query);
				query.getBody().remove(atomToBeRemoved);
				databaseAtoms.remove(atomToBeRemoved);
				i--;
			}
		}
		
		twoAtomQs++;
		return query;
	}
	
	private boolean checkRedundant(CQIE db, Set<Term> groundTerms, Function atomToBeRemoved) {
		List<Function> atomsToLeave = new ArrayList<>(db.getBody().size() - 1);
		Set<Term> variablesInAtomsToLeave = new HashSet<>();
		for (Function a: db.getBody()) 
			if (a != atomToBeRemoved) {
				atomsToLeave.add(a);
				collectVariables(variablesInAtomsToLeave, a);
			}
		
		if (!variablesInAtomsToLeave.containsAll(groundTerms)) {
			return false;
		}

		CQIE q0 = datalogFactory.getCQIE(db.getHead(), atomsToLeave);
		// if db is homomorphically embeddable into q0
		if (computeHomomorphsim(q0, db) != null) {
			oneAtomQs++;
			return true;
		}
		return false;
	}


	private static void collectVariables(Set<Term> vars, Function atom) {
		Deque<Term> terms = new LinkedList<>(atom.getTerms());
		while (!terms.isEmpty()) {
			Term t = terms.pollFirst();
			if (t instanceof Variable)
				vars.add(t);
			else if (!(t instanceof Constant))
				terms.addAll(((Function)t).getTerms());
		}		
	}
/*	
	private static boolean containsConstants(Function atom) {
		Deque<Term> terms = new LinkedList<>(atom.getTerms());
		while (!terms.isEmpty()) {
			Term t = terms.pollFirst();
			if (t instanceof Constant)  
				return true;
			else if (!(t instanceof Variable))
				terms.addAll(((Function)t).getTerms());
		}		
		return false;
	}
*/	
	@Override
	public String toString() {
		if (dependencies != null)
			return dependencies.toString();
		
		return "(empty)";
	}
}
