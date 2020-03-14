package display;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import voronoi.queue.Event;

import java.util.List;

/**
 * @author Willem Paul
 */
public class Plotter
{
	private double windowWidth;
	private double windowHeight;
	private GraphicsContext graphicsContext;
	private int scale;

	public Plotter(double windowWidth, double windowHeight, GraphicsContext graphicsContext)
	{
		this.windowWidth = windowWidth;
		this.windowHeight = windowHeight;
		this.graphicsContext = graphicsContext;
		this.scale = 1;
	}

	/**
	 * Plots the points specified by {@code events}, scaled according to the spread of the points.
	 *
	 * @param events The list of points to be plotted.
	 */
	public void plotSiteEvents(List<Event> events)
	{
		graphicsContext.setFill(Color.RED);

		for (Event event : events)
		{
			double factor = Math.min((windowWidth * 0.85) / 2.0, (windowHeight * 0.85) / 2.0) / scale;
			Point2D scaledPoint = scalePoint(event.getCoordinates().getX(), event.getCoordinates().getY(), factor);
			graphicsContext.fillOval(scaledPoint.getX(), scaledPoint.getY(),5, 5);
		}
	}

	public void setScale(int minX, int maxX, int minY, int maxY)
	{
		int maxMax = Math.max(maxX, maxY);
		int maxMin = Math.max(Math.abs(minX), Math.abs(minY));
		this.scale = Math.max(maxMax, maxMin);
	}

	private Point2D scalePoint(int x, int y, double factor)
	{
		return new Point2D((factor * x) + (windowWidth / 2.0), (-factor * y) + (windowHeight / 2.0));
	}
}