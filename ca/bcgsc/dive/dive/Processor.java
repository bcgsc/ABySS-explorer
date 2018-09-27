
package ca.bcgsc.dive.dive;

import ca.bcgsc.dive.stat.N50stats;
import java.io.*;
import java.util.ArrayList;
import ca.bcgsc.dive.util.*;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 *
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 */
public final class Processor {

    public static long calculateContiguity2(String path, long mcl, int k, int unit)
            throws FileNotFoundException, IOException, InterruptedException{
        long sos = 0;

        BufferedReader br = new BufferedReader(new FileReader(path));
        String line = null;

            if ((line = br.readLine()) != null) {
                if (line.startsWith(">")) // FASTA format (*.fa)
                {
                        while(line != null) {
                            if(Thread.interrupted()){
                                throw new java.lang.InterruptedException();
                            }

                            int scaffoldLength = 0;
                            while((line = br.readLine()) != null) {
                                if(Thread.interrupted()){
                                    throw new java.lang.InterruptedException();
                                }

                                if(line.startsWith(">")) {
                                    break;
                                }
                                int[] arr = Utilities.getScaffoldLength(line);
                                scaffoldLength += arr[0];
                            }
                            if(scaffoldLength >= mcl){

                                long length = scaffoldLength;
                                if(unit == N50stats.BP || (unit == N50stats.KMER && length >= k) || (unit == N50stats.NR_KMER && length >= 2*k-1))
                                {
                                    if(unit == N50stats.KMER)
                                    {
                                        length -= k-1;
                                    }
                                    else if(unit == N50stats.NR_KMER)
                                    {
                                        length = length -2*(k-1);
                                    }

                                    // no adjustments needed for BP

                                    sos += length*length;
                                }

                            }
                        }
                }
                else //must be an ADJ file (*.adj)
                {
                    //isADJFile = true;

                    if (unit == N50stats.NOLBP) {

                        if(Utilities.getLengthFromLineInADJFile(line) >= mcl)
                        {
                            int length = Utilities.getAdjustedLengthFromLineInADJFile(line, k);
                            if (length > 0) {

                                long lengthL = (long) length;
                                sos += lengthL * lengthL;
                            }
                        }

                        // get contig lengths from rest of the file
                        while ((line = br.readLine()) != null) {
                            if(Thread.interrupted()){
                                throw new java.lang.InterruptedException();
                            }

                            if(Utilities.getLengthFromLineInADJFile(line) >= mcl)
                            {
                                int length = Utilities.getAdjustedLengthFromLineInADJFile(line, k);
                                if (length > 0) {

                                    long lengthL = (long) length;
                                    sos += lengthL * lengthL;
                                }
                            }
                        }
                    }
                    else {
                        String[] fields = line.split(" ");
                        Integer length = new Integer(fields[1]);
                        if(length >= mcl){

                            long lengthL = (long) length;
                            sos += lengthL * lengthL;

                            fields = null;
                        }

                        // get contig lengths from rest of the file
                        while ((line = br.readLine()) != null) {
                            if(Thread.interrupted()){
                                throw new java.lang.InterruptedException();
                            }

                            fields = line.split(" ");
                            length = new Integer(fields[1]);
                            if(length >= mcl){

                                long lengthL = (long) length;
                                sos += lengthL * lengthL;
                            }
                        }
                    }
                }
            }

        return (long) Math.rint(Math.sqrt(sos));
    }

//    public static long calculateContiguity(long mcl, String path, boolean useScaffold) throws FileNotFoundException, IOException
//    {
//        long sos = 0;
//
//        BufferedReader br = new BufferedReader(new FileReader(path));
//        String line = null;
//
//            if ((line = br.readLine()) != null) {
//                if (line.startsWith(">")) // FASTA format (*.fa)
//                {
//                    if(useScaffold)
//                    {
//                        String[] fields = line.split(" ");
//                        boolean needToCount = true;
//
//                        if (fields.length >= 2 && Utilities.isNonNegativeInteger(fields[1])) {
//                            long length = new Long(fields[1]); // the second field is the contig length
//                            if(length >= mcl)
//                            {
//                                sos += length*length;
//                            }
//                            fields = null;
//                            needToCount = false;
//                        }
//
//                        // get contig lengths from rest of the file
//                        long count = 0;
//                        while ((line = br.readLine()) != null) {
//                            if (line.startsWith(">")) {
//                                if (count >= mcl) {
//                                    sos += count*count;
//                                }
//                                count = 0; // reset the count
//                                fields = line.split(" ");
//                                needToCount = true;
//
//                                if (fields.length >= 2 && Utilities.isNonNegativeInteger(fields[1])) {
//                                    int length = new Integer(fields[1]);
//                                    if(length >= mcl)
//                                    {
//                                        sos += length*length;
//                                    }
//                                    needToCount = false;
//                                }
//                                else {
//	                            count += Utilities.getScaffoldSpan(line);
//	                        }
//                            } else if (needToCount) {
//                                count += Utilities.getScaffoldSpan(line);
//                            }
//                        }
//                        if (count > mcl) {
//                            sos += count*count;
//                        }
//                    }
//                    else
//                    {
//                        String[] fields = line.split(" ");
//                        Integer scaffoldLength = null;
//                        if (fields.length >= 2 && Utilities.isNonNegativeInteger(fields[1])) {
//                            scaffoldLength = new Integer(fields[1]);
//                            fields = null;
//
//                            int numN = 0;
//                            while((line = br.readLine()) != null)
//                            {
//                                if(line.startsWith(">"))
//                                {
//                                    break;
//                                }
//                                numN += Utilities.countN(line);
//                            }
//                            int length = scaffoldLength - numN;
//                            if(length >= mcl)
//                            {
//                                sos += length*length;
//                            }
//                        }
//                        while(line != null)
//                        {
//                            fields = line.split(" ");
//                            if (fields.length >= 2 && Utilities.isNonNegativeInteger(fields[1])) {
//                                scaffoldLength = new Integer(fields[1]);
//                                fields = null;
//
//                                int numN = 0;
//                                while((line = br.readLine()) != null)
//                                {
//                                    if(line.startsWith(">"))
//                                    {
//                                        break;
//                                    }
//                                    numN += Utilities.countN(line);
//                                }
//                                int length = scaffoldLength - numN;
//                                if(length >= mcl)
//                                {
//                                    sos += length*length;
//                                }
//                            }
//                            else
//                            {
//                                int contigLength = Utilities.countNucleotides(line);
//                                while((line = br.readLine()) != null)
//                                {
//                                    if(line.startsWith(">"))
//                                    {
//                                        break;
//                                    }
//                                    contigLength += Utilities.countNucleotides(line);
//                                }
//                                if(contigLength >= mcl)
//                                {
//                                    sos += contigLength*contigLength;
//                                }
//                            }
//                        }
//                    }
//                }
//                else //must be an ADJ file (*.adj)
//                {
//                    String[] fields = line.split(" ");
//                    int length = new Integer(fields[1]); // the second field is the contig length
//                    if(length >= mcl)
//                    {
//                        sos += length*length;
//                    }
//                    fields = null;
//
//                    // get contig lengths from rest of the file
//                    while ((line = br.readLine()) != null) {
//                        fields = line.split(" ");
//                        length = new Integer(fields[1]);
//                        if(length >= mcl)
//                        {
//                            sos += length*length;
//                        }
//                    }
//                }
//            }
//
//        return (long) Math.rint(Math.sqrt(sos));
//    }

//    /*dir must be a working directory*/
//    public static String getBestAssemblyPath(String dir)
//    {
//        File workDir = new File(dir);
//        String[] kDirNames = workDir.list(new KDirectoryNameFilter());
//        int numKDir = kDirNames.length;
//
//        long maxContiguity = -1;
//        String bestAssemblyPath = null;
//
//        String fileName = null;
//
//        for(int j=0; j<numKDir; j++)
//        {
//            try {
//                String path = dir + File.separator + kDirNames[j];
//
//                if(fileName == null){
//                    File kDir = new File(path);
//                    String[] names = kDir.list(new NameSuffixFilenameFilter("-contigs.fa"));
//                    if(names.length == 0){
//                        continue;
//                    }
//                    fileName = names[0];
//                }
//
//                String contigFilePath = path + File.separator + fileName;
//
//                if(Utilities.fileExist(contigFilePath)){
//                    long contiguity = calculateContiguity(100, contigFilePath, true);
//                    if (contiguity > maxContiguity) {
//                        maxContiguity = contiguity;
//                        bestAssemblyPath = path;
//                    }
//                }
//            } catch (FileNotFoundException ex) {
//                ex.printStackTrace();
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//
//                //            int k = Utilities.getKFromPath(path);
//                //            N50stats stats = new N50stats(200);
//                //            String contigFilePath = path + File.separator + Utilities.getSampleNameFromPath(path) + "-contigs.fa";
//                //            try {
//                //                stats.readFile(contigFilePath, k, N50stats.KMER, true);
//                //                long contiguity = stats.getContiguity();
//                //
//                //                if(contiguity > maxContiguity)
//                //                {
//                //                    maxContiguity = contiguity;
//                //                    bestAssemblyPath = path;
//                //                }
//                //
//                //            } catch (Exception e) {
//                //                //do nothing
//                //                e.printStackTrace();
//                //            }
//        }
//
//        return bestAssemblyPath;
//    }

