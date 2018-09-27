/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.bcgsc.dive.stat;

import ca.bcgsc.abyssexplorer.parsers.DotParser;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import ca.bcgsc.dive.util.Utilities;
import java.io.File;

/**
 *
 * @author kmnip
 */
public class N50stats {

    public final static int BP = 0;
    public final static int KMER = 1;
    public final static int NR_KMER = 2;
    public final static int NOLBP = 3;

    private long mcl = 1;
    private long n50 = 0;
    private long contiguity = 0;
    private long reconstruction = 0;
    private long span = 0;
    private int n = 0;
    private int nLargerThanOrEqualToMCL = 0;
    private int nLargerThanOrEqualToN50 = 0;
    private long sos = 0;
    // lengthArr = null;

    public N50stats(long mcl)
    {
        this.mcl = mcl;        
    }

    public N50stats(long n50, long contiguity, long reconstruction, int n, int nLargerThanOrEqualToMCL, int nLargerThanOrEqualToN50, long span){
        this.n50 = n50;
        this.contiguity = contiguity;
        this.reconstruction = reconstruction;
        this.n = n;
        this.nLargerThanOrEqualToMCL = nLargerThanOrEqualToMCL;
        this.nLargerThanOrEqualToN50 = nLargerThanOrEqualToN50;
        this.span = span;
    }
    
    public void readFile(String path, int k, int unit, boolean useFai) throws FileNotFoundException, IOException, InterruptedException
    {
        sos = 0;
        reconstruction = 0;
        span = 0;
        nLargerThanOrEqualToMCL = 0;
        
        ArrayList<Integer> lengthArr = new ArrayList<Integer>();
        ArrayList<Integer> spanArr = new ArrayList<Integer>();        
        n = 0;
        
        if(path.endsWith(".fa") || path.endsWith(".fasta")){
            if (useFai && Utilities.fileExist(path + ".fai")){ 
                System.out.println("Reading FASTA index ...");
                BufferedReader br = new BufferedReader(new FileReader(path + ".fai"));
                String line = null;

                // get contig lengths from rest of the file
                while ((line = br.readLine()) != null) {
                    if(Thread.interrupted()){
                        throw new java.lang.InterruptedException();
                    }

                    int length = Utilities.getLengthFromLineInFAIFile(line);
                    if(length >= mcl){
                        lengthArr.add(length);

                        if(unit == KMER  && length >= k){
                            length -= k-1;
                        }                    

                        long lengthL = (long) length;
                        sos += lengthL * lengthL;
                        reconstruction += lengthL;
                        span += lengthL;
                    }
                    n++;
                }
                br.close();
            }
            else{
                BufferedReader br = new BufferedReader(new FileReader(path));
                String line = null;
                int scaffoldLength = 0;
                int numNs = 0; 
                while((line = br.readLine()) != null) {
                    if(Thread.interrupted()){
                        throw new java.lang.InterruptedException();
                    }
                    if(line.startsWith(">")) {
                        n++;
                        if(scaffoldLength >= mcl){
                            lengthArr.add(scaffoldLength);
                            int scaffoldSpan = scaffoldLength + numNs;
                            spanArr.add(scaffoldSpan);

                            long length = scaffoldLength;
                            if(unit == KMER  && length >= k){
                                length -= k-1;
                                scaffoldSpan -= k-1;
                            }

                            sos += length*length;
                            reconstruction += length;
                            span += scaffoldSpan;                            
                        }
                        scaffoldLength = 0;
                        numNs = 0;                        
                        continue;
                    }
                    
                    int[] arr = Utilities.getScaffoldLength(line);
                    scaffoldLength += arr[0];
                    numNs += arr[1];
                }
                br.close();
            }
        }
        else if(path.endsWith(".adj")){
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = null;

            // get contig lengths from rest of the file
            while ((line = br.readLine()) != null) {
                if(Thread.interrupted()){
                    throw new java.lang.InterruptedException();
                }

                int length = Utilities.getLengthFromLineInADJFile(line);
                if(length >= mcl){
                    lengthArr.add(length);

                    if(unit == KMER  && length >= k){
                        length -= k-1;
                    }                    
                    
                    long lengthL = (long) length;
                    sos += lengthL * lengthL;
                    reconstruction += lengthL;
                    span += lengthL;
                }
                n++;
            }
            br.close();
        }
        else if(path.endsWith(".dot")) {
            DotParser dotParser = new DotParser();
            dotParser.open(new File(path));
            while (true) {
                if(Thread.interrupted()){
                    throw new java.lang.InterruptedException();
                }

                int length = dotParser.getNextContigLength();
                if (length < 0) { break; } // end of file
                if(length >= mcl) {
                    lengthArr.add(length); 
                    
                    if(unit == KMER  && length >= k){
                        length -= k-1;
                    }                    
                    
                    long lengthL = (long) length;
                    sos += lengthL * lengthL;
                    reconstruction += lengthL;
                    span += lengthL;
                }
                n++;
            }
            dotParser.close();
        }


            //count number of contigs larger than MCL
            nLargerThanOrEqualToMCL = lengthArr.size();

            contiguity = (long) Math.rint(Math.sqrt(sos));
            long halfReconstruction = reconstruction/2;

            //sort the array
            Collections.sort(lengthArr);

            // calculate N50 and number of contigs longer than it
            long currentSum = 0;
            for(int i=nLargerThanOrEqualToMCL-1; i>=0; i--)
            {
                long length = lengthArr.get(i);

                if(unit == KMER)
                    length -= k-1;

                currentSum += length;
                if(currentSum <= halfReconstruction)
                {
                    nLargerThanOrEqualToN50++;
                    //n50 = length;
                }
                else
                {
                    nLargerThanOrEqualToN50++;
                    n50 = length;
                    break;
                }
            }
        
    }  

    public long getNumContigs()
    {
        return n;
    }

    public long getN50()
    {
        return n50;
    }

    public long getContiguity()
    {
        return contiguity;
    }

    public long getReconstruction()
    {
        return reconstruction;
    }

    public long getSpan()
    {
        return span;
    }

    public long getNumContigsLongerThanOrEqualToMinContigLength()
    {
        return nLargerThanOrEqualToMCL;
    }

    public long getNumContigsLongerThanOrEqualToN50()
    {
        return nLargerThanOrEqualToN50;
    }
}
