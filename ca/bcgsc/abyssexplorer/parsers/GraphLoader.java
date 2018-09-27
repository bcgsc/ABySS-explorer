package ca.bcgsc.abyssexplorer.parsers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeListener;

import ca.bcgsc.abyssexplorer.graph.AbyssGraph2;
import ca.bcgsc.abyssexplorer.graph.DistanceEstimate;
import ca.bcgsc.abyssexplorer.graph.Edge;
import ca.bcgsc.abyssexplorer.graph.Vertex;
import ca.bcgsc.dive.dive.Dive;
import ca.bcgsc.dive.util.FileNameRegexFilter;
import edu.uci.ics.jung.visualization.util.ChangeEventSupport;
import edu.uci.ics.jung.visualization.util.DefaultChangeEventSupport;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * This class contains all of the methods for loading parsed
 * data values into the graph structure. 
 * 
 * Loads relations between a source edge (sE) and a target
 * edge (tE). Vertices for the sE are assigned ids v1 and v2,
 * and vertices for the tE are v2 and v3, so the connection
 * is captured by:
 * 
 * v1 -[sE]-> v2 -[tE]-> v3
 * 
 * @author Cydney Nielsen
 *
 */
public class GraphLoader implements ChangeEventSupport {
	
	protected ChangeEventSupport changeSupport = new DefaultChangeEventSupport(this);

	// private static final Runtime s_runtime = Runtime.getRuntime ();
	
	// input
	protected File fPath; // user selected path/file
	protected GraphParser parser;
	
	// internal
	protected File adjFile;
	protected String adjFileType; // one of "abyss", "dot"
	protected List<File> aFiles; // additional files to parse
	
	// data structure to populate
	protected AbyssGraph2 g;
	protected String graphName; // should be contained in g
	
	// to generate new vertex ids
	private List<Integer> nextVid;
	private int vCounter;
	
//	// to keep track of total num of distance estimates
//	private int numDE = 0;
//	// to keep track of total num of paired-end contigs
//	private int numPE = 0;
	
	// to monitor parse status
	private String status;
        
        private int longestContigId = -1;
        private int shortestContigId = -1;
	private int k = -1;
        private boolean paired_end = false;
        
        private static Pattern pathExtension = Pattern.compile("\\s*.*-6\\.path(?:\\d+)?\\s*");

        public void clear(){
	    fPath = null;
	    parser = null;
	    adjFile = null;
	    adjFileType = null;
	    aFiles = null;
	    g = null;
	    graphName = null;
	    nextVid = null;
	    vCounter = 0;
	    status = null;        
            longestContigId = -1;
            shortestContigId = -1;
	    k = -1;
            paired_end = false;
        }

        public void setK(int k){
            this.k = k;
        }

	public String getStatus() {
		return status;
	}
	
	public AbyssGraph2 getParsedGraph() {
		return g;
	}
	
        public boolean isPairedEndAssembly(){
            return paired_end; //g.isPairedEndAssembly();
        }

        public String getAdjFileType(){
            return adjFileType;
        }

        public void load(File f)  throws IOException, InterruptedException{
            load(f, Dive.CONTIGS);
        }
        
	/**
	 * Specify input as one of:
	 * 1. A directory containing ABySS output files
	 * 2. An ABySS adjacency file
	 * 3. A DOT file (must contain an 'adj' graph)
	 * 
	 * @param f
	 * @param p
	 * @throws IOException
	 */
	public void load(File f, int assemblyMode) throws IOException, InterruptedException {
		// long m = s_runtime.totalMemory () - s_runtime.freeMemory ();
		// System.out.println("initializing: memory usage = " + m);
		fPath = f;
		vCounter = 0;
		nextVid = new LinkedList<Integer>();
		parserSetup(assemblyMode);
		if(parser instanceof AbyssParser){
                    ((AbyssParser) parser).setK(k);
                }

		status = "estimating graph size...";
		fireStateChanged();
		g = parser.initializeGraph();
                g.setPairedEndAssembly(paired_end);
		// m = s_runtime.totalMemory () - s_runtime.freeMemory ();
		// System.out.println("initialized graph: memory usage = " + m);
//		if(parser instanceof AbyssParser){
//                    largestContigId = ((AbyssParser) parser).getLargestContigId();
//                    smallestContigId = ((AbyssParser) parser).getSmallestContigId();
//                }
//                else if(parser instanceof DotParser){
//                    largestContigId = ((DotParser) parser).getLargestContigId();
//                    smallestContigId = ((DotParser) parser).getSmallestContigId();
//                }
		status = "parsing adjacency graph...";
		fireStateChanged();
		handleAdjData();
		// m = s_runtime.totalMemory () - s_runtime.freeMemory ();
		// System.out.println("parsed adj file: memory usage = " + m);

                if(f.isDirectory()){
                    status = "parsing additional data graphs...";
                    fireStateChanged();
                    handleAdditionalData();
                }
                
		// m = s_runtime.totalMemory () - s_runtime.freeMemory ();
		// System.out.println("num unique dist estimates: " + numDE);
		// System.out.println("num paired-end contigs: " + numPE);
		// System.out.println("handled additional files: memory usage = " + m);
		 
		status = "complete";
		fireStateChanged();
		
		// m = s_runtime.totalMemory () - s_runtime.freeMemory ();
		// System.out.println("done: memory usage = " + m);
	}
	
