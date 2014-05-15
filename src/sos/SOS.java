package sos;

import java.util.*;


/**
 * This class contains the simulated operating system (SOS).  Realistically it
 * would run on the same processor (CPU) that it is managing but instead it uses
 * the real-world processor in order to allow a focus on the essentials of
 * operating system design using a high level programming language.
 *
 *
 * @author adamsl15 	- Logan Adams
 * @author roddy15 		- Nate Roddy
 * @author domingue15	- Fatima Dominguez
 * @author nelsond15	- Danny Nelson
 * @author tsai15		- Eric Tsai
 * @author delhotal15	- Taryn Delhotal
 * @author schumach15	- Derek Schumacher
 * @author lazatin14	- Jessica Lazatin
 * @author harber14		- Rose Harber
 * 
 * HW8
 */

// Extends TrapHandler within CPU
public class SOS implements CPU.TrapHandler
{

	//======================================================================
	//Constants
	//----------------------------------------------------------------------

	//These constants define the system calls this OS can currently handle
	public static final int SYSCALL_EXIT     			= 0;   			// exit the current program 
	public static final int SYSCALL_OUTPUT   			= 1;    		// outputs a number 
	public static final int SYSCALL_GETPID   			= 2;    		// get current process id 
	public static final int SYSCALL_OPEN    			= 3;    		// access a device 
	public static final int SYSCALL_CLOSE   			= 4;    		// release a device 
	public static final int SYSCALL_READ    			= 5;    		// get input from device 
	public static final int SYSCALL_WRITE   			= 6;    		// send output to device 
	public static final int SYSCALL_EXEC 			    = 7;		    // spawn a new process 
	public static final int SYSCALL_YIELD   			= 8;    		// yield the CPU to another process 
	public static final int SYSCALL_COREDUMP 			= 9;    		// print process state and exit 

	public static final int SYSCALL_SUCCESS				=  0;			// system call was successful
	public static final int	SYSCALL_READ_FAIL			= -1;			// device could not be read from
	public static final int	SYSCALL_WRITE_FAIL			= -2;			// device could not write data
	public static final int SYSCALL_OPEN_FAIL			= -4;			// device could not be opened
	public static final int	SYSCALL_CLOSE_FAIL			= -8;			// device could not be closed
	public static final int	SYSCALL_DEVICE_UNAVAILABLE	= -16;			// device was unavailable
	public static final int SYSCALL_DEVICE_UNSHAREABLE	= -32;			// device is not shareable
	public static final int SYSCALL_DEVICE_UNREADABLE	= -64;			// device is set to unreadable
	public static final int SYSCALL_DEVICE_UNWRITEABLE	= -128;			// device is set to unwriteable
	public static final int SYSCALL_DEVICE_DOESNT_EXIST = -256;			// device was not found in m_devices array

	public static final int IDLE_PROC_ID    = 999; 						// this process is used as the idle process' id


	//======================================================================
	//Member variables
	//----------------------------------------------------------------------

	/**
	 * This flag causes the SOS to print lots of potentially helpful
	 * status messages
	 **/
	public static final boolean m_verbose = true;

	/**
	 * The CPU the operating system is managing.
	 **/
	private CPU m_CPU = null;

	/**
	 * The RAM attached to the CPU.
	 **/
	private RAM m_RAM = null;
	
	/**
	 * The MMU attached to the CPU.
	 */
	private MMU m_MMU = null;

	/**
	 * The Current Process running on the CPU.
	 **/
	private ProcessControlBlock m_currProcess = null;

	/**
	 * A Vector of devices currently installed on the System.
	 **/
	private Vector<DeviceInfo> m_devices = null;

	/**
	 * A Vector of programs that are currently "running"
	 */
	private Vector<Program> m_programs = null;

	/**
	 * The Unique ID of the next process
	 */
	private int m_nextProcessID = 1001;

	/**
	 * List of all processes currently loaded into RAM and in one of the major states
	 */
	private Vector<ProcessControlBlock> m_processes = null;
	
	/**
	 * List of all blocks of RAM that are not currently allocated to a process
	 */
	private Vector<MemBlock> m_freeList = null;



	/*======================================================================
	 * Constructors & Debugging
	 *----------------------------------------------------------------------
	 */

	/**
	 * The constructor does nothing special
	 */
	public SOS(CPU c, RAM r, MMU m)
	{
		// Init member list
		m_CPU = c;
		m_RAM = r;
		m_MMU = m;
		m_CPU.registerTrapHandler(this);
		m_devices = new Vector<DeviceInfo>(0);
		m_programs = new Vector<Program>();
		m_processes = new Vector<ProcessControlBlock>();
		m_freeList = new Vector<MemBlock>(0);
		m_freeList.add(new MemBlock(0, m_MMU.getSize()));
		initPageTable();
	}//SOS ctor

	/**
	 * Does a System.out.print as long as m_verbose is true
	 **/
	public static void debugPrint(String s)
	{
		if (m_verbose)
		{
			System.out.print(s);
		}
	}

	/**
	 * Does a System.out.println as long as m_verbose is true
	 **/
	public static void debugPrintln(String s)
	{
		if (m_verbose)
		{
			System.out.println(s);
		}
	}

	/*======================================================================
	 * Memory Block Management Methods
	 *----------------------------------------------------------------------
	 */

	/**
	 * push
	 *
	 * Pushes a value onto the stack by incrementing the stack pointer
	 * then saving the value to that location in memory
	 *
	 * @param val - the value that will be pushed to the top of the stack
	 */
	private void push(int val) {
		m_CPU.setSP(m_CPU.getSP() + 1);
		m_MMU.write(m_CPU.getSP(), val);
	}


	/**
	 * pop 
	 *
	 * Returns the top of stack as an integer
	 * 
	 * @return integer - current stack value
	 *      
	 */
	private int pop() {
		int currentStackValue = m_MMU.read(m_CPU.getSP());
		m_CPU.setSP(m_CPU.getSP() - 1);
		return currentStackValue;
	}

