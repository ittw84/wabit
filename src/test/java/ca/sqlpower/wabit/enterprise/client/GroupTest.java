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

package ca.sqlpower.wabit.enterprise.client;

import java.util.Set;

import ca.sqlpower.enterprise.client.Group;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.wabit.AbstractWabitObjectTest;
import ca.sqlpower.wabit.WabitObject;
import ca.sqlpower.wabit.WabitWorkspace;

public class GroupTest extends AbstractWabitObjectTest {
	
	private Group group;
	
	@Override
	public Set<String> getPropertiesToNotPersistOnObjectPersist() {
		Set<String> ignored = super.getPropertiesToNotPersistOnObjectPersist();
		ignored.add("authority");
		ignored.add("grants");
		ignored.add("members");
		return ignored;
	}
	
	@Override
	public Set<String> getPropertiesToIgnoreForPersisting() {
		Set<String> ignored = super.getPropertiesToIgnoreForPersisting();
		//The new value maker currently returns the workspace used in the test
		//as the parent for this object so the parent object never changes.
		//To properly test parents the new value would have to be a different
		//workspace but the group in the test would still have to exist under
		//the current workspace in the test.
		ignored.add("parent");
		return ignored;
	}
	
	@Override
	public Class<? extends WabitObject> getParentClass() {
		return WabitWorkspace.class;
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		group = new Group("group");
		
		getWorkspace().setUUID(WabitWorkspace.SYSTEM_WORKSPACE_UUID);
		getWorkspace().addChild(group, 0);
	}

	@Override
	public SPObject getObjectUnderTest() {
		return group;
	}

}
