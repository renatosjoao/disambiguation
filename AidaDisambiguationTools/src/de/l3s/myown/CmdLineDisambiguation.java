package de.l3s.myown;

import java.util.ArrayList;

import mpi.aida.CommandLineDisambiguator;

public class CmdLineDisambiguation {

	private static String numThreads = "-c";
	private static String dir = "-d";
	private String output = "-o";
	private String timing = "-z";
	
	public static void main(String[] args){
		String input = "/home/renato/output";
		CommandLineDisambiguator clD = new CommandLineDisambiguator();
		try {
			String[] arguments = {"-c","5","-d","-i",input,"-o","HTML","-z"};
			clD.run(arguments);

		} catch (Exception e) {
		
			e.printStackTrace();
		
		}
	
	}
}