	/**
	 * allocBlock
	 * 
	 * This function decides where to load a new process based on the size
	 * of its address space - Uses a first fit algorithm
	 * 
	 * @param size - the size that is needed for the process
	 * @return the base address of the block that is being allocated for the process
	 */
    private int allocBlock(int size)
    {
    	// The amount of external fragmentation we have as a result of processes moving around
    	int fragSize = 0;
    	
    	// If we have no free spaces at all, return immediately
    	if(m_freeList.size() == 0) {
    		return -1;
    	}
    	
    	// TODO : Allocblock
    	
    	// Find if there is a MemBlock that is large enough to hold the entire program
    	for(int i = 0; i < m_freeList.size(); i++) {
    		// Determine if we can hold the new program
    		int currBlockSize = m_freeList.get(i).getSize();  
    		if(currBlockSize >= size) {
    			int addr = m_freeList.get(i).getAddr();
    			m_freeList.remove(i);
    			// Create a space in m_freeList that is the difference as long as it is non-zero
    			if(currBlockSize != size) {
    				// Move in a new memory block that is the remaining space if there is any
    				m_freeList.add(new MemBlock(addr + size, currBlockSize - size));
    			}
    			return addr;
    		}
    		// Increase our fragmentation size
    		fragSize += currBlockSize;
    	}
    	
    	// If we do not have enough space after potential rearranging, exit
    	if(fragSize < size) {
    		return -1;
    	}
    	System.out.println("Before Compaction");
    	printMemAlloc();
    	// In the even our process list is not sorted, it must be
        Collections.sort(m_processes);
    	
		// Force the first process to be moved to the base of RAM, which should be the top of the page table
		m_processes.get(0).move(m_MMU.getNumPages());
		
		// Get info about the lowest process in RAM
		int prevBase = m_processes.get(0).getRegisterValue(CPU.BASE);
		int prevLim = m_processes.get(0).getRegisterValue(CPU.LIM);

		// Move each process down, so that all processes are compacted in the lowest part of RAM
		for(int i = 1; i < m_processes.size(); i++) {
			m_processes.get(i).move(prevBase + prevLim);
			prevBase = m_processes.get(i).getRegisterValue(CPU.BASE);
			prevLim = m_processes.get(i).getRegisterValue(CPU.LIM);
		}
		// Remove all other MemBlocks from the vector so we only have the remaining space at the top of RAM
		m_freeList.clear();

		// If we have extra space left over after adding the process, put the remaining free memory together on top of RAM
		if(fragSize != size) {
			m_freeList.add(new MemBlock(prevBase + prevLim + size, fragSize - size));
		}
		
		// Return the base of the new MemBlock that we have just created
		return prevBase + prevLim;
    }//allocBlock

    
	/**
	 * freeCurrProcessMemblock
	 * 
	 * This method adjusts the m_freeList variable to account for the fact that 
	 * the current process is about to be killed by combining the surrounding free 
	 * space into a single block
	 * 
	 */
    private void freeCurrProcessMemBlock()
    {
    	// Determine the information about the current process
    	int base = m_currProcess.getRegisterValue(CPU.BASE);
    	int limit = m_currProcess.getRegisterValue(CPU.LIM);
    	// Check to ensure that we only ever combine the correct free blocks
    	boolean mergeBlock = false;
    	
    	// So we don't have to rely on others to sort m_freeList for us
    	Collections.sort(m_freeList);
    	
    	// Iterate through the free blocks, find the ones near the current process and combine them
    	for(int i = 0; i < m_freeList.size(); i++) {
    		int addr = m_freeList.get(i).getAddr();
    		int size = m_freeList.get(i).getSize();
    		
    		// Determine if we can be in the case where there could be 2 surrounding memBlocks
    		if(i != (m_freeList.size() - 1)) {
    			// Get info about the block that is above the current block
    			int addr_2 = m_freeList.get(i+1).getAddr();
        		int size_2 = m_freeList.get(i+1).getSize();
        		// If we have surrounding memory blocks, then combine all of them
    			if((base == (addr + size)) && (addr_2 == (base + limit))) {
    				m_freeList.get(i).m_addr = addr;
    				m_freeList.get(i).m_size = size + limit + size_2;
    				m_freeList.remove(i+1);
    				mergeBlock = true;
    				break;
    			}
    		}		
    		// If the free space is below the current process
    		if(base == (addr + size)) {
    			m_freeList.get(i).m_addr = addr;
    			m_freeList.get(i).m_size = size + limit;
    			mergeBlock = true;
    			break;
    		}
    		// If the free space is above the current process
    		if(addr == (base + limit)) {
    			m_freeList.get(i).m_addr = base;
    			m_freeList.get(i).m_size = size + limit;
    			mergeBlock = true;
    			break;
    		}
    	}
    	
    	// If we have not merged with another free block yet, we must be surrounded by 2 other processes
    	if(!mergeBlock) {
    		// Just free the address space that is occupied by this process then
    		m_freeList.add(new MemBlock(base, limit));
    	}
    	
    }//freeCurrProcessMemBlock
    
    /**
    * printMemAlloc                 *DEBUGGING*
    *
    * outputs the contents of m_freeList and m_processes to the console and
    * performs a fragmentation analysis.  It also prints the value in
    * RAM at the BASE and LIMIT registers.  This is useful for
    * tracking down errors related to moving process in RAM.
    *
    * SIDE EFFECT:  The contents of m_freeList and m_processes are sorted.
    *
    */
   private void printMemAlloc()
   {
       //If verbose mode is off, do nothing
       if (!m_verbose) return;

       //Print a header
       System.out.println("\n----------========== Memory Allocation Table ==========----------");
       
       //Sort the lists by address
       Collections.sort(m_processes);
       Collections.sort(m_freeList);

       //Initialize references to the first entry in each list
       MemBlock m = null;
       ProcessControlBlock pi = null;
       ListIterator<MemBlock> iterFree = m_freeList.listIterator();
       ListIterator<ProcessControlBlock> iterProc = m_processes.listIterator();
       if (iterFree.hasNext()) m = iterFree.next();
       if (iterProc.hasNext()) pi = iterProc.next();

       //Loop over both lists in order of their address until we run out of
       //entries in both lists
       while ((pi != null) || (m != null))
       {
           //Figure out the address of pi and m.  If either is null, then assign
           //them an address equivalent to +infinity
           int pAddr = Integer.MAX_VALUE;
           int mAddr = Integer.MAX_VALUE;
           if (pi != null)  pAddr = pi.getRegisterValue(CPU.BASE);
           if (m != null)  mAddr = m.getAddr();

           //If the process has the lowest address then print it and get the
           //next process
           if ( mAddr > pAddr )
           {
               int size = pi.getRegisterValue(CPU.LIM);  // This was edited to reflect BASE + LIM structure
               System.out.print(" Process " + pi.processId +  " (addr=" + pAddr + " size=" + size + " words");
               System.out.print(" / " + (size / m_MMU.getPageSize()) + " pages)" );
               System.out.print(" @BASE=" + m_MMU.read(pi.getRegisterValue(CPU.BASE))
                                + " @SP=" + m_MMU.read(pi.getRegisterValue(CPU.SP)));
               System.out.println();
               if (iterProc.hasNext())
               {
                   pi = iterProc.next();
               }
               else
               {
                   pi = null;
               }
           }//if
           else
           {
               //The free memory block has the lowest address so print it and
               //get the next free memory block
               System.out.println("    Open(addr=" + mAddr + " size=" + m.getSize() + ")");
               if (iterFree.hasNext())
               {
                   m = iterFree.next();
               }
               else
               {
                   m = null;
               }
           }//else
       }//while
           
       //Print a footer
       System.out.println("-----------------------------------------------------------------");
       
   }//printMemAlloc
    
    /*======================================================================
     * Virtual Memory Methods
     *----------------------------------------------------------------------
     */

    /**
     * initPageTable
     * 
     * initializes the area of RAM that the MMU will use for its page table
     * 
     */
    private void initPageTable()
    {
        // TODO : Implement this method
    	
    	int numFrames = m_MMU.getNumFrames();
    	int numPages = m_MMU.getNumPages();
    	int pageSize = m_MMU.getPageSize();
    	
    	for(int i = 0; i < numPages; i++) {
    		m_MMU.write(i, i * pageSize);
    	}
    	
    	m_freeList.removeAllElements();
    	m_freeList.add(new MemBlock(numPages, (m_MMU.getSize() - numPages)));

    }//initPageTable


