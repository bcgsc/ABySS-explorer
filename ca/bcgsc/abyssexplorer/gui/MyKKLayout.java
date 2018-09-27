package ca.bcgsc.abyssexplorer.gui;

/**
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 */

import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.graph.Graph;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MyKKLayout<V,E> extends KKLayout<V,E> {

    private int maxIterations = 2000;
    private int currIteration = 0;
    private double minPercentChangeInEnergy = 0.1D; //0.1%
    private double initialPercentChangeInEnergy = -1D;
    private double percentChangeInEnergy = -1D;
    private double energyTenIterationsAgo = -1D;
    private boolean energyChangeTooSmall = false;

    public MyKKLayout(Graph<V,E> g){
        super(g);
        setAdjustForGravity(true);
    }

    public void setDiameter(double d){
        this.diameter = d;
    }

    @Override
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        super.setMaxIterations(maxIterations);
    }

    @Override
    public void initialize(){
        currIteration = 0;
        super.initialize();
    }

    @Override
    public void step(){
        currIteration++;
        if(currIteration >= 10 && (currIteration % 10) == 1){ //computed across 10 iterations
            double e = getEnergy();
            if(e <= 0D){
                energyChangeTooSmall = true;
            }

            if(energyTenIterationsAgo > 0D && e > 0D){
                percentChangeInEnergy = (energyTenIterationsAgo - e)/energyTenIterationsAgo * 100D;
                if(initialPercentChangeInEnergy < 0D){
                    initialPercentChangeInEnergy = percentChangeInEnergy;
                }

                energyChangeTooSmall = percentChangeInEnergy < minPercentChangeInEnergy;
            }
            energyTenIterationsAgo = e;
        }

        try{
            super.step();
        }
        catch(NullPointerException e){} // do nothing
    }

    @Override
    public void reset(){
        energyChangeTooSmall = false;
        currIteration = 0;
        super.reset();
    }

    // percent change is computed across 10 iterations
    public void setMinDeltaEnergyNeededToContinue(double percentChange){
        minPercentChangeInEnergy = percentChange;
    }

    public int getIterationProgress(){
        return (int) Math.rint(Math.floor(currIteration*100.0/maxIterations));
    }

    public int getEnergyMinimizationProgress(){
        if(energyChangeTooSmall){
            return 100;
        }

        return (int) Math.rint(Math.floor((initialPercentChangeInEnergy - percentChangeInEnergy)*100.0/(initialPercentChangeInEnergy - minPercentChangeInEnergy)));
    }

//    @Override
//    public boolean done(){
//        return super.done() || energyChangeTooSmall;
//    }

    public boolean energyMinimized(){
        return energyChangeTooSmall;
    }

    private static Pattern statusPattern = Pattern.compile("Kamada-Kawai V=.*E=(-?\\d*\\.?\\d*E?-?\\d*).*");

    private double getEnergy(){
        String status = super.getStatus();
//        System.out.println(status);
        Matcher m = statusPattern.matcher(status);
        if(m.matches()){
            String e = m.group(1);
//            System.out.println("parsed: "+ e);
            return Double.parseDouble(e);
        }
        return -1D;
    }
}
