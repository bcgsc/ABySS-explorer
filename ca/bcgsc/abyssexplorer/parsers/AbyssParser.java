package ca.bcgsc.abyssexplorer.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import ca.bcgsc.abyssexplorer.graph.AbyssGraph;
import ca.bcgsc.abyssexplorer.graph.AbyssGraph2;

/**
 * Custom parser for ABySS output (compatible with 
 * output files from ABySS version 1.1.0 and higher).
 * 
 * @author Cydney Nielsen
 * 
 */
public class AbyssParser implements GraphParser {
	
	protected BufferedReader inputStream;
	protected String mode; // one of 'adj', 'dist', 'fasta', or 'path'
	
	// adj patterns

//        // 78 145 3063	; 79+	; 83- [d=-13]
//        protected static Pattern adjPattern_1_2_3 = Pattern.compile("\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)\\s*;(\\s+\\d+[+-](?:\\s*\\[d=-?\\d+\\])?)*\\s*;(\\s+\\d+[+-](?:\\s*\\[d=-?\\d+\\])?)*\\s*");
//	
//	// ABySS 1.1.2 versions - e.g. "0 49 101        ; 58- 71-       ; 3+ 78-"
//	protected static Pattern adjPattern_1_1_2 = Pattern.compile("^\\s*(\\d+) (\\d+) (\\d+)\t;( \\d+[+-])*\t;( \\d+[+-])*\\s*$", Pattern.MULTILINE);
//	// ABySS 1.1.0 (and 1.1.1) versions - e.g. "4 46 3926+ 47847- 569231+ 588716- ; 1077218- 1077220- 1677662+"
//	protected static Pattern adjPattern_1_1_0 = Pattern.compile("^\\s*(\\d+) (\\d+)( \\d+[+-])* ;( \\d+[+-])*\\s*$", Pattern.MULTILINE);
	
	// dist pattern 
	
	// ABySS 1.1.x versions - e.g. "5 7-,-17,718,0.7 ; 11-,-46,24,3.8 61+,-46,1168,0.5"
	protected static Pattern distPattern = Pattern.compile("^\\s*(\\d+)(\\s*\\d+[+-],-?\\d+,\\d+,\\d+\\.\\d+)*\\s*;(\\s*\\d+[+-],-?\\d+,\\d+,\\d+\\.\\d+)*\\s*$", Pattern.MULTILINE);

	// fasta header pattern - e.g. ">1770690 299 4826 1696362+,69659-" (last field optional)
	protected static Pattern fastaPattern = Pattern.compile("^>(\\d+)\\s*(\\d+)\\s*(\\d+)(\\s*\\d+[+-N](,...)*(,\\d+[+-N])*)?\\s*$", Pattern.MULTILINE);
        
        // path pattern - e.g. "96	51+ 80- 75- 32- 75- 61- 91- 20-"
        protected static Pattern pathPattern = Pattern.compile("\\s*(\\d+)((\\s*\\d+[+-N])*)?\\s*");
        
        protected static Pattern idStrandPattern = Pattern.compile("(\\d+[+-N])");
        
        private static Pattern pathExtension = Pattern.compile("\\s*.*-\\d+\\.path(?:\\d+)?\\s*");

//        protected Matcher ap_1_2_3;
//	protected Matcher ap_1_1_2;
//	protected Matcher ap_1_1_0;
	protected Matcher dp;
	protected Matcher fp;
        protected Matcher pp;
	
	protected Object dummyO = new Object();
//        protected int largestContigId = -1;
//        protected int smallestContigId = -1;
        protected int k = -1;
	
	protected void regexSetup(CharSequence s) {
                //ap_1_2_3 = adjPattern_1_2_3.matcher(s);
		//ap_1_1_2 = adjPattern_1_1_2.matcher(s);
		//ap_1_1_0 = adjPattern_1_1_0.matcher(s);
		dp = distPattern.matcher(s);
		fp = fastaPattern.matcher(s);
                pp = pathPattern.matcher(s);
	}
	