    /**
     * createPageTableEntry
     *
     * is a helper method for {@link #printPageTable} to create a single entry
     * in the page table to print to the console.  This entry is formatted to be
     * exactly 35 characters wide by appending spaces.
     *
     * @param pageNum is the page to print an entry for
     *
     */
    private String createPageTableEntry(int pageNum)
    {
        int frameNum = m_MMU.read(pageNum);
        int baseAddr = frameNum * m_MMU.getPageSize();

        //check to see if student has pre-shifted frame numbers
        //in their page table and, if so, correct the values
        if (frameNum / m_MMU.getPageSize() != 0)
        {
            baseAddr = frameNum;
            frameNum /= m_MMU.getPageSize();
        }

        String entry = "page " + pageNum + "-->frame "
                          + frameNum + " (@" + baseAddr +")";

        //pad out to 35 characters
        String format = "%s%" + (35 - entry.length()) + "s";
        return String.format(format, entry, " ");
        
    }//createPageTableEntry

    /**
     * printPageTable      *DEBUGGING*
     *
     * prints the page table in a human readable format
     *
     */
    private void printPageTable()
    {
        //If verbose mode is off, do nothing
        if (!m_verbose) return;

        //Print a header
        System.out.println("\n----------========== Page Table ==========----------");

        //Print the entries in two columns
        for(int i = 0; i < m_MMU.getNumPages() / 2; i++)
        {
            String line = createPageTableEntry(i);                       //left column
            line += createPageTableEntry(i + (m_MMU.getNumPages() / 2)); //right column
            System.out.println(line);
        }
        
        //Print a footer
        System.out.println("-----------------------------------------------------------------");
        
    }//printPageTable
    
    
    


	/*======================================================================
	 * Device Management Methods
	 *----------------------------------------------------------------------
	 */

	/**
	 * registerDevice
	 *
	 * adds a new device to the list of devices managed by the OS
	 *
	 * @param dev     the device driver
	 * @param id      the id to assign to this device
	 * 
	 */
	public void registerDevice(Device dev, int id)
	{
		m_devices.add(new DeviceInfo(dev, id));
	}//registerDevice

	/*======================================================================
	 * Process Management Methods
	 *----------------------------------------------------------------------
	 */

	/**
	 * printProcessTable      **DEBUGGING**
	 *
	 * prints all the processes in the process table
	 */
	private void printProcessTable()
	{
		debugPrintln("");
		debugPrintln("Process Table (" + m_processes.size() + " processes)");
		debugPrintln("======================================================================");
		for(ProcessControlBlock pi : m_processes)
		{
			debugPrintln("    " + pi);
		}//for
		debugPrintln("----------------------------------------------------------------------");

	}//printProcessTable

	/**
	 * removeCurrentProcess
	 * 
	 * Removes the current process from the Process Table, frees the memory that
	 * it had been using, and attempts to schedule a new process
	 */
	public void removeCurrentProcess()
	{
		// Free the block of memory that was allocated
		freeCurrProcessMemBlock();
		System.out.println("Removing Process " + m_currProcess.getProcessId());
		m_processes.remove(m_currProcess);
		// Print memAlloc Table to readability
		System.out.println("RemoveCurr");
		printMemAlloc();
		// Attempt to schedule a new process
		m_currProcess = null;
		scheduleNewProcess();
	}//removeCurrentProcess

	/**
	 * getNextProcess
	 *
	 * selects process based on the state of the current process and the 
	 * priorities of the other processes to determine if it should 
	 * continue running the current process and avoid a context switch or 
	 * continue running. 
	 *
	 * @return a reference to the ProcessControlBlock struct of the selected process
	 * -OR- null if no non-blocked process exists
	 */
	ProcessControlBlock getNextProcess() {
		// Determine if the currently running process should stay running
		if (m_currProcess != null && !m_currProcess.isBlocked()) {
			// Determine if we have passed our allocated number of rounds on the CPU
			if(m_currProcess.quantumCount < ProcessControlBlock.MAX_QUANTUM_CYCLES) {
				m_currProcess.quantumCount++;
				return m_currProcess;
			}
			else {
				m_currProcess.quantumCount = 0;
			}
		}		
		
		// Gather information about other processes if we want a new process
		updatePriority();
		ProcessControlBlock newProc = null;

		// Set the priority as small as it will go
		double highestPriority = -(Double.MAX_VALUE);

		// Iterate and determine the process with the highest priority.  It gets to go next
		for(int i = 0; i < m_processes.size(); i++) {
			ProcessControlBlock thisProcess = m_processes.get(i);
			if(!m_processes.get(i).isBlocked() &&  (thisProcess.getPriority() > highestPriority)) {
				highestPriority = thisProcess.getPriority();
				newProc = thisProcess;
			}
		}

		// Return the chosen process.  If all are blocked this will be null
		return newProc;

	}//getNextProcess


	/**
	 * updatePriority
	 *
	 * Updates the priorities of all PCBs currently active using the average starve
	 * time of each one.  
	 *
	 */
	void updatePriority() {
		for(int i = 0; i < m_processes.size(); i++) {
			m_processes.get(i).setPriority((m_processes.get(i).avgStarve));
		}
	}//priority


	/**
	 * getRandomProcess
	 *
	 * selects a non-Blocked process at random from the ProcessTable.
	 *
	 * @return a reference to the ProcessControlBlock struct of the selected process
	 * -OR- null if no non-blocked process exists
	 */
	ProcessControlBlock getRandomProcess() {
		//Calculate a random offset into the m_processes list
		int offset = ((int)(Math.random() * 2147483647)) % m_processes.size();

		//Iterate until a non-blocked process is found
		ProcessControlBlock newProc = null;
		for(int i = 0; i < m_processes.size(); i++)
		{
			newProc = m_processes.get((i + offset) % m_processes.size());
			if ( ! newProc.isBlocked())
			{
				return newProc;
			}
		}//for

		return null;        // no processes are Ready
	}//getRandomProcess

	/**
	 * scheuleNewProcess      
	 *
	 * Schedules a new process according to the following terms
	 * 	1. If there are no remaining processes, close the simulation
	 *  2. Save the current PCB and get a random process and load its PCB 
	 *     then associate the current process with it
	 */
	public void scheduleNewProcess()
	{   
		// Debugging is turned off here for HW6 completion
		//printProcessTable();

		// If the m_processes vector is empty, then exit    	
		if(m_processes.isEmpty()) {
			debugPrintln("No more processes to run.  Stopping.");
			System.exit(0);
		}

		// If the current process is the idle process, we should remove it.  Why keep it in process table?
		if(m_currProcess != null) {
			if(m_currProcess.getProcessId() == IDLE_PROC_ID) {
				removeCurrentProcess();
			}
		}

		// Get a new process and associate its PCB with the current process
		ProcessControlBlock nextProcess = getRandomProcess();

		// If we have no ready processes, create an idle one and return
		if(nextProcess == null) {
			createIdleProcess();
			return;
		}   	
		if(m_currProcess != null) {
			// Save the current state of the registers of the process
			if(m_currProcess.getProcessId() == nextProcess.getProcessId()) {
				return;
			}
			// Otherwise we have a ready process and we want to load it into the CPU
			m_currProcess.save(m_CPU);
		}
		// We need to switch and restore the register values to the new current process
		m_currProcess = nextProcess;
		m_currProcess.restore(m_CPU);

	}//scheduleNewProcess

