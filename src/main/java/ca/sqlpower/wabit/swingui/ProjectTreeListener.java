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

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.Olap4jDataSource;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.swingui.SPSUtils;
import ca.sqlpower.util.UserPrompter;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;
import ca.sqlpower.util.UserPrompterFactory.UserPromptType;
import ca.sqlpower.wabit.QueryCache;
import ca.sqlpower.wabit.WabitDataSource;
import ca.sqlpower.wabit.WabitObject;
import ca.sqlpower.wabit.olap.OlapQuery;
import ca.sqlpower.wabit.report.CellSetRenderer;
import ca.sqlpower.wabit.report.ContentBox;
import ca.sqlpower.wabit.report.Layout;
import ca.sqlpower.wabit.report.ResultSetRenderer;
import ca.sqlpower.wabit.swingui.action.AddDataSourceAction;
import ca.sqlpower.wabit.swingui.action.CopyQueryAction;
import ca.sqlpower.wabit.swingui.action.EditCellAction;
import ca.sqlpower.wabit.swingui.action.NewLayoutAction;
import ca.sqlpower.wabit.swingui.action.NewOLAPQueryAction;
import ca.sqlpower.wabit.swingui.action.NewQueryAction;
import ca.sqlpower.wabit.swingui.tree.ProjectTreeCellRenderer;

/**
 * This listener is the main listener on the project tree in Wabit.
 * It will listen and handle all tree events from creating new elements
 * to changing the view based on selected nodes.
 */
public class ProjectTreeListener extends MouseAdapter {
	
	private static final Logger logger = Logger.getLogger(ProjectTreeListener.class);
	
    public static final Icon DB_ICON = new ImageIcon(ProjectTreeCellRenderer.class.getClassLoader().getResource("icons/dataSources-db.png"));
    public static final Icon OLAP_DB_ICON = new ImageIcon(ProjectTreeCellRenderer.class.getClassLoader().getResource("icons/dataSources-olap.png"));
	
	private final WabitSwingSession session;