        public int getIdOfLongestContig(){
            return longestContigId;
        }

        public int getIdOfShortestContig(){
            return shortestContigId;
        }

	/**
	 * Uses input file names to determine what parser to use.
	 * Opens the adjaceny file and stores the names of any 
	 * extra graph files to parse. 
	 * 
	 * @throws IOException
	 */
	protected void parserSetup(int assemblyMode) throws IOException {
		aFiles = new ArrayList<File>();
		String fPathName = fPath.getAbsolutePath();
		boolean foundAdj = false;
		if (fPath.isFile()) {
                    paired_end = false;
			if (fPathName.endsWith(".adj")) {
				parser = new AbyssParser();
				adjFile = fPath;
				adjFileType = "abyss";
				foundAdj = true;
			} else if (fPathName.endsWith(".dot")) {
				parser = new DotParser();
				adjFile = fPath;
				adjFileType = "dot";
				foundAdj = true; // not guaranteed to be 'adj' at this point
			}

//                        // look for additional files if appropriate
//                        if (foundAdj && !fPath.getName().endsWith(".dot")){
//                                for (File fn: fPath.getParentFile().listFiles()) {
//                                        String fName = fn.getName();
//                                        if (fName.endsWith(".dist")) {
//                                                // a valid ABySS graph file to parse
//                                                aFiles.add(fn);
//                                        } else if (fName.endsWith("-contigs.fa")) {
//                                                // a valid ABySS graph file to parse
//                                                aFiles.add(fn);
//                                        }
//                                }
//                        }
		} else if (fPath.isDirectory()) {
                    String fName = null;
                    switch(assemblyMode){
                        case Dive.SCAFFOLDS:
                            for (File fn: fPath.listFiles()) {
                                fName = fn.getName();
                                if (fName.endsWith("-scaffolds.dot") || fName.endsWith("-8.dot")) {
                                        parser = new DotParser();
                                        adjFile = fn;
                                        adjFileType = "dot";
                                        foundAdj = true;
                                        System.out.println("Adjacency based on: " + fName);
                                        break;
                                }
                            }
                            for (File fn: fPath.listFiles()) {
                                fName = fn.getName();
                                if(pathExtension.matcher(fName).matches()){
                                    aFiles.add(fn);
                                }
                            }
                            paired_end = true;
                            break;
                        case Dive.CONTIGS:
                            for (File fn: fPath.listFiles()) {
                                fName = fn.getName();
                                if (fName.endsWith("-contigs.dot") || fName.endsWith("-6.dot")) {
                                        parser = new DotParser();
                                        adjFile = fn;
                                        adjFileType = "dot";
                                        foundAdj = true;
                                        System.out.println("Adjacency based on: " + fName);
                                        break;
                                }
                            }
                            for (File fn: fPath.listFiles()) {
                                fName = fn.getName();                            
                                if (fName.endsWith("-5.path") || fName.endsWith("-6.dist.dot")){
                                    aFiles.add(fn);
                                }
                            }
                            paired_end = true;
                            break;
                        case Dive.UNITIGS:
                            // first look for an ABySS file
                            File[] adjFiles = fPath.listFiles(new FileNameRegexFilter(".*\\.adj"));
                            File[] dotFiles = fPath.listFiles(new FileNameRegexFilter(".*\\.dot"));

                            int count = 5;
                            while(count>0 && !foundAdj){
                                String countStr = new Integer(count).toString();
                                    for (File fn: dotFiles) {
                                        fName = fn.getName();
                                        if (fName.endsWith("-" + countStr + ".dot")) {
                                                parser = new DotParser();
                                                adjFile = fn;
                                                adjFileType = "dot";
                                                foundAdj = true;
                                                break;
                                        }
                                    }
                                if(!foundAdj){
                                    for (File fn: adjFiles) {
                                            fName = fn.getName();
                                            if (fName.endsWith("-"+ countStr + ".adj")) {
                                                    parser = new AbyssParser();
                                                    adjFile = fn;
                                                    adjFileType = "abyss";
                                                    foundAdj = true;
                                                    break;
                                            }
                                    }
                                }                                
                                count--;
                            }
                            if(!foundAdj){
                                for (File fn: dotFiles) {
                                        fName = fn.getName();
                                        if (fName.endsWith(".dot")) {
                                                parser = new DotParser();
                                                adjFile = fn;
                                                adjFileType = "dot";
                                                foundAdj = true;
                                                break;
                                        }
                                }
                                if(!foundAdj){
                                    for (File fn: adjFiles) {
                                            fName = fn.getName();
                                            if (fName.endsWith(".adj")) {
                                                    parser = new AbyssParser();
                                                    adjFile = fn;
                                                    adjFileType = "abyss";
                                                    foundAdj = true;
                                                    break;
                                            }
                                    }
                                }
                            }
                            
                            if (foundAdj){//&& adjFileType.equals("abyss")) {
                                for (File fn: fPath.listFiles()) {
                                    fName = fn.getName();
                                    if (!adjFileType.equals("dot") && fName.endsWith("-3.dist")) {
                                            // a valid ABySS graph file to parse
                                            aFiles.add(fn);
//                                        } else if (fName.endsWith("-contigs.fa")) {
//                                                // a valid ABySS graph file to parse
//                                                aFiles.add(fn);
                                    }
                                }
                            }
                            
                            for (File fn: fPath.listFiles()) {
                                fName = fn.getName();                            
                                if (fName.endsWith("-5.path")){
                                    aFiles.add(fn);
                                }
                            }

                            if(adjFile != null){
                                System.out.println("Adjacency based on: " + adjFile.getName());
                            }
                            paired_end = false;
                            break;
                        }
		}

		if (!foundAdj) {
                    throw new IOException("Cannot find adjacency graph in " + fPathName);
		}
		parser.open(adjFile);
	}
	
