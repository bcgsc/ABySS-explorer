package ca.bcgsc.abyssexplorer.writers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.bcgsc.abyssexplorer.graph.AbyssGraph2;
import ca.bcgsc.abyssexplorer.graph.DistanceEstimate;
import ca.bcgsc.abyssexplorer.graph.Edge;
import ca.bcgsc.abyssexplorer.graph.Vertex;

/**
 * Custom file writer for an AbyssGraph into DOT output.
 * 
 * WARNING: VERSION NOT YET STABLE 
 * 
 * @author Cydney Nielsen
 * 
 */
public class DotWriter {
	
	private static Charset charset = Charset.forName("ISO-8859-15");
	private static CharsetEncoder encoder = charset.newEncoder();
	
	public void write(String fn, AbyssGraph2 g) {

		try {
			FileOutputStream fos = new FileOutputStream(new File(fn));
			FileChannel oFc = fos.getChannel();
			writeAdjGraph(oFc, g);			
			oFc.close();
		} catch (IOException e) {
			System.err.println("ClusterWriter:writeTree Error\n" + e);
			System.exit(1);
		}
	}
	
	private void writeAdjGraph(FileChannel oFc, AbyssGraph2 g) throws IOException{

		String text = "digraph " + g.getName() + " {\n";//\nmode=\"adj\";\n";
		oFc.write(encoder.encode(CharBuffer.wrap(text)));
		for (int i=0; i<g.getEdgeCount(); i++) {
			Edge edge = g.getEdge(i);
			// write in order "+" then "-"
                        if(edge != null){
                            String eLabel = edge.getLabel();
                            if (eLabel.endsWith("+")) {
                                    // "+" as source
                                    writeEdgeConnections(oFc, edge);
                                    // "-" as source
                                    edge.reverseComplement();
                                    writeEdgeConnections(oFc, edge);
                            } else {
                                    // "+" as source
                                    edge.reverseComplement();
                                    writeEdgeConnections(oFc, edge);
                                    // "-" as source
                                    edge.reverseComplement();
                                    writeEdgeConnections(oFc, edge);
                            }
                        }
		}
		oFc.write(encoder.encode(CharBuffer.wrap("}\n")));
	}
	
	private void writeEdgeConnections(FileChannel oFc, Edge edge) throws CharacterCodingException, IOException {
		
		// write all contig connections using this edge as a source
		String eLabel = edge.getLabel();
		Vertex v2 = edge.getVertices()[1];
		int ePole = v2.getIncomingPole(edge);
		String text = "";

                if(edge.getLen() != null && edge.getCoverage() != null){
                    text += "\"" + eLabel + "\" [l=" + edge.getLen() + " c=" + edge.getCoverage() + "]\n";
                }

		// handle straight-forward cases
		List<Edge> outgoing = v2.getOutgoing();
		//text += "\"" + eLabel + "\"";
		List<String> labels = new ArrayList<String>();
		for (Edge oEdge: outgoing) {
			// only report correct pole connections
			if (ePole == v2.getOutgoingPole(oEdge)) {
				String oLabel = oEdge.getLabel();
				labels.add(oLabel);
			}
		}
		// handle cases not currently in correct orientation
		List<Edge> incoming = v2.getIncoming();
		for (Edge iEdge: incoming) {
			// only report correct pole connections, correcting for current orientation
			byte iPole = v2.getIncomingPole(iEdge);
			if (!v2.isPalindrome()) {
				iPole = v2.getFlippedPole(iPole);
			}
			if (ePole == iPole) {
				String iLabel = iEdge.getLabel();
				if (eLabel.equals(iLabel)) { continue; } // skip self case (source is incoming)
				labels.add(iEdge.getRevCompLabel());
			}
		}
		// finally, check if v2 is a palindrome
		if (v2.isPalindrome()) {
			// source edge can connect to its reverse complement
			labels.add(edge.getRevCompLabel());
		}
                
		if (labels.size() > 0) {
//			text += "\n";
//		} else {
                    text += "\"" + eLabel + "\"";
			Collections.sort(labels);
			text += " -> {";
			for (String l: labels) {
				text += " \"" + l + "\"";
			}
			text += " }\n";
		}
		// one line per source edge



                List<DistanceEstimate> opde = edge.getOutboundPartners();
                if(opde != null){
                    for(DistanceEstimate de : opde){
                        text += "\"" + eLabel + "\" -> \"" + de.getLabel() + "\" [d=" + de.getDist().toString() + " e=" + de.getError().toString() + " n=" + de.getNumPairs() + "]\n";
                    }
                }

//                List<DistanceEstimate> ipde = edge.getInboundPartners();
//                if(ipde != null){
//                    for(DistanceEstimate de : ipde){
//                        text += "\"" + eLabel + "\" -> \"" + de.getLabel() + "\" [d=" + de.getDist().toString() + " e=" + de.getError().toString() + " n=" + de.getNumPairs() + "]\n";
//                    }
//                }

                oFc.write(encoder.encode(CharBuffer.wrap(text)));
	}
	
}

