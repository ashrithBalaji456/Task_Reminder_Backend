package com.application.taskmanager.analytics.chart;

import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
public class ChartGenerationService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEE (dd/MM)");

    public byte[] generateDailyCompletionChart(Map<LocalDate, Long> dailyCompletions, String title) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            dailyCompletions.forEach((date, count) -> {
                dataset.addValue(count, "Tasks Completed", date.format(DAY_FORMATTER));
            });

            JFreeChart chart = ChartFactory.createBarChart(
                    title,
                    "Day",
                    "Completed Tasks",
                    dataset,
                    PlotOrientation.VERTICAL,
                    false,
                    true,
                    false
            );

            chart.setBackgroundPaint(Color.WHITE);
            chart.getCategoryPlot().setBackgroundPaint(new Color(245, 247, 250));
            chart.getCategoryPlot().getRenderer().setSeriesPaint(0, new Color(40, 167, 69));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 600, 320);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating daily completion chart: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    public byte[] generateStatusDistributionChart(long completed, long pending, long moved, long cancelled) {
        try {
            DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
            dataset.setValue("Completed (" + completed + ")", completed);
            dataset.setValue("Pending (" + pending + ")", pending);
            dataset.setValue("Moved (" + moved + ")", moved);
            dataset.setValue("Cancelled (" + cancelled + ")", cancelled);

            JFreeChart chart = ChartFactory.createPieChart(
                    "Task Status Overview",
                    dataset,
                    true,
                    true,
                    false
            );

            chart.setBackgroundPaint(Color.WHITE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 550, 320);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating status distribution chart: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    public byte[] generatePriorityBreakdownChart(long highCompleted, long highPending, long medCompleted, long medPending, long lowCompleted, long lowPending) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            dataset.addValue(highCompleted, "Completed", "High Priority");
            dataset.addValue(highPending, "Pending", "High Priority");

            dataset.addValue(medCompleted, "Completed", "Medium Priority");
            dataset.addValue(medPending, "Pending", "Medium Priority");

            dataset.addValue(lowCompleted, "Completed", "Low Priority");
            dataset.addValue(lowPending, "Pending", "Low Priority");

            JFreeChart chart = ChartFactory.createBarChart(
                    "Priority Breakdown",
                    "Priority",
                    "Task Count",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );

            chart.setBackgroundPaint(Color.WHITE);
            chart.getCategoryPlot().setBackgroundPaint(new Color(248, 249, 250));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 600, 320);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating priority breakdown chart: {}", e.getMessage(), e);
            return new byte[0];
        }
    }
}
