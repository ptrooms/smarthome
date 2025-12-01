/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 * 15nov25 ptrooms: nuber series to allow duplicates
 * 14nov25 ptrooms: fix error that blockewd groups
 * 10nov25 ptrooms: area painting when an item/group contains "/" (slash) which will be stripped
 * 08nov25 ptrooms: shifting X-axis left/lright using "<" & ">" which will nbe stripped from the field
 * 04nov25 ptrooms: support multiplication (asterisk *) & addition (power ^) to values using item/groupname+- and logaritme with dpi=123
 */
package org.eclipse.smarthome.ui.internal.chart.defaultchartprovider;

//     java.awt.... Java Foundation Classes   [https://en.wikipedia.org/wiki/Abstract_Window_Toolkit]
//          vs Swing (GUI widget toolkit) Oracle's Java Foundation Classes, extention of AWT
//          swing cursus: [https://www.guru99.com/nl/java-swing-gui.html]
//      java.awt.Graphics allow an application to draw contexts [https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics.html]
//      java.awt.Font represents a specific font face, which can be used for rendering texts. , read [https://examples.javacodegeeks.com/java-development/desktop-java/swing/java-awt-graphics-example/]
import java.awt.BasicStroke;    // render Graphics2D [https://docs.oracle.com/javase/8/docs/api/java/awt/BasicStroke.html]
import java.awt.Color;          // Colors set/get using RGB [https://docs.oracle.com/javase/8/docs/api/java/awt/Color.html]
import java.awt.Graphics2D;     // extends Graphics , to advanced [https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html]
import java.awt.image.BufferedImage;    // Image Colormodel/Raster [https://docs.oracle.com/javase/8/docs/api/java/awt/image/BufferedImage.html]
import java.time.ZonedDateTime; // immutable 2007-12-03T10:15:30+01:00 Europe/Paris [https://docs.oracle.com/javase/8/docs/api/java/time/ZonedDateTime.html]
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;      // https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html
import java.util.Date;          // https://docs.oracle.com/javase/8/docs/api/java/util/Date.html
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.apache.commons.lang.StringUtils;     // https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html

import org.eclipse.smarthome.core.i18n.TimeZoneProvider;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.persistence.FilterCriteria;               // file:///home/pafoxp/code-openhab/ptrooms_smarthome/bundles/core/org.eclipse.smarthome.core.persistence/src/main/java/org/eclipse/smarthome/core/persistence/FilterCriteria.java
import org.eclipse.smarthome.core.persistence.FilterCriteria.Ordering;
import org.eclipse.smarthome.core.persistence.HistoricItem;                 // file:///home/pafoxp/code-openhab/ptrooms_smarthome/bundles/core/org.eclipse.smarthome.core.persistence/src/main/java/org/eclipse/smarthome/core/persistence/HistoricItem.java
import org.eclipse.smarthome.core.persistence.PersistenceService;
import org.eclipse.smarthome.core.persistence.PersistenceServiceRegistry;
import org.eclipse.smarthome.core.persistence.QueryablePersistenceService;
import org.eclipse.smarthome.core.types.State;

import org.eclipse.smarthome.ui.chart.ChartProvider;
import org.eclipse.smarthome.ui.internal.chart.ChartServlet;
// doc/api xchart: http://192.168.1.8/xchart_apidocs/index.html [file:///home/pafoxp/code-xchart/target.doc/site/apidocs]
import org.eclipse.smarthome.ui.items.ItemUIRegistry;       // file:///home/pafoxp/code-openhab/ptrooms_smarthome/bundles/ui/org.eclipse.smarthome.ui/src/main/java/org/eclipse/smarthome/ui/internal/items/ItemUIRegistryImpl.java
import org.knowm.xchart.Chart;			// https://github.com/knowm/XChart
import org.knowm.xchart.ChartBuilder;
import org.knowm.xchart.Series;
import org.knowm.xchart.SeriesLineStyle;
import org.knowm.xchart.SeriesMarker;
// import org.apache.logging.log4j.Logger.*;

