package ca.bcgsc.dive.util;

/**
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 * Updated on July 15, 2010
 */
import ca.bcgsc.abyssexplorer.parsers.DotParser;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


public final class Utilities {

    /**
     * Sum of the list of integers.
     * @param list  a list of integers
     * @return the sum
     */
    public static long getSum(List<Integer> list)
    {
        long sum = 0;
        for(Integer x : list){
        	sum += (long) x;
        }
        return sum;
    }

    /**
     * Sum of a list of integers with an adjustment of minus k-1 for each integer. Only integers >= k would be added.
     * @param list  a list of integers
     * @param k  the value of k
     * @return the sum
     */
    public static long getSum(List<Integer> list, int k)
    {
        long sum = 0;
        long adjustmentForKmer = k-1;        
        //int count = 0;

        for(Integer x : list){
        	long contigLength = x;
        	if(contigLength >= k) // make sure that contigs < k are not added
        		sum += (contigLength - adjustmentForKmer);
        	//else
        	//	count++;
        }

        //if(count >0)
        //	System.out.println("WARNING: " + count + " contigs < k(=" + k + "). These contigs were not included.");

        return sum;
    }

    /**
     * Calculate the length of the nucleotide sequence. Only A, T, C, and G would be counted.
     * @param sequence  the nucleotide sequence
     * @return the length
     */
    public static int countNucleotides(String sequence)
    {
    	char[] charArr = sequence.toCharArray();
    	int count = 0;

    	for(char character : charArr)
    	{
    		char c = Character.toUpperCase(character);
    		switch(c)
    		{
    			case 'A':
    				count++;
    				break;
    			case 'T':
    				count++;
    				break;
    			case 'C':
    				count++;
    				break;
    			case 'G':
    				count++;
    				break;
    		}
    	}

    	return count;
    }

    public static int countN(String sequence)
    {
    	char[] charArr = sequence.toCharArray();
    	int count = 0;

    	for(char character : charArr)
    	{
    		char c = Character.toUpperCase(character);
    		if(c == 'N')
  				count++;
    	}

    	return count;
    }    
    
    public static int getScaffoldSpan(String sequence)
    {
    	int[] arr = getScaffoldLength(sequence);

    	return arr[0] + arr[1];
    }

    public static int[] getScaffoldLength(String sequence)
    {
    	char[] charArr = sequence.toCharArray();
    	int nucleotides = 0;
        int numNs = 0;

    	for(char character : charArr)
    	{
    		char c = Character.toUpperCase(character);
    		switch(c)
    		{
    			case 'A':
    				nucleotides++;
    				break;
    			case 'T':
    				nucleotides++;
    				break;
    			case 'C':
    				nucleotides++;
    				break;
    			case 'G':
    				nucleotides++;
    				break;
    			case 'N':
    				numNs++;
    				break;
    		}
    	}

    	return new int[]{nucleotides, numNs};
    }

    /**
     * Test whether the file at the specified path exists.
     * @param path  the path to be tested
     * @return whether the file at the specified path exists or not
     */
    public static boolean fileExist(String path)
    {
        File f = new File(path);
        return f.exists();
    }

    /**
     * Test whether the string is a non-negative integer.
     * @param str  the string to be tested
     * @return whether the string is a non-negative integer or not
     */
    public static boolean isNonNegativeInteger(String str)
    {
    	try{
            if(Long.parseLong(str)<0)
                return false;
    	}
    	catch(Exception e){
    	    return false;
    	}
    	return true;
    }

//    /**
//     * Returns the assembly name and file name.
//     * ie. if path is "/.../k57/abc.fa" then return 'k57/abc.fa'
//     *
//     * @param path  the path of the file
//     */
//    public static String getAssemblyNameFromPath(String path)
//    {
//    	// ie. path "/.../k57/abc.fa" should return 'k57/abc.fa'
//    	if(path == null || path.isEmpty())
//            return null;
//
//    	// ie. path ".../k57/..." should return 57
//    	int indexOfSlashInFront = path.lastIndexOf(File.separator + "k");
//    	int length = path.length();
//    	while(indexOfSlashInFront >= 0)
//    	{
//            int i = indexOfSlashInFront+2;
//            while(i < length && isNonNegativeInteger( Character.toString(path.charAt(i))) )
//            {
//                i++;
//            }
//
//            if(i != indexOfSlashInFront+2)
//            {
//                return path.substring(indexOfSlashInFront+1);
//            }
//
//            indexOfSlashInFront = path.lastIndexOf(File.separator + "k", indexOfSlashInFront-1);
//    	}
//
//    	return null;
//    }

