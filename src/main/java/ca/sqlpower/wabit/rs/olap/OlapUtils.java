/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of Wabit.
 *
 * Wabit is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wabit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.wabit.rs.olap;

import java.sql.SQLException;
import java.util.Map;

import javax.annotation.Nonnull;

import org.olap4j.Axis;
import org.olap4j.CellSet;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Member;
import org.olap4j.query.Query;
import org.olap4j.query.QueryAxis;
import org.olap4j.query.QueryDimension;
import org.olap4j.query.Selection;

import ca.sqlpower.wabit.rs.OlapResultSet;

/**
 * This is a class full of static helper methods to assist with the execution of
 * olap queries
 * 
 * @author thomas
 * 
 */
public class OlapUtils {
    
    private OlapUtils() {
        //Do nothing
    }
    
    /**
     * Tests whether or not the given parent member has the other member as one
     * of its descendants--either a direct child, or a child of a child, and so
     * on. Does not consider parent to be a descendant of itself, so in the case
     * both arguments are equal, this method returns false.
     * 
     * @param parent
     *            The parent member
     * @param testForDescendituitivitiness
     *            The member to check if it has parent as an ancestor
     */
    public static boolean isDescendant(Member parent, Member testForDescendituitivitiness) {
        if (testForDescendituitivitiness.equals(parent)) return false;
        while (testForDescendituitivitiness != null) {
            if (testForDescendituitivitiness.equals(parent)) return true;
            testForDescendituitivitiness = testForDescendituitivitiness.getParentMember();
        }
        return false;
    }

    /**
     * Tests whether or not the given parent member is the same as or has the
     * other member as one of its descendants--either a direct child, or a child
     * of a child, and so on.
     * 
     * @param parent
     *            The parent member
     * @param testForDescendituitivitiness
     *            The member to check if it is parent or has parent as an ancestor
     */
    public static boolean isDescendantOrEqualTo(
            Member parent, Member testForEquidescendituitivitiness) {
        return parent.equals(testForEquidescendituitivitiness) ||
            isDescendant(parent, testForEquidescendituitivitiness);
    }
    
	/**
	 * Tests whether or not the given parent member has the other member as one
	 * of a direct child. Does not consider parent to be a direct child of
	 * itself, so in the case both arguments are equal, this method returns
	 * false.
	 * 
	 * @param parent
	 *            The potential parent member
	 * @param testForChildishness
	 *            The member to check if it has parent as its parent member
	 * @return True if testForChildishness is a direct child of parent. False
	 *         otherwise.
	 */
    public static boolean isChild(Member parent, Member testForChildishness) {
    	return parent.equals(testForChildishness.getParentMember());
    }
    
    /**
     * This method returns a deep copy of an MDX Query because there is no such
     * method in the API.
     * 
     * @param query
     *            This is the {@link Query} that is being copied
     * @return This returns the copied Query, it is a new copy that only shares
     *         {@link Dimension}s and {@link Member}
     *         
     * @throws SQLException
     */
    public static Query copyMDXQuery(Query query) throws SQLException {
        if (query == null) return null;
        Query modifiedMDXQuery = new Query(query.getName(), query.getCube());
        for (Map.Entry<Axis, QueryAxis> axisEntry : query.getAxes().entrySet()) {
        	if (axisEntry.getKey() == null) continue;
        	QueryAxis copiedAxis = modifiedMDXQuery.getAxes().get(axisEntry.getKey());
        	copiedAxis.setNonEmpty(axisEntry.getValue().isNonEmpty());
            for (QueryDimension oldDimension : axisEntry.getValue().getDimensions()) {
            	QueryDimension copiedDimension = modifiedMDXQuery.getDimension(oldDimension.getName());
            	copiedDimension.setHierarchizeMode(oldDimension.getHierarchizeMode());
                for (Selection selection : oldDimension.getInclusions()) {
                    copiedDimension.include(selection.getOperator(), selection.getMember());
                }
                
                for (Selection selection : oldDimension.getExclusions()) {
                	copiedDimension.exclude(selection.getOperator(), selection.getMember());
                }
                copiedAxis.getDimensions().add(copiedDimension);
            }
            modifiedMDXQuery.getAxes().put(axisEntry.getKey(), copiedAxis);
        }
        return modifiedMDXQuery;
    }

    /**
     * Creates and populates an OlapResultSet from the given CellSet, which must
     * not be null.
     * 
     * @param execute
     * @return
     * @throws RuntimeException
     *             in the unlikely event that the OlapResultSet throws
     *             SQLException while it's being populated.
     * @see OlapResultSet
     */
    public static OlapResultSet toResultSet(@Nonnull CellSet cellSet) {
        try {
            OlapResultSet ors;
            ors = new OlapResultSet();
            ors.populate(cellSet);
            return ors;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