    public static ArrayList<String> splitIntoChunks(String query)
    {
        if(query == null)
            return null;

        ArrayList<String> list = new ArrayList<String>();

        int startIndex = 0;
        int currentIndex = 0;
        int len = query.length();


        if(len <= 8000)
        {
            int count = 25;

            while(currentIndex+1 < len && (currentIndex = query.indexOf(">", currentIndex+1)) > 0)
            {
                count--;

                if(count == 0)
                {
                    String chunk = query.substring(startIndex, currentIndex);
                    list.add(chunk);

                    // reset the values
                    startIndex = currentIndex;
                    count = 25;
                }
            }

            if(count > 0)
            {
                list.add(query.substring(startIndex));
            }
        }
        else
        {
            int numSeq = 0;
            String chunk = "";

            while(currentIndex+1 < len && (currentIndex = query.indexOf(">", startIndex+1)) > 0)
            {
                numSeq++;
                int seqLen = currentIndex - startIndex;

                // ignore all sequences that are longer than 8000 chars
                if(seqLen <= 8000)
                {
                    if(chunk.length() + seqLen <= 8000 && numSeq <= 25)
                    {
                        chunk += query.substring(startIndex, currentIndex);
                    }
                    else
                    {
                        list.add(chunk);
                        numSeq = 1;
                        chunk = query.substring(startIndex, currentIndex);
                    }
                }
                else{
                    String header = query.substring(startIndex, query.indexOf("\n", startIndex));
                    System.out.println("\"" + header + " ...\" is too long.");
                }

                startIndex = currentIndex;
            }

             int seqLen = len - startIndex;

//             // ignore all sequences that are longer than 8000 chars
//             if(seqLen <= 8000)
//             {
                 if(chunk.length() + seqLen <= 8000)
                 {
                     chunk += query.substring(startIndex);
                     list.add(chunk);
                 }
                 else
                 {
                     list.add(chunk);

                     if(seqLen <= 8000){
                        list.add( query.substring(startIndex) );
                     }
                    else{
                        String header = query.substring(startIndex, query.indexOf("\n", startIndex));
                        System.out.println("\"" + header + " ...\" is too long.");
                    }
                 }
//             }
        }

        return list;
    }

//    private static int findFirstSquareLarger(int n)
//    {
//        int q = 1;
//        while(n > q*q)
//        {
//            q++;
//        }
//        return q;
//    }
//
//    public static void tileAllFrames(JDesktopPane desktopPane)
//    {
//        JInternalFrame[] frames = desktopPane.getAllFrames();
//        int numFrames = frames.length;
//
//        if(numFrames > 0)
//        {
//            int h = findFirstSquareLarger(numFrames);
//            int w = h;
//            if(numFrames <= h*(h-1))
//            {
//                w = h-1;
//            }
//
//            int r = 0;
//            int c = 0;
//            int width = desktopPane.getWidth()/w;
//            int height = desktopPane.getHeight()/h;
//
//            for(int i=0; i<numFrames; i++)
//            {
//                JInternalFrame frame = frames[i];
//                try {
//                    frame.setMaximum(false);
//                    frame.setIcon(false);
//                } catch (PropertyVetoException ex) {
//                    ex.printStackTrace();
//                }
//
//                int x = c*width;
//                int y = r*height;
//                frame.setBounds(x, y, width, height);
//
//                r++;
//                if(r >= h)
//                {
//                    r=0;
//                    c++;
//                }
//            }
//        }
////        else
////        {
////            String msg = "There are no windows!";
////            JOptionPane.showMessageDialog(desktopPane, msg, "Error", JOptionPane.ERROR_MESSAGE);
////        }
//    }

//    public static void minimizeAllFrames(JDesktopPane desktopPane)
//    {
//        JInternalFrame[] frames = desktopPane.getAllFrames();
//        int numFrames = frames.length;
//        if(numFrames > 0)
//        {
//            for(int i=0; i<numFrames; i++)
//            {
//                try {
//                    frames[i].setIcon(true);
//                } catch (PropertyVetoException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }
////        else
////        {
////            String msg = "There are no windows!";
////            JOptionPane.showMessageDialog(desktopPane, msg, "Error", JOptionPane.ERROR_MESSAGE);
////        }
//    }

//    public static void restoreAllFrames(JDesktopPane desktopPane)
//    {
//        JInternalFrame[] frames = desktopPane.getAllFrames();
//        int numFrames = frames.length;
//        if(numFrames > 0)
//        {
//            for(int i=numFrames-1; i>=0; i--)
//            {
//                JInternalFrame f = frames[i];
//
//                try {
//                    f.setIcon(false);
//                } catch (PropertyVetoException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }
////        else
////        {
////            String msg = "There are no windows!";
////            JOptionPane.showMessageDialog(desktopPane, msg, "Error", JOptionPane.ERROR_MESSAGE);
////        }
//    }

//    public static void cascadeAllFrames(JDesktopPane desktopPane)
//    {
//        JInternalFrame[] frames = desktopPane.getAllFrames();
//        int numFrames = frames.length;
//        if(numFrames > 0)
//        {
//            int shift = 20;
//            for(int i=numFrames-1; i>=0; i--)
//            {
//                JInternalFrame f = frames[i];
//                int x = (numFrames-i-1) * shift;
//                f.setLocation(x, x);
//                try {
//                    f.setIcon(false);
//                } catch (PropertyVetoException ex) {
//                    ex.printStackTrace();
//                }
//
//                //desktopPane.setSelectedFrame(f);
//            }
//        }
////        else
////        {
////            String msg = "There are no windows!";
////            JOptionPane.showMessageDialog(desktopPane, msg, "Error", JOptionPane.ERROR_MESSAGE);
////        }
//    }

//    public static void disposeAllFrames(JDesktopPane desktopPane)
//    {
//        JInternalFrame[] frames = desktopPane.getAllFrames();
//        int numFrames = frames.length;
//        if(numFrames > 0)
//        {
//            String msg = "These windows can not be recovered if they are disposed.\nAre you sure you want to continue?";
//            int value = JOptionPane.showConfirmDialog(desktopPane.getTopLevelAncestor(), msg, "Confirm disposing all windows", JOptionPane.YES_NO_OPTION);
//
//            if(value == JOptionPane.YES_OPTION)
//            {
//                for(int i=0; i<numFrames; i++)
//                {
//                    frames[i].dispose();
//                }
//            }
//        }
////        else
////        {
////            String msg = "There are no windows!";
////            JOptionPane.showMessageDialog(desktopPane, msg, "Error", JOptionPane.ERROR_MESSAGE);
////        }
//    }

