package ca.bcgsc.dive.stat;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 * Updated on Jun 16, 2010
 */

public class HistogramStats {
	
	private ArrayList<Long> counts = null;
	private ArrayList<Long> bins = null;
	private int lowerBoundIndex = 0; // index of smallest positive bin that is not an outlier
	private int upperBoundIndex = 0; // index of largest positive bin that is not an outlier
	private int indexOfQ1 = -1;
	private int indexOfMedian = -1;
	private int indexOfQ3 = -1;
	private double mean = 0;
	private double variance = 0;
	private double stdev = 0;
	private double qFactor = 0;
        private HistogramStats statsForNegativeData = null;
        private int percentPlus = 100;
        private boolean flipped = false;
        private int indexOfSmallestPositiveFragmentSize = 0;

        public HistogramStats(ArrayList<Long> bins, ArrayList<Long> counts, boolean calculateMean){
            this(bins, counts, calculateMean, true);
        }

	public HistogramStats(ArrayList<Long> bins, ArrayList<Long> counts, boolean calculateMean, boolean trimOutliers)
	{
		this.bins = bins;
		this.counts = counts;

                indexOfSmallestPositiveFragmentSize = 0;
                for(int i=0; i<bins.size(); i++){
                    if(bins.get(i) > 0L){
                        indexOfSmallestPositiveFragmentSize = i;
                        break;
                    }
                }
                
                long numPositive = getSum(counts, indexOfSmallestPositiveFragmentSize, bins.size()-1); // number of positive fragment sizes
                long numNegative = 0L;
                if(indexOfSmallestPositiveFragmentSize > 0){
                    numNegative = getSum(counts, 0, indexOfSmallestPositiveFragmentSize-1);  // number of negative and zero fragment sizes
                }
                long total = numPositive + numNegative;
                float percentPlusInUntrimmedHistogram = 100.0F* numPositive/total; // percentage of positive fragment sizes

                if(percentPlusInUntrimmedHistogram < 50.0F){
                    int len = bins.size();

                    ArrayList<Long> newCounts = new ArrayList<Long>(len);
                    ArrayList<Long> newBins = new ArrayList<Long>(len);
                    for(int i=len-1; i>=0; i--){
                        newCounts.add(counts.get(i));
                        newBins.add(bins.get(i) * (-1L));
                    }

                    bins = newBins;
                    counts = newCounts;
                    this.bins = bins;
                    this.counts = counts;

                    indexOfSmallestPositiveFragmentSize = len-1 - indexOfSmallestPositiveFragmentSize;
                    percentPlusInUntrimmedHistogram = 100.0F - percentPlusInUntrimmedHistogram;
                    flipped = true; // flag that data has been negated
                }

                percentPlus = (int) Math.rint(percentPlusInUntrimmedHistogram);

                if(trimOutliers){
                    findBoundsIndexes();
                }
                else{
                    lowerBoundIndex = 0;
                    upperBoundIndex = bins.size()-1;
                }

                findPercentiles(lowerBoundIndex, upperBoundIndex);

                if(calculateMean)
                {
                    calculateMean();
                    calculateStdevAndVar();
                }

                double median = (double)bins.get(indexOfMedian);
                double q1 = (double)bins.get(indexOfQ1);
                double q3 = (double)bins.get(indexOfQ3);
		qFactor = median/(double)Math.abs(q3-q1);

                if(((indexOfSmallestPositiveFragmentSize > 0 && bins.get(indexOfSmallestPositiveFragmentSize-1) < 0L)
                        || (indexOfSmallestPositiveFragmentSize > 1 && bins.get(indexOfSmallestPositiveFragmentSize-2) < 0L))
                        && percentPlusInUntrimmedHistogram < 99.0F){
                    
                    int len = indexOfSmallestPositiveFragmentSize;
                    int startingIndex = indexOfSmallestPositiveFragmentSize -1;

//                    // remove the bin with zero fragment sizes if it exists
//                    if(bins.get(startingIndex) == 0L){
//                        startingIndex--;
//                        len--;
//                    }
                    
                    ArrayList<Long> newCounts = new ArrayList<Long>(len);
                    ArrayList<Long> newBins = new ArrayList<Long>(len);

                    // negate the data so the negative fragment sizes becomes positive
                    for(int i=startingIndex; i>=0; i--){
                        newBins.add(bins.get(i)* (-1L));
                        newCounts.add(counts.get(i));
                    }

                    // get the statistics for these negative fragment sizes
                    statsForNegativeData = new HistogramStats(newBins, newCounts, true, trimOutliers);
                }

//		if(trimOutliers){
//                    findBoundsIndexes();
//                }
//                else{
//                    // no outlier trmming needed, so set the bounds to the edge
//                    lowerBoundIndexWithNegativeData = 0;
//                    upperBoundIndex = bins.size()-1;
//
//                    lowerBoundIndexWithoutNegativeData = 0;
//                    if(bins.get(lowerBoundIndexWithoutNegativeData) < 0L){
//                        for(int i=lowerBoundIndexWithoutNegativeData; i<bins.size(); i++){
//                            if(bins.get(i) >= 0L){
//                                lowerBoundIndexWithoutNegativeData = i;
//                                break;
//                            }
//                        }
//                    }
//                }
//
//		findPercentiles(lowerBoundIndexWithNegativeData, upperBoundIndex);
//
//                /* if the majority (over 50%) of fragment sizes are negative, then negate the data (ie. (+) fragment sizes becomes (-) and vice versa)*/
//                if(percentPlusInUntrimmedHistogram < 50.0F){
//                    int len = bins.size();
//
//                    int tmp = upperBoundIndex;
//                    lowerBoundIndexWithoutNegativeData = len-1 - lowerBoundIndexWithoutNegativeData;
//                    upperBoundIndex = len-1 - lowerBoundIndexWithNegativeData;
//                    lowerBoundIndexWithNegativeData = len-1 - tmp;
//
//                    ArrayList<Long> newCounts = new ArrayList<Long>(len);
//                    ArrayList<Long> newBins = new ArrayList<Long>(len);
//                    for(int i=len-1; i>=0; i--){
//                        newCounts.add(counts.get(i));
//                        newBins.add(bins.get(i) * (-1L));
//                    }
//
//                    bins = newBins;
//                    counts = newCounts;
//                    this.bins = bins;
//                    this.counts = counts;
//
//                    flipped = true; // flag that data has been negated
//                }
//
//                /* if there are negative fragment sizes*/
//                if(lowerBoundIndexWithoutNegativeData != lowerBoundIndexWithNegativeData){
//                    // get the percentiles from the non-negative fragment sizes only
//                    findPercentiles(lowerBoundIndexWithoutNegativeData, upperBoundIndex);
//
////                    long numPositive = getSum(counts, lowerBoundIndexWithoutNegativeData, bins.size()-1); // number of positive fragment sizes
////                    long numNegative = getSum(counts, 0, lowerBoundIndexWithoutNegativeData-1);  // number of negative and zero fragment sizes
////                    long total = numPositive + numNegative;
////
////                    percentPlus = (int) Math.rint(100.0* numPositive/total); // percentage of non-negative fragment sizes
//
//                    if(numNegative > 0.01*total){ // more than 1% fragment sizes are negative
//                        // negate the data so the negative fragment sizes becomes positive
//                        int len = lowerBoundIndexWithoutNegativeData -1 - lowerBoundIndexWithNegativeData + 1;
//                        ArrayList<Long> newCounts = new ArrayList<Long>(len);
//                        ArrayList<Long> newBins = new ArrayList<Long>(len);
//                        for(int i=lowerBoundIndexWithoutNegativeData -1; i>=lowerBoundIndexWithNegativeData; i--){
//                            newBins.add(bins.get(i)* (-1L));
//                            newCounts.add(counts.get(i));
//                        }
//
//                        // get the statistics for these negative fragment sizes
//                        statsForNegativeData = new HistogramStats(newBins, newCounts, true, false);
//                    }
//                }
//
//                if(calculateMean)
//                {
//                    calculateMean();
//                    calculateStdevAndVar();
//                }
//
//                double median = (double)bins.get(indexOfMedian);
//                double q1 = (double)bins.get(indexOfQ1);
//                double q3 = (double)bins.get(indexOfQ3);
//		qFactor = median/(double)Math.abs(q3-q1);
	}
	