import org.knowm.xchart.StyleManager.LegendPosition;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This default chart provider generates time-series charts for a given set of items.
 *
 * See {@link ChartProvider} and {@link ChartServlet} for further details.
 *
 * @author Chris Jackson - Initial contribution
 * @author Holger Reichert - Support for themes, DPI, legend hiding
 * @author Christoph Weitkamp - Consider default persistence service
 */
@Component(immediate = true)
public class DefaultChartProvider implements ChartProvider {

    private final Logger logger = LoggerFactory.getLogger(DefaultChartProvider.class);

    private TimeZoneProvider timeZoneProvider;
    protected ItemUIRegistry itemUIRegistry;
    private PersistenceServiceRegistry persistenceServiceRegistry;

    private int legendPosition = 0;

    private static final ChartTheme[] CHART_THEMES_AVAILABLE = { new ChartThemeWhite(), new ChartThemeBright(),
            new ChartThemeDark(), new ChartThemeBlack() };
    public static final String CHART_THEME_DEFAULT_NAME = "bright";
    private Map<String, ChartTheme> chartThemes = null;

    public static final int DPI_DEFAULT = 96;


    private String numericString  = "01234567890-.";  // ptrooms: used to check for numerics using operators
    private double itemZoom = 0;    // used in calculations multiply
    /* define Y-axsis addition  */
    private double itemAdd  = 0;    // used in calculation adding value
    private int itemArea     = -1;  // line chart, else area with color 0-11
    private int itemLineType = -1;  // Xchart SeriesLineStyle : 0/NONE, 1/SOLID, 1/DASH_DOT, 2/DASH_DASH, 3/DOT_DOT 
    private int itemMarker   = -1;  // Xchart SeriesMarker :     -1/NONE, 0/CIRCLE, 1/DIAMOND, 2/SQUARE, 3/4 TRIANGLE_DOWN/UP
    private int itemColor    = -1;  // Xchart SeriesColo   : default, 0-11/BLUE,GREEN,RED,YELLOW,MAGENTA,PINK,LIGHT_GREY, CYAN, BROWN,BLACK
    private long itemTime  = 0L;        // shifttime

    @Reference              // Annotation Type Referenc, form of metadata 
    public void setItemUIRegistry(ItemUIRegistry itemUIRegistry) {
        this.itemUIRegistry = itemUIRegistry;
    }

    public void unsetItemUIRegistry(ItemUIRegistry itemUIRegistry) {
        this.itemUIRegistry = null;
    }

    @Reference
    protected void setPersistenceServiceRegistry(PersistenceServiceRegistry persistenceServiceRegistry) {
        this.persistenceServiceRegistry = persistenceServiceRegistry;
    }

    protected void unsetPersistenceServiceRegistry(PersistenceServiceRegistry persistenceServiceRegistry) {
        this.persistenceServiceRegistry = null;
    }

    @Reference
    public void setTimeZoneProvider(TimeZoneProvider timeZoneProvider) {
        this.timeZoneProvider = timeZoneProvider;
    }

    public void unsetTimeZoneProvider(TimeZoneProvider timeZoneProvider) {
        this.timeZoneProvider = null;
    }

