package ca.bcgsc.abyssexplorer.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import ca.bcgsc.abyssexplorer.graph.AbyssGraph;
import ca.bcgsc.abyssexplorer.graph.AbyssGraph2;

/**
 * Custom parser for DOT output.
 * 
 * 
 * @author Cydney Nielsen
 * 
 */
public class DotParser implements GraphParser {
	
	BufferedReader inputStream;
	
	// first line of a graph - eg. digraph ht14 {
	protected static Pattern headerPattern = Pattern.compile("digraph\\s*(\\S+)\\s*\\{\\s*");
	// last line of a graph
	protected static Pattern footerPattern = Pattern.compile("^\\s*\\}\\s*$", Pattern.MULTILINE);
	protected static Pattern modePattern = Pattern.compile("^\\s*(\")?mode(\")?\\s*=\\s*(\")?(\\w+)(\")?;\\s*$", Pattern.MULTILINE);

        // eg. k=48
        protected static Pattern kValuePattern = Pattern.compile("\\s*k=(\\d+).*");
        
        // eg. graph [k=48]
        protected static Pattern kValuePattern2 = Pattern.compile("\\s*graph\\s*\\[k=(\\d+).*\\].*");

        // eg. edge[d=-47]
        protected static Pattern globalDistancePattern = Pattern.compile("\\s*edge\\s*\\[d=([+-]\\d+)\\]\\s*;?\\s*");
        
        // eg. "32+" [l=48]
        protected static Pattern lengthPattern = Pattern.compile("\\s*\"(\\d+[+-])\"\\s*\\[l=(\\d+)\\]\\s*;?\\s*");
        
        // eg. "32+" [l=48?????]
        protected static Pattern lengthOnlyPattern = Pattern.compile("\\s*\"\\d+\\+\"\\s*\\[l=(\\d+).*\\]\\s*;?\\s*");
        
        // eg. "32+" [l=48 c=7207] // c is the mean coverage
        protected static Pattern lengthMeanCoverageAttributePattern = Pattern.compile("\\s*\"(\\d+[+-])\"\\s*\\[l=(\\d+)\\s*c=(\\d+\\.?\\d*)\\]\\s*;?\\s*");
        
        // eg. "32+" [l=48 C=7207] // C is the absolute coverage
        protected static Pattern lengthAbsoluteCoverageAttributePattern = Pattern.compile("\\s*\"(\\d+[+-])\"\\s*\\[l=(\\d+)\\s*C=(\\d+\\.?\\d*)\\]\\s*;?\\s*");

	// vertex pattern - e.g. "0+"
	protected static Pattern vertexPattern = Pattern.compile("\"(\\d+[+-N])\"");

	// adj pattern - e.g. "0+" -> { "513714-" "540790+" }
        //               or   "0+" -> "513714-"
	protected static Pattern adjPattern = Pattern.compile("\\s*\"(\\d+[+-])\"\\s*->\\s*\\{?\\s*((\"\\d+[+-]\"\\s*)+)\\s*\\}?\\s*;?\\s*");

        // adj pattern - eg. "0+" -> "39+" [d=-25]
        protected static Pattern adjWithDistancePattern = Pattern.compile("\\s*\"(\\d+[+-])\"\\s*->\\s*\"\\{?\\s*(\\d+[+-])\"\\s*}?\\s*\\[d=([+-]?\\d+)\\]\\s*;?\\s*");

        // dist pattern - e.g. "0+" -> "39+" [d=-25 e=1 n=10]
	protected static Pattern distEstPattern = Pattern.compile("\\s*\"(\\d+[+-])\"\\s*->\\s*\"(\\d+[+-])\"\\s*\\[d=([+-]?\\d+)\\s*e=(\\d+\\.\\d+)\\s*n=(\\d+)\\]\\s*;?\\s*");

        // path pattern - e.g. subgraph "1978+" { "14+" -> "1396+" -> "552+" -> "359-" -> "74N" -> "139+" }
        protected static Pattern pathPattern = Pattern.compile("\\s*subgraph\\s*\"(\\d+)\\+\"\\s*\\{\\s*\"(\\d+[+-N])\"\\s*(->\\s*\"\\d+[+-N]\"\\s*)+\\}\\s*", Pattern.MULTILINE);

	protected Matcher hp;
	protected Matcher fp;
	protected Matcher mp;
	protected Matcher vp; 
	protected Matcher ap;
	protected Matcher dp;
        protected Matcher lp;
        protected Matcher lop;
        protected Matcher attp_mc;
        protected Matcher attp_ac;
        protected Matcher gdp;
        protected Matcher adp;
        protected Matcher kp;
        protected Matcher kp2;
        protected Matcher pp;
	
