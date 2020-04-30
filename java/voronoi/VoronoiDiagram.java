package voronoi;

import auxiliary.Circle;
import auxiliary.MathOps;
import auxiliary.Point;
import dcel.DCELEdge;
import dcel.DCELFace;
import dcel.DCELVertex;
import dcel.DoublyConnectedEdgeList;
import voronoi.tree.ArcSegment;
import voronoi.tree.Breakpoint;
import voronoi.tree.TreeQuery;

import java.util.*;

/**
 * @author Willem Paul
 */
public class VoronoiDiagram extends DoublyConnectedEdgeList
{
	private static double sweepLinePos = Double.MIN_VALUE;
	private static double firstSiteSweepLinePos = Double.MIN_VALUE;

	private final PriorityQueue<Point> queue;
	private final TreeMap<ArcSegment, CircleEvent> status;
	private final HashSet<Breakpoint> breakpoints;

	/**
	 * Constructs a Voronoi diagram from the given set of sites using Steven Fortune's line sweep algorithm.
	 *
	 * @param sites the list of sites for which to construct a Voronoi diagram
	 */
	public VoronoiDiagram(Set<SiteEvent> sites)
	{
		super();

		this.queue = new PriorityQueue<>(sites);
		this.status = new TreeMap<>();
		this.breakpoints = new HashSet<>();

		/* If this is true, there are no site points and thus, there is nothing to be done. */
		if (sites.isEmpty()) return;

		createVoronoiDiagram();
	}

	/**
	 * Returns the current y-position of the sweep line.
	 *
	 * @return the current y-position of the sweep line
	 */
	public static double getSweepLinePos()
	{
		return sweepLinePos;
	}

	private void createVoronoiDiagram()
	{
		while (!queue.isEmpty())
		{
			Point event = queue.poll();
			sweepLinePos = event.getY();
			if (firstSiteSweepLinePos == Double.MIN_VALUE) firstSiteSweepLinePos = event.getY();
			if (event.getClass() == CircleEvent.class)
				handleCircleEvent((CircleEvent) event);
			else if (event.getClass() == SiteEvent.class)
				handleSiteEvent((SiteEvent) event);
			else
				throw new IllegalArgumentException("Non-event element in the queue");
		}

		computeBoundingBox();
		connectInfiniteEdges();
		this.computeFaces();
	}