    @Activate
    protected void activate() {
        logger.debug("Starting up default chart provider.");
        String themeNames = Arrays.stream(CHART_THEMES_AVAILABLE) //
                .map(t -> t.getThemeName()) //
                .collect(Collectors.joining(", "));
        logger.debug("Available themes for default chart provider: {}", themeNames);
    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public BufferedImage createChart(String serviceId, String theme, Date startTime, Date endTime, int height,
            int width, String items, String groups, Integer dpiValue, Boolean legend)
            throws ItemNotFoundException, IllegalArgumentException {
        logger.debug(
                "Rendering chart: service: '{}', theme: '{}', startTime: '{}', endTime: '{}', width: '{}', height: '{}', items: '{}', groups: '{}', dpi: '{}', legend: '{}'",
                serviceId, theme, startTime, endTime, width, height, items, groups, dpiValue, legend);

        // If a persistence service is specified, find the provider, or use the default provider
        // Null comparison always yields false: The variable serviceId cannot be null at this location
        if (serviceId == "") serviceId = "rrd4j";             // ptrooms: assume we use this, to prevent mapdb has precedende
        PersistenceService service = (serviceId == null) ? persistenceServiceRegistry.getDefault()
                : persistenceServiceRegistry.get(serviceId);

        // Did we find a service?
        QueryablePersistenceService persistenceService = (service instanceof QueryablePersistenceService)
                ? (QueryablePersistenceService) service
                : (QueryablePersistenceService) persistenceServiceRegistry.getAll() //
                        .stream() //
                        .filter(it -> it instanceof QueryablePersistenceService) //
                        .findFirst() //
                        .orElseThrow(() -> new IllegalArgumentException("No Persistence service found."));

        int seriesCounter = 0;

        // get theme
        ChartTheme chartTheme = getChartTheme(theme);

        // get DPI
        int dpi;
        if (dpiValue != null && dpiValue > 0) {
            dpi = dpiValue;
        } else {
            dpi = DPI_DEFAULT;
        }

        // Create Chart
        // 04nov25 ptrooms add name
        // Chart chart = new ChartBuilder().width(width).height(height).build();
        // Chart chart = new ChartBuilder().width(width).height(height).title(getClass().getSimpleName()).build();
        String titleString = "";
        if (items  != null) titleString += items;
        if (titleString.length() > 0) titleString += ":"; 
        if (groups != null) titleString += groups;
        if (titleString.length() == 0) titleString += "None"; 
        Chart chart = new ChartBuilder().width(width).height(height).title(titleString).build();

        // Define the time axis - the defaults are not very nice
        long period = (endTime.getTime() - startTime.getTime()) / 1000;
        String pattern = "HH:mm";                   // https://www.digitalocean.com/community/tutorials/java-simpledateformat-java-date-format
    
        if (period <= 600) { // 10 minutes
            pattern = "mm:ss";
        } else if (period <= 86400) { // 1 day
            pattern = "HH:mm";
        } else if (period <= 604800) { // 1 week
            pattern = "EEE d";
        } else if (period > 31536000) { // 1 Year   ptrooms
            pattern = "MMM/yy";
        } else {
            pattern = "d MMM";
        }

        chart.getStyleManager().setDatePattern(pattern);        // file:///home/pafoxp/code-xchart/xchart/src/main/java/org/knowm/xchart/StyleManager.java
        // axis
        chart.getStyleManager().setAxisTickLabelsFont(chartTheme.getAxisTickLabelsFont(dpi));
        chart.getStyleManager().setAxisTickLabelsColor(chartTheme.getAxisTickLabelsColor());
        chart.getStyleManager().setXAxisMin(startTime.getTime());
        chart.getStyleManager().setXAxisMax(endTime.getTime());
        int yAxisSpacing = Math.max(height / 10, chartTheme.getAxisTickLabelsFont(dpi).getSize());
        chart.getStyleManager().setYAxisTickMarkSpacingHint(yAxisSpacing);
        // chart
        chart.getStyleManager().setChartBackgroundColor(chartTheme.getChartBackgroundColor());
        chart.getStyleManager().setChartFontColor(chartTheme.getChartFontColor());
        chart.getStyleManager().setChartPadding(chartTheme.getChartPadding(dpi));
        chart.getStyleManager().setPlotBackgroundColor(chartTheme.getPlotBackgroundColor());
        float plotGridLinesDash = (float) chartTheme.getPlotGridLinesDash(dpi);
        float[] plotGridLinesDashArray = { plotGridLinesDash, plotGridLinesDash };
        chart.getStyleManager().setPlotGridLinesStroke(
                new BasicStroke((float) chartTheme.getPlotGridLinesWidth(dpi), 0, 2, 10, plotGridLinesDashArray, 0));
        chart.getStyleManager().setPlotGridLinesColor(chartTheme.getPlotGridLinesColor());
        // legend
        chart.getStyleManager().setLegendBackgroundColor(chartTheme.getLegendBackgroundColor());
        chart.getStyleManager().setLegendFont(chartTheme.getLegendFont(dpi));
        chart.getStyleManager().setLegendSeriesLineLength(chartTheme.getLegendSeriesLineLength(dpi));
        
        // set logscale, note: negative values are by definition not supported
        if (dpi == 123) chart.getStyleManager().setYAxisLogarithmic(true);      // activate log
        // if (dpi == 123) chart.getStyleManager().setYAxisMin(-10);            // test behavior, negativ, no graph
        // logger.debug("Processing groups: {}, items: {}", groups, items );
        // Loop through all the items
        // String numericString  = "01234567890.-";        // ptrooms: used to check for numerics using operators
        if (items != null) {
            logger.debug("Processing items: {}", items);
            String[] itemNames = items.split(",");
            for (String itemName : itemNames) {
                itemTime  = 0L;
                itemArea  = -1;                         // line chart, else area
                itemZoom  = 0;
                itemAdd   = 0;
                itemLineType = -1;   // solid line
                itemMarker   = -1;     // no markers
                itemColor    = -1;   //  automatic

                String itemString = processFormula(itemName);
                Item item = itemUIRegistry.getItem(itemString);
                if (addItem(chart, persistenceService, startTime, endTime, item, seriesCounter, chartTheme, 
                        dpi, itemZoom, itemAdd, itemTime, itemArea, itemLineType, itemMarker, itemColor)) {
                    seriesCounter++;
                }
            }
        }
        // Loop through all the groups and add each item from each group
        if (groups != null) {
            logger.debug("Processing groups: {}", groups);
            String[] groupNames = groups.split(",");
            for (String groupName : groupNames) {
                itemZoom = 0;       // no mulitplication
                itemAdd  = 0;       // no addition
                itemTime  = 0L;     // shifttime
                itemArea  = -1;  // no area
                itemLineType = -1;   // solid line
                itemMarker   = -1;   // no markers
                itemColor    = -1;   //  automatic                String groupString = processFormula(groupName);
                String itemString = processFormula(groupName);
                Item item = itemUIRegistry.getItem(itemString);

                if (item instanceof GroupItem) {
                    GroupItem groupItem = (GroupItem) item;
                    for (Item member : groupItem.getMembers()) {
                        logger.trace("Getting group: {}, item: {}", itemString, member);
                        if (addItem(chart, persistenceService, startTime, endTime, member, seriesCounter, chartTheme,
                                dpi, itemZoom, itemAdd, itemTime, itemArea, itemLineType, itemMarker, itemColor) ) {
                            seriesCounter++;
                        }
                    }
                } else {
                    throw new ItemNotFoundException("Item '" + item.getName() + "' defined in groups is not a group.");
                }
            }
        }

        Boolean showLegend = null;

        // If there are no series, render a blank chart
        if (seriesCounter == 0) {
            // always hide the legend
            showLegend = false;

            List<Date> xData = new ArrayList<Date>();
            List<Number> yData = new ArrayList<Number>();

            xData.add(startTime);
            yData.add(0);
            xData.add(endTime);
            yData.add(0);

            Series series = chart.addSeries("NONE", xData, yData);
            series.setMarker(SeriesMarker.NONE);
            series.setLineStyle(new BasicStroke(0f));
            // nok: series.setChartXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);
            // ok : series.setSeriesType(Series.SeriesType.Area);
        }

        // if the legend is not already hidden, check if legend parameter is supplied, or calculate a sensible value
        if (showLegend == null) {
            if (legend == null) {
                // more than one series, show the legend. otherwise hide it.
                showLegend = seriesCounter > 1;
            } else {
                // take value from supplied legend parameter
                showLegend = legend;
            }
        }

        // Legend position (top-left or bottom-left) is dynamically selected based on the data
        // This won't be perfect, but it's a good compromise
        if (showLegend) {
            if (legendPosition < 0) {
                chart.getStyleManager().setLegendPosition(LegendPosition.InsideNW);
            } else {
                chart.getStyleManager().setLegendPosition(LegendPosition.InsideSW);
            }
        } else { // hide the whole legend
            chart.getStyleManager().setLegendVisible(false);
        }

        // Write the chart as a PNG image
        BufferedImage lBufferedImage = new BufferedImage(chart.getWidth(), chart.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D lGraphics2D = lBufferedImage.createGraphics();
        chart.paint(lGraphics2D);
        return lBufferedImage;
    }

    double convertData(State state) {
        if (state instanceof DecimalType) {
            return ((DecimalType) state).doubleValue();
        } else if (state instanceof OnOffType) {
            return (state == OnOffType.OFF) ? 0 : 1;
        } else if (state instanceof OpenClosedType) {
            return (state == OpenClosedType.CLOSED) ? 0 : 1;
        } else {
            logger.debug("Unsupported item type in chart: {}", state.getClass().toString());
            return 0;
        }
    }
    
    boolean addItem(Chart chart, QueryablePersistenceService service, Date timeBegin, Date timeEnd, Item item,
            int seriesCounter, ChartTheme chartTheme, int dpi, 
            double itemZoom, double itemAdd, long itemTime, 
            int itemArea, int itemLineType, int itemMarker, int itemColor) {
                
        Color color = chartTheme.getLineColor(seriesCounter);
        if  (itemColor >= 0) color = chartTheme.getLineColor(itemColor);    // see Xchart SeriesColor 0/BLUE-11/BLACK

        // Get the item label
        String label = null;
        if (itemUIRegistry != null) {
            // Get the item label
            label = itemUIRegistry.getLabel(item.getName());
            if (label != null && label.contains("[") && label.contains("]")) {
                label = label.substring(0, label.indexOf('['));
            }
        }
        if (label == null) {
            label = item.getName();
        }

        Iterable<HistoricItem> result;
        FilterCriteria filter;

        // Generate data collections
        List<Date> xData = new ArrayList<Date>();
        List<Number> yData = new ArrayList<Number>();

        // Declare state here so it will hold the last value at the end of the process
        State state = null;

        // First, get the value at the start time.
        // This is necessary for values that don't change often otherwise data will start
        // after the start of the graph (or not at all if there's no change during the graph period)
        filter = new FilterCriteria();
        filter.setEndDate(ZonedDateTime.ofInstant(timeBegin.toInstant(), timeZoneProvider.getTimeZone()));
        filter.setItemName(item.getName());
        filter.setPageSize(1);
        filter.setOrdering(Ordering.DESCENDING);
        result = service.query(filter);
        if (result.iterator().hasNext()) {
            HistoricItem historicItem = result.iterator().next();

            state = historicItem.getState();
            
            // xData.add(timeBegin);                                // java.util.date
            xData.add(new Date(timeBegin.getTime() + itemTime));    // ptrooms: we shift X-axis by item symbol gt/lt

            // yData.add(convertData(state)); // Double.valueOf(itemZoom)
            // ptrooms 02nov25, check if we can influence value by label
            logger.debug("Plotting item {}, date: {}, value: {}", item.getName(), (new Date(timeBegin.getTime())) , convertData(state) );
            yData.add( calculateState(state, label, itemZoom, itemAdd) );
        }

        // Now, get all the data between the start and end time
        filter.setBeginDate(ZonedDateTime.ofInstant(timeBegin.toInstant(), timeZoneProvider.getTimeZone()));
        filter.setEndDate(ZonedDateTime.ofInstant(timeEnd.toInstant(), timeZoneProvider.getTimeZone()));
        filter.setPageSize(Integer.MAX_VALUE);
        filter.setOrdering(Ordering.ASCENDING);

        // Get the data from the persistence store
        result = service.query(filter);
        Iterator<HistoricItem> it = result.iterator();

        // Iterate through the data
        while (it.hasNext()) {
            HistoricItem historicItem = it.next();

            // For 'binary' states, we need to replicate the data
            // to avoid diagonal lines
            if (state instanceof OnOffType || state instanceof OpenClosedType) {
                Calendar cal = Calendar.getInstance();      // returns fields initialized with the current date and time
                cal.setTime(historicItem.getTimestamp());   //      set field with value of historicItem (= using java.util.Date)
                cal.add(Calendar.MILLISECOND, -1);          // get and set indicating the millisecond within the second

                //// example: xData.add(  new Date(timeBegin.getTime() + itemTime)  );  // Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT 
                // xData.add(cal.getTime());                       // Returns a Date object representing this Calendar's time value
                xData.add( new Date((cal.getTime()).getTime() + itemTime) );  // Get Date(get cal to Date) to millies + time)

                // xData.add(new Date(date + itemTime)); // ??? ptrooms we want to shift 
                // yData.add(convertData(state));
                // ptrooms 02nov25, check if we can influence value by label                
                
                if (itemZoom != 0 && itemZoom != 0) {
                    yData.add((convertData(state)*Double.valueOf(itemZoom))+Double.valueOf(itemAdd) );
                } else if (itemZoom != 0) {
                    yData.add(convertData(state)*Double.valueOf(itemZoom));
                } else if (itemAdd != 0) {
                    yData.add(convertData(state)+Double.valueOf(itemAdd));
                } else if (label.contains("*1") && (state instanceof DecimalType)) {
                    // yData.add((((DecimalType) state).doubleValue()));
                    yData.add(convertData(state)*Double.valueOf(1));
                } else if (label.contains("*2") && (state instanceof DecimalType)) {
                    yData.add(convertData(state)*Double.valueOf(2));
                } else if (label.contains("*3") && (state instanceof DecimalType)) {
                    yData.add(convertData(state)*Double.valueOf(3));
                } else {
                    yData.add(convertData(state));
                }
            }

            state = historicItem.getState();
            // xData.add(historicItem.getTimestamp());
            logger.trace("Plotting item {}, date: {}, value: {}", item.getName(), (new Date((historicItem.getTimestamp()).getTime())) , convertData(state) );
            xData.add(new Date((historicItem.getTimestamp()).getTime() + itemTime));
            // yData.add(convertData(state));
            // ptrooms 02nov25, check if we can influence value by label
            yData.add( calculateState(state, label, itemZoom, itemAdd) );
        }

        // Lastly, add the final state at the endtime
        if (state != null) {
            logger.debug("Plotting last item {}, date: {}, value: {}", item.getName(), (new Date(timeEnd.getTime())) , convertData(state) );
            // xData.add(timeEnd);
            xData.add(new Date(timeEnd.getTime() + itemTime));
            // ptrooms 02nov25, check if we can influence value by label
            yData.add( calculateState(state, label, itemZoom, itemAdd) );
        }

        // Add the new series to the chart - only if there's data elements to display
        // The chart engine will throw an exception if there's no data
        if (xData.size() == 0) {
            return false;
        }

        // If there's only 1 data point, plot it again!
        if (xData.size() == 1) {
            xData.add(xData.iterator().next());
            yData.add(yData.iterator().next());
        }
        
        /* tbd
        *      series.setChartXYSeriesRenderStyle(series.XYSeriesRenderStyle.Area); // https://knowm.org/open-source/xchart/xchart-example-code/
        * 
        */

        Series series = chart.addSeries( (String) (seriesCounter+"="+label), xData, yData);
        float lineWidth = (float) chartTheme.getLineWidth(dpi);
        
        // series.setLineStyle(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));        
        series.setLineStyle(SeriesLineStyle.SOLID);        
        if      (itemLineType ==  0) series.setLineStyle(SeriesLineStyle.NONE);
        else if (itemLineType ==  1) series.setLineStyle(SeriesLineStyle.DASH_DOT);
        else if (itemLineType ==  2) series.setLineStyle(SeriesLineStyle.DASH_DASH);
        else if (itemLineType ==  3) series.setLineStyle(SeriesLineStyle.DOT_DOT);
  
        series.setMarker(SeriesMarker.NONE);
        if      (itemMarker   ==  0) series.setMarker(SeriesMarker.CIRCLE);
        else if (itemMarker   ==  1) series.setMarker(SeriesMarker.DIAMOND);
        else if (itemMarker   ==  2) series.setMarker(SeriesMarker.SQUARE);
        else if (itemMarker   ==  3) series.setMarker(SeriesMarker.TRIANGLE_DOWN);
        else if (itemMarker   ==  4) series.setMarker(SeriesMarker.TRIANGLE_UP);

        series.setLineColor(color);


        if (itemArea>= 0) series.setSeriesType(Series.SeriesType.Area);     // see file:///home/pafoxp/code-xchart/xchart-demo/src/main/java/org/knowm/xchart/demo/charts/area/AreaLineChart03.java


        // If the start value is below the median, then count legend position down
        // Otherwise count up.
        // We use this to decide whether to put the legend in the top or bottom corner.
        if (yData.iterator().next().floatValue() > ((series.getYMax() - series.getYMin()) / 2 + series.getYMin())) {
            legendPosition++;
        } else {
            legendPosition--;
        }

        return true;
    }

    /**
     * Process formula for shifting/moving/fille graph values
     * Input: (/area<|>timeshift)itemString(*multiplynumber^addnumber)
     * returns: stripped "itemname" with updated itemArea, itemTime, itemZoom, itemAdd
     */    
    String processFormula(String inputString) {
                // itemString = itemString.replace(">", "");
        String itemString = inputString;

        while (itemString.length() > 0 &&                               // set linetype
                itemString.indexOf('-') == 0 ) {   // starts with "-"
                itemLineType++;
                if (itemString.length() > 0) itemString = itemString.substring(1);      
                else itemString = ""; 
        }

        while (itemString.length() > 0 &&                               // set markertype
                itemString.indexOf('.') == 0 ) {   // starts with "-"
                itemMarker++;
                if (itemString.length() > 0) itemString = itemString.substring(1);      
                else itemString = ""; 
        }

        while (itemString.length() > 0 &&                                  // set Color
                numericString.indexOf(itemString.substring(0,1)) >= 0  &&
                numericString.indexOf(itemString.substring(0,1)) <= 9 ) {
                if (itemString.indexOf('0') == 0 ) itemColor++;                 // increase the color
                else itemColor = Integer.valueOf(itemString.substring(0,1));    // set color 1-9
                if (itemString.length() > 0) itemString = itemString.substring(1);      
                else itemString = ""; 
        }
        
        while (itemString.contains("/") && itemString.length() > 1 ) {      // check area
            itemArea++;
            itemString = itemString.replace("/", "");
            if (itemString.indexOf('/') == 0 ) {                               // at begin
                itemString = itemString.substring(1);
            } else if (itemString.indexOf('/') == itemString.length()-1 ) {           // at end
                itemString = itemString.substring(0, itemString.length());   
            } else {                                                                // in between
                itemString = itemString.substring(0, itemString.indexOf('/')-1) + 
                itemString.substring(itemString.indexOf('/')+1, itemString.length()+1 );
            }
        }

        while (itemString.contains("<") && itemString.length() > 1 ) {      // shift left in time
            // minus 1 day , tbd to improve in matching period of calling ChartServlet.java
            itemTime -= 86400000L;
            // itemString = itemString.replace("<", "");
                   if (itemString.indexOf('<') == 0 ) {                               // at begin
                       itemString = itemString.substring(1);
            } else if (itemString.indexOf('<') == itemString.length()-1 ) {           // at end
                       itemString = itemString.substring(0, itemString.length()-2 );   
            } else {                                                                // in between
                       itemString = itemString.substring(0, itemString.indexOf('<')-1) + 
                       itemString.substring(itemString.indexOf('<')+1, itemString.length()-1 );
            }
        }
        while (itemString.contains(">") && itemString.length() > 1 ) {      // shift right intime
            // minus 1 day , tbd to improve in matching period of calling ChartServlet.java
            itemTime += 86400000L;
            // itemString = itemString.replace(">", "");
                   if (itemString.indexOf('>') == 0 ) {                               // at begin
                    itemString = itemString.substring(1);
            } else if (itemString.indexOf('>') == itemString.length()-1 ) {           // at end
                    itemString = itemString.substring(0, itemString.length()-2 );   
            } else {                                                                // in between
                    itemString = itemString.substring(0, itemString.indexOf('>')-1) + itemString.substring(itemString.indexOf('>')+1, itemString.length()-1 );
            }
        }

        // itemTime  = new Date(startTime_Here.getTime() + itemTime);

        while (itemString != null && itemString.length() > 0 
                && (itemString.contains("^") || itemString.contains("*") ) ) {

            int pos = itemString.indexOf('*');          // get our operator
            if (pos < 0) pos = itemString.indexOf('^'); 
            pos++;                                      // position after operator
            if (pos >= (itemString.length())) {         // operator is only or toward end of string
                if (pos == 1) itemString = ""; 
                else itemString = itemString.substring(0, pos-1);  // strip to ignore end
                continue;
            }
            /* scan for numerics after operator next position */
            String valuestring    = ""; 
            while (pos < itemString.length() ) {
                if (numericString.indexOf(itemString.substring(pos,pos+1)) < 0 ) break; // exit if no next number

                /*  does not work as string is reference and no tprimitive
                    // itemString.substring(pos,pos).compareTo("-") == 0 ||
                */
                valuestring = valuestring + itemString.substring(pos,pos+1);        // end is not including
                logger.trace("numeric item {}, pos+1={}, numeric:{}, valuestring:{}", itemString, pos, itemString.substring(pos,pos+1), valuestring );
                if (pos < itemString.length()-1)
                        itemString = itemString.substring(0,pos) + itemString.substring(pos+1);
                else    itemString = itemString.substring(0,pos);
            }

            /* set operator value */  // note substring is from-including, until (excluding)
            if (itemString.substring(pos-1,pos).equals("*"))
                    itemZoom = Double.valueOf(valuestring);
            else    itemAdd  = Double.valueOf(valuestring);
            
            /* strip single operator before pos from string */
            if      (pos == 1) {
                    if (itemString.length() > 1) itemString = itemString.substring(pos);                  // start
                    else itemString = "";
            } else {
                    if (itemString.length() > 2) {
                        if      (pos >= itemString.length()) itemString = itemString.substring(0,pos-1);  // start
                        else    itemString = itemString.substring(0,pos-1) + itemString.substring(pos) ;  // between
                    } else      itemString = itemString.substring(0,pos-1);                               // end
            }
            logger.trace("on item {}, pos+1={}, numerics: {}", itemString, pos, valuestring );                    
        }
        return itemString;
    }

    /** 
     * return double State by multiplying and/or adding paramters
     *   at no fomrula, check/test label for operator *1,2,3 to multiply by 1,2,3
     */
    double calculateState(State state, String label, double itemZoom, double itemAdd) {
        if (itemZoom != 0 && itemZoom != 0) {
            return (convertData(state)*Double.valueOf(itemZoom))+Double.valueOf(itemAdd);
        } else if (itemZoom != 0) {
            return (convertData(state)*Double.valueOf(itemZoom)); // ptrooms: we change Y-axis datascale by item symbol mulitply *
        } else if (itemAdd != 0) {
            return (convertData(state)+Double.valueOf(itemAdd));  // ptrooms: we shift Y-axis datascale by item symbol mulitply *
        } else if (label.contains("*1") && (state instanceof DecimalType)) {
            // yData.add((((DecimalType) state).doubleValue()));
            return (convertData(state)*Double.valueOf(1));
        } else if (label.contains("*2") && (state instanceof DecimalType)) {
            return (convertData(state)*Double.valueOf(2));
        } else if (label.contains("*3") && (state instanceof DecimalType)) {
            return (convertData(state)*Double.valueOf(3));
        }
        return (convertData(state));
    }


    @Override
    public ImageType getChartType() {
        return (ImageType.png);
    }

    /**
     * Retrieve a chart theme by it's name. If no name is given or no theme with the given name exists, the
     * {@link DefaultChartProvider#CHART_THEME_DEFAULT_NAME default theme} gets returned.
     *
     * @param name the {@link ChartTheme#getThemeName() theme name}
     * @return {@link ChartTheme}
     */
    private ChartTheme getChartTheme(String name) {
        // if the static chartThemes hashmap is nul, we have to fill it first with all available themes,
        // based on the theme name
        if (chartThemes == null) {
            chartThemes = new HashMap<>();
            for (ChartTheme theme : CHART_THEMES_AVAILABLE) {
                chartThemes.put(theme.getThemeName(), theme);
            }
        }
        String chartThemeName = name;
        // no theme name -> default theme
        if (StringUtils.isBlank(name)) {
            chartThemeName = CHART_THEME_DEFAULT_NAME;
        }
        ChartTheme chartTheme = chartThemes.get(chartThemeName);
        if (chartTheme == null) {
            // no theme with the given name found -> default theme
            chartTheme = chartThemes.get(CHART_THEME_DEFAULT_NAME);
        }
        return chartTheme;
    }

}