	public void open(File f) throws IOException {
		String fName = f.getAbsolutePath();
                
                if (inputStream != null){
                    inputStream.close();
                }                
                
		try {
			inputStream = new BufferedReader(new FileReader(fName));
			if (fName.endsWith(".adj")) {
				mode = "adj";
			} else if (fName.endsWith(".dist")) {
				mode = "dist";
			} else if (fName.endsWith(".fa")) {
				mode = "fasta";
                        } else if (fName.endsWith(".path") || pathExtension.matcher(fName).matches()) {
                                mode = "path";
			} else {
				throw (new IOException("Unknown file type: " + fName));
			}
		} catch (FileNotFoundException e) {
			throw (new IOException("No such file: " + fName));
		}
	}
	
	public void close() throws IOException {
		inputStream.close();
	}
	
	public AbyssGraph2 initializeGraph() throws IOException, InterruptedException {
		if (!mode.equals("adj")) {
			throw (new IOException("Must specify a .adj file"));
		}
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
//			Object o = parseAdjLine();
//			if (o == null) {continue;} // no match to known patterns
//
//                        if(o instanceof AdjacentContigs){
//                            AdjacentContigs aContigs = (AdjacentContigs) o;
//
//                            vCount += 1;
//                            // one connection from single source vertex to each target
//                            int e = Integer.parseInt(aContigs.getLabel().replace("+", "").replace("-", ""));
//                            if (e > maxE) { maxE = e; }
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
//                        }
//                        else{
//                            AdjacentContigs[] aContigsList = (AdjacentContigs[]) o;
//                            for(AdjacentContigs aContigs : aContigsList){
//                                vCount += 1;
//                                // one connection from single source vertex to each target
//                                int e = Integer.parseInt(aContigs.getLabel().replace("+", "").replace("-", ""));
//                                if (e > maxE) { maxE = e; }
//
//                                Integer len = aContigs.getLen();
//                                if(len != null){
//                                    if(len > maxLength){
//                                        if(maxLength == 0){
//                                            minLength = len;
//                                            smallestContigId = aContigs.id;
//                                        }
//                                        maxLength = len;
//                                        largestContigId = aContigs.id;
//                                    }
//                                    else if(len < minLength){
//                                        minLength = len;
//                                        smallestContigId = aContigs.id;
//                                    }
//                                }
//                            }
//                        }
//		}
                AbyssGraph2 g = new AbyssGraph2();
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

	/**
	 * Returns null if at the end of the file;
	 * Returns a dummy Object if no match to known patterns
	 */
	public Object parseNextLine() throws IOException, InterruptedException {
                if(Thread.interrupted()){
                    throw new java.lang.InterruptedException();
                }

		Object o = null;
		String s = inputStream.readLine();
		if (s != null) {
			regexSetup(s);
			if (mode.equals("adj")) {
//                            if(ap_1_2_3.matches()){
                                o = matchAdj_1_2_3(s);
//                            }
//                            else{
//                                o = matchAdj(s);
//                            }
			} else if (mode.equals("dist")) {
				o = matchDist(s);
                        } else if (mode.equals("path")){
                                o = matchPath(s);
				if (o == null) {
					o = dummyO;
				}
			} else if (mode.equals("fasta")) {
				o = matchFasta(s);
				if (o == null) {
					// unimportant line in fasta file
					o = dummyO;
				}                                
			} else {
				throw new IOException("Unknown file format");
			}
		}
		return o;
	}
	
	protected Object parseAdjLine() throws IOException {
		String s = inputStream.readLine();
//                System.out.println(s);
//                if(s.startsWith("179558") || s.startsWith("253229")){
//                    System.out.println(s);
//                }
		if (s != null) {
			regexSetup(s);
                        Object o = null;
                        
//                        if(ap_1_2_3.matches()){
                            o = matchAdj_1_2_3(s);
//                        }
//                        else {
//                            return matchAdj(s);
//                        }
                        return o;
		}
		return null;
	}
	
//	/**
//	 * Parse adjacency information from input string
//	 * Compatible with output files from ABySS 1.1.0 and higher. 
//	 * Returns null if no match.
//	 * @param s
//	 * @return
//	 */
//	protected AdjacentContigs matchAdj(String s) throws IOException {
//
//		AdjacentContigs aContigs = null;
//		if (ap_1_1_2.find() == true) {
//			aContigs = matchAdj_1_1_2(s);
//		} else if (ap_1_1_0.find() == true) {
//			aContigs = matchAdj_1_1_0(s);
//		}
//		return aContigs;
//	}

	
	/**
	 * Parse ABySS 1.1.0/1.1.1 adjacency information from input string
	 * (e.g. "0 47 69+ 78+ ; 36+ 41-"). Returns null if no match.
	 * @param s
	 * @return
	 */
	public AdjacentContigs matchAdj_1_1_0(String s) throws IOException {
		
		String[] parts = s.split(" ");
		String label = parts[0] + "+";
		Integer len = Integer.parseInt(parts[1]);
		List<String> outbound = new ArrayList<String>(); 
		List<String> inbound = new ArrayList<String>();
		boolean isOutbound = true;
		for (int i=2; i<parts.length; i++) {
			if (parts[i].equals(";")) {
				isOutbound = false;
				continue;
			}
			if (isOutbound) {
				outbound.add(parts[i]);
			} else {
				inbound.add(parts[i]);
			}
		}
		
		AdjacentContigs aContigs = null;
		if (label != null) {
			aContigs = new AdjacentContigs(label, len);
		}
		if (!outbound.isEmpty()) {
			aContigs.addOutbound(outbound);
		}
		if (!inbound.isEmpty()) {
			aContigs.addInbound(inbound);
		}
		
		return aContigs;
	}
	
	/**
	 * Parse ABySS 1.1.2+ adjacency information from input string
	 * (e.g. "0 49 101        ; 58- 71-       ; 3+ 78-"). 
	 * Returns null if no match.
	 * @param s
	 * @return
	 */
	public AdjacentContigs matchAdj_1_1_2(String s) throws IOException {
		
		String[] parts = s.split("\t");
		String[] info_parts = parts[0].split(" ");
		String label = info_parts[0] + "+";
		Integer len = Integer.parseInt(info_parts[1]);
		Integer cov = Integer.parseInt(info_parts[2]); // This is the absolute coverage

//                String dum = null;
//                if(cov.intValue() == 0){
//                    dum = cov.toString();
//                }


		List<String> outbound = new ArrayList<String>(); 
		List<String> inbound = new ArrayList<String>();
		String[] oTemp = parts[1].split(" ");
		String[] iTemp = parts[2].split(" ");
		if (oTemp.length > 1) {
			for (String o: oTemp) {
				if (o.equals(";")) { continue; }
				outbound.add(o);
			}
		}
		if (iTemp.length > 1) {
			for (String i: iTemp) {
				if (i.equals(";")) { continue; }
				inbound.add(i);
			}
		}
	
		AdjacentContigs aContigs = null;
		if (label != null) {
                    if(k > 0){
			aContigs = new AdjacentContigs(label, len, ((float)cov)/(float)(len - k +1)); // mean coverage
                    }
                    else{
                        aContigs = new AdjacentContigs(label, len, cov); // absolute coverage
                    }
		}
		if (!outbound.isEmpty()) {
			aContigs.addOutbound(outbound);
		}
		if (!inbound.isEmpty()) {
			aContigs.addInbound(inbound);
		}
		
		return aContigs;
	}

        /*
         * @pre k > 0
         */
        public AdjacentContigs[] matchAdj_1_2_3(String s) throws IOException {
            String[] parts = s.split(";");
            String[] info_parts = parts[0].trim().split(" ");

            String label = info_parts[0] + "+";
            Integer len = Integer.parseInt(info_parts[1]);
            Integer cov = Integer.parseInt(info_parts[2]); // This is the absolute coverage
            float meanCoverage = (float)cov;
            if (k > 0){
                meanCoverage = meanCoverage/(float)(len - k +1);
            }

            ArrayList<AdjacentContigs> adjContigsList = new ArrayList<AdjacentContigs>();

            if(parts.length > 1){
                String[] oTemp = parts[1].trim().split(" ");
                if (oTemp.length > 0) {
                    for (int index = 0; index < oTemp.length; index++) {
                        if(!oTemp[index].equals("")){
                            AdjacentContigs aContigs = new AdjacentContigs(label, len, meanCoverage);
                            aContigs.addOutbound(oTemp[index]);

                            if(index+1 < oTemp.length && oTemp[index+1].startsWith("[d=")){
                                int d = Integer.parseInt(oTemp[index+1].substring(3, oTemp[index+1].indexOf("]")));
                                aContigs.setDistance(d);
                                index++;
                            }
                            else{
                                if(k>0){
                                    aContigs.setDistance(-(k-1)); // global distance
                                }
                                else{
                                    aContigs.setDistance(0);
                                }
                            }

                            adjContigsList.add(aContigs);
                        }
                    }
                }

                if(parts.length > 2){
                    String[] iTemp = parts[2].trim().split(" ");
                    if (iTemp.length > 0) {
                        for (int index = 0; index < iTemp.length; index++) {
                            if(!iTemp[index].equals("")){
                                AdjacentContigs aContigs = new AdjacentContigs(label, len, meanCoverage);
                                aContigs.addInbound(iTemp[index]);

                                if(index+1 < iTemp.length && iTemp[index+1].startsWith("[d=")){
                                    int d = Integer.parseInt(iTemp[index+1].substring(3, iTemp[index+1].indexOf("]")));
                                    aContigs.setDistance(d);
                                    index++;
                                }
                                else{
                                    aContigs.setDistance(-(k-1));
                                }

                                adjContigsList.add(aContigs);
                            }
                        }
                    }
                }
            }

            if(adjContigsList.isEmpty()){
                adjContigsList.add(new AdjacentContigs(label, len, meanCoverage));
            }

            return adjContigsList.toArray(new AdjacentContigs[adjContigsList.size()]);
        }


        public void setK(int k){
            this.k = k;
        }
	
	/**
	 * Parse distance estimate information from input string
	 * (e.g. "5 7-,-17,718,0.7 ; 11-,-46,24,3.8 61+,-46,1168,0.5").
	 * Returns null if no match.
	 * @param s
	 * @return
	 * @throws IOException
	 */
	protected List<PairedEndPartners> matchDist(String s) throws IOException {
		
		if (!dp.find()) { return null; }
		String[] parts = s.split(" ");
		List<PairedEndPartners> dList = new ArrayList<PairedEndPartners>(parts.length-2);

		// extract source label
		int sId = Integer.parseInt(parts[0]);
		byte sStrand = 0; // file format specifies positive strand orientation

		// extract targets and stats
		int dId; byte dStrand; 
		int dist; int nPairs; float error;
		boolean outbound = true;
		for (int i=1; i<parts.length; i++) {
			if (parts[i].equals(";")) {
				outbound = false;
				continue;
			}
                        
			String[] values = parts[i].split(",");
			if (values[0].endsWith("+")) {
				dId = Integer.parseInt(values[0].replace("+", ""));
				dStrand = 0;
			} else {
				dId = Integer.parseInt(values[0].replace("-", ""));
				dStrand = 1;
			}
			dist = Integer.parseInt(values[1]);
			nPairs = Integer.parseInt(values[2]);
			error = Float.parseFloat(values[3]);
			if (outbound) {
				dList.add(new PairedEndPartners(sId, sStrand, dId, dStrand, dist, nPairs, error));
			} else {
				dList.add(new PairedEndPartners(dId, dStrand, sId, sStrand, dist, nPairs, error));
			}
		}
		return dList;
	}
		
	/**
	 * Parse paired-end information from an input string from a file with the "-contigs.fa" postfix
	 * (e.g. ">1770690 299 4826 1696362+,69659-"). 
	 * Returns null if no match.
	 * @param s
	 * @return
	 * @throws IOException
	 */
	public PairedEndContig matchFasta(String s) throws IOException {
		if (!fp.find()) { return null; }
		String[] parts = s.split(" ");
		PairedEndContig pe = new PairedEndContig(Integer.parseInt(fp.group(1)));
		// parts[1] = length
		pe.setCoverage(Integer.parseInt(parts[2]));
		if (parts.length == 4) {
			for (String m: parts[3].split(",")) {
//                            if(!m.endsWith("N")){ // ignore names like "44N" because "44" is not the id
                            if(!m.trim().equals("...")){
				pe.addMember(m);
                            }
//                            }
			}
		}
		return pe;
	}

        /**
         * Parse paired-end information from input string from a file with the "-5.path" postfix
         * Returns null if no match.
         * @param s
         * @return
         * @throws IOException 
         */
        public PairedEndContig matchPath(String s) throws IOException {
            if (!pp.find()) {return null; }
            PairedEndContig pe = new PairedEndContig(Integer.parseInt(pp.group(1)));                
            Matcher ispm = idStrandPattern.matcher(pp.group(2));
            while(ispm.find()){
                String se_id = ispm.group();
                pe.addMember(se_id);
            }

            return pe;
        }
        
}
