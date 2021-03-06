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

package ca.sqlpower.wabit.report;

import java.awt.Color;
import java.awt.Graphics2D;

import ca.sqlpower.object.SPVariableResolver;
import ca.sqlpower.wabit.WabitObject;

/**
 * Interface for providers of rendered (absolute layout) content.
 */
public interface ReportContentRenderer extends WabitObject {
	
	public enum BackgroundColours {
		LIGHT_PINK(new Color(0xffcccc), "Light Pink"),
		ORANGE(new Color(0xffcc99), "Orange"),
		YELLOW(new Color(0xffffcc), "Yellow"),
		GREEN(new Color(0xccffcc), "Green"), 
		LIGHT_BLUE(new Color(0xccffff), "Light Blue"), 
		PURPLE(new Color(0xccccff), "Purple"),
		BLUE(new Color(0x99ccff), "Blue"),
		PINK(new Color(0xffccff), "Pink"),
		DARK_GREY(new Color(0xcccccc), "Dark Grey"),
		GREY(new Color(0xdddddd), "Grey"),
		LIGHT_GREY(new Color(0xeeeeee), "Light Grey"),
		WHITE(new Color(0xffffff), "White");
		
		private final Color colour;
		private final String colourName;
		
		/**
		 * Background Colour for a newly created Label.
		 */
		public final static BackgroundColours DEFAULT_BACKGROUND_COLOUR = BackgroundColours.WHITE;

		private BackgroundColours(Color colour, String colourName) {
			this.colour = colour;
			this.colourName = colourName;
		}
		
		public Color getColour() {
			return colour;
		}
		
		public String getColourName() {
			return colourName;
		}
	}

	/**
	 * Renders as much report content as will fit within the bounds of the given
	 * content box.
	 * <p>
	 * Report content renderers can be implemented in two different ways:
	 * <ul>
	 * <li>Label-like renderers attempt to show all their content every time
	 * they are called, and never ask for another page. These renderers don't
	 * necessarily render exactly the same content on every page--a footer label
	 * with a page number variable is a good example.
	 * <li>Resultset-like renderers that show as much content as possible each
	 * time they are called, and keep track of what to start rendering on the
	 * next call. These types of renderers ask for more pages until they have
	 * nothing left to render. If called again, they simply do not draw
	 * anything.
	 * 
	 * @param g
	 *            The graphics to render into. The origin (top left corner or
	 *            (0,0)) of this graphics is translated to the top-left corner
	 *            of the content box.
	 * @param contentBox
	 *            The box that determines the size and shape that the rendered
	 *            data must fit within. You can ignore the X and Y coordinates
	 *            of the box because the given graphics object's origin is
	 *            already set to this box's origin.
	 *            It is also recommended to ignore the width and height properties
	 *            and to conform to those provided by the method parameters 
	 *            since the actual space available might not be the same.
	 * @param scaleFactor
	 *            The amount of scaling currently in effect. The nominal size of
	 *            a unit when displayed via the given graphics is scaleFactor/72
	 *            inches.
	 * @param pageIndex
	 *            The zero-based page number for which the corresponding report
	 *            content will be rendered.
	 * @param printing
	 *            This tells the renderer if the report is being printed. If
	 *            set to true the full report will be built, otherwise a cached
                  result will be used. Set this to true if the report is being
                  printed.
	 * @return True if this renderer has more data to render, and would like to
	 *         be called upon again. Returning true will typically cause the
	 *         report to grow by another page. The final page of the report is
	 *         the first one where all content renderers involved return false.
	 */
    boolean renderReportContent(
    		Graphics2D g,
    		double width, 
    		double height, 
    		double scaleFactor, 
    		int pageIndex, 
    		boolean printing, 
    		SPVariableResolver variablesContext);

    /**
     * Tells this content renderer that the next call to
     * {@link #renderReportContent(Graphics2D, ContentBox, double)} should
     * produce the first page of output again.
     */
    void resetToFirstPage();
    
	Color getBackgroundColour();

	/**
	 * Refreshes the data being rendered by this ReportContentRenderer so that
	 * it has the most recent data. If the data this renderer renders is static,
	 * then it doesn't need to do anything (no-op).
	 */
	void refresh();

}