	protected final static Object dummyO = new Object();
//        protected int largestContigId = -1;
//        protected int smallestContigId = -1;
        protected Integer globalDistance = null;
        protected int k = -1;

//        protected boolean isPairedEnd = true;

	protected void regexSetup(CharSequence s) {
		hp = headerPattern.matcher(s);
		fp = footerPattern.matcher(s);
		mp = modePattern.matcher(s);
		vp = vertexPattern.matcher(s);
		ap = adjPattern.matcher(s);
		dp = distEstPattern.matcher(s);
                attp_mc = lengthMeanCoverageAttributePattern.matcher(s);
                attp_ac = lengthAbsoluteCoverageAttributePattern.matcher(s);
                lp = lengthPattern.matcher(s);
                lop = lengthOnlyPattern.matcher(s);
                gdp = globalDistancePattern.matcher(s);
                adp = adjWithDistancePattern.matcher(s);
                kp = kValuePattern.matcher(s);
                kp2 = kValuePattern2.matcher(s);
                pp = pathPattern.matcher(s);
	}
	
	public void open(File f) throws IOException {
//            largestContigId = -1;
//            smallestContigId = -1;
            globalDistance = -1;
            k = -1;

            if (inputStream != null){
                inputStream.close();
            }
            
		String fName = f.getAbsolutePath();
		try {
			inputStream = new BufferedReader(new FileReader(fName));
//                        String fileName = f.getName();
//                        if(fileName.endsWith("-contigs.dot") || fileName.endsWith("-6.dot")){
//                            isPairedEnd = true;
//                        }
//                        else if(fileName.endsWith("-5.dot")){
//                            isPairedEnd = false;
//                        }

		} catch (FileNotFoundException e) {
			throw (new IOException("No such file: " + fName));
		}
	}
	
	public void close() throws IOException {
		inputStream.close();
	}
	
	public AbyssGraph2 initializeGraph() throws IOException, InterruptedException {
//		boolean foundAdjGraph = false;
//		// count the number of vertices
//		int vCount = 0; 
//		// find the largest contig label id 
//		// not equivalent to single-end contig count in ABySS
//		int maxE = 0;
//                int maxLength = 0;
//                int minLength = 0;
//                largestContigId = -1;
//                smallestContigId = -1;
//		while (inputStream.ready()) {
//			Object o = parseNextLine();
//			if (o == null) {continue;} // no match to known patterns
//			if (o instanceof AdjacentContigs) {
//                            AdjacentContigs aContigs = (AdjacentContigs) o;
//				foundAdjGraph = true;
//				vCount += 1;
//				// one connection from single source vertex to each target
//				String label = aContigs.getLabel();
//				int e = Integer.parseInt(label.replace("+", "").replace("-", ""));
//				if (e > maxE) { maxE = e; }
//				// System.out.println(vCount + " " + maxE);
//
//                            Integer len = aContigs.getLen();
//                            if(len != null){
//                                if(len > maxLength){
//                                    if(maxLength == 0){
//                                        minLength = len;
//                                        smallestContigId = aContigs.id;
//                                    }
//                                    maxLength = len;
//                                    largestContigId = aContigs.id;
//                                }
//                                else if(len < minLength){
//                                    minLength = len;
//                                    smallestContigId = aContigs.id;
//                                }
//                            }
//			}
//                        else if(o instanceof PairedEndContig){
//                            isPairedEnd = false;
//                        }
//		}
//		int eCount = maxE + 1; // 0 based labels
//		
//		if (!foundAdjGraph) { 
//			throw (new IOException("DOT file must contain an 'adj' graph"));
//		}
		
		//System.out.println("found " + vCount + " vertices and " + eCount + " edges");
		
		// actual vCount typically < eCount, but > eCount/2, so using vCount=eCount reasonable estimate
		//AbyssGraph g = new AbyssGraph(eCount, eCount);
                AbyssGraph2 g = new AbyssGraph2();

//                g.setPairedEndAssembly(isPairedEnd);
		return g;
	}

//        // longest contig's id
//        public int getLargestContigId(){
//            System.out.println("id of largest contig: "  + largestContigId);
//            return largestContigId;
//        }
//
//        // shortest contig's id
//        public int getSmallestContigId(){
//            return smallestContigId;
//        }
	
