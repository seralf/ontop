package it.unibz.inf.ontop.spec.dbschema.impl;

import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.dbschema.*;
import it.unibz.inf.ontop.dbschema.MetadataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * Used for reading user-supplied information about unique constraints and foreign keys
 * Needed for better performance in cases where views and materialized views are present.
 *
 * Associated JUnit Tests @TestImplicitDBConstraints, @TestQuestImplicitDBConstraints
 *
 *  @author Dag Hovland (first version)
 *
 */

public class ImplicitDBConstraintsProvider implements MetadataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImplicitDBConstraintsProvider.class);

    private final QuotedIDFactory idFactory;

    // List of two-element arrays: table id and a comma-separated list of columns
    private final ImmutableList<String[]> uniqueConstraints;

    // List of four-element arrays: foreign key table id, comma-separated foreign key columns,
    //                              primary key (referred) table id, comma-separated primary key columns
    private final ImmutableList<String[]> foreignKeys;

    ImplicitDBConstraintsProvider(QuotedIDFactory idFactory,
                                  ImmutableList<String[]> uniqueConstraints,
                                  ImmutableList<String[]> foreignKeys) {
        this.idFactory = idFactory;
        this.uniqueConstraints = uniqueConstraints;
        this.foreignKeys = foreignKeys;
    }

    /**
     * Extracts relation IDs for all relations referred to by the user supplied foreign keys
     * (but not the relations of the foreign keys)
     *
     * @return relation ids that are referred to by foreign keys
     */
    @Override
    public ImmutableList<RelationID> getRelationIDs() {
        Set<RelationID> referredTables = new HashSet<>();

        for (String[] fk : foreignKeys) {
            RelationID pkTableId = getRelationIDFromString(fk[2]);
            referredTables.add(pkTableId);
        }

        return ImmutableList.copyOf(referredTables);
    }

    @Override
    public ImmutableList<RelationDefinition> getRelations(RelationID relationID) {
        return ImmutableList.of();
    }

    /**
     *
     * Inserts the user-supplied primary keys / unique constraints columns into the metadata object
     */
    @Override
    public void insertIntegrityConstraints(ImmutableDBMetadata md) {

        int counter = 0; // id of the generated constraint

        for (String[] constraint : uniqueConstraints) {
            try {
                ConstraintDescriptor uc = getConstraintDescriptor(md, constraint[0], constraint[1].split(","));
                UniqueConstraint.BuilderImpl builder = UniqueConstraint.builder(uc.table, uc.table.getID().getTableName() + "_USER_UC_" + counter);
                for (Attribute a : uc.attributes)
                    builder.addDeterminant(a);
                uc.table.addUniqueConstraint(builder.build());
                counter++;
            }
            catch (ImplicitDBContraintsRuntimeException e) {
                LOGGER.warn("Error in user-supplied unique constraints: {}.", e.getMessage());
            }
        }

        for (String[] constraint : foreignKeys) {
            try {
                String[] fkAttrs = constraint[1].split(",");
                String[] pkAttrs = constraint[3].split(",");
                if (fkAttrs.length != pkAttrs.length)
                    throw new ImplicitDBContraintsRuntimeException("Different number of columns in " + constraint);

                ConstraintDescriptor fk = getConstraintDescriptor(md, constraint[0], fkAttrs);
                ConstraintDescriptor pk = getConstraintDescriptor(md, constraint[2], pkAttrs);
                ForeignKeyConstraint.Builder builder = ForeignKeyConstraint.builder(fk.table, pk.table);
                for (int i = 0; i < pkAttrs.length; i++)
                    builder.add(fk.attributes[i], pk.attributes[i]);

                fk.table.addForeignKeyConstraint(
                        builder.build(fk.table.getID().getTableName() + "_USER_FK_" + pk.table.getID().getTableID().getName() + "_" + counter));
                counter++;
            }
            catch (ImplicitDBContraintsRuntimeException e) {
                LOGGER.warn("Error in user-supplied foreign key constraints: {}.", e.getMessage());
            }
        }
    }

    private static final class ConstraintDescriptor {
        DatabaseRelationDefinition table;
        Attribute[] attributes;
    }

    private static final class ImplicitDBContraintsRuntimeException extends Exception {
        ImplicitDBContraintsRuntimeException(String s) { super(s); }
    }

    private ConstraintDescriptor getConstraintDescriptor(MetadataLookup md, String tableName, String[] attributeNames) throws ImplicitDBContraintsRuntimeException {
        ConstraintDescriptor result = new ConstraintDescriptor();

        RelationID relationId = getRelationIDFromString(tableName);
        RelationDefinition relation = md.getRelation(relationId)
                .orElseThrow(() -> new ImplicitDBContraintsRuntimeException("Relation " + relationId + " not found"));

        if (!(relation instanceof DatabaseRelationDefinition))
            throw new ImplicitDBContraintsRuntimeException("Relation " + relation + " is not a " + DatabaseRelationDefinition.class.getName());

        result.table = (DatabaseRelationDefinition)relation;
        result.attributes = new Attribute[attributeNames.length];
        for (int i = 0; i < attributeNames.length; i++) {
            QuotedID attributeId = idFactory.createAttributeID(attributeNames[i]);
            result.attributes[i] = result.table.getAttribute(attributeId);
            if (result.attributes[i] == null)
                throw new ImplicitDBContraintsRuntimeException("Attribute " + attributeId + " not found in " + relationId);
        }
        return result;
    }

    private RelationID getRelationIDFromString(String tableName) {
        String[] names = tableName.split("\\.");
        return (names.length == 1)
                ? idFactory.createRelationID(null, tableName)
                : idFactory.createRelationID(names[0], names[1]);
    }
}
