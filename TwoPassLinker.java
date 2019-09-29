/**
@author: Cary Wu
@description: An implementation of a 2 pass linker that relocates relative addresses and resolves external references
@input: A series of object modules, each containing 3 parts: definition list, use list, and program text
@output: A load module that can be loaded and run by the system 
*/
//import java.io.*;
//import java.io.File;
//import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.*;
import java.util.TreeMap;
//import org.apache.commons.io.IOUtils;

public class TwoPassLinker {
    
    static int numOfMods; 
    static HashMap<String, Integer> definitions = new HashMap<String, Integer>(); //stores symbols and absolute addresses
    static ArrayList<String> multiplyDefined = new ArrayList<String>(); //keeps track of multiply defined symbols 
    static ArrayList<String> definedOutside = new ArrayList<String>(); //keeps track of definitions defined outside module length
    static HashMap<String, Integer> modNumber = new HashMap<String, Integer>(); //keeps track of defined symbols and corresponding module number
    static HashMap<Integer, String> errorMsg = new HashMap<Integer, String>(); //keeps track of error messages where <K,V> = <num, errorMsg>    
    static ArrayList<String> warnings = new ArrayList<String>(); //collects list of warnings to be printed at the end.   
    static HashMap<Integer, String> map = new HashMap<Integer, String>(); //output of Second Pass, <K,V> = <num, final address>
    static ArrayList<String> usedSymbols = new ArrayList<String>(); //list of all used symbols, mainly for error check
    static ArrayList<String> usedNotDefined = new ArrayList<String>(); //list of symbols that are used but were not defined
    static ArrayList<String> definedNotUsed = new ArrayList<String>(); //list of defined symbols that were never used 
    static ArrayList<Integer> immediateOnUseList = new ArrayList<Integer>(); //list of type 1 addresses that appears on use list
    
    static TreeMap<String, Integer> sorted_defs = new TreeMap<String, Integer>();//sort hashmap by using treemap

    /**
     *The passOne method scans through the standard input and computes the absolute address for all defined symbols
     */
    public static void passOne(Scanner sc1){
	numOfMods = Integer.parseInt(sc1.next());
	int nd, nu, nt, base = 0;
       	
        String symbol;
	int relative, absolute;
	ArrayList<String> defsInThisMod = new ArrayList<String>(); //temporary list of definitions for each module, mainly used for error detection of symbol defined outside module
	for (int mod = 0; mod < numOfMods; mod++){
	    nd = Integer.parseInt(sc1.next());

	    for (int sym = 0; sym < nd; sym++){
		symbol = sc1.next();
		//Error check: symbol size should be at most 8 characters;
		if(symbol.length() > 8){
		    System.out.println("Error: expected limit for symbol size is 8 characters, program will be terminated.");
		    System.exit(0);
		}
		//Error check: this symbol is multiply defined
		if (definitions.containsKey(symbol)) {
		    sc1.next();
		    multiplyDefined.add(symbol);
		}
		else {
		    //store definition and absolute address
		    relative = Integer.parseInt(sc1.next());
		    absolute = relative + base; //relative address relocation
		    definitions.put(symbol, absolute);
		    modNumber.put(symbol, mod);
		    defsInThisMod.add(symbol);
		}
	    }
	    nu = Integer.parseInt(sc1.next());
      	    for (int use = 0; use < nu; use++){
	       sc1.next();
	       sc1.next();
	    }
	    
	    nt = Integer.parseInt(sc1.next());
	    for (int i = 0; i < nt; i++){
		sc1.next();
	    }

	    //Error check: if an address appearing in a defintion exceeds size of the module, treat the address as 0 (relative)
	    for (String sym : defsInThisMod){
		if (definitions.get(sym) >= base+nt){
		    definedOutside.add(sym);
		    definitions.put(sym,base); //treat address as 0 (relative)
		}
	    }

	    //sizesOfMods.add(nt);
	    base = base + nt;

	    defsInThisMod.clear();
	    
	}
    } //end_passOne

