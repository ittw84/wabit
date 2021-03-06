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

package ca.sqlpower.wabit.swingui.chart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXLabel;
import org.jfree.chart.JFreeChart;

import ca.sqlpower.object.AbstractPoolingSPListener;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.swingui.table.EditableJTable;
import ca.sqlpower.swingui.table.ResultSetTableModel;
import ca.sqlpower.wabit.WabitObject;
import ca.sqlpower.wabit.WabitUtils;
import ca.sqlpower.wabit.WabitWorkspace;
import ca.sqlpower.wabit.report.chart.Chart;
import ca.sqlpower.wabit.report.chart.ChartDataChangedEvent;
import ca.sqlpower.wabit.report.chart.ChartDataListener;
import ca.sqlpower.wabit.report.chart.ChartType;
import ca.sqlpower.wabit.report.chart.ColumnRole;
import ca.sqlpower.wabit.report.chart.DatasetType;
import ca.sqlpower.wabit.report.chart.LegendPosition;
import ca.sqlpower.wabit.rs.WabitResultSetProducer;
import ca.sqlpower.wabit.swingui.WabitIcons;
import ca.sqlpower.wabit.swingui.WabitPanel;
import ca.sqlpower.wabit.swingui.WabitSwingSession;
import ca.sqlpower.wabit.swingui.WabitToolBarBuilder;
import ca.sqlpower.wabit.swingui.chart.effect.ChartAnimation;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.FormLayout;
import com.itextpdf.text.Font;

/**
 * Provides a complete GUI for setting up and modifying a Wabit Chart object.
 */
public class ChartPanel implements WabitPanel {
    
    static final Logger logger = Logger.getLogger(ChartPanel.class);

    /**
     * Key for client property stored in the chart chooser toggle buttons.
     */
    private static final String CHART_TYPE_PROP_KEY = ChartPanel.class.getName() + ".CHART_TYPE_PROP_KEY";

    private final Icon BAR_CHART_ICON = new ImageIcon(ChartPanel.class.getResource("/icons/32x32/chart-bar.png"));
    private final Icon LINE_CHART_ICON = new ImageIcon(ChartPanel.class.getResource("/icons/32x32/chart-line.png"));
    private final Icon PIE_CHART_ICON = new ImageIcon(ChartPanel.class.getResource("/icons/32x32/chart-pie.png"));
    private final Icon SCATTER_CHART_ICON = new ImageIcon(ChartPanel.class.getResource("/icons/32x32/chart-scatter.png"));
    
    /**
     * The panel that holds the chart editor UI.
     */
    private final JPanel panel = new JPanel();
    
    /**
     * Contains all the toggle buttons that are for choosing the chart type.
     * Each button in this group has a client property indicating the {@link ChartType} constant it represents.
     * 
     */
    ButtonGroup chartTypeButtonGroup = new ButtonGroup();

    private final WabitToolBarBuilder toolBarBuilder = new WabitToolBarBuilder();

    /**
     * Contains all the queries in the workspace. This will let the user
     * choose which query to chart.
     */
    private final JComboBox queryComboBox;

    /**
     * Holds the chart's Y axis label.
     */
    private final JTextField yaxisNameField = new JTextField();

    /**
     * A label for the y axis field. This is a member variable so we can make
     * the label invisible when the field is not needed.
     */
    private final JLabel yaxisNameLabel = new JLabel("Y Axis Label");
    
    /**
     * Holds the chart's X axis label.
     */
    private final JTextField xaxisNameField = new JTextField();

    /**
     * A label for the x axis field. This is a member variable so we can make
     * the label invisible when the field is not needed.
     */
    private final JLabel xaxisNameLabel = new JLabel("X Axis Label");

    /**
     * Slider for the rotation of the x-axis tick labels.
     */
    private final JSlider xaxisLabelRotationSlider = new JSlider(-90, 90);
    
    /**
     * A label for the x axis rotation slider. This is a member variable so we can make
     * the label invisible when the field is not needed.
     */
    private final JLabel xaxisLabelRotationLabel = new JLabel("Label Rotation");