    /**
     * Extract the value of 'k' from the the path of the file.
     * @param path  the cannonical path of the file
     * @pre path must be in the format of "/.../kXX/..." where XX is a positive integer
     * @return the value of 'k' if found; -1 otherwise
     */
    public static int getKFromPath(String path)
    {
    	if(path == null || path.isEmpty())
            return -1;

    	// ie. path ".../k57/..." should return 57
    	int indexOfSlashInFront = path.lastIndexOf(File.separator + "k");
    	int length = path.length();
    	while(indexOfSlashInFront >= 0)
    	{
            int i = indexOfSlashInFront+2;
            while(i < length && isNonNegativeInteger( Character.toString(path.charAt(i))) )
            {
                i++;
            }

            if(i != indexOfSlashInFront+2)
            {
                String kStr = path.substring(indexOfSlashInFront+2, i);
                return Integer.parseInt(kStr);
            }

            indexOfSlashInFront = path.lastIndexOf(File.separator + "k", indexOfSlashInFront-1);
    	}

    	return -1;
    }

    /**
     * Extract the value of 'k' from the assembly name.
     * @param name  the assembly name
     * @pre name  must be in the format of "kXX??" where 'XX' is a positive integer and '??' is a string.
     * @return the value of 'k' if found; -1 otherwise
     */
    public static int getKValueFromAssemblyName(String name)
    {
    	// value of "k" is the first integer in the name

    	if(name == null || name.isEmpty())
    		return -1;

    	int indexToStop = -1;

    	int len = name.length();
    	for(int i=1; i<len; i++)
    	{
    		String c = Character.toString(name.charAt(i));
    		if(isNonNegativeInteger(c))
    		{
    			indexToStop = i;
    		}
    		else
    		{
    			break;
    		}
    	}

    	if(indexToStop > 1)
    	{
    		if(indexToStop+1 <= len -1)
    			return Integer.parseInt(name.substring(1,indexToStop+1));
    		else
    			return Integer.parseInt(name.substring(1));
    	}

    	return -1;
    }


    /*
     * Example 1:
     * workingDir: /projects/ABySS/assemblies/SJ026/abyss-1.1.1
     * sample name: SJ026
     *
     * Example 2:
     * workingDir: /projects/ABySS/assemblies/CE0071
     * sample name: CE0071
     */
    public static String getSampleNameFromPath(String workingDir)
    {
        String[] fields = workingDir.split(File.separator);

        for(int i=0; i< fields.length; i++)
        {
            if(fields[i].equals("assemblies"))
            {
                return fields[i+1];
            }
        }

        return null;
    }

    public static int getAdjustedLengthFromLineInADJFile(String line, int k) {
        int adjustment = k - 1;
        String[] semiColonSplit = line.split(";");
        int adjustedLength = -1;
        int numOuts = 0;
        int numIns = 0;
        if(semiColonSplit.length == 3){ // ABySS 1.1.2
            String[] idLength = semiColonSplit[0].trim().split(" ");
            String[] outgoingEdges = semiColonSplit[1].trim().split(" ");
            String[] incomingEdges = semiColonSplit[2].trim().split(" ");

            adjustedLength = Integer.parseInt(idLength[1]);
            numOuts = outgoingEdges.length;
            numIns = incomingEdges.length;
        }
        else if(semiColonSplit.length == 2){ // ABySS 1.1.1 and earlier
            String[] idLengthAndOutgoingEdges = semiColonSplit[0].trim().split(" ");
            String[] incomingEdges = semiColonSplit[1].trim().split(" ");

            adjustedLength = Integer.parseInt(idLengthAndOutgoingEdges[1]); // initialize to the contig length in number of bp
            numOuts = idLengthAndOutgoingEdges.length - 2; // -1 for contig id and -1 for contig length
            numIns = incomingEdges.length;
        }

        if (numOuts == 1) // out-degree = 1
        {
            adjustedLength -= adjustment;
        }

        if (numIns == 1) // in-degree = 1
        {
            adjustedLength -= adjustment;
        }

        return adjustedLength;
    }

