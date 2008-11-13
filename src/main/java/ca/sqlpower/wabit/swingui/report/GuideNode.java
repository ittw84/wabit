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

package ca.sqlpower.wabit.swingui.report;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.log4j.Logger;

import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.wabit.report.Guide;
import ca.sqlpower.wabit.report.Guide.Axis;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * A guide is an object visible only at design time which has a particular
 * (movable) position that other nodes can snap to and be positioned relative
 * to.
 */
public class GuideNode extends PNode {

    private static final Logger logger = Logger.getLogger(GuideNode.class);
    
    private BasicStroke marginStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 4f, new float[] { 12f, 12f }, 0f);
    
    private final Guide model;
    
    /**
     * The colour that this guide will paint in when it's in the default state.
     */
    private Paint normalColour = new Color(0xdddddd);
    
    private Paint mouseOverColour = Color.ORANGE;
    
    /**
     * Creates a new guide node oriented as specified. The orientation of
     * a guide node can't be changed, although its position can.
     * 
     * @param axis Whether this new guide is oriented vertically or horizontally 
     */
    public GuideNode(Axis axis) {
        model = new Guide(axis);
        model.addPropertyChangeListener(modelChangeHandler);
        setPaint(normalColour);
        addInputEventListener(inputEventHandler);
        // Note that guides are pickable so they can get input events, but our
        // custom selection handler treats them as if they were not pickable
    }
    
    @Override
    protected void paint(PPaintContext paintContext) {
        PCamera camera = paintContext.getCamera();
        Graphics2D g2 = paintContext.getGraphics();
        
        g2.setStroke(SPSUtils.getAdjustedStroke(marginStroke, camera.getViewScale()));
        g2.setPaint(getPaint());
        g2.drawLine((int) getX(), (int) getY(), (int) (getX() + getWidth()), (int) (getY() + getHeight()));
    }
    
    public void setGuideOffset(int guideOffset) {
        model.setOffset(guideOffset);
    }
    
    public int getGuideOffset() {
        return model.getOffset();
    }

    @Override
    public void setParent(PNode newParent) {
        logger.debug("Changing parent to " + newParent);
        if (getParent() != null) {
            getParent().removePropertyChangeListener(parentChangeHandler);
        }
        super.setParent(newParent);
        if (newParent != null) {
            adjustBoundsForParent();
            newParent.addPropertyChangeListener(parentChangeHandler);
        }
    }
    
    private void adjustBoundsForParent() {
        PNode parent = getParent();
        Axis axis = model.getAxis();
        int guideOffset = model.getOffset();
        if (axis == Axis.HORIZONTAL) {
            setBounds((int) parent.getX(), guideOffset, (int) parent.getWidth(), 1);
        } else if (axis == Axis.VERTICAL) {
            setBounds(guideOffset, (int) parent.getY(), 1, (int) parent.getHeight());
        } else {
            throw new IllegalStateException("Unknown axis: " + axis);
        }
    }
    
    private final PropertyChangeListener parentChangeHandler = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
            logger.debug("Parent change event: " + evt.getPropertyName());
            if (evt.getPropertyName().contains("bounds")) {
                adjustBoundsForParent();
            }
        }
        
    };

    private final GuideMouseEventHandler inputEventHandler = new GuideMouseEventHandler();

    private class GuideMouseEventHandler extends PBasicInputEventHandler {

        boolean cursorPushed = false;
        
        GuideMouseEventHandler() {
            super();
            getEventFilter().setAcceptsMouseEntered(true);
            getEventFilter().setAcceptsMouseExited(true);
        }
        
        @Override
        public void mouseEntered(PInputEvent event) {
            logger.debug("Mouse entered!");
            super.mouseEntered(event);
            setPaint(mouseOverColour);
            event.pushCursor(new Cursor(
                    model.getAxis() == Axis.HORIZONTAL ? Cursor.N_RESIZE_CURSOR : Cursor.E_RESIZE_CURSOR)); // XXX need custom cursor
            cursorPushed = true;
        }
        
        @Override
        public void mouseExited(PInputEvent event) {
            logger.debug("Mouse exited!");
            super.mouseExited(event);
            setPaint(normalColour);
            if (cursorPushed) {
                event.popCursor();
                cursorPushed = false;
            }
        }
        
        @Override
        public void mouseDragged(PInputEvent event) {
            super.mouseDragged(event);
            Point2D pagePosition = event.getPositionRelativeTo(getParent());
            double offset = model.getAxis() == Axis.HORIZONTAL ? pagePosition.getY() : pagePosition.getX();
            model.setOffset((int) offset);
        }
    }
    
    private PropertyChangeListener modelChangeHandler = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("offset")) {
                adjustBoundsForParent();
            }
        }
        
    };
    
    /**
     * Attempts to snap the given node's bounds so one of its edges sits on this
     * guide. The given node is assumed to have the same coordinate space as this
     * guide, which should be true if this guide and the given node share a parent,
     * and the given node doesn't define its own transform.
     * 
     * @param node
     *            The node whose bounds to tweak
     * @return True if the node's bounds were snapped to this guide; false if
     *         the node was not modified.
     */
    public boolean snap(PNode node, int threshold) {
        boolean snap = false;
        PBounds nodeBounds = node.getGlobalBounds();
        PBounds guideBounds = getGlobalBounds();
        Axis axis = model.getAxis();
        if (axis == Axis.HORIZONTAL) {
            int topEdgeDistance = (int) Math.abs(nodeBounds.getY() - guideBounds.getY());
            int bottomEdgeDistance = (int) Math.abs(nodeBounds.getY() + nodeBounds.getHeight() - guideBounds.getY());
            if (topEdgeDistance < threshold) {
                logger.debug("Top edge snap!");
                nodeBounds.y = guideBounds.getY();
                snap = true;
            } else if (bottomEdgeDistance < threshold) {
                logger.debug("Bottom edge snap!");
                nodeBounds.y = guideBounds.getY() - nodeBounds.getHeight();
                snap = true;
            }
        } else if (axis == Axis.VERTICAL) {
            int leftEdgeDistance = (int) Math.abs(nodeBounds.getX() - guideBounds.getX());
            int rightEdgeDistance = (int) Math.abs(nodeBounds.getX() + nodeBounds.getWidth() - guideBounds.getX());
            if (leftEdgeDistance < threshold) {
                logger.debug("Left edge snap!");
                nodeBounds.x = guideBounds.getX();
                snap = true;
            } else if (rightEdgeDistance < threshold) {
                logger.debug("Right edge snap!");
                nodeBounds.x = guideBounds.getX() - nodeBounds.getWidth();
                snap = true;
            }
        } else {
            throw new IllegalStateException("Unknown axis: " + axis);
        }

        if (snap) {
            node.globalToLocal(nodeBounds);
            node.setBounds(nodeBounds);
        }
        
        return snap;
    }
}
