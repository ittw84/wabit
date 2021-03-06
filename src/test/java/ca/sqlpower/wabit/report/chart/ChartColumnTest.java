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

package ca.sqlpower.wabit.report.chart;

import ca.sqlpower.wabit.AbstractWabitObjectTest;
import ca.sqlpower.wabit.WabitObject;
import ca.sqlpower.wabit.report.chart.ChartColumn.DataType;

public class ChartColumnTest extends AbstractWabitObjectTest {
    
    private ChartColumn chartColumn;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        chartColumn = new ChartColumn("col", DataType.TEXT);
        
        Chart chart = new Chart();
        chart.setName("chart");
        chart.addChild(chartColumn, 0);
        
        getWorkspace().addChart(chart);
    }
    
    @Override
    public WabitObject getObjectUnderTest() {
        return chartColumn;
    }

}
