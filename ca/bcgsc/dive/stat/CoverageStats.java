package ca.bcgsc.dive.stat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 */
public class CoverageStats {

    public static long getMedianKmerCoverage(String path) throws FileNotFoundException, IOException
    {
        ArrayList<Long> histogram = new ArrayList<Long>();
        ArrayList<Long> kmerCoverage = new ArrayList<Long>();

//        try{
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = null;
            
            
            while (( line = br.readLine()) != null)
            {
                String[] arr = line.split("\t");
                kmerCoverage.add(new Long(arr[0]));
                histogram.add(new Long(arr[1]));
            }
            br.close();
//        }
//        catch (IOException ex) {
//            //ex.printStackTrace();
//            return -1;
//        }
        
        return getMedianKmerCoverageHelper(kmerCoverage, histogram);
    }

    private static long getMedianKmerCoverageHelper(ArrayList<Long> kmerCoverage, ArrayList<Long> histogram)
    {
    	int iFLM = getIndexOfMinKmerCoverage(kmerCoverage, histogram);
    	//System.out.println("Minimum k-mer Coverage: " + primaryDataset.getXValue(seriesIndex,iFLM));
    	int startIndex = iFLM;
    	int endIndex = kmerCoverage.size()-1;
    	long oldThreshold = kmerCoverage.get(iFLM);

    	int repeat = 100;
    	int iMedian = -1;
    	long median = -1;
    	while(repeat > 0)
    	{
            //sumFromThreshold = getSum(seriesIndex, startIndex, endIndex);
            //System.out.println("Coverage:" + oldThreshold + ", Reconstruction: " + sumFromThreshold);

            iMedian = getIndexOfMedian(histogram, startIndex, endIndex)-1;
            median = kmerCoverage.get(iMedian);
            long newThreshold = (long) Math.rint(Math.sqrt(median));

            if(oldThreshold == newThreshold)
            {
                //converged

                //System.out.println("Coverage Threshold: " + Math.rint(oldThreshold));
                //System.out.println("Median k-mer coverage: " + median);
                //System.out.println("Reconstruction: " + sumFromThreshold);
                break;
            }

            oldThreshold = newThreshold;
            startIndex = getIndexOfElementLargerThanThreshold(kmerCoverage, oldThreshold);

            repeat--;
    	}

    	return median;
    }

    private static int getIndexOfMinKmerCoverage(ArrayList<Long> kmerCoverage, ArrayList<Long> histogram)
    {
        int len = kmerCoverage.size();
        long min = histogram.get(0);

        int theIndex = 0;
        int count = 0;
    	for(int i=1;i<len;i++)
    	{
            long curr = histogram.get(i);
            if(curr < min)
            {
                theIndex = i;
                min = curr;
                count = 0;
            }
            else if (++count >=4)
            {
                break;
            }
    	}
    	return theIndex;
    }

    private static int getIndexOfMedian(ArrayList<Long> histogram, int firstIndex, int lastIndex)
    {
    	long currSum = 0;
    	long half = (getSum(histogram, firstIndex, lastIndex))/2;
    	int i = 0;
    	for(i=firstIndex; i<lastIndex && currSum<half; i++)
    	{
            currSum += histogram.get(i);
    	}
    	return i;
    }
    
    private static long getSum(ArrayList<Long> list, int firstIndex, int lastIndex)
    {
    	long sum = 0;
    	for(int i=firstIndex; i<=lastIndex; i++)
    	{
            sum += list.get(i);
    	}    	
    	return sum;
    }

    private static int getIndexOfElementLargerThanThreshold(ArrayList<Long> kmerCoverage, long threshold)
    {
    	int len = kmerCoverage.size();
    	for(int i=0; i<=len; i++)
    	{
            if(kmerCoverage.get(i) >= threshold)
                return i;
    	}
    	return -1;
    }

}