	public void handleAdjData() throws IOException, InterruptedException {
		parser.open(adjFile);
		boolean parsed = parseAdjGraph();
		parser.close();
		if (!parsed) {
			status = "Unrecognized file format";
			fireStateChanged();
			throw new IOException("Unable to parse file " + adjFile.getName());
		}
	}
	
	public void handleAdditionalData() throws IOException, InterruptedException {

		if (!aFiles.isEmpty()) {
			for (File f: aFiles) {
				String fName = f.getName();//f.getAbsolutePath();
				if (fName.endsWith(".dist")) {
//                                    String adjname = adjFile.getName();
//                                    String properprefix = adjname.substring(0, adjname.lastIndexOf('-'));                                    
                                    
//					if (adjFileType.equals("abyss")){// && fName.startsWith(properprefix)) {
                                            //System.out.println(fName + ":" + properprefix);
                                            
						// only parse if adj file was in ABySS format
                                                if(parser instanceof DotParser){
                                                    parser = new AbyssParser();
                                                }
						parser.open(f);
                                                System.out.println(fName);
						parseDistGraph();
						parser.close();
//					}
				} else if (fName.endsWith(".path") || fName.contains(".path")) { // don't read from "-contigs.fa" any more
//					if (adjFileType.equals("abyss") ||
//                                                (adjFileType.equals("dot") && (adjFile.getName().endsWith("-contigs.dot") 
//                                                                               || adjFile.getName().endsWith("-6.dot")))) {
						
                                                if(parser instanceof DotParser){
                                                    parser = new AbyssParser();
                                                }
						parser.open(f);
                                                System.out.println(fName);
						parsePairedEndGraph();
						parser.close();
//					}
				} else if (fName.endsWith(".dist.dot")){
                                                if(parser instanceof AbyssParser){
                                                    parser = new DotParser();
                                                }
						parser.open(f);
                                                System.out.println(fName);
						parseDistGraph();
						parser.close();                                    
                                }
			}
		}
//                else {
//			// check for merged dot file
//			if (adjFileType.equals("dot")) {
//				parser.open(adjFile);
//				parseDistGraph();
//				parser.close();
//			}
//
//		}
	
	}
	