    public static int getAdjustedLength(String f, String id, int k){
        File file = new File(f);
        if(!file.exists()) {
            return -1;
        }

        if(k <= 0){

            return -1;
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = null;
            while((line = br.readLine()) != null){
                String[] fields = line.split(" ");
                if(fields[0].equals(id)){
                    return getAdjustedLengthFromLineInADJFile(line, k);
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return -1;
        } catch (IOException ex) {
            ex.printStackTrace();
            return -1;
        }
        
        return -1;
    }

    public static int getLengthFromLineInADJFile(String line)
    {
        String[] fields = line.split(" ");

        if(fields.length < 2 || !isNonNegativeInteger(fields[1].trim()))
        {
            return -1;
        }

        return Integer.parseInt(fields[1]);
    }
    
    public static int getLengthFromLineInFAIFile(String line)
    {
        String[] fields = line.split("\t");

        if(fields.length < 2 || !isNonNegativeInteger(fields[1].trim()))
        {
            return -1;
        }

        return Integer.parseInt(fields[1]);
    }    

    public static String getCanonicalPath(String path)
    {
        File f = new File(path);
        if(!f.exists())
        {
            return null;
        }

        String cpath = null;
        try {
            cpath = f.getCanonicalPath();
        } catch (IOException ex) {
            return null;
        }

        return cpath;
    }

    public static String formatDialogMessage(String msg, int numCharPerLine)
    {
        if(msg == null)
        {
            return null;
        }

        //int numCharPerLine = 50;
        int msgLength = msg.length();

        if(msgLength <= numCharPerLine)
        {
            return msg;
        }

        String newMsg = "";

        String[] lineArr = msg.split("\n");
        int numLines = lineArr.length;
        for(int j=0; j<numLines; j++)
        {

            String line = "";
            String[] wordArr = lineArr[j].split(" ");

            int numWords = wordArr.length;
            for(int i=0; i<numWords; i++)
            {
                String word = wordArr[i];

                if(word.length() >= numCharPerLine)
                {
                    int k=0;
                    while(k<word.length())
                    {
                        int newk = Math.min( k+Math.min(numCharPerLine, numCharPerLine-line.length()) , word.length() );
                        line += word.substring(k, newk);

                        if(line.length() >= numCharPerLine)
                        {
                            newMsg += line + '\n';
                            line = "";
                        }
                        else
                        {
                            line += ' ';
                        }
                        k = newk;
                    }
                }
                else
                {
                    if(line.length() + word.length() > numCharPerLine)
                    {
                        newMsg += line + '\n';
                        line = "";
                    }

                    line += word + ' ';
                }

                if(i>=numWords-1)
                {
                    newMsg += line;
                    line = "";
                }
            }

            if(j<numLines-1)
            {
                newMsg += "\n";
            }

        }

        return newMsg;
    }

    public static String reverseCompliment(String seq)
    {
        char[] arr= seq.toUpperCase().toCharArray();
        int len = arr.length;
        char[] reverseCompliment = new char[len];

        for(int i=0; i<len; i++)
        {
            char c = arr[i];
            switch(c)
            {
                case 'A':
                    reverseCompliment[len-1-i] = 'T';
                    break;
                case 'T':
                    reverseCompliment[len-1-i] = 'A';
                    break;
                case 'C':
                    reverseCompliment[len-1-i] = 'G';
                    break;
                case 'G':
                    reverseCompliment[len-1-i] = 'C';
                    break;
                default:
                    reverseCompliment[len-1-i] = c;
                    break;
            }
        }

        return new String(reverseCompliment);
    }


    public static int numInstances(char c, String str)
    {
        char[] arr = str.toCharArray();
        int count =0;
        for(char character : arr)
        {
            if(character == c)
                count++;
        }
        return count;
    }

    public static int isAbyssDotFile(File f) throws InterruptedException{
        int k = -1;
        DotParser dotParser = new DotParser();
        try{
            dotParser.open(f);
            while (true) {
                Object o = dotParser.parseNextLine();
                if (o == null) { break; } // end of file
                k = dotParser.getK();
                if(k > 0){
                    break;
                }
            }
        } catch (IOException e) {
            k = -1;
        }

        return k;
    }

    /*
     * A directory is an ABySS assembly directory if
     * it contains the value of k from either
     * i) a DOT file
     * ii) the directory name (ie. "k25" for k = 25) but this directory must contain one ore more *.fa or *.adj files
     *
     * A positive k value returned means the directory is an ABySS assembly directory
     * Otherwise, -1 is returned.
     */
    public static int isAbyssAssemblyDirectory(File f) throws InterruptedException{
        int k = -1;
        if(f.isDirectory()){

            int dotK = -1;
            File[] dotFiles = f.listFiles(new FileNameRegexFilter(".*\\.dot"));
            if(dotFiles != null && dotFiles.length > 0){
                for(File aDotFile : dotFiles){
                    int myK = -1;
                    DotParser dotParser = new DotParser();
                    try{
                        dotParser.open(aDotFile);
                        while (true) {
                            Object o = dotParser.parseNextLine();
                            if (o == null) { break; } // end of file
                            myK = dotParser.getK();
                            if(myK > 0){
                                break;
                            }
                        }
                    } catch (IOException e) {
                        continue; //not an ABySS DOT file, ignore this file
                    }

                    if(dotK == -1){
                        dotK = myK;
                    }
                    else{
                        if(dotK != myK){
                            //there is discrepancy in k values; no concensus k value in the directory
                            dotK = -1;
                            break;
                        }
                    }
                }
            }

            if(dotK == -1){ // k not found in the dot file(s)
                // get k from the name
                int nameK = getKValueFromAssemblyName(f.getName());
                if(nameK > 0){
                    //check whether it has *.fa or *.adj files
                    File[] abyssFiles = f.listFiles(new AbyssFilesFilter());
                    if(abyssFiles.length > 0){
                        k = nameK;
                    }
                }
                else{
                    try{
                        File cfile = f.getCanonicalFile();
                        nameK = getKValueFromAssemblyName(cfile.getName());
                        
                        //check whether it has *.fa or *.adj files
                        File[] abyssFiles = f.listFiles(new AbyssFilesFilter());
                        if(abyssFiles.length > 0){
                            k = nameK;
                        }
                    }
                    catch(IOException e){
                        
                    }                    
                }
            }
            else{
                k = dotK;
            }
        }
        return k;
    }

    public static class CellRenderer extends DefaultTableCellRenderer{
        protected int fontStyle = Font.PLAIN;
        public CellRenderer(Color foreground, Color background, int alignment, int fontStyle){
            super();
            this.fontStyle = fontStyle;
            setHorizontalAlignment(alignment);
            if(foreground != null){
                setForeground(foreground);
            }
            if(background != null){
                setBackground(background);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                        boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            Font defaultFont = this.getFont();
            Font font = new Font(defaultFont.getFontName(),fontStyle,defaultFont.getSize());
            cell.setFont(font);
            return cell;
        }
    }

    public static boolean isSymbolicLink(File link){
        try{
            File absFile = link.getAbsoluteFile();
            File canFile = link.getCanonicalFile();
            return !absFile.getName().equals(canFile.getName());
        } catch (IOException e){
            return false;
        }        
    }

    public static File equivalentFile(File symlink, Collection<File>files){
        try{
            String queryCPath = symlink.getCanonicalPath();

            for(File f : files){
                if(!isSymbolicLink(f) && !f.equals(symlink) && queryCPath.equals(f.getCanonicalPath())){
                    return f;
                }
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }

        return null;
    }

    public static boolean hasSymbolicLinkInPath(File link){
        try {
            String absPath = link.getAbsolutePath();
            String canPath = link.getCanonicalPath();
            return !absPath.equals(canPath);
        } catch (IOException ex) {
            return false;
        }
    }

    public static String getLongestCommonPrefix(Collection<String> strCollection){
        String[] strArr = new String[strCollection.size()];
        strArr = strCollection.toArray(strArr);

        if(strArr.length == 1){
            return strArr[0];
        }

        String longestCommonPrefix = "";
        boolean done = false;
        int charAtIndex = 0;

        while(!done){
            String firstStr = strArr[0];
            if(charAtIndex == firstStr.length()){
                done = true;
                break;
            }

            char commonChar = firstStr.charAt(charAtIndex);
            for(int i=1; i<strArr.length; i++){
                String ithStr = strArr[i];
                if(charAtIndex == ithStr.length() || ithStr.charAt(charAtIndex) != commonChar){
                    done = true;
                    break;
                }
            }
            
            if(!done){
                longestCommonPrefix += commonChar;
                charAtIndex++;
            }            
        }

        return longestCommonPrefix;
    }
    
}

