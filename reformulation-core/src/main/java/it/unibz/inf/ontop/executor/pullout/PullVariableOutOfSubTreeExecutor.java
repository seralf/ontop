package it.unibz.inf.ontop.executor.pullout;

import it.unibz.inf.ontop.executor.InternalProposalExecutor;
import it.unibz.inf.ontop.model.ImmutableExpression;
import it.unibz.inf.ontop.model.OBDADataFactory;
import it.unibz.inf.ontop.model.impl.ImmutabilityTools;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import it.unibz.inf.ontop.owlrefplatform.core.basicoperations.InjectiveVar2VarSubstitution;
import it.unibz.inf.ontop.pivotalrepr.IntermediateQuery;
import it.unibz.inf.ontop.pivotalrepr.JoinLikeNode;
import it.unibz.inf.ontop.pivotalrepr.QueryNode;
import it.unibz.inf.ontop.pivotalrepr.SubstitutionResults;
import it.unibz.inf.ontop.pivotalrepr.impl.QueryTreeComponent;
import it.unibz.inf.ontop.pivotalrepr.proposal.InvalidQueryOptimizationProposalException;
import it.unibz.inf.ontop.pivotalrepr.proposal.PullVariableOutOfSubTreeProposal;
import it.unibz.inf.ontop.pivotalrepr.proposal.PullVariableOutOfSubTreeResults;
import it.unibz.inf.ontop.pivotalrepr.proposal.impl.PullVariableOutOfSubTreeResultsImpl;

import java.util.stream.Stream;

import static it.unibz.inf.ontop.executor.substitution.SubstitutionPropagationTools.propagateSubstitutionDown;
import static it.unibz.inf.ontop.model.ExpressionOperation.EQ;

/**
 * TODO: explain
 */
public class PullVariableOutOfSubTreeExecutor<N extends JoinLikeNode>
        implements InternalProposalExecutor<PullVariableOutOfSubTreeProposal<N>, PullVariableOutOfSubTreeResults<N>> {

    private static final OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    @Override
    public PullVariableOutOfSubTreeResults<N> apply(PullVariableOutOfSubTreeProposal<N> proposal,
                                                    IntermediateQuery query,
                                                    QueryTreeComponent treeComponent)
            throws InvalidQueryOptimizationProposalException {

        /**
         * TODO: check for obvious misuse of the proposal
         */

        N newFocusNode = createNewFocusNodeWithAdditionalConditions(proposal);
        treeComponent.replaceNode(proposal.getFocusNode(), newFocusNode);

        QueryNode newSubTreeRoot = propagateRenamings(proposal, query, treeComponent);


        return new PullVariableOutOfSubTreeResultsImpl<>(query, newFocusNode, newSubTreeRoot);

    }

    private N createNewFocusNodeWithAdditionalConditions(PullVariableOutOfSubTreeProposal<N> proposal) {
        N focusNode = proposal.getFocusNode();

        Stream<ImmutableExpression> newConditions = proposal.getRenamingSubstitution().getImmutableMap().entrySet().stream()
                .map(e -> DATA_FACTORY.getImmutableExpression(EQ, e.getKey(), e.getValue()));

        Stream<ImmutableExpression> otherConditions = focusNode.getOptionalFilterCondition()
                .map(exp -> exp.flattenAND().stream())
                .orElseGet(Stream::of);

        return (N) focusNode.changeOptionalFilterCondition(
                ImmutabilityTools.foldBooleanExpressions(Stream.concat(otherConditions, newConditions)));
    }

    /**
     * TODO: explain
     */
    private QueryNode propagateRenamings(PullVariableOutOfSubTreeProposal<N> proposal, IntermediateQuery query,
                                         QueryTreeComponent treeComponent) {

        InjectiveVar2VarSubstitution renamingSubstitution = proposal.getRenamingSubstitution();
        QueryNode originalSubTreeNode = proposal.getSubTreeRootNode();

        SubstitutionResults<? extends QueryNode> rootRenamingResults = originalSubTreeNode
                .applyDescendingSubstitution(renamingSubstitution, query);

        QueryNode newSubTreeRootNode = rootRenamingResults.getOptionalNewNode()
                .orElseThrow(() -> new IllegalStateException("A renaming should always be well-accepted"));

        if (newSubTreeRootNode != originalSubTreeNode) {
            treeComponent.replaceNode(originalSubTreeNode, newSubTreeRootNode);
        }

        /**
         * Updates the tree component
         */
        propagateSubstitutionDown(newSubTreeRootNode, renamingSubstitution, query, treeComponent);

        return newSubTreeRootNode;
    }
}