	public boolean parseAdjGraph() throws IOException, InterruptedException {
		boolean parsed = false;
		AdjacentContigs aContigs;
                PairedEndContig pe;
                Integer globalEdgeDistance = null;
                ArrayList<Integer> distances = new ArrayList<Integer>();

                int count = 0;
                int lines_read = 0;
                
                shortestContigId = -1;
                longestContigId = -1;
                int maxContigLength = Integer.MIN_VALUE;
                int minContigLength = Integer.MAX_VALUE;
                
		while (true) {
                        if(Thread.interrupted()){
                            throw new java.lang.InterruptedException();
                        }
                    
			Object o = parser.parseNextLine();
			if (o == null) { break; } // end of file
                        if(++count >= 10000){
                            lines_read += count;
                            count = 0;
                            status = "parsing adjacency graph...parsed " + lines_read + " lines";                            
                            fireStateChanged();
                            // This is done to give the user a sense of progress.
                        }
                        
                        if(o instanceof AdjacentContigs[]){
                            for(AdjacentContigs a : (AdjacentContigs[]) o){
                                storeAdjacentContigs(a, distances, globalEdgeDistance);
                                
                                Integer ctg_len = a.len;
                                if(ctg_len != null){
                                    if(ctg_len > maxContigLength){
                                        maxContigLength = ctg_len;
                                        longestContigId = a.id;
                                    }
                                    else if (ctg_len < minContigLength){
                                        minContigLength = ctg_len;
                                        shortestContigId = a.id;
                                    }
                                }
                            }
                            parsed = true;
                        }
                        else if (o instanceof AdjacentContigs) {
				aContigs = (AdjacentContigs) o;
				storeAdjacentContigs(aContigs, distances, globalEdgeDistance);
                                
//                                if(aContigs.id == 363645){
//                                    System.out.println("363645");
//                                }
                                
                                Integer ctg_len = aContigs.len;
                                if(ctg_len != null){
                                    if(ctg_len > maxContigLength){
                                        maxContigLength = ctg_len;
                                        longestContigId = aContigs.id;
                                    }
                                    else if (ctg_len < minContigLength){
                                        minContigLength = ctg_len;
                                        shortestContigId = aContigs.id;
                                    }
                                }
                                
				parsed = true;
			}
                        else if (o instanceof PairedEndContig) {
				pe = (PairedEndContig) o;
				if (pe.getNumMembers() > 0) {
                                    try{
					g.setPairedEndContig(pe.getId(), pe.getMembers());
//					numPE += 1;
                                    }
                                    catch(IllegalArgumentException e){
                                        g.removeEdge(g.getEdge(pe.getId()));
                                    }                                         
				}
			}
                        else if (o instanceof PairedEndPartners){
                            setDisEstimate((PairedEndPartners)o);
                        }
                        else if (o instanceof Integer) {
                            globalEdgeDistance = (Integer) o;
                            
                        }
		}

                System.out.println(g.getEdgeCount() + " edges, " + g.getVertexCount() + " vertices");
                
                if(g.isPairedEndAssembly()){
                    //calculate threshold distance for overlaps                    
                    int size = distances.size();
                    if(size > 0){
                        status = "calculating threshold distance for overlaps...";
                        fireStateChanged();
                        
                        Collections.sort(distances);
                        int q1Index = (int)Math.floor(size*0.25);
                        int q3Index = (int)Math.floor(size*0.75);
                        Integer q1 = distances.get(q1Index);
                        Integer q3 = distances.get(q3Index);

                        Integer lowerBound = (int)Math.rint(q1 - 3 * (q3 - q1));

                        //System.out.println(lowerBound + " " + q1 + " " + q3);

                        System.out.println("threshold distance for overlaps: " + lowerBound);
                        
                        status = "Classifying overlaps...";
                        fireStateChanged();
                        g.parsedActions(lowerBound);
                        status = "Classifying overlaps...done";
                        fireStateChanged();
                    }
                }

		return parsed;
	}


        private void storeAdjacentContigs(AdjacentContigs aContigs, ArrayList<Integer> distances, Integer globalEdgeDistance) throws IOException{
            // get/create the source edge
            String sContigLabel = aContigs.getLabel();
            Edge sEdge = g.getEdge(sContigLabel); // ensures correct orientation
            boolean alreadyExist = true;
            if (sEdge == null) {
                    sEdge = new Edge(sContigLabel);
                    alreadyExist = false;
                    g.addEdge(sEdge);
            }
            Float cov = aContigs.getCov();
            if(cov != null){
                sEdge.setCoverage(cov);
            }
            Integer len = aContigs.getLen();
            if(len != null){
                sEdge.setLen(len);
            }

            // add all in/out bound edges
            if (aContigs.hasInbound()) {
                    // correct orientation
                    sEdge.reverseComplement();

                    for (String iLabel: aContigs.getInbound()) {
                        String label = flipStrand(iLabel);
                            setConnections(sEdge, label);

                            if(aContigs.getDistance() != null){
                                Vertex dv = sEdge.getDestVertex();
                                dv.addDistance(Integer.toString(sEdge.getId()), label.replace("+", "").replace("-", ""), aContigs.getDistance());
                                distances.add(aContigs.getDistance());
                            }
                            else if(globalEdgeDistance != null){
                                Vertex dv = sEdge.getDestVertex();
                                dv.addDistance(Integer.toString(sEdge.getId()), label.replace("+", "").replace("-", ""), globalEdgeDistance);
                                distances.add(globalEdgeDistance);
                            }
                    }

                    // switch orientation back
                    sEdge.reverseComplement();
            }
            if (aContigs.hasOutbound()) {
                    for (String oLabel: aContigs.getOutbound()) {
                            setConnections(sEdge, oLabel);

                            if(aContigs.getDistance() != null){
                                Vertex dv = sEdge.getDestVertex();
                                dv.addDistance(Integer.toString(sEdge.getId()), oLabel.replace("+", "").replace("-", ""), aContigs.getDistance());
                                distances.add(aContigs.getDistance());
                            }
                            else if(globalEdgeDistance != null){
                                Vertex dv = sEdge.getDestVertex();
                                dv.addDistance(Integer.toString(sEdge.getId()), oLabel.replace("+", "").replace("-", ""), globalEdgeDistance);
                                distances.add(globalEdgeDistance);
                            }
                    }
            }
            if (!aContigs.hasInbound() && !aContigs.hasOutbound() && !alreadyExist) {
                    storeSingleton(sEdge);
            }
        }
	