	private void handleSiteEvent(SiteEvent event)
	{
		faces.add(event.getCell());

		if (status.isEmpty())
		{
			status.put(new ArcSegment(event), null);
			return;
		}

		Map.Entry<ArcSegment, CircleEvent> entryAbove = status.floorEntry(new TreeQuery(event));
		ArcSegment alpha = entryAbove.getKey();

		if (entryAbove.getValue() != null) queue.remove(entryAbove.getValue());

		status.remove(alpha);

		DCELEdge edge1 = new DCELEdge(DCELEdge.EdgeType.VORONOI_EDGE);
		DCELEdge edge2 = new DCELEdge(DCELEdge.EdgeType.VORONOI_EDGE, edge1);
		edges.add(edge1);
		edges.add(edge2);

		//DCELEdge.setTwinPair(edge1, edge2);
		calculateDirections(edge1, edge2, alpha.getSite(), event);

		/* Handle case in which multiple points have the same y-coordinate as the first */
		if (sweepLinePos == firstSiteSweepLinePos)
		{
			Breakpoint breakpoint;
			ArcSegment leftArcSegment, rightArcSegment;

			if (alpha.getSite().getX() < event.getX())
			{
				breakpoint = new Breakpoint(alpha.getSite(), event, null);

				if (!edge1.isDirectedUp())
				{
					breakpoint.setTracedEdge(edge1);

					edge1.setIncidentFace(event.getCell());
					edge2.setIncidentFace(alpha.getSite().getCell());

					event.getCell().setOuterComponent(edge1);
					alpha.getSite().getCell().setOuterComponent(edge2);
				}
				else
				{
					breakpoint.setTracedEdge(edge2);

					edge1.setIncidentFace(alpha.getSite().getCell());
					edge2.setIncidentFace(event.getCell());

					alpha.getSite().getCell().setOuterComponent(edge1);
					event.getCell().setOuterComponent(edge2);
				}

				leftArcSegment = new ArcSegment(alpha.getSite(), alpha.getLeftBreakpoint(), breakpoint);
				rightArcSegment = new ArcSegment(event, breakpoint, alpha.getRightBreakpoint());
			}
			else
			{
				breakpoint = new Breakpoint(event, alpha.getSite(), null);

				if (!edge1.isDirectedUp())
				{
					breakpoint.setTracedEdge(edge1);

					edge1.setIncidentFace(alpha.getSite().getCell());
					edge2.setIncidentFace(event.getCell());

					alpha.getSite().getCell().setOuterComponent(edge1);
					event.getCell().setOuterComponent(edge2);
				}
				else
				{
					breakpoint.setTracedEdge(edge2);

					edge1.setIncidentFace(event.getCell());
					edge2.setIncidentFace(alpha.getSite().getCell());

					event.getCell().setOuterComponent(edge1);
					alpha.getSite().getCell().setOuterComponent(edge2);
				}

				leftArcSegment = new ArcSegment(event, alpha.getLeftBreakpoint(), breakpoint);
				rightArcSegment = new ArcSegment(alpha.getSite(), breakpoint, alpha.getRightBreakpoint());
			}

			breakpoints.add(breakpoint);

			status.put(leftArcSegment, null);
			status.put(rightArcSegment, null);

			return;
		}

		Breakpoint newLeftBreakpoint = new Breakpoint(alpha.getSite(), event);
		Breakpoint newRightBreakpoint = new Breakpoint(event, alpha.getSite());

		if (newLeftBreakpoint.isMovingRight())
		{
			if (edge1.isDirectedRight() || edge1.isDirectedUp())
			{
				newLeftBreakpoint.setTracedEdge(edge1);
				newRightBreakpoint.setTracedEdge(edge2);

				edge1.setIncidentFace(event.getCell());
				edge2.setIncidentFace(alpha.getSite().getCell());

				event.getCell().setOuterComponent(edge1);
				alpha.getSite().getCell().setOuterComponent(edge2);
			}
			else
			{
				newRightBreakpoint.setTracedEdge(edge1);
				newLeftBreakpoint.setTracedEdge(edge2);

				edge1.setIncidentFace(alpha.getSite().getCell());
				edge2.setIncidentFace(event.getCell());

				event.getCell().setOuterComponent(edge2);
				alpha.getSite().getCell().setOuterComponent(edge1);
			}
		}
		else
		{
			if (edge1.isDirectedRight() || edge1.isDirectedUp())
			{
				newLeftBreakpoint.setTracedEdge(edge2);
				newRightBreakpoint.setTracedEdge(edge1);

				edge1.setIncidentFace(alpha.getSite().getCell());
				edge2.setIncidentFace(event.getCell());

				event.getCell().setOuterComponent(edge2);
				alpha.getSite().getCell().setOuterComponent(edge1);
			}
			else
			{
				newRightBreakpoint.setTracedEdge(edge2);
				newLeftBreakpoint.setTracedEdge(edge1);

				edge1.setIncidentFace(event.getCell());
				edge2.setIncidentFace(alpha.getSite().getCell());

				event.getCell().setOuterComponent(edge1);
				alpha.getSite().getCell().setOuterComponent(edge2);
			}
		}

		ArcSegment leftArcSegment = new ArcSegment(alpha.getSite(), alpha.getLeftBreakpoint(), newLeftBreakpoint);
		ArcSegment centerArcSegment = new ArcSegment(event, newLeftBreakpoint, newRightBreakpoint);
		ArcSegment rightArcSegment = new ArcSegment(alpha.getSite(), newRightBreakpoint, alpha.getRightBreakpoint());

		breakpoints.add(newLeftBreakpoint);
		breakpoints.add(newRightBreakpoint);

		status.put(leftArcSegment, null);
		status.put(centerArcSegment, null);
		status.put(rightArcSegment, null);

		checkForCircleEvent(leftArcSegment);
		checkForCircleEvent(rightArcSegment);
	}