	/**
	 * addProgram
	 *
	 * registers a new program with the simulated OS that can be used when the
	 * current process makes an Exec system call.  (Normally the program is
	 * specified by the process via a filename but this is a simulation so the
	 * calling process doesn't actually care what program gets loaded.)
	 *
	 * @param prog  the program to add
	 *
	 */
	public void addProgram(Program prog)
	{
		m_programs.add(prog);
	}//addProgram

	/**
	 * selectBlockedProcess
	 *
	 * select a process to unblock that might be waiting to perform a given
	 * action on a given device.  This is a helper method for system calls
	 * and interrupts that deal with devices.
	 *
	 * @param dev   the Device that the process must be waiting for
	 * @param op    the operation that the process wants to perform on the
	 *              device.  Use the SYSCALL constants for this value.
	 * @param addr  the address the process is reading from.  If the
	 *              operation is a Write or Open then this value can be
	 *              anything
	 *
	 * @return the process to unblock -OR- null if none match the given criteria
	 */
	public ProcessControlBlock selectBlockedProcess(Device dev, int op, int addr)
	{
		ProcessControlBlock selected = null;
		for(ProcessControlBlock pi : m_processes)
		{
			if (pi.isBlockedForDevice(dev, op, addr))
			{
				selected = pi;
				break;
			}
		}//for

		return selected;
	}//selectBlockedProcess

	/**
	 * createIdleProcess
	 *
	 * creates a one instruction process that immediately exits.  This is used
	 * to buy time until device I/O completes and unblocks a legitimate
	 * process.
	 *
	 */
	public void createIdleProcess()
	{
		int progArr[] = { 0, 0, 0, 0,   //SET r0=0
				0, 0, 0, 0,   //SET r0=0 (repeated instruction to account for vagaries in student implementation of the CPU class)
				10, 0, 0, 0,   //PUSH r0
				15, 0, 0, 0 }; //TRAP
	
		
		// Initialize the starting position for this program
		int baseAddr = allocBlock(m_MMU.getPageSize());

		// If allocBlock says we have no room left, print a warning and exit
		if(baseAddr== -1) {
			System.out.println("ERROR: Not enough space in RAM.  Halting simulation.");
			// Then halt the simulation
			System.exit(0);
		}
		
		// Load the program into RAM
		for(int i = 0; i < progArr.length; i++)
		{
			m_MMU.write(baseAddr + i, progArr[i]);
		}
		
		// Save the register info from the current process (if there is one)
		if (m_currProcess != null)
		{
			m_currProcess.save(m_CPU);
		}

		// Set the appropriate registers
		m_CPU.setPC(baseAddr);
		m_CPU.setSP(baseAddr + progArr.length + 10);
		m_CPU.setBASE(baseAddr);
		m_CPU.setLIM(progArr.length + 20);			// This was modified to fit with the base+limit structure of the program

		// Save the relevant info as a new entry in m_processes
		m_currProcess = new ProcessControlBlock(IDLE_PROC_ID);  
		m_processes.add(m_currProcess);

	}//createIdleProcess


	/**
	 * createProcess
	 *
	 * Pushes a value onto the stack by incrementing the stack pointer
	 * then saving the value to that location in memory
	 *
	 * @param prog  the program that will be loaded into RAM and executed
	 * @param allocSize  the amount of space in RAM that the program is to be allocated
	 */
	public void createProcess(Program prog, int allocSize)
	{
		// Determine the program that we need to read
		int process [] = prog.export();
		
		if(allocSize % m_MMU.getPageSize() != 0) {
			allocSize += (m_MMU.getPageSize() - allocSize % m_MMU.getPageSize());
		}

		// Initialize the starting position for this program
		int base = allocBlock(allocSize);

		// If allocBlock says we have no room left, print a warning
		if(base == -1) {
			System.out.println("ERROR: Not enough space in RAM.  Returning to caller.");
			// Then return to the calling process
			return;
		}
		
		// If we have a previously running process, save its state
		if(m_currProcess != null) {
			m_currProcess.save(m_CPU);
		}

		// Get the next process ID, increment the overall ID counter, and associate the current ID
		ProcessControlBlock newBlock = new ProcessControlBlock(m_nextProcessID);
		m_processes.add(newBlock);
		m_currProcess = newBlock;
		m_nextProcessID++;

		// Set all important registers correctly using allocation size
		m_CPU.setBASE(base);
		m_CPU.setLIM(allocSize);

		// SP grows from the top of the instructions upward
		m_CPU.setPC(m_CPU.getBASE());
		m_CPU.setSP(m_CPU.getBASE() + prog.getSize());

		// Go ahead and load the process into RAM
		for(int i = 0; i < process.length; i++) {
			m_MMU.write(i + m_CPU.getBASE(), process[i]);   
		}
		
		// Save to ensure proper register values and printMemAlloc table for info
		m_currProcess.save(m_CPU);
		System.out.println("Create Process");
		printMemAlloc();
		

	}//createProcess


	/*======================================================================
	 * Program Management Methods
	 *----------------------------------------------------------------------
	 */


	/*======================================================================
	 * Interrupt Handlers
	 *----------------------------------------------------------------------
	 */

	/**
	 * interruptIllegalMemoryAccess
	 *
	 * Prints a warning about illegal memory access and then exits gracefully
	 *
	 * @param addr the current address location that is illegal
	 */
	@Override
	public void interruptIllegalMemoryAccess(int addr) {
		System.out.println("ERROR: Illegal Memory Access: " + addr + 
				" (Base: " + m_CPU.getBASE() + " Limit: " + m_CPU.getLIM() + ")");
		System.exit(0);		
	}

	/**
	 * interruptDivideByZero
	 *
	 * Prints a warning that the user has divided by 0 and then exits gracefully
	 */
	@Override
	public void interruptDivideByZero() {
		System.out.println("ERROR: Divide by Zero.");
		System.exit(0);	
	}

	/**
	 * interruptIllegalInstruction
	 *
	 * Prints a warning that the instruction was illegal
	 * and then exits gracefully
	 *
	 * @param instr [] the instruction that was illegal
	 */
	@Override
	public void interruptIllegalInstruction(int[] instr) {
		System.out.println("ERROR: Illegal Instruction: " + 
				instr[0] + " " + instr[1] + " " + instr[2] + " " + instr[3]);
		System.exit(0);	
	}

	/**
	 * interruptClock
	 *
	 * Interrupts the CPU every CLOCK_FREQ for timing and scheduling purposes
	 */
	@Override
	public void interruptClock() {
		scheduleNewProcess();
	}	

	/**
	 * interruptIOReadComplete
	 *
	 * Determines which device has finished being interrupted, and goes 
	 * about unblocking it.  Then it pushes the correct values to the 
	 * stack of the process as well as the read values
	 *
	 * @param devID the ID of the device that we are communicating with
	 * @param addr	the address that we are reading from on the device
	 * @param data	the data that we have read from the device
	 */
	@Override
	public void interruptIOReadComplete(int devID, int addr, int data) {
		Device found = null;
		// Find the device with the correct devID
		for(DeviceInfo deviceLookup : m_devices) {
			if(deviceLookup.getId() == devID) {
				// Once we find it break out to save time
				found = deviceLookup.getDevice();
				break;
			}
		}

		// Find a process that was currently blocked for this device and begin unblocking it
		ProcessControlBlock interruptedPCB = selectBlockedProcess(found, SYSCALL_READ, addr);
		if(interruptedPCB == null) {
			return;
		}

		// Need to write to the stack of the blocked process - the data and the success code
		int interruptedSP = interruptedPCB.getRegisterValue(CPU.SP);
		interruptedSP++;
		interruptedPCB.setRegisterValue(CPU.SP, interruptedSP);
		m_MMU.write(interruptedSP, data);

		interruptedSP++;
		interruptedPCB.setRegisterValue(CPU.SP, interruptedSP);
		m_MMU.write(interruptedSP, SYSCALL_SUCCESS);

		// Unblock the Process and Push address and success code to the stack
		//debugPrintln("Process " + interruptedPCB.getProcessId() + " is moved to the ready state");
		interruptedPCB.unblock();
	}