	public ProjectTreeListener(WabitSwingSession session) {
		this.session = session;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		maybeShowPopup(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		maybeShowPopup(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		Object lastPathComponent = getLastPathComponent(e);
		if (e.isPopupTrigger()) {
			maybeShowPopup(e);
		}
		if (lastPathComponent != null && lastPathComponent instanceof WabitObject) {
			session.getProject().setEditorPanelModel((WabitObject) lastPathComponent);
			if (e.getButton() == MouseEvent.BUTTON1  && e.getClickCount() == 2) {
				JTree tree = (JTree)e.getSource();
				tree.startEditingAtPath(tree.getSelectionPath());
			}
		}
	}

	private Object getLastPathComponent(MouseEvent e) {
		JTree t = (JTree) e.getSource();
		int row = t.getRowForLocation(e.getX(), e.getY());
		TreePath tp = t.getPathForRow(row);
		Object lastPathComponent = null;
		if (tp != null) {
			lastPathComponent = tp.getLastPathComponent();
			logger.debug("Clicked on " + lastPathComponent.getClass());
		}
		return lastPathComponent;
	}
	
	
	private class DeleteFromTreeAction extends AbstractAction {
		
		Object item ;
		public DeleteFromTreeAction(Object node) {
			super("Delete");
			item = node;
		}

		public void actionPerformed(ActionEvent e) {
			
			if(item instanceof QueryCache) {
				int response = JOptionPane.showOptionDialog(session.getFrame(), "By deleting this query, you will be deleting layout parts dependent on it\n" +
						"Do you want to proceed with deleting?", "Delete Query", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[] {"Ok", "Cancel"}, null);
				if(response == 0) {
					final QueryCache query = (QueryCache)item;
					session.getProject().removeQuery(query, session);
					removeLayoutPartsDependentOnQuery(query);
				} else {
					return;
				}
			} else if (item instanceof WabitDataSource) {
				WabitDataSource wabitDS = (WabitDataSource) item;
				int response = JOptionPane.showOptionDialog(session.getFrame(),
							"Are you sure you want to delete the data source " + wabitDS.getName() + ", its queries,\n and all report content boxes associated with the data source?", 
							"Delete Data Source", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
		                    new Object[] {"Delete All", "Replace", "Cancel"}, null);
		            if (response == 0) {
		            	session.getProject().removeDataSource((WabitDataSource)item);
		            	if (wabitDS.getSPDataSource() instanceof JDBCDataSource) {
			                //A temporary list is used instead of directly using session.getProject().getQueries()
			            	//to prevent a ConcurrentModificationException
			            	List <QueryCache> queries = new ArrayList<QueryCache>(session.getProject().getQueries());
			            	for(QueryCache query : queries) {
			                	if(item.equals(query.getWabitDataSource())) {
			                		removeLayoutPartsDependentOnQuery(query);
			                		session.getProject().removeQuery(query, session);
			                	}
			                }
		            	} else if (wabitDS.getSPDataSource() instanceof Olap4jDataSource) {
			            	List <OlapQuery> olapQueries = new ArrayList<OlapQuery>(session.getProject().getOlapQueries());
			            	logger.debug("Project has " + olapQueries.size() + " queries");
			            	for(OlapQuery query : olapQueries) {
			            		logger.debug("Currently on query '" + query.getName() + "'");
			            		if(wabitDS.getSPDataSource().equals(query.getOlapDataSource())) {
			            			logger.debug("Removing this query");
			            			removeLayoutPartsDependentOnOlapQuery(query);
			            			session.getProject().removeOlapQuery(query);
			            		}
			            	}
		            	}
		            } else if(response == 1) {
		            	UserPrompter dbPrompter = session.createUserPrompter("Replacing " + wabitDS.getName(), 
		            													wabitDS.getSPDataSource() instanceof Olap4jDataSource ?
		            														UserPromptType.OLAP_DATA_SOURCE : UserPromptType.JDBC_DATA_SOURCE, 
												            			UserPromptOptions.OK_NEW_CANCEL, 
												            			UserPromptResponse.CANCEL, 
												            			null, "OK", "Create New", "Cancel");
		            	UserPromptResponse getResponseType = dbPrompter.promptUser();
		        		if (getResponseType == UserPromptResponse.OK || getResponseType == UserPromptResponse.NEW) {
		        			session.getProject().removeDataSource((WabitDataSource)item);
		        			SPDataSource ds = (SPDataSource) dbPrompter.getUserSelectedResponse();
		        			session.getProject().addDataSource(ds);
		        			if (ds instanceof JDBCDataSource) {
		        				List <QueryCache> queries = new ArrayList<QueryCache>(session.getProject().getQueries());
				            	for(QueryCache query : queries) {
				                	if(item.equals(query.getWabitDataSource())) {
				                		removeLayoutPartsDependentOnQuery(query);
				                		int queryIndex = session.getProject().getQueries().indexOf(query);
										session.getProject().getQueries().get(queryIndex).setDataSource((JDBCDataSource)ds);
				                	}
				                }
		        			} else if (ds instanceof Olap4jDataSource) {
		        				List <OlapQuery> queries = new ArrayList<OlapQuery>(session.getProject().getOlapQueries());
				            	for(OlapQuery query : queries) {
				                	if(wabitDS.getSPDataSource().equals(query.getOlapDataSource())) {
				                		removeLayoutPartsDependentOnOlapQuery(query);
				                		int queryIndex = session.getProject().getOlapQueries().indexOf(query);
										session.getProject().getOlapQueries().get(queryIndex).setOlapDataSource((Olap4jDataSource)ds);
				                	}
				                }
		        			}
			            	
		        		} else {
		        			return;
		        		}
		        	} else if (response == JOptionPane.CLOSED_OPTION || response == 2) {
		            	return;
		            } 
		    } else if (item instanceof Layout) {
				session.getProject().removeLayout((Layout)item);
		    } else if (item instanceof OlapQuery) {
				int response = JOptionPane.showOptionDialog(session.getFrame(), "By deleting this query, you will be deleting layout parts dependent on it\n" +
						"Do you want to proceed with deleting?", "Delete Query", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[] {"Ok", "Cancel"}, null);
				if(response == 0) {
					final OlapQuery query = (OlapQuery)item;
					session.getProject().removeOlapQuery(query);
					removeLayoutPartsDependentOnOlapQuery(query);
				} else {
					return;
				}
			} else {
				logger.debug("This shoudl not Happen");
			}
			
		}

		/**
		 * Removes any content boxes dependent on the query passed to the method
		 * @param query
		 */
		private void removeLayoutPartsDependentOnQuery(QueryCache query) {
			for(Layout layout :session.getProject().getLayouts()) {
				List<ContentBox> cbList = new ArrayList<ContentBox>(layout.getPage().getContentBoxes());
				for(ContentBox cb : cbList) {
				    if(cb.getContentRenderer() instanceof ResultSetRenderer &&((ResultSetRenderer) cb.getContentRenderer()).getQuery() == query) {
				    	int layoutIndex = session.getProject().getLayouts().indexOf(layout);
						session.getProject().getLayouts().get(layoutIndex).getPage().removeContentBox(cb);
					}
				}
			}
		}
		
		/**
		 * Removes any content boxes dependent on the query passed to the method
		 * @param query
		 */
		private void removeLayoutPartsDependentOnOlapQuery(OlapQuery query) {
			for(Layout layout :session.getProject().getLayouts()) {
				List<ContentBox> cbList = new ArrayList<ContentBox>(layout.getPage().getContentBoxes());
				for(ContentBox cb : cbList) {
				    if(cb.getContentRenderer() instanceof CellSetRenderer &&((CellSetRenderer) cb.getContentRenderer()).getOlapQuery() == query) {
				    	layout.getPage().removeContentBox(cb);
					}
				}
			}
		}
	}

	/**
	 * This will Display a List of options once you right click on the ProjectTree.
	 */
	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger()) {
			return;
		}
		
		JPopupMenu menu = new JPopupMenu();
		menu.add(new AbstractAction("Database Connection Manager...") {

			public void actionPerformed(ActionEvent e) {
				session.getDbConnectionManager().showDialog(session.getFrame());
			}
		});

		menu.add(createDataSourcesMenu());

		menu.addSeparator();

		menu.add(new NewQueryAction(session));
		
		menu.add(new NewOLAPQueryAction(session));
		
		final Object lastPathComponent = getLastPathComponent(e);

		if (lastPathComponent instanceof WabitDataSource) {
			SPDataSource ds = ((WabitDataSource) lastPathComponent).getSPDataSource();
			if (ds instanceof JDBCDataSource) {
				menu.add(new NewQueryAction(session, (JDBCDataSource) ds));
			}
			if (ds instanceof Olap4jDataSource) {
				menu.add(new NewOLAPQueryAction(session, (Olap4jDataSource) ds));
			}
		}
		
		menu.add(new NewLayoutAction(session));
		
		if (lastPathComponent != null) {
			menu.addSeparator();
			if (lastPathComponent instanceof QueryCache) {
			    menu.add(new CopyQueryAction(session, (QueryCache) lastPathComponent));
			}
			
			JTree tree = (JTree) e.getSource();
			//For some bizarre reason, you cannot select a node
			//in the JTree on right-click. So the coordinates for e.getSource()
			//are different from e.getPoint()
			tree.setSelectionRow(tree.getRowForLocation(e.getX(), e.getY()));
			menu.add(new EditCellAction(tree));
		}
		if (lastPathComponent instanceof QueryCache || lastPathComponent instanceof WabitDataSource
				|| lastPathComponent instanceof Layout || lastPathComponent instanceof OlapQuery) {
			menu.add(new DeleteFromTreeAction(lastPathComponent));
		}
		
		if (lastPathComponent instanceof QueryCache) {
			menu.addSeparator();
			menu.add(new AbstractAction("Stop Running") {
				public void actionPerformed(ActionEvent e) {
					((QueryCache) lastPathComponent).stopRunning();
				}
			});
		}

		menu.show(e.getComponent(), e.getX(), e.getY());
	}
	
    /**
     * Creates a JMenu with an item for each data source defined in the context's
     * data source collection. When one of these items is selected, it invokes an
     * action that adds that data source to the project. 
     */
	public JMenu createDataSourcesMenu() {
        JMenu dbcsMenu = new JMenu("Add Data Source"); //$NON-NLS-1$
//        dbcsMenu.add(new JMenuItem(new NewDataSourceAction(this)));
//        dbcsMenu.addSeparator();

        for (SPDataSource dbcs : session.getContext().getDataSources().getConnections()) {
        	JMenuItem newMenuItem = new JMenuItem(new AddDataSourceAction(session.getProject(), dbcs));
        	if (dbcs instanceof Olap4jDataSource) {
        		newMenuItem.setIcon(OLAP_DB_ICON);
        	} else if (dbcs instanceof JDBCDataSource) {
        		newMenuItem.setIcon(DB_ICON);
        	}
        	dbcsMenu.add(newMenuItem);
        }
        SPSUtils.breakLongMenu(session.getFrame(), dbcsMenu);
        
        return dbcsMenu;
	}

}
