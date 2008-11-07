/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.wabit.swingui.querypen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import ca.sqlpower.wabit.swingui.Container;
import ca.sqlpower.wabit.swingui.Item;
import ca.sqlpower.wabit.swingui.Section;
import ca.sqlpower.wabit.swingui.event.ContainerItemEvent;
import ca.sqlpower.wabit.swingui.event.ContainerModelListener;
import edu.umd.cs.piccolo.PCanvas;

/**
 * This container is used to hold a generic list of items in
 * the same section.
 */
public class ItemContainer implements Container {
	
	private static final Logger logger = Logger.getLogger(ItemContainer.class);

	/**
	 * The user visible name to this container.
	 */
	private String name;

	/**
	 * This section holds all of the Items containing the strings in this
	 * container.
	 */
	private Section section;
	
	/**
	 * A list of listeners that will tell other objects when the model changes.
	 */
	private final List<ContainerModelListener> modelListeners;
	
	public ItemContainer(String name, MouseState mouseState, PCanvas canvas) {
		this.name = name;
		modelListeners = new ArrayList<ContainerModelListener>();
		section = new ObjectSection();
		((ObjectSection)section).setParent(this);
		logger.debug("Container created.");
	}
	
	public Object getContainedObject() {
		return section.getItems();
	}

	public Item getItem(Object item) {
		for (Item i : section.getItems()) {
			if (i.getItem().equals(item)) {
				return i;
			}
		}
		return null;
	}
	
	public void addItem(Item item) {
		section.addItem(item);
		for (ContainerModelListener l : modelListeners) {
			l.itemAdded(new ContainerItemEvent(item));
		}
	}
	
	public void removeItem(Item item) {
		section.removeItem(item);
		for (ContainerModelListener l : modelListeners) {
			l.itemRemoved(new ContainerItemEvent(item));
		}
	}

	public String getName() {
		return name;
	}

	public List<Section> getSections() {
		return Collections.singletonList(section);
	}

	public void addContainerModelListener(ContainerModelListener l) {
		modelListeners.add(l);
	}
	
	public void removeContainerModelListener(ContainerModelListener l) {
		modelListeners.remove(l);
	}

}