    /**
     *The main method outputs the Symbol Table and Memory Map computed from passOne and passTwo
     */
    public static void main(String[] args) {
	Scanner sc = new Scanner(System.in);
	String input = "";
	while (sc.hasNext()){
	    input = input + sc.next() + " ";
	    
	}
	Scanner scanner1 = new Scanner(input);
	Scanner scanner2 = new Scanner(input);
    
	passOne(scanner1); 	
	System.out.println("Symbol Table: ");	 
	String symbol;
	 //sort hashmap keys by copying to a treemap
	sorted_defs.putAll(definitions);
	for (HashMap.Entry<String,Integer> sym : sorted_defs.entrySet()){
	    symbol = sym.getKey();
	    
	    System.out.print(symbol + "=" + sym.getValue().toString());

	    //Error Check: multiply defined, print error message
	    if (multiplyDefined.contains(symbol)) {
		System.out.print(" Error: " + symbol + " is multiply defined; first value used.");
	    }    
	    //Error Check: if an address appearing in a definition exceeds size of the module, treat the address as 0 (relative)
	    else if (definedOutside.contains(symbol)) {
		System.out.print(" Error: The definition of "+ symbol + " is outside of module " + modNumber.get(symbol).toString() + "; zero (relative) used.");
	    }
	    System.out.println();
	}
	
	System.out.println();
	System.out.println("Memory Map: ");
	passTwo(scanner2);
	
	String formatted;
	int num = 0;
	while (map.containsKey(num)){
	    formatted = String.format("%-4s %4s ", num+":", map.get(num));
	    System.out.printf(formatted);
	    //print error message if there is one
	    if (errorMsg.containsKey(num)) {
		System.out.print(errorMsg.get(num));
	    }
	  
	    System.out.println();
	    num++;
	}

	
	System.out.println();
	//print warning messages
	for(String warning: warnings){
	    System.out.println(warning);
	}
    } //end_Main
	
