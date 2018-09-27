package ca.bcgsc.abyssexplorer.parsers;

import java.io.File;
import java.io.IOException;

import ca.bcgsc.abyssexplorer.graph.AbyssGraph2;

/**
 * Interface for ABySS supported graph parsers.
 * 
 * @author Cydney Nielsen
 *
 */
public interface GraphParser {
		
	public void open(File f) throws IOException;
	
	public void close() throws IOException;
	
	/**
	 * Sets the graph name and initializes the data structures
	 * based on the graph size.
	 * 
	 * @return initialized graph
	 * @throws IOException 
	 */
	public AbyssGraph2 initializeGraph() throws IOException, InterruptedException;
	
	public Object parseNextLine() throws IOException, InterruptedException;
	
}