    /**
     * Checkbox to control the {@link Chart#isGratuitouslyAnimated()} property.
     */
    private JCheckBox gratuitousAnimationCheckbox = new JCheckBox("");
    
    /**
     * The table that shows values returned from the queries. The headers
     * added to this table will allow users to define which column is the
     * category and which ones are series.
     */
    private final JTable resultTable = new EditableJTable();

    /**
     * Holds the chart's legend position.
     */
    private final JComboBox legendPositionComboBox;

    /**
     * This panel will display a JFreeChart that is a preview of what the
     * user has selected from the result table. This chart should look
     * identical to what would be shown on a report.
     */
    private final org.jfree.chart.ChartPanel chartPanel = new WabitJFreeChartPanel(null);

    /**
     * The label that holds any errors encountered while attempting to create a
     * chart from the query. It should disappear when the chart has been created
     * successfully, but reappear if there's an error.
     * <p>
     * To modify this value (that is, set or clear it), use the {@link #showError()} method.
     */
    private final JXLabel chartError = new JXLabel();

    /**
     * This is the default renderer for the headers of the table displaying the
     * values from the query. It's wrapped by {@link #currentHeaderCellRenderer},
     * which uses it to render the standard headers below our custom
     * "column role" components.
     */
    private final TableCellRenderer defaultHeaderCellRenderer;

    /**
     * Wrapper around the regular {@link #defaultHeaderCellRenderer}. Places the
     * appropriate combo boxes above the standard table headers to allow users
     * to specify the roles of columns (series, categories, or x-axis values) in
     * a chart.
     */
    private ChartTableHeaderCellRenderer currentHeaderCellRenderer;

    /**
     * The header legend of the current table header renderer gets put in the
     * NORTH segment of this panel.
     */
    private JComponent headerLegendContainer = new JPanel(new BorderLayout());

    /**
     * The data model for this component.
     */
    final Chart chart;
    
    private final JCheckBox xAxisAuto = new JCheckBox();
    private final JLabel xAxisAutoLabel = new JLabel("X axis auto range");
    private final JCheckBox yAxisAuto = new JCheckBox();
    private final JLabel yAxisAutoLabel = new JLabel("Y axis auto range");
    
    private final JSpinner xAxisMax = new JSpinner();
    private final JLabel xAxisMaxLabel = new JLabel("      X axis max");
    private final JSpinner yAxisMax = new JSpinner();
    private final JLabel yAxisMaxLabel = new JLabel("      Y axis max");
    private final JSpinner xAxisMin = new JSpinner();
    private final JLabel xAxisMinLabel = new JLabel("      X axis min");
    private final JSpinner yAxisMin = new JSpinner();
    private final JLabel yAxisMinLabel = new JLabel("      Y axis min");
    