	/**
	 *The passTwo method scans through the standard input and computes the new addresses for each instruction for the memory map.
	 */
    public static void passTwo(Scanner sc2){

	int nd, nu, nt, base = 0;
	int type, dummyAdd, abs, num;
	String useSymbol, s, errMsg;
	int givenAddress, rel, loc, val, address;
	ArrayList<String> usesThisMod = new ArrayList<String>(); //holds all the symbols that were used in a module
	ArrayList<Integer> dummyAddresses = new ArrayList<Integer>(); //
	HashMap<Integer,Integer> chain = new HashMap<Integer, Integer>(); //<K,V> = <num, the instruction> for external addresses in the module 
	ArrayList<Integer> multiplyUsed = new ArrayList<Integer>(); //location of instructions in the module that are multiply used by multiple symbols 
	
	numOfMods = sc2.nextInt();
	
	for(int mod = 0; mod < numOfMods; mod++){

	    nd = sc2.nextInt();
	    for(int sym = 0; sym < nd; sym++){
		sc2.next();
		sc2.nextInt();
	    }

	    nu = sc2.nextInt(); 
	    for (int use = 0; use < nu; use++){
		useSymbol = sc2.next();	
		//check if the use symbol is defined
		if (!definitions.containsKey(useSymbol)){
		    usedNotDefined.add(useSymbol);
   
		}		
		//add to list of used symbols 
	        usedSymbols.add(useSymbol);
	      
	        dummyAdd = sc2.nextInt();
		//check if multiple uses point to the same instruction
	       	if (dummyAddresses.contains(dummyAdd)){
		    multiplyUsed.add(dummyAdd+base);//store location of the instruction that has multiple uses pointed to it 
		    int idx = dummyAddresses.indexOf(dummyAdd);
		    usesThisMod.set(idx, useSymbol); //update the first usage with this later usage 
		}
		else{
		    usesThisMod.add(useSymbol);
		    //add to corresponding list of dummy addresses 
		    dummyAddresses.add(dummyAdd);
		}
	    }//end uses list

	    nt = sc2.nextInt(); //length of module
	    for(int n = 0; n < nt; n++) {  
		num = base + n; 
		givenAddress = sc2.nextInt(); 
		//compute address type which is the last digit
		type = givenAddress % 10;
	        address = givenAddress/10 ; //ignore last digit and get 4 digit instruction
		switch (type) {
		
		//immediate address --> unchanged
	        case 1:
		    //check if this immediate address appears in use list
		    if (dummyAddresses.contains(n)){
			//treat as external
			chain.put(num, address);
			immediateOnUseList.add(address);
			//System.out.println("Error: Immediate address on use list; treated as External");
		    }
		 
	            map.put(num, Integer.toString(address));
		   
		    break;

		    
	        //absolute address --> unchanged
	        case 2:
		    //Error check: absolute address exceeds size of machine
		  
		    if((address%1000) > 200){
			map.put(num, Integer.toString((address/1000)+199) );
			errMsg = "Error: Absolute address exceeds the size of the machine. Largest legal value (199) will be used.";
		        errorMsg.put(num, errMsg);
		    }
		    else{
			map.put(num, Integer.toString(address));
		    }
	 
		    break;

		    
		// relative address --> relative address relocation
	        case 3:
			address = base+address;
			map.put(num, Integer.toString(address)); 
		    
			break;


		//external address --> resolving external references
	        case 4:
		    //Error check: external address not on use list    
		    map.put(num, Integer.toString(address));
		    chain.put(num, address);
		    break;
		}//end switch case
	    }//end iterating through instructions

	    int key;
	    boolean errorNotDefined = false; 
	    while (!dummyAddresses.isEmpty()){ //not end of use
		key = dummyAddresses.remove(0) + base;
		s = usesThisMod.remove(0);
		int instr;
		if (multiplyUsed.contains(key)){
		    errMsg = "Error: Multiple symbols listed as used for this instruction. All uses but the last one are ignored.";
		    errorMsg.put(key, errMsg);
		}
		if (!chain.isEmpty()){
		    instr = chain.remove(key); //get the instruction   
		}
		else{
		    break;
		}
       
		if(usedNotDefined.contains(s)){ //value of 0 is used
       		    abs = 0;
		    errorNotDefined = true;
       		}
	       
       		else{
       		    abs = definitions.get(s);
       		}
		
		int next;
		while ((instr%1000) != 777) {
		    if(immediateOnUseList.contains(instr)){
			errMsg = "Error: Immediate address on use list; treated as External.";
			errorMsg.put(key, errMsg);
		    }
		    
		    map.replace(key, Integer.toString(((instr/1000)*1000)+abs));
		
		    if(errorNotDefined){
			errMsg = "Error: " + s + " is not defined; zero used." ;
			errorMsg.put(key, errMsg);

		    }
		    key = instr%1000 + base;
		    if (!chain.containsKey(key)){
			instr = Integer.parseInt(map.get(key));
			errMsg = "Error: Immediate address on use list; treated as External.";
			errorMsg.put(key, errMsg);
		    }
		    else{
			instr = chain.remove(key);
		    }
		}
	       
		//reached final use
	       	map.replace(key, Integer.toString(((instr/1000)*1000)+abs));
		if(errorNotDefined){
		    errMsg = "Error: " + s + " is not defined; zero used";
		    errorMsg.put(key, errMsg);
		}	
	    }
	    if(!chain.isEmpty()){
		for(Integer i : chain.keySet()){
		    errMsg = "Error: " + "E type address not on use chain; treated as I type.";
		    errorMsg.put(i, errMsg);
		}
	    }
	    
	        dummyAddresses.clear();    					
		usesThisMod.clear();
		chain.clear();
		base = base+nt;

	}
	    /*
if (!dummyAddresses.contains(n)){
			map.add(Integer.toString(address));
			errMsg = "Error: External address not on use list, treated as immediate address.";
			map.add(errMsg);
		    }
		    else{
	    */
				
	//compute list of defined symbols that were not used
	for(String symb : sorted_defs.keySet()){
	    if(!usedSymbols.contains(symb)){
		warnings.add("Warning: "+symb+" was defined in module "+modNumber.get(symb).toString()+" but was never used.");
	    }
	}
  
    }//end_passTwo
    
}//end_class

