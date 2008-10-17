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

package ca.sqlpower.wabit.swingui.event;

import ca.sqlpower.wabit.swingui.JoinLine;
import ca.sqlpower.wabit.swingui.MouseStatePane;
import ca.sqlpower.wabit.swingui.MouseStatePane.MouseStates;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolox.nodes.PStyledText;

/**
 * Creates a join between two columns in two different tables.
 */
public class CreateJoinEventHandler extends PBasicInputEventHandler {
	
	private MouseStatePane mouseStatePane;
	private PStyledText leftText;
	private PStyledText rightText;
	private PLayer joinLayer;

	public CreateJoinEventHandler(MouseStatePane mouseStatePane, PLayer joinLayer) {
		this.mouseStatePane = mouseStatePane;
		this.joinLayer = joinLayer;
	}
	
	@Override
	public void mousePressed(PInputEvent event) {
		super.mousePressed(event);
		if (mouseStatePane.getMouseState().equals(MouseStates.CREATE_JOIN)) {
			if (event.getPickedNode() instanceof PStyledText) {
				if (leftText == null) {
					leftText = (PStyledText)event.getPickedNode();
				} else if (rightText == null) {
					rightText = (PStyledText)event.getPickedNode();
					joinLayer.addChild(new JoinLine(leftText, rightText));
					leftText = null;
					rightText = null;
					mouseStatePane.setMouseState(MouseStates.READY);
				} else {
					throw new IllegalStateException("Trying to create a join while both ends have already been specified.");
				}
			} else {
				leftText = null;
				rightText = null;
				mouseStatePane.setMouseState(MouseStates.READY);
			}
		}
	}
}