	/**
	 * interruptIOWriteComplete
	 *
	 * Determines which device has finished being interrupted, and goes 
	 * about unblocking it.  Then it pushes the correct success code to the
	 * stack of the process
	 *
	 * @param devID the ID of the device that we are communicating with
	 * @param addr	the address that we are writing to on the device
	 */
	@Override
	public void interruptIOWriteComplete(int devID, int addr) {
		Device found = null;
		// Iterate and find the device that matches the devID
		for(DeviceInfo deviceLookup : m_devices) {
			if(deviceLookup.getId() == devID) {
				found = deviceLookup.getDevice();
				break;
			}
		}
		// Find the process that was blocked for this device and let it unblock
		ProcessControlBlock interruptedPCB = selectBlockedProcess(found, SYSCALL_WRITE, addr);
		if(interruptedPCB == null) {
			return;
		}

		// Push a success code to the blocked process stack
		int interruptedSP = interruptedPCB.getRegisterValue(CPU.SP);
		interruptedSP++;
		interruptedPCB.setRegisterValue(CPU.SP, interruptedSP);
		m_MMU.write(interruptedSP, SYSCALL_SUCCESS);

		// Unblock it
		//debugPrintln("Process " + interruptedPCB.getProcessId() + " is moved to the ready state");
		interruptedPCB.unblock();
	}



	/*======================================================================
	 * System Calls
	 *----------------------------------------------------------------------
	 */


	/**
	 * systemCall
	 *
	 * Method that is called when the trap instruction is called.
	 * It handles the trap differently based on the values currently
	 * pushed to the stack
	 *
	 */
	public void systemCall()
	{
		// Determine what system call it is by retrieving data from the stack
		int syscall = pop();

		// Switch based on the syscall
		switch(syscall)
		{
		// Each syscall has its own method to perform operations
		case SYSCALL_EXIT:
			syscallExit();
			break;
		case SYSCALL_OUTPUT:
			syscallOutput();
			break;
		case SYSCALL_GETPID:
			syscallGetPID();
			break;
		case SYSCALL_OPEN:
			syscallOpen();
			break;
		case SYSCALL_CLOSE:
			syscallClose();
			break;
		case SYSCALL_READ:
			syscallRead();
			break;
		case SYSCALL_WRITE:
			syscallWrite();
			break;         
		case SYSCALL_EXEC:
			syscallExec();
			break;
		case SYSCALL_YIELD:
			syscallYield();
			break;
		case SYSCALL_COREDUMP:
			syscallCoreDump();
			break;
		default:        
			// If the syscall is invalid, warn the user
			System.out.println("ERROR: Invalid Syscall: " + syscall);
			break;          
		}//switch

	}

	/**
	 * syscallExit
	 *
	 * Removes the current Process from the Process Table
	 *
	 */
	private void syscallExit() {
		removeCurrentProcess();
	}//exit

	/**
	 * syscallOutput
	 *
	 * Retrieves the values that the user has put onto the stack and prints it
	 *
	 */
	private void syscallOutput() {
		int output = pop();
		System.out.println("OUTPUT: " + output);
	}//output

	/**
	 * syscallGetPID
	 *
	 * Gets the PID of the current process and pushes it to the stack for use later
	 *
	 */
	private void syscallGetPID() {
		int processID = m_currProcess.getProcessId();
		push(processID);
	}//getPID

	/**
	 * syscallOpen
	 *
	 * Attempts to open the device via the given DeviceID.  If this is not
	 * possible, or the device has already been opened, an error code it returned.
	 * Otherwise, the current process is added to the DeviceInfo, and a success
	 * status code is returned
	 *
	 */
	private void syscallOpen() {
		int deviceID = pop();
		// Start the error code at success and decrement as necessary
		int statusCode = SYSCALL_SUCCESS;

		for(DeviceInfo deviceLookup : m_devices) {
			if(deviceLookup.getId() == deviceID) {
				// Determine if we can open the device
				if(deviceLookup.containsProcess(m_currProcess)) {
					statusCode += SYSCALL_DEVICE_UNAVAILABLE;
				}
				// Check shareable and use for multiple processes
				if(!deviceLookup.getDevice().isSharable() && !deviceLookup.unused()) {
					// Add the device to the current list for the device, but block the process
					deviceLookup.addProcess(m_currProcess);
					m_currProcess.block(m_CPU, deviceLookup.getDevice(), SYSCALL_OPEN, 0); // what does the 0 do?

					// Push a success status code.  Then go about scheduling a new process
					statusCode = SYSCALL_SUCCESS;
					push(statusCode);
					scheduleNewProcess();
					return;
				}

				// If everything is good so far, go ahead and open the device
				if(statusCode == SYSCALL_SUCCESS) {
					deviceLookup.addProcess(m_currProcess);
					push(statusCode);
				}
				else {
					push(SYSCALL_OPEN_FAIL + statusCode);
				}
				return;
			}		
		}//for
		// Device does not exist if it isn't found in the for loop + open failure
		push(SYSCALL_OPEN_FAIL + SYSCALL_DEVICE_DOESNT_EXIST);
	}//open

	/**
	 * syscallClose
	 *
	 * Attempts to close the device given the Device ID.  However,
	 * an error code is calculated and sent if the device has not been opened,
	 * cannot be closed, or was unavailable at the moment.  Otherwise, the process
	 * is removed from the device and a success code (0) is returned
	 *
	 */
	private void syscallClose() {
		int deviceID = pop();
		int statusCode = SYSCALL_SUCCESS;

		for(DeviceInfo deviceLookup : m_devices) {
			if(deviceLookup.getId() == deviceID) {
				// Check to make sure we can remove the device
				if(!deviceLookup.containsProcess(m_currProcess)) {
					statusCode += SYSCALL_DEVICE_UNAVAILABLE;
				}
				// If we have no other problems, then remove the process and push the status code
				if(statusCode == SYSCALL_SUCCESS) {
					deviceLookup.removeProcess(m_currProcess);
					push(statusCode);
					// Once we have successfully closed a device, any of the other processes that want the device can use it
					ProcessControlBlock unblockProcess = selectBlockedProcess(deviceLookup.getDevice(), SYSCALL_OPEN, 0);
					if(unblockProcess != null) {
						// Unblock the first one that we find
						//debugPrintln("Processs " + unblockProcess.getProcessId() + " is moved to the ready state.");
						unblockProcess.unblock();

					}
				}
				else {
					// Close fail and other errors that have been detected
					push(SYSCALL_CLOSE_FAIL + statusCode);
				}
				return;
			}
		}//for
		// Device does not exist if it isn't found in the for loop + close failure
		push(SYSCALL_CLOSE_FAIL + SYSCALL_DEVICE_DOESNT_EXIST);
	}//close