    /**
     * path can be either:
     * 1. the path of a k-directory
     * 2. the path of a FASTA file
     */
    public static Set<SearchResults> findContigsInPath(String path, String[] contigIDs)
    {
        File f = new File(path);

        ArrayList<String> contigsIdList = new ArrayList<String>(contigIDs.length);
//        ArrayList<String> seqList = new ArrayList<String>(contigIDs.length);
        HashSet<SearchResults> results = new HashSet<SearchResults>(contigIDs.length);
        
        for(int i=0; i<contigIDs.length;i++)
        {
            String id = contigIDs[i].trim();
            if(id.endsWith("+"))
            {
                id = id.substring(0, id.length()-1);
            }

            if(!contigsIdList.contains(id))
            {
                contigsIdList.add(id);
            }
        }

        if(f.isFile())
        {
            results.add( findContigsInFile(path, contigsIdList) );
            return results;
        }
        else if(f.isDirectory())
        {
            String[] faFiles = f.list(new FileNameRegexFilter(".*-[345678]\\.fa"));

            for(String fileName : faFiles){
                if((new File(path + File.separator + fileName)).isFile()){
                    SearchResults r = findContigsInFile(path + File.separator + fileName, contigsIdList);
                    if(r.seqs != null && r.seqs.size() > 0){
                        results.add(r);                        
                    }
                }
            }

//            if(results != null && results.size() > 0 && contigsIdList.size() <= 0){
//                return results;
//            }
//
//            String[] dashFourDotFa = f.list(new NameSuffixFilenameFilter("-4.fa"));
//
//            for(String fileName : dashFourDotFa){
//                SearchResults r = findContigsInFile(path + File.separator + fileName, contigsIdList);
//                if(r.seqs != null && r.seqs.size() > 0){
//                    results.add(r);
//                }
//            }

            if(results != null && results.size() > 0){
                return results;
            }
        }

        return null;
    }