	/*
	 * Find the indexes of the bins containing Q1, Q3 in the 'bins' array.
	 * @pre counts and bins are non-empty
	 */
	private void findBoundsIndexes()
	{		
		long totalCount = getSum(counts, indexOfSmallestPositiveFragmentSize, counts.size()-1);
		long sumCountFromMinToQ1 = (long) Math.ceil(totalCount*0.25);		
		long sumCountFromMinToQ3 = (long) Math.ceil(totalCount*0.75);
		
		// find the bins containing Q1, Q3
		int numItems = bins.size();
		int numItemsBelow = 0;
		boolean foundQ1 = false;
		boolean foundQ3 = false;
		int Q1binIndex = -1; //index of the bin containing Q1 in the 'bins' array
		int Q3binIndex = -1; //index of the bin containing Q3 in the 'bins' array
		for(int i=indexOfSmallestPositiveFragmentSize; i<numItems && (!foundQ1 || !foundQ3); i++)
		{
			numItemsBelow += counts.get(i);
			if(!foundQ1 && numItemsBelow >= sumCountFromMinToQ1)
			{
				Q1binIndex = i;
				foundQ1 = true;
			}
			
			if(!foundQ3 && numItemsBelow >= sumCountFromMinToQ3)
			{
				Q3binIndex = i;
				foundQ3 = true;
				break;
			}			
		}
		
		// calculate the upper bound and lower bound for removing outliers
		long Q1 = bins.get(Q1binIndex);
		long Q3 = bins.get(Q3binIndex);
		long IQR = Math.abs(Q3 - Q1);
		long lowerBound = Math.max(1L, (long) Math.rint(Q1 - 1.5*IQR));
		long upperBound = (long) Math.rint(Q3 + 1.5*IQR);
		
		// find the index of the smallest bin that is not an outlier
		for(int i=indexOfSmallestPositiveFragmentSize; i<numItems; i++)
		{
			if(bins.get(i) >= lowerBound)
			{
				lowerBoundIndex = i;
				break;
			}	
		}
		
		//find the index of the largest bin that is not an outlier
		for(int i=numItems-1; i>=indexOfSmallestPositiveFragmentSize; i--)
		{
			if(bins.get(i) <= upperBound)
			{
				upperBoundIndex = i;
				break;
			}	
		}
	}	

		
	/*
	 * Finds indexes of Q1, median, and Q3 of the dataset with outliers removed.
	 * @pre indexes of the bins containing Q1, Q3 in the 'bins' array are found
	 */
	private void findPercentiles(int lowIndex, int highIndex)
	{
		long totalCount = getSum(counts, lowIndex, highIndex);
		long sumCountFromMinToQ1 = (long) Math.ceil(totalCount*0.25);
		long sumCountFromMinToQ2 = (long) Math.ceil(totalCount*0.5);
		long sumCountFromMinToQ3 = (long) Math.ceil(totalCount*0.75);		
		
		int numItemsBelow = 0;
		boolean foundQ1 = false;
		boolean foundQ2 = false;
		boolean foundQ3 = false;
		
		for(int i=lowIndex; i<=highIndex; i++)
		{
			numItemsBelow += counts.get(i);
			if(!foundQ1 && numItemsBelow >= sumCountFromMinToQ1)
			{
				indexOfQ1 = i;
				foundQ1 = true;
			}
			
			if(!foundQ2 && numItemsBelow >= sumCountFromMinToQ2)
			{
				indexOfMedian = i;
				foundQ2 = true;				
			}				
			
			if(!foundQ3 && numItemsBelow >= sumCountFromMinToQ3)
			{
				indexOfQ3 = i;
				foundQ3 = true;
				break;
			}			
		}
	}
	