        public int getNextContigLength() throws IOException, InterruptedException {
            if(Thread.interrupted()){
                throw new java.lang.InterruptedException();
            }

            String s = inputStream.readLine();                
            if (s == null) { return -1; }
            
            lop = lengthOnlyPattern.matcher(s);
            if (lop.matches()) {
                return Integer.parseInt(lop.group(1));
            }
            
            return 0;
        }
        
	/**
	 * Returns null at end of file
	 */
	public Object parseNextLine() throws IOException, InterruptedException {
                if(Thread.interrupted()){
                    throw new java.lang.InterruptedException();
                }

		String s = inputStream.readLine();                
		if (s == null) { return null; }               
		regexSetup(s);

        	Object o = matchDist();
                if(o != null){
                    return o;
                }

                o = matchAdjWithDistance();
                if(o != null){
                    return o;
                }

                o = matchPath();
                if(o != null){
                    return o;
                }

                o = matchAdj();
                if(o != null){
                    return o;
                }

                matchGlobalDistance();

                matchKValue();
                
		return dummyO;
	}
	
	private AdjacentContigs matchAdj() {
            if(attp_mc.matches()){
                return new AdjacentContigs(attp_mc.group(1), Integer.parseInt(attp_mc.group(2)), Float.parseFloat(attp_mc.group(3)) );
            }
            else if(attp_ac.matches()){
                int len = Integer.parseInt(attp_ac.group(2));
                float cov = Float.parseFloat(attp_ac.group(3));
                if(k > 0){
                    return new AdjacentContigs(attp_ac.group(1), len, cov/(float)(len - k +1)); // mean coverage
                }
                return new AdjacentContigs(attp_ac.group(1), len, cov);
            }                    
            else if(lp.matches()){
                return new AdjacentContigs(lp.group(1), Integer.parseInt(lp.group(2)));
            }

		if (!ap.find()) { return null; }
		AdjacentContigs aContigs;
		int mStart = ap.start();
		// find source vertex pattern
		vp.find(mStart);
		aContigs = new AdjacentContigs(vp.group(1));
		mStart = vp.end();
		// find all partner vertex patterns
		while (vp.find(mStart)) {
			aContigs.addOutbound(vp.group(1));
			mStart = vp.end();
		}

                if(globalDistance != null){
                    aContigs.setDistance(globalDistance);
                }
		return aContigs;
	}
	
	private PairedEndPartners matchDist() {
		if (!dp.find()) { return null; }
		String src = dp.group(1); int sId; byte sStrand; 
		if (src.endsWith("+")) {
			sId = Integer.parseInt(src.replace("+",""));
			sStrand = 0;
		} else {
			sId = Integer.parseInt(src.replace("-", ""));
			sStrand = 1;
		}
		String des = dp.group(2); int dId; byte dStrand;
		if (des.endsWith("+")) {
			dId = Integer.parseInt(des.replace("+", ""));
			dStrand = 0;
		} else {
			dId = Integer.parseInt(des.replace("-", ""));
			dStrand = 1;
		}
		int dist = Integer.parseInt(dp.group(3));
                float err = Float.parseFloat(dp.group(4));
                int num = Integer.parseInt(dp.group(5));
		PairedEndPartners d = new PairedEndPartners(sId, sStrand, dId, dStrand, dist, num, err);
		return d;
	}

        private PairedEndContig matchPath(){
            if(!pp.matches()){
                return null;
            }

            int peId = Integer.parseInt(pp.group(1));

            PairedEndContig pe = new PairedEndContig(peId);

            pe.addMember(pp.group(2));

            int start = pp.start(2);

            vp.find(start);
            pe.addMember(vp.group(1));
            start = vp.end();
            while (vp.find(start)) {
                    pe.addMember(vp.group(1));
                    start = vp.end();
            }

            return pe;
        }

        private void matchGlobalDistance(){
            if(gdp.matches()){
                globalDistance = new Integer(gdp.group(1));
            }            
        }

        private AdjacentContigs matchAdjWithDistance(){
            if(adp.matches()){
                AdjacentContigs aContigs = new AdjacentContigs(adp.group(1));
                aContigs.addOutbound(adp.group(2));
                aContigs.setDistance(Integer.parseInt(adp.group(3)));
                return aContigs;
            }
            return null;
        }

        private void matchKValue(){
            if(kp.matches()){
                k = new Integer(kp.group(1));
            }
            else if(kp2.matches()){
                k = new Integer(kp2.group(1));
            }
        }

        public int getK(){
            return k;
        }
	
}