	/**
	 * syscallRead
	 *
	 * Attempts to read from the device given by its ID.  If the device 
	 * has not been opened, or cannot be read from for other reasons, an
	 * error code it thrown.  Otherwise, the data is read and pushed to the 
	 * stack.  Then, the process is blocked.  If the device cannot read due
	 * to other processes using the device, it will wait to run the read 
	 * syscall until next time
	 * 
	 */
	private void syscallRead() {
		// Get the address to write to and the device ID
		int addr = pop();
		int deviceID = pop();
		int statusCode = SYSCALL_SUCCESS;

		for(DeviceInfo deviceLookup : m_devices) {
			if(deviceLookup.getId() == deviceID) {
				Device currentDevice = deviceLookup.getDevice();
				// Ensure that we can read from the device
				if(!currentDevice.isAvailable()) {
					// We should probably push the values back onto the stack
					push(deviceID);
					push(addr);
					push(SYSCALL_READ);
					// Decrement the PC so that the Trap is re-executed
					m_CPU.decrementPC();
					// Pick a new process
					scheduleNewProcess();
					return;
				}

				// If we have no opened the process we have a problem
				if(!deviceLookup.containsProcess(m_currProcess)) {
					statusCode += SYSCALL_OPEN_FAIL;
				}

				// Need to ensure that we can read from the device before we read from the device
				if(!currentDevice.isReadable()) {
					statusCode += SYSCALL_DEVICE_UNREADABLE;
				}

				if(statusCode == SYSCALL_SUCCESS) {
					// Push the read data to the stack once we read from the device
					currentDevice.read(addr);
					// Then block and get a new process to run while we wait
					m_currProcess.block(m_CPU, currentDevice, SYSCALL_READ, addr);
					scheduleNewProcess();
				}
				else {
					push(statusCode + SYSCALL_READ_FAIL);
				}
				return;
			}
		}//for
		// Device does not exist if it isn't found in the for loop + read failure
		push(SYSCALL_READ_FAIL + SYSCALL_DEVICE_DOESNT_EXIST);
	}//read

	/**
	 * syscallWrite
	 *
	 * Attempts to write data to a device after checking to see if the 
	 * device can be written to and is open.  Once these conditions are met,
	 * the OS will write data to the device followed by a block to ensure
	 * that no one else writes to the device.  It will also schedule a new 
	 * process if it cannot write due to other processes using the device 
	 *
	 */
	private void syscallWrite() {
		// Get the data and address to write to
		int data = pop();
		int addr = pop();
		int deviceID = pop();
		int statusCode = SYSCALL_SUCCESS;

		for(DeviceInfo deviceLookup : m_devices) {
			if(deviceLookup.getId() == deviceID) {
				Device currentDevice = deviceLookup.getDevice();
				// Ensure that the current device can be written to
				if(!currentDevice.isAvailable()) {
					// We should probably push the values back onto the stack
					push(deviceID);
					push(addr);
					push(data);
					push(SYSCALL_WRITE);
					// Decrement the program counter so we re-execute the TRAP instruction
					m_CPU.decrementPC();
					// Pick a new process
					scheduleNewProcess();
					return;
				}
				// We must open a device before we can write to it
				if(!deviceLookup.containsProcess(m_currProcess)) {
					statusCode += SYSCALL_OPEN_FAIL;
				}
				// The device must also be writeable
				if(!currentDevice.isWriteable()) {
					statusCode += SYSCALL_DEVICE_UNWRITEABLE;
				}
				if(statusCode == SYSCALL_SUCCESS) {
					// Write the data to the device
					currentDevice.write(addr, data);
					// Then block and get a new process to run while we wait
					m_currProcess.block(m_CPU, currentDevice, SYSCALL_WRITE, addr);
					scheduleNewProcess();
				}
				else {
					// Push a write fail and the other errors that were calculated
					push(statusCode + SYSCALL_WRITE_FAIL);
				}
				return;
			}
		}//for
		// Device does not exist if it isn't found in the for loop + write failure
		push(SYSCALL_WRITE_FAIL + SYSCALL_DEVICE_DOESNT_EXIST);
	}//write

	/**
	 * syscallExec
	 *
	 * creates a new process.  The program used to create that process is chosen
	 * semi-randomly from all the programs that have been registered with the OS
	 * via {@link #addProgram}.  Limits are put into place to ensure that each
	 * process is run an equal number of times.  If no programs have been
	 * registered then the simulation is aborted with a fatal error.
	 *
	 */
	private void syscallExec()
	{
		//If there is nothing to run, abort.  This should never happen.
		if (m_programs.size() == 0)
		{
			System.err.println("ERROR!  syscallExec has no programs to run.");
			System.exit(-1);
		}

		//find out which program has been called the least and record how many
		//times it has been called
		int leastCallCount = m_programs.get(0).callCount;
		for(Program prog : m_programs)
		{
			if (prog.callCount < leastCallCount)
			{
				leastCallCount = prog.callCount;
			}
		}

		//Create a vector of all programs that have been called the least number
		//of times
		Vector<Program> cands = new Vector<Program>();
		for(Program prog : m_programs)
		{
			cands.add(prog);
		}

		//Select a random program from the candidates list
		Random rand = new Random();
		int pn = rand.nextInt(m_programs.size());
		Program prog = cands.get(pn);

		//Determine the address space size using the default if available.
		//Otherwise, use a multiple of the program size.
		int allocSize = prog.getDefaultAllocSize();
		if (allocSize <= 0)
		{
			allocSize = prog.getSize() * 2;
		}

		//Load the program into RAM
		createProcess(prog, allocSize);

	}//exec



	/**
	 * syscallYield
	 *
	 * If the current process is willing to be stopped, then schedule a new process to run
	 *
	 */
	private void syscallYield()
	{
		scheduleNewProcess();
	}//yield


	/**
	 * syscallCoreDump
	 *
	 * Uses the regDump functionality to print the current states of all registers
	 * as well as printing the top 3 items that are on the stack and exits the program
	 *
	 */
	private void syscallCoreDump() {
		// Print the states of all registers
		m_CPU.regDump();
		// Let the user know that the stack is being printed
		System.out.println("Items on Stack");
		// Print the top 3 items on the stack
		for(int i = 0; i < 3; ++i) {
			int itemToPop = pop();
			System.out.println("Item " + (i+1) + ": " + itemToPop);
		} //for
		// Exit the program
		syscallExit();
	}//coredump


	//======================================================================
	// Inner Classes
	//----------------------------------------------------------------------

	/**
	 * class ProcessControlBlock
	 *
	 * This class contains information about a currently active process.
	 */
	private class ProcessControlBlock implements Comparable<ProcessControlBlock>
	{
		/**
		 * a unique id for this process
		 */
		private int processId = 0;

		/**
		 * These are the process' current registers.  If the process is in the
		 * "running" state then these are out of date
		 */
		private int[] registers = null;

		/**
		 * If this process is blocked a reference to the Device is stored here
		 */
		private Device blockedForDevice = null;

		/**
		 * If this process is blocked a reference to the type of I/O operation
		 * is stored here (use the SYSCALL constants defined in SOS)
		 */
		private int blockedForOperation = -1;

		/**
		 * If this process is blocked reading from a device, the requested
		 * address is stored here.
		 */
		private int blockedForAddr = -1;