	private void handleCircleEvent(CircleEvent event)
	{
		ArcSegment alpha = event.getDisappearingArcSegment();
		status.remove(alpha);

		Map.Entry<ArcSegment, CircleEvent> leftEntry = status.lowerEntry(alpha);
		Map.Entry<ArcSegment, CircleEvent> rightEntry = status.higherEntry(alpha);

		ArcSegment leftNeighbor = leftEntry.getKey();
		ArcSegment rightNeighbor = rightEntry.getKey();

		Breakpoint oldLeftBreakpoint = leftNeighbor.getRightBreakpoint();
		Breakpoint oldRightBreakpoint = rightNeighbor.getLeftBreakpoint();

		if (!(oldLeftBreakpoint.getTracedEdge().getTwin().isDirectedUp() &&
				oldLeftBreakpoint.getTracedEdge().getOrigin() == null))
			breakpoints.remove(oldLeftBreakpoint);

		if (!(oldRightBreakpoint.getTracedEdge().getTwin().isDirectedUp() &&
				oldRightBreakpoint.getTracedEdge().getOrigin() == null))
			breakpoints.remove(oldRightBreakpoint);

		DCELEdge edge1 = new DCELEdge(DCELEdge.EdgeType.VORONOI_EDGE);
		DCELEdge edge2 = new DCELEdge(DCELEdge.EdgeType.VORONOI_EDGE, edge1);
		edges.add(edge1);
		edges.add(edge2);

		//DCELEdge.setTwinPair(edge1, edge2);
		calculateDirections(edge1, edge2, leftNeighbor.getSite(), rightNeighbor.getSite());

		Breakpoint newBreakpoint = new Breakpoint(leftNeighbor.getSite(), rightNeighbor.getSite(), null);

		if (newBreakpoint.isMovingRight())
		{
			if (edge1.isDirectedRight() || edge1.isDirectedUp()) newBreakpoint.setTracedEdge(edge1);
			else newBreakpoint.setTracedEdge(edge2);
		}
		else
		{
			if (edge1.isDirectedRight() || edge1.isDirectedUp()) newBreakpoint.setTracedEdge(edge2);
			else newBreakpoint.setTracedEdge(edge1);
		}

		leftNeighbor.setRightBreakpoint(newBreakpoint);
		rightNeighbor.setLeftBreakpoint(newBreakpoint);

		breakpoints.add(newBreakpoint);

		if (leftEntry.getValue() != null) queue.remove(leftEntry.getValue());
		if (rightEntry.getValue() != null) queue.remove(rightEntry.getValue());

		DCELVertex vertex = new DCELVertex(event.getCircle().getCenter(), newBreakpoint.getTracedEdge());
		vertices.add(vertex);

		oldRightBreakpoint.getTracedEdge().getTwin().setOrigin(vertex);
		oldLeftBreakpoint.getTracedEdge().getTwin().setOrigin(vertex);
		newBreakpoint.getTracedEdge().setOrigin(vertex);

		oldRightBreakpoint.getTracedEdge().getTwin().setPrev(oldLeftBreakpoint.getTracedEdge());
		oldLeftBreakpoint.getTracedEdge().getTwin().setPrev(newBreakpoint.getTracedEdge().getTwin());
		newBreakpoint.getTracedEdge().setPrev(oldRightBreakpoint.getTracedEdge());

		oldRightBreakpoint.getTracedEdge().setNext(newBreakpoint.getTracedEdge());
		oldLeftBreakpoint.getTracedEdge().setNext(oldRightBreakpoint.getTracedEdge().getTwin());
		newBreakpoint.getTracedEdge().getTwin().setNext(oldLeftBreakpoint.getTracedEdge().getTwin());

		checkForCircleEvent(leftNeighbor);
		checkForCircleEvent(rightNeighbor);
	}

	private void checkForCircleEvent(ArcSegment arcSegment)
	{
		/* If the arc has no left or right neighbors (i.e. if it is on the end of the beach line), there can be no
		circle event */
		if (arcSegment.getLeftBreakpoint() == null || arcSegment.getRightBreakpoint() == null) return;

		Point p1 = arcSegment.getLeftBreakpoint().getLeftArcSegment();
		Point p2 = arcSegment.getSite();
		Point p3 = arcSegment.getRightBreakpoint().getRightArcSegment();

		/* If the points make a clockwise turn, we have a circle */
		if (MathOps.crossProduct(p1, p2, p3) < 0)
		{
			Circle circle = MathOps.circle(p1, p2, p3);
			CircleEvent circleEvent = new CircleEvent(circle, arcSegment);

			/* Add the circle event to the queue and update the pointer in the status tree */
			queue.add(circleEvent);
			status.put(arcSegment, circleEvent);
		}
	}

