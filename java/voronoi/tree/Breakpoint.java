package voronoi.tree;

import auxiliary.Parabola;
import auxiliary.Point;
import voronoi.VoronoiDiagram;
import voronoi.dcel.DCELEdge;

/**
 * @author Willem Paul
 */
public class Breakpoint
{
	private Point leftArcSegment, rightArcSegment;
	private DCELEdge tracedEdge;

	private double cachedSweepLinePos;
	private Point cachedBreakpoint;

	public Breakpoint(Point leftArcSegment, Point rightArcSegment, DCELEdge tracedEdge)
	{
		this.leftArcSegment = leftArcSegment;
		this.rightArcSegment = rightArcSegment;
		this.tracedEdge = tracedEdge;
		cachedSweepLinePos = Double.MIN_VALUE;
		cachedBreakpoint = null;
	}

	/**
	 * Calculates the x-coordinate of the breakpoint. All {@code Point} objects returned by this method have a
	 * y-coordinate of 0, as the y-coordinate is irrelevant to searching the status tree.
	 *
	 * @return A {@code Point} representing the x-coordinate of the breakpoint.
	 */
	public Point getCoordinates()
	{
		/* If the sweep line is at the same position, there's no need to recalculate the breakpoint */
		double currentSweepLinePos = VoronoiDiagram.getSweepLinePos();
		if (currentSweepLinePos == cachedSweepLinePos && cachedBreakpoint != null)
			return cachedBreakpoint;

		cachedSweepLinePos = currentSweepLinePos;

		Parabola left = new Parabola(leftArcSegment, currentSweepLinePos);
		Parabola right = new Parabola(rightArcSegment, currentSweepLinePos);

		/* Handle new site point case (degenerate parabola) */
		if (leftArcSegment.getY() == currentSweepLinePos)
		{
			cachedBreakpoint = new Point(leftArcSegment.getX(), right.getYFromX(leftArcSegment.getX()));
			return cachedBreakpoint;
		}
		else if (rightArcSegment.getY() == currentSweepLinePos)
		{
			cachedBreakpoint = new Point(rightArcSegment.getX(), left.getYFromX(rightArcSegment.getX()));
			return cachedBreakpoint;
		}

		double[] intersections = Parabola.intersectionX(left, right);

		/* Choose the correct intersection */
		double breakpointX;
		if (leftArcSegment.getY() < rightArcSegment.getY()) breakpointX = Math.max(intersections[0], intersections[1]);
		else breakpointX = Math.min(intersections[0], intersections[1]);

		cachedBreakpoint = new Point(breakpointX, left.getYFromX(breakpointX));
		return cachedBreakpoint;
	}

	public Point getLeftArcSegment()
	{
		return leftArcSegment;
	}

	public Point getRightArcSegment()
	{
		return rightArcSegment;
	}

	public DCELEdge getTracedEdge()
	{
		return tracedEdge;
	}
}