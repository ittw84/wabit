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

package ca.sqlpower.wabit.swingui;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

import junit.framework.TestCase;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.util.DefaultUserPrompterFactory;
import ca.sqlpower.wabit.StubWabitSession;
import ca.sqlpower.wabit.StubWabitSessionContext;
import ca.sqlpower.wabit.WabitDataSource;
import ca.sqlpower.wabit.WabitObject;
import ca.sqlpower.wabit.WabitSession;
import ca.sqlpower.wabit.WabitSessionContextImpl;
import ca.sqlpower.wabit.WabitSessionImpl;
import ca.sqlpower.wabit.WabitWorkspace;
import ca.sqlpower.wabit.image.WabitImage;
import ca.sqlpower.wabit.report.Report;
import ca.sqlpower.wabit.report.chart.Chart;
import ca.sqlpower.wabit.rs.olap.OlapQuery;
import ca.sqlpower.wabit.rs.query.QueryCache;

public class WabitSwingSessionContextImplTest extends TestCase {
    
    private static class TestingWabitSessionContextImpl 
            extends WabitSessionContextImpl {

        public TestingWabitSessionContextImpl(
                boolean terminateWhenLastSessionCloses, boolean useJmDNS,
                DataSourceCollection<SPDataSource> dsCollection, String path, boolean saveCollection)
                throws IOException, SQLObjectException {
            super(terminateWhenLastSessionCloses, useJmDNS, dsCollection, path, saveCollection);
        }

        @Override
        public void setPlDotIniPath(String plDotIniPath) {
            super.setPlDotIniPath(plDotIniPath);
        }
        
    }
    
    private PlDotIni plIni;
    private JDBCDataSource jdbcDS;
    
    @Override
    protected void setUp() throws Exception {
        plIni = new PlDotIni();
        plIni.read(new File("src/test/resources/pl.regression.ini"));
        jdbcDS = plIni.getDataSource("regression_test", JDBCDataSource.class);
    }
    
    public void testImportingIntoActiveSession() throws Exception {
        TestingWabitSessionContextImpl delegateContext = 
            new TestingWabitSessionContextImpl(true, false, plIni, "", false);
        delegateContext.setPlDotIniPath("src/test/resources/pl.regression.ini");
        
        final WabitSwingSessionContextImpl context = 
            new WabitSwingSessionContextImpl(delegateContext, true, 
                    new DefaultUserPrompterFactory());
        
        //the current thread is the foreground thread in tests or else 
        //exceptions will be thrown on events.
        WabitSwingSession session = new WabitSwingSessionImpl(context, 
        		new WabitSessionImpl(delegateContext)) {
        	@Override
        	public boolean isForegroundThread() {
        		return true;
        	}
        };
        context.registerChildSession(session);
        //the current thread is the foreground thread in tests or else 
        //exceptions will be thrown on events.
        WabitSwingSession inactiveSession = new WabitSwingSessionImpl(context, 
        		new WabitSessionImpl(delegateContext)) {
        	@Override
        	public boolean isForegroundThread() {
        		return true;
        	}
        };
        context.registerChildSession(inactiveSession);
        
        context.setActiveSession(session);
        assertTrue(session.getWorkspace().getChildren().isEmpty());
        assertTrue(inactiveSession.getWorkspace().getChildren().isEmpty());

        WabitSession stubWabitSession = 
            new StubWabitSession(new StubWabitSessionContext());
        final WabitWorkspace dummyWorkspace = new WabitWorkspace();
        dummyWorkspace.setSession(stubWabitSession);
        WabitDataSource ds = new WabitDataSource(jdbcDS);
        dummyWorkspace.addDataSource(ds);
        QueryCache cache = new QueryCache(context);
        cache.setName("cache");
        dummyWorkspace.addQuery(cache, stubWabitSession);
        OlapQuery olapQuery = new OlapQuery(context);
        olapQuery.setName("olap");
        dummyWorkspace.addOlapQuery(olapQuery);
        WabitImage image = new WabitImage();
        image.setName("image");
        dummyWorkspace.addImage(image);
        Chart chart = new Chart();
        chart.setName("chart");
        dummyWorkspace.addChart(chart);
        Report report = new Report("New report");
        report.setName("report");
        dummyWorkspace.addReport(report);
        
        Runnable r = new Runnable() {
			public void run() {
				context.importIntoActiveSession(dummyWorkspace.getChildren(WabitObject.class));
			}
		};
		SwingUtilities.invokeAndWait(r);
        assertTrue(inactiveSession.getWorkspace().getChildren().isEmpty());
        WabitWorkspace activeWorkspace = session.getWorkspace();
        assertEquals(1, activeWorkspace.getQueries().size());
        assertEquals(cache.getName(), activeWorkspace.getQueries().get(0).getName());
        assertEquals(1, activeWorkspace.getOlapQueries().size());
        assertEquals(olapQuery.getName(), 
                activeWorkspace.getOlapQueries().get(0).getName());
        assertEquals(1, activeWorkspace.getImages().size());
        assertEquals(image.getName(), activeWorkspace.getImages().get(0).getName());
        assertEquals(1, activeWorkspace.getCharts().size());
        assertEquals(chart.getName(), activeWorkspace.getCharts().get(0).getName());
        assertEquals(1, activeWorkspace.getReports().size());
        assertEquals(report.getName(), activeWorkspace.getReports().get(0).getName());
        
        assertEquals(1, activeWorkspace.getDataSources().size());
        assertEquals(jdbcDS, activeWorkspace.getDataSources().get(0).getSPDataSource());
        assertEquals(6, activeWorkspace.getChildren().size());
    }

}
