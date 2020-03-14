package voronoi.dcel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Willem Paul
 */
public class DCELFace
{
	private String name;
	private DCELEdge outerComponent;
	private List<DCELEdge> innerComponents;

	public DCELFace(int index, DCELEdge outerComponent, DCELEdge... innerComponents)
	{
		this.name = outerComponent == null ? "uf" : "c" + index;
		this.outerComponent = outerComponent;
		this.innerComponents = new ArrayList<>();
		Collections.addAll(this.innerComponents, innerComponents);
	}

	public String getName()
	{
		return name;
	}

	public DCELEdge getOuterComponent()
	{
		return outerComponent;
	}

	public void setOuterComponent(DCELEdge outerComponent)
	{
		this.outerComponent = outerComponent;
	}

	public List<DCELEdge> getInnerComponents()
	{
		return innerComponents;
	}

	public void setInnerComponents(List<DCELEdge> innerComponents)
	{
		this.innerComponents = innerComponents;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append(name).append("  ");
		builder.append(outerComponent.getName()).append("  ");

		if (innerComponents.isEmpty()) builder.append("nil");
		else
		{
			builder.append('[');
			for (DCELEdge e : innerComponents)
			{
				builder.append(e.toString()).append("; ");
			}
			builder.append(']');
		}

		return builder.toString();
	}
}