	public boolean parseDistGraph() throws IOException, InterruptedException {
		System.out.println("parsing dist file...");
		boolean parsed = false;
		while (true) {
			Object o = parser.parseNextLine();
			if (o == null) { break; } // end of file
			if (o instanceof List) {
				for (PairedEndPartners d: ((List<PairedEndPartners>) o)) {
                                    try{
					setDisEstimate(d);
                                    }
                                    catch(IllegalArgumentException e){
                                        System.out.println("Cannot find edge " + d.getSourceLabel());
                                    }
					//TODO: catch exception when these edges do not yet exist in the graph
				}
			}
                        else if (o instanceof PairedEndPartners){
                            setDisEstimate((PairedEndPartners)o);
                        }
			parsed = true;
		}
		return parsed;
	}
	
	public boolean parsePairedEndGraph() throws IOException, IllegalArgumentException, InterruptedException {
		System.out.println("parsing path file...");
		boolean parsed = false;
		PairedEndContig pe;
		while (true) {
			Object o = parser.parseNextLine();
			if (o == null) { break; } // end of file
			if (o instanceof PairedEndContig) {
				pe = (PairedEndContig) o;
				// handle only the paired end cases
				if (pe.getNumMembers() > 0) {
                                    try{
					g.setPairedEndContig(pe.getId(), pe.getMembers());
//					numPE += 1;
                                    }
                                    catch(IllegalArgumentException e){
                                        g.removeEdge(pe.getId());
                                    }                                        
				}
			}
			parsed = true;
		}
		return parsed;
	}
	
	public void countGraphComponents() {
		System.out.println("total vertices: " + g.getVertexCount());
		System.out.println("total edges: " + g.getEdgeCount());
		int vCount = 0; int lastIndex = 0;
		for (int i=0; i < g.getVertexCount(); i++) {
			if (g.getVertex(i) != null) {
				vCount++;
				lastIndex = i;
			}
		}
		System.out.println("total non-null vertices: " + vCount + "; last non-null index: " + lastIndex);
		int eCount = 0; lastIndex = 0;
		for (int i=0; i < g.getEdgeCount(); i++) {
			if (g.getEdge(i) != null) {
				eCount++;
				lastIndex = i;
			}
		}
		System.out.println("total non-null edges: " + eCount + "; last non-null edge: " + lastIndex);
	}
	
	protected void printVertices() {
		for (int i=0; i < g.getVertexCount(); i++) {
			Vertex v = g.getVertex(i);
			if (v != null) {
				System.out.println("vertex: " + v.getId());
				System.out.println("Incoming");
				for (Edge e: v.getIncoming()) {
					System.out.println(e.getLabel() + " " + v.getIncomingPole(e));
				}
				System.out.println("Outgoing");
				for (Edge e: v.getOutgoing()) {
					System.out.println(e.getLabel() + " " + v.getOutgoingPole(e));
				}
			}
		}
	}
	
	protected String flipStrand(String l) {
		if (l.endsWith("+")) {
			return l.replace("+","") + "-";
		} else {
			return l.replace("-","") + "+";
		}
	}

	private void setDisEstimate(PairedEndPartners d) {
		Edge sEdge = g.getEdge(d.getSourceLabel()); // ensures correct orientation
		if (sEdge == null) {
			throw new IllegalArgumentException("source edge cannot be null");
		}
		Edge dEdge = g.getEdge(d.getDestLabel()); // ensures correct orientation
		if (dEdge == null) {
			throw new IllegalArgumentException("destination edge cannot be null");
		}
		Integer dis = d.getDis(); 
		Float error = d.getError();
		Integer numPairs = d.getNumPairs();
		List<DistanceEstimate> out = sEdge.getOutboundPartners();
		// only keep if has not occurred already
		if (out == null || !checkForDuplicate(dEdge, out)) {
			sEdge.addOutboundPartner(dEdge, dis, error, numPairs);
//			numDE += 1;
		}
		List<DistanceEstimate> in = dEdge.getInboundPartners();
		if (in == null || !checkForDuplicate(sEdge, in)) {
			// currently only keep first occurrence
			dEdge.addInboundPartner(sEdge, dis, error, numPairs);
//			numDE += 1;
		}
	}
	
	private boolean checkForDuplicate(Edge e, List<DistanceEstimate> dists) {
		boolean duplicate = false;
		for (DistanceEstimate d: dists) {
			if (e.getLabelObject().equals(d.getLabelObject())) {
				duplicate = true;
				break;
			}
		}
		return duplicate;
	}
	
	private void setConnections(Edge sEdge, String tContigLabel) throws IOException {
		String sContigNoStrand = getStrandless(sEdge.getLabel());
		String tContigNoStrand = getStrandless(tContigLabel);
		if (sContigNoStrand.equals(tContigNoStrand)) {
			storeSelfLoop(sEdge.getLabel(), tContigLabel, sEdge);
		} else {
			Edge tEdge = g.getEdge(tContigLabel);
			if (tEdge == null) {
				tEdge = new Edge(tContigLabel); 
			}
			storeEdgeRelation(sEdge, tEdge);
		}		
	}
	
