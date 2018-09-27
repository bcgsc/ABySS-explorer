package ca.bcgsc.abyssexplorer.visualization.decorators;

import java.awt.Shape;

import ca.bcgsc.abyssexplorer.graph.Edge;
import ca.bcgsc.abyssexplorer.graph.Vertex;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.EdgeIndexFunction;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.decorators.AbstractEdgeShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.EdgeShape.IndexedRendering;
import edu.uci.ics.jung.visualization.decorators.EdgeShape.Loop;

/**
 * Based on Tom Nelson's edu.uci.ics.jung.visualization.decorators.EdgeShape.
 * Customized to handle ABySS-Explorer squiggle edges.
 * 
 * @author Cydney Nielsen
 * 
 */
public class AbyssEdgeShape<V,E> {

	/** a convenience instance for other edge shapes to use for self-loop edges where parallel instances will not overlay each other. */
	protected static Loop loop = new Loop();
	      
	protected static int lenScale = 200;

	/**
	 * An edge shape that renders as a QuadCurve between vertex endpoints.
	 */
	public static class Squiggle<V, E> extends
			AbstractEdgeShapeTransformer<V, E> implements
			IndexedRendering<V, E> {

		/** singleton instance of the QuadCurve shape. */
		// private static QuadCurve2D instance = new QuadCurve2D.Float();

		private SquiggleGenerator sg = new SquiggleGenerator();

		/** The parallel edge index function. */
		protected EdgeIndexFunction<V, E> parallelEdgeIndexFunction;

                /** The shape for Squiggles that are one cycle long*/
                protected EdgeShape.QuadCurve<V,E> quad = new EdgeShape.QuadCurve<V,E>();


		@SuppressWarnings("unchecked")
		public void setEdgeIndexFunction(
				EdgeIndexFunction<V, E> parallelEdgeIndexFunction) {
			this.parallelEdgeIndexFunction = parallelEdgeIndexFunction;
			loop.setEdgeIndexFunction(parallelEdgeIndexFunction);
                        quad.setEdgeIndexFunction(parallelEdgeIndexFunction);
		}
		
		public void setLenScale(int s) {
			lenScale = s;
		}

		/**
		 * Gets the edge index function.
		 * 
		 * @return the parallelEdgeIndexFunction
		 */
		public EdgeIndexFunction<V, E> getEdgeIndexFunction() {
			return parallelEdgeIndexFunction;
		}

		/**
		 * Get the shape for this edge, returning either the shared instance or,
		 * in the case of self-loop edges, the Loop shared instance.
		 * 
		 * @param context the context
		 * 
		 * @return the shape
		 */
		@SuppressWarnings("unchecked")
		public Shape transform(Context<Graph<V, E>, E> context) {

			// SimpleGraph graph = (SimpleGraph) context.graph;
			Graph graph = context.graph;
			E e = context.element;
			Pair<V> endpoints = graph.getEndpoints(e);
			if (endpoints != null) {
				boolean isLoop = endpoints.getFirst().equals(endpoints.getSecond());
				if (isLoop) {
//                                    if(endpoints.getFirst() instanceof Vertex && e instanceof Edge){
//                                        Vertex v1 = (Vertex)endpoints.getFirst();
//                                        Edge e1 = (Edge) e;
//
//                                        if(v1.getPole(e1, "in") != v1.getPole(e1, "out")){
//                                            // a palindromic contig
//
//
//                                        }
//                                    }


					return loop.transform(context);
				}
			}

//			int index = 1;
//			if (parallelEdgeIndexFunction != null) {
//				index = parallelEdgeIndexFunction.getIndex(graph, e);
//			}
//
//			float controlY = control_offset_increment
//					+ control_offset_increment * index;
			// instance.setCurve(0.0f, 0.0f, 0.5f, controlY, 1.0f, 0.0f);
			// return instance;

			// AbyssExplorer ae = AbyssExplorer.getAbyssExplorer();
			
			int n = 0;
                        if(e instanceof Edge){
                            int len = ((Edge) e).getLen();
                            if (len != 0) {
                                    Double d = (double) len / lenScale;
                                    n = d.intValue();
                                    if (n == 0) {
                                            n = 1;
                                    }

                                    if(n == 1){ // squiggle is only one cycle long
                                        return quad.transform(context);
                                    }
                            }
                        }
			return sg.buildSquiggle(0.0f, 0.0f, 1.0f, 0.0f, 40.0f, n);
		}
	}
}