	/*
	 * @pre counts and bins are not empty and lowerBoundIndexWithoutNegativeData and upperBoundIndex are found
	 */
	private void calculateMean()
	{
		long sumBins = 0;
		long sumCounts = 0;
		
		for(int i=lowerBoundIndex; i<=upperBoundIndex; i++)
		{
			long count = counts.get(i);
			long bin = bins.get(i);
			
			sumBins += bin*count;
			sumCounts += count;
		}
		
		mean = (double)sumBins/(double)sumCounts;
	}
	
	/*
	 * @pre mean is calculated
	 */
	private void calculateStdevAndVar()
	{
		long sumSquareDifferences = 0;
		long sumCounts = 0;		
		
		for(int i=lowerBoundIndex; i<=upperBoundIndex; i++)
		{
			long count = counts.get(i);
			sumCounts += count;
			long bin = bins.get(i);
			
			while(count>0)
			{
				double difference = bin-mean;
				sumSquareDifferences += difference*difference;
				count--;
			}					
		}
		
		variance = (double)sumSquareDifferences/(double)sumCounts;
		stdev = Math.sqrt(variance);
	}
	
    /*
     * Return the sum of the array of integers.
     */
    private long getSum(List<Long> arr)
    {        
        long sum = 0;
        int len = arr.size();
        for(int i=0; i<len; i++)
        {
           sum += arr.get(i);
        }

        return sum;
    }	
	
