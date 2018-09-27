package ca.bcgsc.abyssexplorer.visualization.decorators;

import java.awt.geom.GeneralPath;

/**
 * Computes the squiggle path for ABySS-Explorer edges.
 * 
 * @author Cydney Nielsen
 * 
 */
public class SquiggleGenerator {
	
	/**
	 * Builds the squiggle.
	 * 
	 * @param x1 the x1
	 * @param y1 the y1
	 * @param x2 the x2
	 * @param y2 the y2
	 * @param amp the amp
	 * @param n the n
	 * 
	 * @return the general path
	 */
	public GeneralPath buildSquiggle(float x1, float y1, 
			float x2, float y2, float amp, int n) {
		
		GeneralPath squiggle = new GeneralPath();

		// set the starting coordinate
		squiggle.moveTo(x1, y1);
		
		
		if(n==0){
			
			squiggle.lineTo(x2, y2);
			return squiggle;
		}

		// figure out the dimensions of the internal curves
		float d = Math.abs(x2-x1);
		float T = 2*d;
		float curveWidth = d/n;
		float curveHeightStep = Math.abs(y2-y1)/n;
		
		// add interal curves
		float start_x = x1;
		float start_y = y1;
		for (int i = 0; i < n; i++) {			
			float mid_x = start_x + (curveWidth/2);
			// determine the amplitude for this curve
			Double radians = null;
			if (mid_x <= T/6) {
				radians = (mid_x/T)*2*Math.PI;
				radians += (Math.PI/T) * mid_x;
			} else if (mid_x <= T/2) {
				radians = (mid_x/T)*2*Math.PI;
				radians += -1.0 * (Math.PI/(2*T))*mid_x + Math.PI/4;
			} else {
				System.out.println("Unexpected value of mid_x " + mid_x);
				System.exit(0);
			}
			Double normAmp = amp * Math.sin(radians);
			float mid_y = start_y + (curveHeightStep/2) - normAmp.floatValue();
			float end_x = start_x + curveWidth;
			if (i%2 == 0) {
				mid_y = start_y + (curveHeightStep/2) + normAmp.floatValue();
			}
			float end_y = start_y + curveHeightStep;
			squiggle.quadTo(mid_x, mid_y, end_x, end_y);
			start_x += curveWidth;
			start_y += curveHeightStep;
		}
		
		return squiggle;
	}
}