	private String getStrandless(String s) {
		return s.replace("+", "").replace("-","");
	}
	
	/**
	 * Stores self loop cases, such as those resulting from:
	 * 1. directed repeats (e.g. "1+" -> "1+")
	 * 2. palindromes (e.g. "1+" -> "1-")
	 * Assumes base contig ids are the same (i.e. without strand)
	 * 
	 * @param sLabel source label for edge e
	 * @param tLabel target label for edge e
	 * @param e edge object to add 
	 */
	private void storeSelfLoop(String sLabel, String tLabel, Edge e) {
		Vertex[] vertices = e.getVertices();
		if (vertices[0] == null) {
			storeSelfLoop_case0(sLabel, tLabel, e);
		} else {
			storeSelfLoop_case1(sLabel, tLabel, e, vertices);
		}
	}
	
	/**
	 * Handles the case where a self-loop edge does not yet exist in the graph
	 * 
	 * @param sLabel source label for edge e
	 * @param tLabel target label for edge e
	 * @param e edge object to add
	 */
	private void storeSelfLoop_case0(String sLabel, String tLabel, Edge e) {
		if (sLabel.equals(tLabel)) {
			// directed repeats (e.g. "1+" -> "1+")
			Vertex v = new Vertex(getNextVid());
			v.addOutgoing(e, (byte) 0);
			v.addIncoming(e, (byte) 0);
			e.setVertices(v,v);
			g.addVertex(v);
			g.addEdge(e);
		} else {
			// palindrome case (e.g. "1+" -> "1-")
			Vertex v1 = new Vertex(getNextVid());
			Vertex v2 = new Vertex(getNextVid());
			v1.addOutgoing(e, (byte) 0);
			v2.addIncoming(e, (byte) 0);
			v2.setIsPalindrome(true);
			e.setVertices(v1,v2);
			g.addVertex(v1); g.addVertex(v2);
			g.addEdge(e);
		}
		// printVertices();
	}
	
	/**
	 * Handles the case where a self-loop edge already exists in the graph
	 * 
	 * @param sLabel source label for edge e
	 * @param tLabel target label for edge e
	 * @param e edge object
	 * @param vertices vertices for edge e
	 */
	private void storeSelfLoop_case1(String sLabel, String tLabel, Edge e, Vertex[] vertices) {
		// check if self-loop status captured in graph yet
		if (vertices[0].getId() != vertices[1].getId()) {
			if (sLabel.equals(tLabel)) {
				// directed repeats (e.g. "1+" -> "1+")
				// merge the vertices
				Vertex v0 = vertices[0];
                                Vertex v1 = vertices[1];
				boolean flip = needFlip(e, vertices[1], e, vertices[0]);
				// capture all previous connections
				replaceVertex(v1, v0, flip);  // flip pole of target if needed
				nextVid.add(v1.getId());
				g.removeVertex(v1);
				e.setVertices(v0, v0);
			} else {
				// palindrome case (e.g. "1+" -> "1-")
				vertices[1].setIsPalindrome(true);
			}
		}
		// palindrome may not have been caught yet though
		if (!sLabel.equals(tLabel)) {
			vertices[1].setIsPalindrome(true);
		}
	}
	
	private void storeEdgeRelation(Edge sEdge, Edge tEdge) throws IOException {
		Vertex[] sVertices = sEdge.getVertices();
		Vertex[] tVertices = tEdge.getVertices();
		if (sVertices[1] == null && tVertices[0] == null) {
			storeEdgeRelation_case0(sEdge, tEdge);
		} else if (sVertices[1] == null) {
			storeEdgeRelation_case1(sEdge, tEdge, tVertices);
		} else if (tVertices[0] == null) {
			storeEdgeRelation_case2(sEdge, sVertices, tEdge);
		} else {
			storeEdgeRelation_case3(sEdge, sVertices, tEdge, tVertices);
		}
	}
	
	/**
	 * Handles the case where both edges do not yet exist in the graph
	 * 
	 * @param sE source edge
	 * @param tE target edge
	 */
	private void storeEdgeRelation_case0(Edge sE, Edge tE) {
		// connection = (v1 -[sE]-> v2 -[tE]-> v3)
		// create three new vertices
		Vertex v1 = new Vertex(getNextVid());
		v1.addOutgoing(sE, (byte) 0);
		Vertex v2 = new Vertex(getNextVid());
		v2.addIncoming(sE, (byte) 0);
		v2.addOutgoing(tE, (byte) 0);
		Vertex v3 = new Vertex(getNextVid());
		v3.addIncoming(tE, (byte) 0);
		sE.setVertices(v1, v2);
		tE.setVertices(v2, v3);
		g.addVertex(v1); g.addVertex(v2); g.addVertex(v3);
		g.addEdge(sE); g.addEdge(tE);
	}
	
