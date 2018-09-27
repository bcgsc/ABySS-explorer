package ca.bcgsc.dive.dive;

import java.util.Arrays;

/**
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 */
public class PlotSettings {
    public String fileName;
    public int minContigLength;
    public boolean logY;
    public boolean xAxisInUnitOfLength;
    public int unit;
    public int[] selectedKs;
//    public boolean isPairedEnd;

    public boolean showSquiggles;
    public int lengthScale;
    public boolean showLabels;
    public boolean showPEContigs;
//    public boolean showPEPartners;

    public boolean useStepSize;
    public int stepSize;

    public boolean drawN50plot;
    public boolean drawCoveragePlot;
    public boolean drawFragmentSizeDistribution;
    public String[] selectedLibs;
       
    public int assemblyMode;

    public PlotSettings(String fileName,
                        int minContigLength,
                        boolean logY,
                        boolean xAxisInUnitOfLength,
                        int unit,
                        int[] selectedKs,
                        int assemblyMode,
                        boolean showSquiggles,
                        int lengthScale,
                        boolean showLabels,
                        boolean showPEContigs,
//                        boolean showPEPartners,
                        boolean useStepSize,
                        int stepSize,
                        boolean drawN50plot,
                        boolean drawFragmentSizeDistribution,
                        String[] selectedLibs,
                        boolean drawCoveragePlot){
        this.fileName = fileName;
        this.minContigLength = minContigLength;
        this.logY = logY;
        this.xAxisInUnitOfLength = xAxisInUnitOfLength;
        this.unit = unit;
        this.selectedKs = selectedKs;
//        this.isPairedEnd = isPairedEnd;\
        this.assemblyMode = assemblyMode;
        this.showSquiggles = showSquiggles;
        this.lengthScale = lengthScale;
        this.showLabels = showLabels;
        this.showPEContigs = showPEContigs;
//        this.showPEPartners = showPEPartners;
        this.useStepSize = useStepSize;
        this.stepSize = stepSize;
        this.drawN50plot = drawN50plot;
        this.drawCoveragePlot = drawCoveragePlot;
        this.drawFragmentSizeDistribution = drawFragmentSizeDistribution;
        this.selectedLibs = selectedLibs;
    }

    public boolean equals(PlotSettings other){
            return ((fileName == null && other.fileName == null) || (fileName != null && other.fileName != null && fileName.equals(other.fileName)))
                    && minContigLength == other.minContigLength
                    && logY == other.logY
                    && xAxisInUnitOfLength == other.xAxisInUnitOfLength
                    && unit == other.unit
                    && Arrays.equals(selectedKs, other.selectedKs)
//                    && isPairedEnd == other.isPairedEnd
                    && assemblyMode == other.assemblyMode
                    && showSquiggles  == other.showSquiggles
                    && lengthScale  == other.lengthScale
                    && showLabels  == other.showLabels
                    && showPEContigs  == other.showPEContigs
//                    && showPEPartners  == other.showPEPartners
                    && useStepSize == other.useStepSize
                    && stepSize == other.stepSize
                    && drawN50plot == other.drawN50plot
                    && drawCoveragePlot == other.drawCoveragePlot
                    && drawFragmentSizeDistribution == other.drawFragmentSizeDistribution
                    && Arrays.equals(selectedLibs, other.selectedLibs);
    }

    public boolean haveSameStatsSettings(PlotSettings other){
        return minContigLength == other.minContigLength
             && unit == other.unit
             && fileName.equals(other.fileName)
            && haveSameAssembliesSelected(other);
    }

    public boolean haveSameN50plotSettings(PlotSettings other){
        boolean same = minContigLength == other.minContigLength
             && unit == other.unit
             && fileName.equals(other.fileName)
             && haveSameAssembliesSelected(other)
             && drawN50plot == other.drawN50plot;
        
        if(drawN50plot){
             same = same && logY == other.logY
             && xAxisInUnitOfLength == other.xAxisInUnitOfLength;
        }
        
        return same;
    }

    public boolean haveSameCoveragePlotSettings(PlotSettings other){
        return drawCoveragePlot == other.drawCoveragePlot
             && haveSameAssembliesSelected(other);
    }

    public boolean haveSameFragmentSizeDistributionSettings(PlotSettings other){
        return drawFragmentSizeDistribution == other.drawFragmentSizeDistribution
                && haveSameAssembliesSelected(other)
                && Arrays.equals(selectedLibs, other.selectedLibs);
    }

    public boolean haveSamePlotsSettings(PlotSettings other){
        if(other == null){
            return false;
        }

        boolean same = minContigLength == other.minContigLength
               && unit == other.unit
               && haveSameAssembliesSelected(other)
               && drawN50plot == other.drawN50plot
               && drawCoveragePlot == other.drawCoveragePlot
               && drawFragmentSizeDistribution == other.drawFragmentSizeDistribution;

        if(fileName != null){
            same = same && fileName.equals(other.fileName);
        }

        if(drawN50plot){
             same = same && logY == other.logY
             && xAxisInUnitOfLength == other.xAxisInUnitOfLength;
        }

        if(drawFragmentSizeDistribution){
            same = same && Arrays.equals(selectedLibs, other.selectedLibs);
        }

        return same;
    }

    public boolean haveSameNavigatorSettings(PlotSettings other){
//        return isPairedEnd == other.isPairedEnd
        return assemblyMode == other.assemblyMode
                && showSquiggles == other.showSquiggles
                && lengthScale == other.lengthScale
                && showLabels == other.showLabels
                && showPEContigs == other.showPEContigs
//                && showPEPartners == other.showPEPartners
                && useStepSize == other.useStepSize
                && stepSize == other.stepSize;
    }

    public boolean haveSameAssembliesSelected(PlotSettings other){
        return Arrays.equals(selectedKs, other.selectedKs);
    }
        
    public void copyStatsSettings(PlotSettings other){
        minContigLength = other.minContigLength;
        unit = other.unit;
        fileName = other.fileName;
    }

    public void copyPlotsSettings(PlotSettings other){
        minContigLength = other.minContigLength;
        unit = other.unit;
        fileName = other.fileName;
        logY = other.logY;
        xAxisInUnitOfLength = other.xAxisInUnitOfLength;
        drawN50plot = other.drawN50plot;
        drawCoveragePlot = other.drawCoveragePlot;
        drawFragmentSizeDistribution = other.drawFragmentSizeDistribution;
        selectedLibs = other.selectedLibs;
    }

    public void copyNavigatorSettings(PlotSettings other){
//        isPairedEnd = other.isPairedEnd;
        assemblyMode = other.assemblyMode;
        showSquiggles = other.showSquiggles;
        lengthScale = other.lengthScale;
        showLabels = other.showLabels;
        showPEContigs = other.showPEContigs;
//        showPEPartners = other.showPEPartners;
        useStepSize = other.useStepSize;
        stepSize = other.stepSize;
    }

    public void copyAssembliesSelected(PlotSettings other){
        selectedKs = other.selectedKs;
    }
}