	// TODO Handle case where edge intersects a corner of the bounding box
	private void connectInfiniteEdges()
	{
		/* If there are exactly four vertices, they are the corners of the bounding box and there are no Voronoi vertices. */
		boolean noVertices = vertices.size() == 4;

		/* If this is true, then there is only one face and thus, only one site point. */
		if (noVertices && faces.size() == 2)
		{
			DCELFace face = faces.get(0).isUnbounded() ? faces.get(1) : faces.get(0);
			getBoundingBox().getInnerEdge().setIncidentFace(face);
			face.setOuterComponent(getBoundingBox().getInnerEdge());
			return;
		}

		for (Breakpoint breakpoint : breakpoints)
		{
			DCELEdge edge = breakpoint.getTracedEdge();
			Point origin;

			if (noVertices)
			{
				if (edge.getOrigin() != null && edge.getTwin().getOrigin() != null) continue;

				origin = MathOps.midpoint(breakpoint.getLeftArcSegment(), breakpoint.getRightArcSegment());

				Point intersection1 = getBoundingBox().getIntersection(origin, edge.getDirection());
				Point intersection2 = getBoundingBox().getIntersection(origin, edge.getTwin().getDirection());

				DCELVertex vertex1 = new DCELVertex(DCELVertex.VertexType.BOUNDING_VERTEX, intersection1, edge.getTwin());
				DCELVertex vertex2 = new DCELVertex(DCELVertex.VertexType.BOUNDING_VERTEX, intersection2, edge);
				vertices.add(vertex1);
				vertices.add(vertex2);

				edge.setOrigin(vertex2);
				edge.getTwin().setOrigin(vertex1);

				DCELEdge outerBoundingEdge1 = getBoundingBox().getIntersectedEdge(intersection1);
				DCELEdge innerBoundingEdge1 = outerBoundingEdge1.getTwin();
				DCELEdge newOuterBoundingEdge1 = new DCELEdge(DCELEdge.EdgeType.BOUNDING_EDGE, innerBoundingEdge1);
				DCELEdge newInnerBoundingEdge1 = new DCELEdge(DCELEdge.EdgeType.BOUNDING_EDGE, outerBoundingEdge1);

				DCELEdge outerBoundingEdge2 = getBoundingBox().getIntersectedEdge(intersection2);
				DCELEdge innerBoundingEdge2 = outerBoundingEdge2.getTwin();
				DCELEdge newOuterBoundingEdge2 = new DCELEdge(DCELEdge.EdgeType.BOUNDING_EDGE, innerBoundingEdge2);
				DCELEdge newInnerBoundingEdge2 = new DCELEdge(DCELEdge.EdgeType.BOUNDING_EDGE, outerBoundingEdge2);

				edges.add(newOuterBoundingEdge1);
				edges.add(newInnerBoundingEdge1);
				edges.add(newOuterBoundingEdge2);
				edges.add(newInnerBoundingEdge2);

				newOuterBoundingEdge1.setOrigin(vertex1);
				newInnerBoundingEdge1.setOrigin(vertex1);
				newOuterBoundingEdge2.setOrigin(vertex2);
				newInnerBoundingEdge2.setOrigin(vertex2);

				newOuterBoundingEdge1.setIncidentFace(outerBoundingEdge1.getIncidentFace());
				newOuterBoundingEdge2.setIncidentFace(outerBoundingEdge2.getIncidentFace());

				newOuterBoundingEdge1.setNext(outerBoundingEdge1.getNext());
				outerBoundingEdge1.setNext(newOuterBoundingEdge1);
				newInnerBoundingEdge1.setNext(innerBoundingEdge1.getNext());
				innerBoundingEdge1.setNext(edge.getTwin());
				edge.setNext(newInnerBoundingEdge1);

				newOuterBoundingEdge2.setNext(outerBoundingEdge2.getNext());
				outerBoundingEdge2.setNext(newOuterBoundingEdge2);
				newInnerBoundingEdge2.setNext(innerBoundingEdge2.getNext());
				innerBoundingEdge2.setNext(edge);
				edge.getTwin().setNext(newInnerBoundingEdge2);

				newOuterBoundingEdge1.setPrev(outerBoundingEdge1);
				newOuterBoundingEdge1.getNext().setPrev(newOuterBoundingEdge1);
				newInnerBoundingEdge1.setPrev(edge);
				newInnerBoundingEdge1.getNext().setPrev(newInnerBoundingEdge1);
				edge.getTwin().setPrev(innerBoundingEdge1);

				newOuterBoundingEdge2.setPrev(outerBoundingEdge2);
				newOuterBoundingEdge2.getNext().setPrev(newOuterBoundingEdge2);
				newInnerBoundingEdge2.setPrev(edge.getTwin());
				newInnerBoundingEdge2.getNext().setPrev(newInnerBoundingEdge2);
				edge.setPrev(innerBoundingEdge2);
			}
			else
			{
				if (edge.getOrigin() != null)
					origin = edge.getOrigin().getCoordinates();
				else
				{
					// TODO Issue with more than three cocircular points where one pair of edges has no origin points
					edge = breakpoint.getTracedEdge().getTwin();
					origin = edge.getOrigin().getCoordinates();
				}

				getBoundingBox().connectEdge(this, origin, edge);
			}
		}
	}

	private void calculateDirections(DCELEdge edge1, DCELEdge edge2, Point p1, Point p2)
	{
		double vy = -(p1.getX() - p2.getX());
		double vx = p1.getY() - p2.getY();

		edge1.setDirection(new double[]{vx, vy});
		edge2.setDirection(new double[]{-vx, -vy});
	}

	@Override
	protected void computeFaces()
	{
		for (DCELEdge edge : edges)
		{
			if (edge.getIncidentFace() != null && !edge.getIncidentFace().isUnbounded())
			{
				DCELEdge e = edge.getNext();
				DCELFace face = edge.getIncidentFace();

				while (e != edge)
				{
					e.setIncidentFace(face);
					e = e.getNext();
				}
			}
		}
	}
}