	/**
	 * Handles the case where the target edge, but not the source edge, exist in the graph
	 * 
	 * @param sE source edge
	 * @param tE target edge
	 * @param tV target vertices (already added)
	 */
	private void storeEdgeRelation_case1(Edge sE, Edge tE, Vertex[] tV) {
		// create one new vertex
		Vertex v1 = new Vertex(getNextVid());
		Vertex v2 = tV[0];
		byte p2 = v2.getOutgoingPole(tE); // tE is in the correct orientation
		v1.addOutgoing(sE, p2);
		v2.addIncoming(sE, p2);
		sE.setVertices(v1, v2);
		tE.setFirstVertex(v2);
		g.addVertex(v1);
		g.addEdge(sE);
		// printVertices();
	}
	
	/**
	 * Handles the case where the source edge, but not the target edge, exist in the graph
	 * 
	 * @param sE source edge
	 * @param sV source vertices (already added)
	 * @param tE target edge
	 */
	private void storeEdgeRelation_case2(Edge sE, Vertex[] sV, Edge tE) {
		// create one new vertex
		Vertex v2 = sV[1];
		Vertex v3 = new Vertex(getNextVid());
		byte p2 = v2.getIncomingPole(sE); // sE is in the correct orientation
		v2.addOutgoing(tE, p2);
		v3.addIncoming(tE, p2);
		tE.setVertices(v2, v3);
		sE.setSecondVertex(v2);
		g.addVertex(v3);
		g.addEdge(tE);
	}
	
	/**
	 * Handles the case where both target and source edge exist in the graph
	 * 
	 * @param sE source edge
	 * @param sV source vertices (already added)
	 * @param tE target edge
	 * @param tV target vertices (already added)
	 */
	private void storeEdgeRelation_case3(Edge sE, Vertex[] sV, Edge tE, Vertex[] tV) throws IOException {

		// check whether common node already exists
		if (sV[1].getId() == tV[0].getId()) {
			if (sV[1].getIncomingPole(sE) != tV[0].getOutgoingPole(tE)) {
//				throw(new IOException("Error parsing graph files. Please report this problem to the development team."));
				// System.out.println("src " + sE.getLabel() + " " + sV[0].getId() + " " + sV[1].getId() + " " + sV[1].getPole(sE, "in") + " " + sV[1].isPalindrome());
				// System.out.println("tar " + tE.getLabel() + " " + tV[0].getId() + " " + tV[0].getPole(tE, "out") + " " + tV[0].isPalindrome() + " " + tV[1].getId());
//                            return;
			}
			return;
		}
		
		// check for evidence of and handle more complex cases
		if (handleRepeats(sE, sV, tE, tV)) { return; }

		// previously unrecognized connection - create one new vertex
		Vertex v2_1 = sV[1]; Vertex v2_2 = tV[0];
		Vertex v2 = v2_1;
		boolean flip = needFlip(sE, v2_1, tE, v2_2);
		// capture all previous connections to v2_2
		replaceVertex(v2_2, v2, flip);  // flip pole of target if needed
		nextVid.add(v2_2.getId());
		g.removeVertex(v2_2);
		sE.setSecondVertex(v2);
		tE.setFirstVertex(v2);
		
		// printVertices();
	}
	
	private boolean needFlip(Edge sE, Vertex v2_1, Edge tE, Vertex v2_2) {
		
		// pole is irrelevant in case of palindromes
        if (v2_1.isPalindrome() || v2_2.isPalindrome()) { return false; }
		
		boolean flip = false;
		// use the source edge pole as reference
		int pole = v2_1.getIncomingPole(sE); 
		// determine whether target edge pole is compatible
		if (v2_2.getOutgoingPole(tE) != pole) {
			flip = true;
		}
		return flip;
	}
	
	private void replaceVertex(Vertex o, Vertex n, boolean flip) {
		if (o.isPalindrome() && !n.isPalindrome()) {
			n.setIsPalindrome(true);
		}
		for (Edge e: o.getIncoming()) {
			e.setSecondVertex(n);
			byte pole = o.getIncomingPole(e);
			if (flip) {
				n.addIncoming(e, o.getFlippedPole(pole));
			} else {
				n.addIncoming(e, pole);
			}
		}
		for (Edge e: o.getOutgoing()) {
			e.setFirstVertex(n);
			byte pole = o.getOutgoingPole(e);
			if (flip) {
				n.addOutgoing(e, o.getFlippedPole(pole));
			} else {
				n.addOutgoing(e, pole);
			}
		}

                // adds all the distances from the original vertex
                HashMap<String, Integer> mappings = o.getDistanceMappings();
                if(mappings != null && !mappings.isEmpty()){
                    n.addDistanceMappings(mappings);
                }
	}
	