    /*
     * Return the sum of the array of integers from lower index to upper index inclusive.
     */
    private long getSum(List<Long> arr, int lower, int upper)
    {        
        long sum = 0;
        int endIndex = Math.min(upper, arr.size()-1);       
        for(int i=Math.max(lower, 0); i<=endIndex; i++)
        {
           sum += arr.get(i);
        }

        return sum;
    }
    
    public int getLowerBoundIndex()
    {
    	return lowerBoundIndex;
    }
    
    public int getUpperBoundIndex()
    {
    	return upperBoundIndex;
    }    
    
//    public List<Integer> getCountsWithoutOutliers()
//    {
//    	List<Integer> list = counts.subList(lowerBoundIndex, upperBoundIndex);
//    	list.add(counts.get(upperBoundIndex));    	    	
//    	return list;
//    }
//    
//    public List<Integer> getBinsWithoutOutliers()
//    {
//    	List<Integer> list = bins.subList(lowerBoundIndex, upperBoundIndex);
//    	list.add(bins.get(upperBoundIndex));    	    	
//    	return list;    	
//    }
    
//    @SuppressWarnings("unchecked")
//	public ArrayList<Integer> getCounts()
//    {
//    	return (ArrayList<Integer>) counts.clone();
//    }
//    
//    @SuppressWarnings("unchecked")
//	public ArrayList<Integer> getBins()
//    {
//    	return (ArrayList<Integer>) bins.clone();
//    }
    
    public long getMin()
    {
    	return bins.get(lowerBoundIndex);
    }
    
    public long getMax()
    {
    	return bins.get(upperBoundIndex);
    }    
    
    public long getQ1()
    {
    	return bins.get(indexOfQ1);
    }
    
    public long getQ3()
    {
    	return bins.get(indexOfQ3);
    }
    
    public long getMedian()
    {
    	return bins.get(indexOfMedian);
    }
    
    public double getMean()
    {
    	return mean;
    }
    
    public double getVariance()
    {
    	return variance;
    }
    
    public double getStdev()
    {
    	return stdev;
    }
    
    public double getQFactor()
    {
    	return qFactor;
    }

    public ArrayList<Long> getBins(){
        return bins;
    }

    public ArrayList<Long> getCounts(){
        return counts;
    }

    public HistogramStats getStatsForNegativeData(){
        return statsForNegativeData;
    }

    public int getIndexOfSmallestPositiveSize(){
        return indexOfSmallestPositiveFragmentSize;
    }

    public int getPercentageOfPostiveSizes(){
        return percentPlus;
    }

    public boolean isFlipped(){
        return flipped;
    }
}