    public static class SearchResults{
        public String fileName;
        public Set<String> contigIds;
        public Set<String> seqs;

        public SearchResults(String FileName, Set<String> contigIds, Set<String> seqs){
            this.fileName = FileName;
            this.contigIds = contigIds;
            this.seqs = seqs;
        }
    }

    public static SearchResults findContigsInFile(String path, ArrayList<String> contigIDs)
    {
        int count = contigIDs.size();
        ArrayList<String> heads = new ArrayList<String>(count);
        HashSet<String> seqs = new HashSet<String>(count);
        HashSet<String> contigIds = new HashSet<String>();

        for(int i=0; i<count; i++)
        {
            String id = contigIDs.get(i);

            if(id.endsWith("-"))
            {
                id = id.substring(0, id.length()-1);
            }
            heads.add('>' + id + ' ');
        }

        File file = new File(path);
        //String seq = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = null;
            boolean dontRead = false;
            while ( dontRead ||
                    (line = br.readLine()) != null ) {

                dontRead = false;
                int numHeads = heads.size();
                ArrayList<Integer> indexesToRemove = new ArrayList<Integer>();
                String sharedSequence = null;                
                for(int i=0; i<numHeads; i++)
                {
                    if(line.startsWith(heads.get(i)))
                    {
                        String seq = null;

                        String id = contigIDs.get(i);
                        if(id.endsWith("-"))
                        {
                            int firstSpaceIndex = line.indexOf(" ");
                            String headerLine = line.substring(0,firstSpaceIndex) + '-' + line.substring(firstSpaceIndex);

//                            if(seq == null)
//                            {
                                seq = headerLine;
//                            }
//                            else
//                            {
//                                seq += '\n' + headerLine;
//                            }

                            if(sharedSequence == null)
                            {
                                // get sequence
                                String originalSeq = "";
                                String sequenceLine = null;
                                while((sequenceLine = br.readLine()) != null) {
                                    if(sequenceLine.startsWith(">"))
                                    {
                                        dontRead = true;
                                        line = sequenceLine;
                                        break;
                                    }

                                    originalSeq += '\n' + sequenceLine;
                                }
                                sharedSequence = originalSeq;
                            }

                            //get reverse compliment of the sequence
                            String reverseCompliment = Utilities.reverseCompliment(sharedSequence);
                            if(reverseCompliment.endsWith("\n"))
                            {
                                reverseCompliment = reverseCompliment.substring(0, reverseCompliment.length()-1);
                            }
                            seq += '\n' + reverseCompliment;
                        }
                        else
                        {
//                            if(seq == null)
//                            {
                                seq = line;
//                            }
//                            else
//                            {
//                                seq += '\n' + line;
//                            }

                            if(sharedSequence == null)
                            {
                                // get sequence
                                String originalSeq = "";
                                String sequenceLine = null;
                                while((sequenceLine = br.readLine()) != null) {
                                    if(sequenceLine.startsWith(">"))
                                    {
                                        dontRead = true;
                                        line = sequenceLine;
                                        break;
                                    }

                                    originalSeq += '\n' + sequenceLine;
                                }
                                sharedSequence = originalSeq;
                            }

                            seq += sharedSequence;
                        }

                        contigIds.add(id);
                        seqs.add(seq);
                        indexesToRemove.add(i);
                        count--;
                    }
                }

                // remove the contig ids found, so we won't look for them again in the next iteration
                int len = indexesToRemove.size();
                for(int i=len-1; i>=0; i--)
                {
                    int index = indexesToRemove.get(i);
                    contigIDs.remove(index);
                    heads.remove(index);
                }

                if(count <= 0)
                    break;
            }
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        } finally {
            try {
                if(br != null)
                    br.close();
            } catch (IOException ex) {
                return null;
            }
        }