	/**
	 * Checks for and handles special repeat cases. It is assumed that 
	 * v2_1 and v2_2 are not the same vertex (check externally).
	 * 
	 * @param v1 source start vertex
	 * @param v2_1 source end vertex
	 * @param v2_2 target end vertex
	 * @param sE source edge
	 * @param tE target edge
	 * @return whether repeat case was encountered
	 */
	private boolean handleRepeats(Edge sE, Vertex[] sV, Edge tE, Vertex[] tV) {
		Vertex v1 = sV[0]; Vertex v2_1 = sV[1];
		Vertex v2_2 = tV[0]; Vertex v3 = tV[1];
		
		boolean mystatus = false;
		// check for an existing connection
		if (v2_1.getId() == v3.getId()) {
			// target edge loops back to v2_1; replace v2_2 with v2_1
			int iPole = v2_1.getIncomingPole(tE);
			if (iPole != v2_1.getIncomingPole(sE)) {
				// inverted repeat
				tE.setHasInvertedRepeats(true);
			}
                        else{
                            tE.setHasInvertedRepeats(false);
                        }
			collapseTargetIntoLoop(sE, v2_1, tE, v2_2);
			mystatus = true;
		} else if (v1.getId() == v2_2.getId()) {
			// source edge loops back to v2_2; replace v2_1 with v2_2
			int oPole = v2_2.getOutgoingPole(sE);
			if (oPole != v2_2.getOutgoingPole(tE)) {
				// inverted repeat
				sE.setHasInvertedRepeats(true);
			}
                        else{
                            tE.setHasInvertedRepeats(false);
                        }
			collapseSourceIntoLoop(sE, v2_1, tE, v2_2);
			mystatus = true;
		}
		return mystatus;
	}
	
	/**
	 * Handles scenario where target edge should be a self-loop. This can happen
	 * because of palindromic repeats, directed, or inverted repeats. Examples  
	 * include when connection x+ -> y+ exists in the graph, and x+ -> y- is  
	 * subsequently parsed, or when x+ -> y+, z+ exists in the graph, and 
	 * y- -> x-, z- is subsequently parsed.
	 * 
	 * @param sE source edge
	 * @param v2_1 source end vertex
	 * @param tE target edge
	 * @param v2_2 target start vertex
	 */
	private void collapseTargetIntoLoop(Edge sE, Vertex v2_1, Edge tE, Vertex v2_2) {		
		// collapse start and end of tE
		Vertex v2 = v2_1;
		// capture all previous connections to v2_2
		boolean flip = needFlip(sE, v2_1, tE, v2_2);
		replaceVertex(v2_2, v2, flip);
		nextVid.add(v2_2.getId());
		g.removeVertex(v2_2);
		sE.setSecondVertex(v2);
		tE.setVertices(v2, v2);
	}
	
	/**
	 * Handles scenario where source edge should be a self-loop. This can happen
	 * because of palindromic repeats, directed, or inverted repeats. Examples  
	 * include when connection x+ -> y+ exists in the graph, and x+ -> y- is  
	 * subsequently parsed, or when x+ -> y+, z+ exists in the graph, and 
	 * y- -> x-, z- is subsequently parsed.
	 * 
	 * @param sE source edge
	 * @param v2_1 source end vertex
	 * @param tE target edge
	 * @param v2_2 target start vertex
	 */
	private void collapseSourceIntoLoop(Edge sE, Vertex v2_1, Edge tE, Vertex v2_2) {
		// collapse the start and end of sE
		Vertex v2 = v2_2;
		// capture all previous connections to v2_1;
		boolean flip = needFlip(sE, v2_1, tE, v2_2);
		replaceVertex(v2_1, v2, flip);
		nextVid.add(v2_1.getId());
		g.removeVertex(v2_1);
		sE.setVertices(v2, v2);
		tE.setFirstVertex(v2);
	}
	
	private void storeSingleton(Edge e) {
		if (e.getVertices()[0] == null) {
			Vertex v1 = new Vertex(getNextVid());
			Vertex v2 = new Vertex(getNextVid());
			v1.addOutgoing(e, (byte) 0);
			v2.addIncoming(e, (byte) 0);
			e.setVertices(v1, v2);
			g.addVertex(v1); g.addVertex(v2);
			g.addEdge(e);
		}
	}

	private int getNextVid() {
		int vid = vCounter;
		if (nextVid.size() != 0) {
			// use a previously cleared vertex id
			vid = nextVid.get(0);
			nextVid.remove(0);
		} else {
			vCounter++;
			// sanity check
			if (vCounter >= g.getVertexCount()) {
				// ensure that it is the next slot
				assert vCounter == g.getVertexCount(): "Error generating next vertex count";
			}
		}
		return vid;
	}
	
//	public static void main(String[] args) throws IOException {
//		
//		File gFile = new File(args[0]);
//		GraphLoader p = new GraphLoader();
//
//		try {
//			p.load(gFile, false);
//		} catch (Exception e) {
//			System.out.println(e);
//			System.exit(0);
//		}
//		p.countGraphComponents();
//		
//	}

    public void addChangeListener(ChangeListener l) {
        changeSupport.addChangeListener(l);
    }

	public void fireStateChanged() {
//TODO: figure out this throws a null pointer exception            
		changeSupport.fireStateChanged();
	}

    public ChangeListener[] getChangeListeners() {
        return changeSupport.getChangeListeners();
    }

    public void removeChangeListener(ChangeListener l) {
        changeSupport.removeChangeListener(l);
    }
	
}