		/**
		 * the time it takes to load and save registers, specified as a number
		 * of CPU ticks
		 */
		private static final int SAVE_LOAD_TIME = 30;

		/**
		 * The maximum number of clock interrupts a process may run through
		 */
		private static final int MAX_QUANTUM_CYCLES = 25;

		/**
		 * Used to store the system time when a process is moved to the Ready
		 * state.
		 */
		private int lastReadyTime = -1;

		/**
		 * Used to store the number of times this process has been in the ready
		 * state
		 */
		private int numReady = 0;

		/**
		 * Used to store the maximum starve time experienced by this process
		 */
		private int maxStarve = -1;

		/**
		 * Used to store the average starve time for this process
		 */
		private double avgStarve = 0;

		/**
		 * Used to store the priority of the process
		 */
		private double priority = -1;
		
		/**
		 * Used to store the max number of times that a process can stay on the CPU
		 */
		private int quantumCount = 0;

		/**
		 * constructor
		 *
		 * @param pid        a process id for the process.  The caller is
		 *                   responsible for making sure it is unique.
		 */
		public ProcessControlBlock(int pid)
		{
			this.processId = pid;
		}

		/**
		 * @return the current process' id
		 */
		public int getProcessId()
		{
			return this.processId;
		}

		/**
		 * @return the last time this process was put in the Ready state
		 */
		public long getLastReadyTime()
		{
			return lastReadyTime;
		}

		/**
		 * gets the current priority of the process
		 */
		public double getPriority()
		{
			return this.priority;
		}

		/**
		 * sets the current priority of the process
		 */
		public void setPriority(double newPriority)
		{
			this.priority = newPriority;
		}

		/**
		 * save
		 *
		 * saves the current CPU registers into this.registers
		 *
		 * @param cpu  the CPU object to save the values from
		 */
		public void save(CPU cpu)
		{
			//A context switch is expensive.  We simluate that here by 
			//adding ticks to m_CPU
			m_CPU.addTicks(SAVE_LOAD_TIME);

			//Save the registers
			int[] regs = cpu.getRegisters();
			this.registers = new int[CPU.NUMREG];
			for(int i = 0; i < CPU.NUMREG; i++)
			{
				this.registers[i] = regs[i];
			}

			//Assuming this method is being called because the process is moving
			//out of the Running state, record the current system time for
			//calculating starve times for this process.  If this method is
			//being called for a Block, we'll adjust lastReadyTime in the
			//unblock method.
			numReady++;
			lastReadyTime = m_CPU.getTicks();


		}//save

		/**
		 * restore
		 *
		 * restores the saved values in this.registers to the current CPU's
		 * registers
		 *
		 * @param cpu  the CPU object to restore the values to
		 */
		public void restore(CPU cpu)
		{
			//A context switch is expensive.  We simulate that here by 
			//adding ticks to m_CPU
			m_CPU.addTicks(SAVE_LOAD_TIME);

			//Restore the register values
			int[] regs = cpu.getRegisters();
			for(int i = 0; i < CPU.NUMREG; i++)
			{
				regs[i] = this.registers[i];
			}

			//Record the starve time statistics
			int starveTime = m_CPU.getTicks() - lastReadyTime;
			if (starveTime > maxStarve)
			{
				maxStarve = starveTime;
			}
			double d_numReady = (double)numReady;
			avgStarve = avgStarve * (d_numReady - 1.0) / d_numReady;
			avgStarve = avgStarve + (starveTime * (1.0 / d_numReady));

		}//restore

		/**
		 * block
		 *
		 * blocks the current process to wait for I/O.  The caller is
		 * responsible for calling {@link CPU#scheduleNewProcess}
		 * after calling this method.
		 *
		 * @param cpu   the CPU that the process is running on
		 * @param dev   the Device that the process must wait for
		 * @param op    the operation that the process is performing on the
		 *              device.  Use the SYSCALL constants for this value.
		 * @param addr  the address the process is reading from (for SYSCALL_READ)
		 * 
		 */
		public void block(CPU cpu, Device dev, int op, int addr)
		{
			blockedForDevice = dev;
			blockedForOperation = op;
			blockedForAddr = addr;

		}//block

		/**
		 * unblock
		 *
		 * moves this process from the Blocked (waiting) state to the Ready
		 * state. 
		 *
		 */
		public void unblock()
		{
			//Reset the info about the block
			blockedForDevice = null;
			blockedForOperation = -1;
			blockedForAddr = -1;

			//Assuming this method is being called because the process is moving
			//from the Blocked state to the Ready state, record the current
			//system time for calculating starve times for this process.
			lastReadyTime = m_CPU.getTicks();

		}//unblock

		/**
		 * isBlocked
		 *
		 * @return true if the process is blocked
		 */
		public boolean isBlocked()
		{
			return (blockedForDevice != null);
		}//isBlocked

		/**
		 * isBlockedForDevice
		 *
		 * Checks to see if the process is blocked for the given device,
		 * operation and address.  If the operation is not an open, the given
		 * address is ignored.
		 *
		 * @param dev   check to see if the process is waiting for this device
		 * @param op    check to see if the process is waiting for this operation
		 * @param addr  check to see if the process is reading from this address
		 *
		 * @return true if the process is blocked by the given parameters
		 */
		public boolean isBlockedForDevice(Device dev, int op, int addr)
		{
			if ( (blockedForDevice == dev) && (blockedForOperation == op) )
			{
				if (op == SYSCALL_OPEN)
				{
					return true;
				}

				if (addr == blockedForAddr)
				{
					return true;
				}
			}//if

			return false;
		}//isBlockedForDevice

		/**
		 * compareTo              
		 *
		 * compares this to another ProcessControlBlock object based on the BASE addr
		 * register.  Read about Java's Collections class for info on
		 * how this method can be quite useful to you.
		 */
		public int compareTo(ProcessControlBlock pi)
		{
			return this.registers[CPU.BASE] - pi.registers[CPU.BASE];
		}

		/**
		 * getRegisterValue
		 *
		 * Retrieves the value of a process' register that is stored in this
		 * object (this.registers).
		 * 
		 * @param idx the index of the register to retrieve.  Use the constants
		 *            in the CPU class
		 * @return one of the register values stored in in this object or -999
		 *         if an invalid index is given 
		 */
		public int getRegisterValue(int idx)
		{
			if ((idx < 0) || (idx >= CPU.NUMREG))
			{
				return -999;    // invalid index
			}

			return this.registers[idx];
		}//getRegisterValue

		/**
		 * setRegisterValue
		 *
		 * Sets the value of a process' register that is stored in this
		 * object (this.registers).  
		 * 
		 * @param idx the index of the register to set.  Use the constants
		 *            in the CPU class.  If an invalid index is given, this
		 *            method does nothing.
		 * @param val the value to set the register to
		 */
		public void setRegisterValue(int idx, int val)
		{
			if ((idx < 0) || (idx >= CPU.NUMREG))
			{
				return;    // invalid index
			}

			this.registers[idx] = val;
		}//setRegisterValue