        //return seq;
        return new SearchResults(file.getName(), contigIds, seqs);
    }

    public static boolean isContigID(String str)
    {
        str = str.trim();
        
        if(Utilities.isNonNegativeInteger(str))
        {
            return true;
        }

        if(str.endsWith("+") || str.endsWith("-"))
        {
            if(Utilities.isNonNegativeInteger(str.substring(0,str.length()-1)))
            {
                return true;
            }
        }

        return false;
    }

    //based on code from: http://www.exampledepot.com/egs/javax.swing.table/packcol.html
    public static void packColumns(JTable table, int margin) {
        int n = table.getColumnCount();
        for (int c=0; c<n; c++) {
            packColumn(table, c, margin);
        }
    }

    public static void packColumn(JTable table, int colIndex, int margin) {
        DefaultTableColumnModel colModel = (DefaultTableColumnModel)table.getColumnModel();
        int realCol = table.convertColumnIndexToView(colIndex);
        if(realCol >= 0){
            TableColumn col = colModel.getColumn(realCol);

            // Get width of column header
            TableCellRenderer renderer = col.getHeaderRenderer();
            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }
            Component comp = renderer.getTableCellRendererComponent( table, col.getHeaderValue(), false, false, 0, 0);
            int width = comp.getPreferredSize().width; // Get maximum width of column data

            int n = table.getRowCount();
            for (int r=0; r<n; r++) {
                TableCellRenderer cellRenderer = table.getCellRenderer(r, colIndex);
                comp = cellRenderer.getTableCellRendererComponent( table, table.getValueAt(r, colIndex), false, false, r, colIndex);
                width = Math.max(width, comp.getPreferredSize().width);
            }

            // Add margin to both sides
            width += 2*margin;

            col.setMinWidth(width);
            col.setPreferredWidth(width);
        }
    }


    public static String getAllValuesFromTable(JTable table){
        String str = "";

        int numRows = table.getRowCount();
        int numCols = table.getColumnCount();        
        String[][] strTable = new String[numRows+1][numCols];

        TableColumnModel columnModel = table.getColumnModel();

        int[] maxLengths = new int[numCols];

        // read table header into 2D array
        for(int c=0; c<numCols; c++){
            Object value = columnModel.getColumn(c).getHeaderValue();
            int length = 0;
            if(value != null){
                String valAsStr = value.toString();
                length = valAsStr.length();
                strTable[0][c] = valAsStr;
            }
            else{
                strTable[0][c] = "";
            }

            if(length > maxLengths[c]){
                maxLengths[c] = length;
            }            
        }

        // read table into 2D array
        for(int r=0; r<numRows; r++){
            for(int c=0; c<numCols; c++){
                Object value = table.getValueAt(r, c);
                int length = 0;
                if(value != null){
                    String valAsStr = value.toString();
                    length = valAsStr.length();
                    strTable[r+1][c] = valAsStr;
                }
                else{
                    strTable[r+1][c] = "";
                }

                if(length > maxLengths[c]){
                    maxLengths[c] = length;
                }
            }
        }

        // concatenate all cells in the table and add spaces if needed
        for(int r=0; r<numRows+1; r++){
            for(int c=0; c<numCols; c++){
                String value = strTable[r][c];
                int numSpaceToFill = maxLengths[c] - value.length();
                str += value;
                while(numSpaceToFill > 0){
                    str += " ";
                    numSpaceToFill--;
                }
                if(c < numCols-1){
                    str += " ";
                }
            }
            if(r<numRows){
                str += "\n";
            }
        }

        return str;
    }
}
