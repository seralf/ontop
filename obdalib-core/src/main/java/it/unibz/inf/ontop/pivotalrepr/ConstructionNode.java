package it.unibz.inf.ontop.pivotalrepr;

import java.util.Optional;

import it.unibz.inf.ontop.model.ImmutableSubstitution;
import it.unibz.inf.ontop.model.ImmutableTerm;

/**
 * Head node an IntermediateQuery
 *
 * TODO: further explain
 *
 */
public interface ConstructionNode extends ExplicitVariableProjectionNode {

    /**
     * (Some) projected variable --> transformed variable
     */
    ImmutableSubstitution<ImmutableTerm> getSubstitution();

    /**
     * TODO: explain
     */
    Optional<ImmutableQueryModifiers> getOptionalModifiers();

    @Override
    ConstructionNode clone();

    @Override
    ConstructionNode acceptNodeTransformer(HomogeneousQueryNodeTransformer transformer)
            throws QueryNodeTransformationException;

    /**
     * TODO: find a better name and a better explanation.
     *
     * Equivalent to the regular substitution.
     * All projected variables that are defined from other variables
     * defined in the ancestor nodes have a explicit binding to them
     * in this substitution.
     *
     * In the regular substitution, they could just be bound to another
     * projected variable (INDIRECT).
     *
     */
    ImmutableSubstitution<ImmutableTerm> getDirectBindingSubstitution();

    @Override
    SubstitutionResults<ConstructionNode> applyAscendingSubstitution(
            ImmutableSubstitution<? extends ImmutableTerm> substitution,
            QueryNode childNode, IntermediateQuery query);

    @Override
    SubstitutionResults<ConstructionNode> applyDescendingSubstitution(
            ImmutableSubstitution<? extends ImmutableTerm> substitution, IntermediateQuery query)
            throws QueryNodeSubstitutionException;
}