		/**
		 * overallAvgStarve
		 *
		 * @return the overall average starve time for all currently running
		 *         processes
		 *
		 */
		public double overallAvgStarve()
		{
			double result = 0.0;
			int count = 0;
			for(ProcessControlBlock pi : m_processes)
			{
				if (pi.avgStarve > 0)
				{
					result = result + pi.avgStarve;
					count++;
				}
			}
			if (count > 0)
			{
				result = result / count;
			}

			return result;
		}//overallAvgStarve
		
		
		/**
		 * move 
		 * 
		 * Moves a process to a new location in RAM and updates its stored
		 * register values 
		 * 
		 * @param newBase - the base value that you want to move the process to
		 * @return boolean determining if the move failed or succeeded
		 */
        public boolean move(int newBase)
        {
        	System.out.println("Before:");
        	printPageTable();
        	
        	// Get the old starting location
        	int oldBase = this.getRegisterValue(CPU.BASE);
        	
        	// If we are moving something to its current location, don't bother
        	if(oldBase == newBase) {
        		return true;
        	}
        	
        	System.out.println("New Base: " + newBase + " Old Base: " + oldBase);
        	
        	// If the newbase sucks, stop
        	if(newBase % m_MMU.getPageSize() != 0) {
        		System.out.println("ERROR!");
        	}
        	
        	// Get the register values relative to the old location
        	int oldLim = this.getRegisterValue(CPU.LIM);
        	int oldPC = this.getRegisterValue(CPU.PC);
        	int oldSP = this.getRegisterValue(CPU.SP);
        	
        	// Check if a valid location
        	if(newBase < 0 || (newBase + oldLim) >= m_MMU.getSize()) {
        		return false;
        	}
        	
        	int pageSize = m_MMU.getPageSize();
        	
        	// Determine the number of pages spanned
        	int pagesSpanned = oldLim / pageSize; // Should always yield a whole number
        	int oldPage = oldBase / pageSize; 
        	int newPage = newBase / pageSize; 
        	
        	// Update the Page Table
        	for(int i = 0; i < pagesSpanned; i++) {
        		int temp = m_RAM.read(newPage + i);
        		m_RAM.write(newPage + i, oldBase + (i * pageSize));
        		m_RAM.write(oldPage + i, temp);
        	}
        	
//        	// Set the new Base Value - but not limit
//        	this.setRegisterValue(CPU.BASE, newBase);
//        	// Set the new PC Value based on the difference from the old one
//        	int newPC = newBase + (oldPC - oldBase);
//        	this.setRegisterValue(CPU.PC, newPC);
//        	// Set the new SP Value based on the difference from the old one
//        	int newSP = newBase + (oldSP - oldBase);
//        	this.setRegisterValue(CPU.SP, newSP);
//        	
//        	
//        	// If we are trying to move the current running process
//        	if(this.getProcessId() == m_currProcess.getProcessId()) {
//        		// Set the important values on the CPU back if we are running
//        		m_CPU.setBASE(newBase);
//        		m_CPU.setPC(newPC);
//        		m_CPU.setSP(newSP);
//        	}
        	
        	System.out.println("After:");
        	printPageTable();
        	// Print a statement about the move
        	debugPrintln("Process " + this.getProcessId() + " has moved from " + oldBase + " to " + newBase);
        	printMemAlloc();
			// We have successfully moved the process
        	return true;

        }//move
        
        //TODO
        void printPageTable(){
        	for(int i = 0; i < 64; i++){
        		System.out.println("Slot: "+ i + " Value: " + m_RAM.read(i));
        	}
        }
		/**
		 * toString       **DEBUGGING**
		 *
		 * @return a string representation of this class
		 */
		 public String toString()
		{
			 //Print the Process ID and process state (READY, RUNNING, BLOCKED)
			 String result = "Process id " + processId + " ";
			 if (isBlocked())
			 {
				 result = result + "is BLOCKED for ";
				 //Print device, syscall and address that caused the BLOCKED state
				 if (blockedForOperation == SYSCALL_OPEN)
				 {
					 result = result + "OPEN";
				 }
				 else
				 {
					 result = result + "WRITE @" + blockedForAddr;
				 }
				 for(DeviceInfo di : m_devices)
				 {
					 if (di.getDevice() == blockedForDevice)
					 {
						 result = result + " on device #" + di.getId();
						 break;
					 }
				 }
				 result = result + ": ";
			 }
			 else if (this == m_currProcess)
			 {
				 result = result + "is RUNNING: ";
			 }
			 else
			 {
				 result = result + "is READY: ";
			 }

			 //Print the register values stored in this object.  These don't
			 //necessarily match what's on the CPU for a Running process.
			 if (registers == null)
			 {
				 result = result + "<never saved>";
				 return result;
			 }

			 for(int i = 0; i < CPU.NUMGENREG; i++)
			 {
				 result = result + ("r" + i + "=" + registers[i] + " ");
			 }//for
			 result = result + ("PC=" + registers[CPU.PC] + " ");
			 result = result + ("SP=" + registers[CPU.SP] + " ");
			 result = result + ("BASE=" + registers[CPU.BASE] + " ");
			 result = result + ("LIM=" + registers[CPU.LIM] + " ");

			 //Print the starve time statistics for this process
			 result = result + "\n\t\t\t";
			 result = result + " Max Starve Time: " + maxStarve;
			 result = result + " Avg Starve Time: " + avgStarve;

			 return result;
		}//toString




	}//class ProcessControlBlock

	/**
	 * class DeviceInfo
	 *
	 * This class contains information about a device that is currently
	 * registered with the system.
	 */
	private class DeviceInfo
	{
		/** every device has a unique id */
		private int id;
		/** a reference to the device driver for this device */
		private Device device;
		/** a list of processes that have opened this device */
		private Vector<ProcessControlBlock> procs;

		/**
		 * constructor
		 *
		 * @param d          a reference to the device driver for this device
		 * @param initID     the id for this device.  The caller is responsible
		 *                   for guaranteeing that this is a unique id.
		 */
		public DeviceInfo(Device d, int initID)
		{
			this.id = initID;
			this.device = d;
			d.setId(initID);
			this.procs = new Vector<ProcessControlBlock>();
		}

		/** @return the device's id */
		public int getId()
		{
			return this.id;
		}

		/** @return this device's driver */
		public Device getDevice()
		{
			return this.device;
		}

		/** Register a new process as having opened this device */
		public void addProcess(ProcessControlBlock pi)
		{
			procs.add(pi);
		}

		/** Register a process as having closed this device */
		public void removeProcess(ProcessControlBlock pi)
		{
			procs.remove(pi);
		}

		/** Does the given process currently have this device opened? */
		public boolean containsProcess(ProcessControlBlock pi)
		{
			return procs.contains(pi);
		}

		/** Is this device currently not opened by any process? */
		public boolean unused()
		{
			return procs.size() == 0;
		}

	}//class DeviceInfo
	
    /**
     * class MemBlock
     *
     * This class contains relevant info about a memory block in RAM.
     *
     */
    private class MemBlock implements Comparable<MemBlock>
    {
        /** the address of the block */
        private int m_addr;
        /** the size of the block */
        private int m_size;

        /**
         * ctor does nothing special
         */
        public MemBlock(int addr, int size)
        {
            m_addr = addr;
            m_size = size;
        }

        /** accessor methods */
        public int getAddr() { return m_addr; }
        public int getSize() { return m_size; }
        
        /**
         * compareTo              
         *
         * compares this to another MemBlock object based on address
         */
        public int compareTo(MemBlock m)
        {
            return this.m_addr - m.m_addr;
        }

    }//class MemBlock

};//class SOS