    /**
     * Listens for changes to all text fields on this panel. Updates the chart
     * object and its preview on each document event.
     */
    private DocumentListener documentChangeHandler = new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
            update();
        }
        public void insertUpdate(DocumentEvent e) {
            update();
        }
        public void removeUpdate(DocumentEvent e) {
            update();
        }
        private void update() {
            try {
                skipTextFieldRefresh = true;
                updateChartFromGUI();
            } finally {
                skipTextFieldRefresh = false;
            }
        }
    };

	/**
	 * {@link MouseListener} that listens for mouse clicks to update the chart
	 * from GUI.
	 */
    private MouseListener genericMouseListener = new MouseAdapter() {
    	@Override
    	public void mouseClicked(MouseEvent e) {
    		updateChartFromGUI();
    	}
	};

    private ChangeListener genericChangeHandler = new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            updateChartFromGUI();
        }
    };
    
    /**
     * Listens to changes in the chart's properties (this component's data
     * model) and updates the GUI when appropriate. Compare with
     * {@link #chartDataListener}, which handles events strictly dealing with
     * the chart's current data (rather than its configuration).
     */
    private final SPListener chartListener = new AbstractPoolingSPListener() {

        /**
         * Set of properties that are ignored with regards to marking this chart
         * panel as having unsaved changes.
         */
        private final Set<String> ignorableProperties = new HashSet<String>();
        {
            ignorableProperties.add("unfilteredResultSet");
        }
        
        public void propertyChangeImpl(PropertyChangeEvent evt) {
            logger.debug(
                    "Got chart property change: \""+evt.getPropertyName()+"\" " +
                    		evt.getOldValue() + " -> " + evt.getNewValue());
            
            updateGUIFromChart();
        }
    };

    /**
     * Listens to changes to the chart's dataset and updates the chart preview
     * when appropriate. Compare with {@link #chartListener}, which handles
     * events dealing with the chart's current data (rather than its
     * configuration).
     */
    private final ChartDataListener chartDataListener = new ChartDataListener() {
        public void chartDataChanged(ChartDataChangedEvent evt) {
            if (resultTable.getModel() instanceof ResultSetTableModel) {
                ResultSetTableModel rstm = (ResultSetTableModel) resultTable.getModel();
                logger.debug("Updating table model.");
                rstm.dataChanged();
            }
            updateGUIFromChart();
            ChartPanel.this.chartPanel.repaint();
        }
    };

    /**
     * Protects {@link #updateChartFromGUI()} and {@link #updateGUIFromChart()}
     * against mutual recursion.
     * 
     * @see #skipTextFieldRefresh
     */
    private volatile boolean updating = false;

    /**
     * Protects against trying to update text fields when we're already handling
     * a DocumentChanged event.
     * 
     * @see #updating
     */
    private volatile boolean skipTextFieldRefresh = false;
    
    private final Action refreshDataAction;
    private final Action revertToDefaultsAction;

    /**
     * Listens to all the combo boxes, and updates the chart object whenever an
     * event comes in.
     */
    private final ItemListener genericItemListener = new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            updateChartFromGUI();
        }
    };

    /**
     * Listens to anything that can produce an action event (such as a button),
     * and updates the chart object whenever an event comes in.
     */
    private final ActionListener genericActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            updateChartFromGUI();
        }
    };

    public ChartPanel(WabitSwingSession session, final Chart chart) {
		WabitWorkspace workspace = session.getWorkspace();
        this.chart = chart;
        
        refreshDataAction = new RefreshDataAction(chart);
        revertToDefaultsAction = new RevertToDefaultsAction(this);
        
        resultTable.getTableHeader().setReorderingAllowed(false);
        defaultHeaderCellRenderer = resultTable.getTableHeader().getDefaultRenderer();
        
        List<WabitObject> queries = new ArrayList<WabitObject>();
        queries.addAll(workspace.getQueries());
        queries.addAll(workspace.getOlapQueries());
        queryComboBox = new JComboBox(queries.toArray());
        
        legendPositionComboBox = new JComboBox(LegendPosition.values());
        yaxisNameField.getDocument().addDocumentListener(documentChangeHandler);
        xaxisNameField.getDocument().addDocumentListener(documentChangeHandler);

        xaxisLabelRotationSlider.addChangeListener(genericChangeHandler);
        
        gratuitousAnimationCheckbox.addMouseListener(genericMouseListener);
        
        xAxisAuto.addMouseListener(genericMouseListener);
        yAxisAuto.addMouseListener(genericMouseListener);
        
        xAxisMax.addChangeListener(genericChangeHandler);
        yAxisMax.addChangeListener(genericChangeHandler);
        
        xAxisMin.addChangeListener(genericChangeHandler);
        yAxisMin.addChangeListener(genericChangeHandler);
        
        queryComboBox.addItemListener(genericItemListener);
        legendPositionComboBox.addItemListener(genericItemListener);
        
        this.chartError.setAlignmentX(JXLabel.CENTER_ALIGNMENT);
        this.chartError.setAlignmentY(JXLabel.CENTER_ALIGNMENT);
        this.chartError.setLineWrap(true);
        this.chartError.setIcon(WabitIcons.CHART_32);
        
        buildUI();

        //chartPanel.setChart(null);
        chart.addSPListener(chartListener);
        chart.addChartDataListener(chartDataListener);
        
        chart.refresh();
        updateGUIFromChart();
    }

    /**
     * Displays an error in the chart preview area. The given message replaces
     * any previously existing message. If query execution or anything else
     * fails, this is the appropriate way to hand it.
     * 
     * @param ex
     *            The exception to show. If null, the error component will
     *            become invisible.
     */
    private void showError(String message) {
    	
    	logger.debug("Showing exception message in chart editor:" + message);
        
    	chartError.setText(message);
        
    	chartError.setVisible(true);
    	chartPanel.setChart(null);
    	chartPanel.setVisible(false);
    }
    
    private void showChart(JFreeChart chartToDisplay) {
    	chartError.setText(null);
        chartError.setVisible(false);
        chartPanel.setChart(chartToDisplay);
        chartPanel.setVisible(true);
	}
    
    /**
     * When any aspect of the chart changes, this method should be called to set
     * the state of the editor. This will set up the correct result set as well
     * as display the correct editor in a new edit state.
     */
    void updateGUIFromChart() {
    	
        if (updating) return;
        
        
    	try {
    		updating = true;
    		
    		xaxisLabelRotationSlider.setValue((int) chart.getXAxisLabelRotation());
    		
    		xAxisAuto.setSelected(chart.isAutoXAxisRange());
    		yAxisAuto.setSelected(chart.isAutoYAxisRange());
    		
    		xAxisMax.setEnabled(!chart.isAutoXAxisRange());
    		xAxisMin.setEnabled(!chart.isAutoXAxisRange());
    		yAxisMax.setEnabled(!chart.isAutoYAxisRange());
    		yAxisMin.setEnabled(!chart.isAutoYAxisRange());
    		
    		xAxisMax.setValue(chart.getXAxisMaxRange());
    		yAxisMax.setValue(chart.getYAxisMaxRange());
    		xAxisMin.setValue(chart.getXAxisMinRange());
    		yAxisMin.setValue(chart.getYAxisMinRange());
    		
    		gratuitousAnimationCheckbox.setSelected(chart.isGratuitouslyAnimated());
    		
    		queryComboBox.setSelectedItem(chart.getQuery());
    		setSelectedChartType(chart.getType());
    		
    		
    		if (!skipTextFieldRefresh) {
    			yaxisNameField.setText(chart.getYaxisName());
    			xaxisNameField.setText(chart.getXaxisName());
    		}
        
    		if (chart.getQuery() != null &&
    				chart.getUnfilteredResultSet() != null) {
        			
        		if (currentHeaderCellRenderer != null) {
        			currentHeaderCellRenderer.cleanup();
        		}
        		
        		if (chart.getType().getDatasetType() == DatasetType.CATEGORY) {
        			currentHeaderCellRenderer = new CategoryChartHeaderRenderer(this, resultTable.getTableHeader(), defaultHeaderCellRenderer);
        			resultTable.getTableHeader().setDefaultRenderer(currentHeaderCellRenderer);
        			
        			//Set control components visibility based on chart type
        			if (chart.getType() == ChartType.PIE){
        				xaxisNameLabel.setVisible(false);
        				xaxisNameField.setVisible(false);
        				yaxisNameLabel.setVisible(false);
        				yaxisNameField.setVisible(false);
        				xaxisLabelRotationLabel.setVisible(false);
        				xaxisLabelRotationSlider.setVisible(false);
        				xAxisAuto.setVisible(false);
        				xAxisAutoLabel.setVisible(false);
        				xAxisMax.setVisible(false);
        				xAxisMaxLabel.setVisible(false);
        				xAxisMin.setVisible(false);
        				xAxisMinLabel.setVisible(false);
        				yAxisAuto.setVisible(false);
        				yAxisAutoLabel.setVisible(false);
        				yAxisMax.setVisible(false);
        				yAxisMaxLabel.setVisible(false);
        				yAxisMin.setVisible(false);
        				yAxisMinLabel.setVisible(false);
        			}
        			else{
        				xaxisNameLabel.setVisible(true);
        				xaxisNameField.setVisible(true);
        				yaxisNameLabel.setVisible(true);
        				yaxisNameField.setVisible(true);
        				xaxisLabelRotationLabel.setVisible(true);
        				xaxisLabelRotationSlider.setVisible(true);
        				yAxisAuto.setVisible(true);
        				yAxisAutoLabel.setVisible(true);
        				yAxisMax.setVisible(true);
        				yAxisMaxLabel.setVisible(true);
        				yAxisMin.setVisible(true);
        				yAxisMinLabel.setVisible(true);
    					xAxisAuto.setVisible(false);
    					xAxisAutoLabel.setVisible(false);
        				xAxisMax.setVisible(false);
        				xAxisMaxLabel.setVisible(false);
        				xAxisMin.setVisible(false);
        				xAxisMinLabel.setVisible(false);
        			}
        			
        		} else if (chart.getType().getDatasetType() == DatasetType.XY) {
        			try {
        				currentHeaderCellRenderer = new XYChartHeaderRenderer(this, resultTable.getTableHeader(), defaultHeaderCellRenderer);
        			} catch (SQLException e) {
        				showError(WabitUtils.getRootCause(e).getMessage());
        				return;
        			}
        			resultTable.getTableHeader().setDefaultRenderer(currentHeaderCellRenderer);
        			
        			xaxisNameLabel.setVisible(true);
        			xaxisNameField.setVisible(true);
        			yaxisNameLabel.setVisible(true);
        			yaxisNameField.setVisible(true);
        			xaxisLabelRotationLabel.setVisible(false);
        			xaxisLabelRotationSlider.setVisible(false);
        			xAxisAuto.setVisible(true);
					xAxisAutoLabel.setVisible(true);
    				xAxisMax.setVisible(true);
    				xAxisMaxLabel.setVisible(true);
    				xAxisMin.setVisible(true);
    				xAxisMinLabel.setVisible(true);
        		}
        		
        		headerLegendContainer.removeAll();
        		headerLegendContainer.add(
        				currentHeaderCellRenderer.getHeaderLegendComponent(), BorderLayout.NORTH);
        		headerLegendContainer.revalidate();
        		headerLegendContainer.repaint();
        		
        		if(chart.getLegendPosition() != null) {
        			legendPositionComboBox.setSelectedItem(chart.getLegendPosition());
        		} else {
        			legendPositionComboBox.setSelectedItem(LegendPosition.BOTTOM);
        		}
        		
        		final ResultSetTableModel model = 
        			new ResultSetTableModel(chart.getUnfilteredResultSet());
        		resultTable.setModel(model);
        		ChartTableCellRenderer cellRenderer = new ChartTableCellRenderer(chart);
        		for (Enumeration<TableColumn> tableCols = resultTable.getColumnModel().getColumns();
        				tableCols.hasMoreElements(); ) {
        			TableColumn tc = tableCols.nextElement();
        			tc.setCellRenderer(cellRenderer);
        		}
        		
    		} else {
    			if (currentHeaderCellRenderer != null) {
        			currentHeaderCellRenderer.cleanup();
        		}
    			resultTable.getTableHeader().setDefaultRenderer(defaultHeaderCellRenderer);
    		}
    		
    		updateChartPreview();
    		
    	} finally {
    		updating = false;
    	}
    }

    /**
     * Updates the the JFreeChart preview panel and its associated error
     * message based on the current state of the GUI components in this property
     * panel. The chart generated here is only intended as a preview for the
     * property panel.
     */
    private void updateChartPreview() {
        try {
        	
        	if (chart.getQuery() == null) {
        		showError("Please select a query from the data source selector above.");
        		return;
        	}
        	
        	if (chart.getUnfilteredResultSet() == null) {
        		showError("The selected query does not return any data.");
        		return;
        	}
        	
        	if (chart.findRoleColumns(ColumnRole.CATEGORY).isEmpty() && 
        			(chart.getType() == ChartType.BAR
        					|| chart.getType() == ChartType.PIE
        					|| chart.getType() == ChartType.CATEGORY_LINE)) {
        		showError("Map a column to the category role using the controls above.");
        		return;
        	}
        	if (chart.findRoleColumns(ColumnRole.SERIES).isEmpty()) {
        		showError("Map a column to the series role using the controls above.");
        		return;
        	}
        	
        	
            JFreeChart newJFreeChart = ChartSwingUtil.createChartFromQuery(chart);
            logger.debug("Created new JFree chart: " + newJFreeChart);
            this.showChart(newJFreeChart);
            if (chart.isGratuitouslyAnimated()) {
                ChartAnimation.animateIfPossible(newJFreeChart);
            }
        } catch (Exception ex) {
            showError("Unable to create a chart based on the current settings: " + WabitUtils.getRootCause(ex));
        }
    }

	private void buildUI() {
        
    	// First level in the panel has only 2 rows of 1 column
    	panel.setLayout(
    			new MigLayout(
    					"fill", 
    					"[grow, fill]", 
    					"[shrink]10[grow, fill]"));
    	
    	panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY), 
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

    	
    	// Now we build the upper part. 
    	// The "data" section.
    	JPanel topPanel = 
    			new JPanel(
    					new MigLayout(
    							"fill",
    							"[115]10[fill, grow]",
    							"[]10[]10[400]"));
    	
    	JLabel dataCategoryLabel = new JLabel("Data");
    	dataCategoryLabel.setFont(dataCategoryLabel.getFont().deriveFont(Font.BOLD));
    	topPanel.add(dataCategoryLabel, "span, wrap");
    	
    	topPanel.add(new JLabel("Data source"), "gapleft 15");
    	topPanel.add(queryComboBox, "wrap");
    	
		JScrollPane tableScrollPane = new JScrollPane(resultTable);
		topPanel.add(headerLegendContainer, "gapleft 15, aligny top");
		topPanel.add(tableScrollPane);
    	
    	
    	panel.add(topPanel, "wrap");
    	
    	
    	
    	
    	// Now the lower part. It has two parts. The options scrolling pane and the preview.
    	JPanel bottomPanel = 
			new JPanel(
					new MigLayout(
							"fill, hidemode 2",
							"[grow]10[fill, shrinkprio 101]",
							"[fill]"));
	
		JLabel optionsCategoryLabel = new JLabel("Options");
		optionsCategoryLabel.setFont(optionsCategoryLabel.getFont().deriveFont(Font.BOLD));
		bottomPanel.add(optionsCategoryLabel);
	
		JLabel previewCategoryLabel = new JLabel("Preview");
		previewCategoryLabel.setFont(previewCategoryLabel.getFont().deriveFont(Font.BOLD));
		bottomPanel.add(previewCategoryLabel, "wrap");
    	
		JScrollPane optionsScrollPane = 
				new JScrollPane(
						buildChartPrefsPanel(), 
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
						JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		optionsScrollPane.setBorder(null);
		bottomPanel.add(optionsScrollPane, "push, gapleft 15");
		
		JPanel chartAndErrorPanel = new JPanel(new BorderLayout());
		chartAndErrorPanel.add(chartError, BorderLayout.NORTH);
		chartAndErrorPanel.add(chartPanel, BorderLayout.CENTER);
		bottomPanel.add(chartAndErrorPanel, "width 100%, alignx left, gapleft 15");
		
		
		panel.add(bottomPanel, "height 100%");
        
        toolBarBuilder.add(refreshDataAction);
        toolBarBuilder.add(revertToDefaultsAction);
        
        //Since the first button on the tool bar will be displayed this size will be the
        //same as the font size of a displayed button. If the button wasn't being displayed
        //the font size ends up incorrect
        float fontSize = toolBarBuilder.getToolbar().getComponentAtIndex(0).getFont().getSize();

        toolBarBuilder.addSeparator();
        toolBarBuilder.add(makeChartTypeButton("Bar", ChartType.BAR, BAR_CHART_ICON, fontSize));
        toolBarBuilder.add(makeChartTypeButton("Pie", ChartType.PIE, PIE_CHART_ICON, fontSize));
        toolBarBuilder.add(makeChartTypeButton("Category Line", ChartType.CATEGORY_LINE, 
                LINE_CHART_ICON, fontSize));

        toolBarBuilder.addSeparator();
        toolBarBuilder.add(makeChartTypeButton("Line", ChartType.LINE, LINE_CHART_ICON, fontSize));
        toolBarBuilder.add(makeChartTypeButton("Scatter", ChartType.SCATTER, 
                SCATTER_CHART_ICON, fontSize));
    }

    /**
     * Subroutine of {@link #buildUI()}. Makes a chart type toggle button and
     * adds it to the button group.
     * 
     * @param caption
     *            The text to appear under the button
     * @param type
     *            The type of chart the buttons should select
     * @param icon
     *            The icon for the button
     * @param fontSize
     *            the font size for the toggle buttons. The default font size of
     *            the toggle buttons are different than the default font size of
     *            JButtons on some platforms. This value should be equal to the
     *            JButton font size. This is a float as deriving fonts with a size
     *            takes a float.
     * @return A button properly configured for the new-look Wabit toolbar.
     */
    private JToggleButton makeChartTypeButton(
            String caption, ChartType type, Icon icon, float fontSize) {
        JToggleButton b = new JToggleButton(caption, icon);
        b.putClientProperty(CHART_TYPE_PROP_KEY, type);
        chartTypeButtonGroup.add(b);
        
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        
        // Removes button borders on OS X 10.5
        b.putClientProperty("JButton.buttonType", "toolbar");

        b.addActionListener(genericActionListener);
        
        b.setFont(b.getFont().deriveFont(fontSize));
        
        return b;
    }

    private ChartType getSelectedChartType() {
        for (Enumeration<AbstractButton> buttons = chartTypeButtonGroup.getElements();
                buttons.hasMoreElements(); ) {
            AbstractButton b = buttons.nextElement();
            if (chartTypeButtonGroup.isSelected(b.getModel())) {
                ChartType ct = (ChartType) b.getClientProperty(CHART_TYPE_PROP_KEY);
                logger.debug("Found selected chart type " + ct);
                return ct;
            }
        }
        
        logger.debug("Didn't find any selected chart buttons. Returning null.");
        return null;
    }
    
    /**
     * Selects the appropriate button in {@link #chartTypeButtonGroup}.
     * 
     * @param type The type of chart to select the button of.
     */
    private void setSelectedChartType(ChartType type) {
        for (Enumeration<AbstractButton> buttons = chartTypeButtonGroup.getElements();
        buttons.hasMoreElements(); ) {
            AbstractButton b = buttons.nextElement();
            ChartType ct = (ChartType) b.getClientProperty(CHART_TYPE_PROP_KEY);
            if (ct == type) {
                chartTypeButtonGroup.setSelected(b.getModel(), true);
                return;
            }
        }
        throw new IllegalArgumentException("I can't find a button for chart type " + type);
    }

    /**
     * Subroutine of {@link #buildUI()}. Creates the form that appears to the
     * right of the JFreeChart preview.
     */
    private Component buildChartPrefsPanel() {
        DefaultFormBuilder builder = new DefaultFormBuilder(
                new FormLayout("70dlu, 3dlu, 90dlu"),
                logger.isDebugEnabled() ? new FormDebugPanel() : new JPanel());

        builder.append("Legend Postion", legendPositionComboBox);
        builder.nextLine();
        
        builder.append(yaxisNameLabel, yaxisNameField);
        builder.nextLine();
        
        builder.append(xaxisNameLabel, xaxisNameField);
        builder.nextLine();

        builder.append(xaxisLabelRotationLabel, xaxisLabelRotationSlider);
        builder.nextLine();
        
    	builder.append(this.xAxisAutoLabel, this.xAxisAuto);
    	builder.append(this.xAxisMaxLabel, this.xAxisMax);
    	builder.append(this.xAxisMinLabel, this.xAxisMin);
    	builder.nextLine();
        
    	builder.append(this.yAxisAutoLabel, this.yAxisAuto);
    	builder.append(this.yAxisMaxLabel, this.yAxisMax);
    	builder.append(this.yAxisMinLabel, this.yAxisMin);
    	builder.nextLine();
    	
        builder.append("Gratuitous Animation", gratuitousAnimationCheckbox);
        
        return builder.getPanel();
    }

    public boolean hasUnsavedChanges() {
        //TODO replace the false with the chartHasChanges flag
        //when fixing bug 2002.
        return false;
    }

    public JComponent getPanel() {
        return panel;
    }

    public void discardChanges() {
        logger.debug("Discarding changes");
        cleanup();
        updateChartFromGUI(); //TODO remove this when fixing bug 2002.
    }

    public boolean applyChanges() {
        logger.debug("Applying changes");
        cleanup();
        updateChartFromGUI();
        return true;
    }

    private void cleanup() {
        chart.removeSPListener(chartListener);
        chart.removeChartDataListener(chartDataListener);
    }
    
    /**
     * Updates {@link #chart}'s settings to match those currently in this
     * panel's GUI components. This is the inverse of {@link #updateGUIFromChart()}.
     */
    void updateChartFromGUI() {
        if (updating) return;
        try {
            updating = true;
            
            if (getSelectedChartType() != chart.getType()) {
                chart.setType(getSelectedChartType());
            }
            
            chart.setLegendPosition((LegendPosition) legendPositionComboBox.getSelectedItem());
            chart.setYaxisName(yaxisNameField.getText());
            chart.setXaxisName(xaxisNameField.getText());
            chart.setXAxisLabelRotation(xaxisLabelRotationSlider.getValue());
            chart.setGratuitouslyAnimated(gratuitousAnimationCheckbox.isSelected());
            chart.setAutoXAxisRange(xAxisAuto.isSelected());
            chart.setAutoYAxisRange(yAxisAuto.isSelected());
            chart.setXAxisMaxRange(Double.parseDouble(xAxisMax.getValue().toString()));
            chart.setYAxisMaxRange(Double.parseDouble(yAxisMax.getValue().toString()));
            chart.setXAxisMinRange(Double.parseDouble(xAxisMin.getValue().toString()));
            chart.setYAxisMinRange(Double.parseDouble(yAxisMin.getValue().toString()));
            
            if (queryComboBox.getSelectedItem() != chart.getQuery()) {
               chart.setQuery((WabitResultSetProducer) queryComboBox.getSelectedItem());
               chart.refresh();
            }
            
        } finally {
            updating = false;
        }
        
        /*
         * Because we were purposely ignoring events from the chart while updating it,
         * we now have to do an explicit refresh of the table and chart preview. The
         * same interlock that prevented mutual recursion during the GUI->Chart update
         * prevents Chart->GUI updates from re-calling this method.
         */
        updateGUIFromChart();
    }

    public String getTitle() {
        return "Chart Editor - " + chart.getName();
    }

    public JComponent getSourceComponent() {
        // nothing drags onto this
        return null;
    }
    
    public Chart getChart() {
        return chart;
    }
    
    /**
     * Returns the leading X coordinate (within the table of data) of the given
     * column. This is a helper method for the table header cell renderers.
     */
    int getXPositionOfColumn(final TableColumnModel model, final int columnIndex) {
        int sum = 0;
        for (int i = 0; i < columnIndex; i++) {
            sum += model.getColumn(i).getWidth();
        }
        logger.debug("X position of column " + columnIndex + " is " + sum + " according to " + model);
        return sum;
    }

    public JToolBar getToolbar() {
        return toolBarBuilder.getToolbar();
    }


}
