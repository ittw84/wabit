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

package ca.sqlpower.wabit.swingui;

/**
 * Classes that contain a mouse state should implement this interface.
 */
public interface MouseState {

	/**
	 * The states a mouse can be on the query pen.
	 */
	public enum MouseStates {
		READY, 
		CREATE_JOIN, 
		CREATE_BOX, 
		CREATE_LABEL,
		CREATE_HORIZONTAL_GUIDE, 
		CREATE_VERTICAL_GUIDE, 
	}
	
	public MouseStates getMouseState();
	
	public void setMouseState(MouseStates state);
}
