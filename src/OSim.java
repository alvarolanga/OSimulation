import java.util.*; //import Java utilities

public class OSim { //Main class for the Operating System

    static class Process { //Process info stored here

        int pid; //process ID
        int arrivalTime; //time when the process arrives
        int burstTime; //how long a process needs CPU
        int remainingTime; //time left
        int memoryRequired; //memory needed
        int startTime = -1; //when the process starts
        int completionTime = -1; //when the process finishes
        int waitingTime = 0; //time waiting in queue
        int turnaroundTime = 0; //total time from arrival to completion
        boolean isCompleted = false; //true if done

        Process(int pid, int arrivalTime, int burstTime, int memoryRequired) {
            this.pid = pid;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.remainingTime = burstTime;
            this.memoryRequired = memoryRequired;
        }
    }

    static class MemoryBlock { //info about memory blocks
        int start, size; //memory block position and size
        boolean isFree = true; //true if not used
        int allocatedTo = -1; //process using it (-1 = none)

        MemoryBlock(int start, int size) {
            this.start = start;
            this.size = size;
        }
    }

    static class OperatingSystem {
        List<Process> processes; //all processes
        List<MemoryBlock> memory; //all memory blocks
        int totalMemory; //total memory size
        String memoryStrategy = "First-Fit"; //default memory allocation method

        OperatingSystem(int memorySize) {
            this.totalMemory = memorySize;
            this.memory = new ArrayList<>();
            this.memory.add(new MemoryBlock(0, memorySize)); //start with one free block
            this.processes = new ArrayList<>();
        }
        //Set the memory allocation strategy
        void setMemoryStrategy(String strategy) {
            this.memoryStrategy = strategy;
        }

        void addProcess(Process p) {
            processes.add(p); //add new process
        }

        boolean allocateMemory(Process p) { //allocates memory to a process using the strategy that the user chooses
            if (memoryStrategy.equals("First-Fit")) {
                for (MemoryBlock block : memory) {
                    if (block.isFree && block.size >= p.memoryRequired) {
                        block.isFree = false;
                        block.allocatedTo = p.pid;
                        return true; //memory given
                    }
                }
            } else if (memoryStrategy.equals("Best-Fit")) {
                MemoryBlock best = null;
                for (MemoryBlock block : memory) {
                    if (block.isFree && block.size >= p.memoryRequired) {
                        if (best == null || block.size < best.size) {
                            best = block; //choose smallest block that fits
                        }
                    }
                }
                if (best != null) {
                    best.isFree = false;
                    best.allocatedTo = p.pid;
                    return true; //memory given
                }
            }
            return false; //not enough memory
        }

        void deallocateMemory(int pid) { //moves allocatted memory to a process
            for (MemoryBlock block : memory) {
                if (block.allocatedTo == pid) {
                    block.isFree = true;
                    block.allocatedTo = -1;
                    break; //memory released
                }
            }
        }

        void runFCFS() { //runs first come first served
            processes.sort(Comparator.comparingInt(p -> p.arrivalTime)); //order by arrival
            int currentTime = 0;
            int cpuBusyTime = 0;

            for (Process p : processes) {
                if (!allocateMemory(p)) {
                    System.out.println("Not enough memory for Process " + p.pid);
                    continue; //skip if memory fails
                }

                currentTime = Math.max(currentTime, p.arrivalTime); //wait if needed
                p.startTime = currentTime;
                p.completionTime = currentTime + p.burstTime;
                p.waitingTime = p.startTime - p.arrivalTime;
                p.turnaroundTime = p.completionTime - p.arrivalTime;
                p.isCompleted = true;

                currentTime = p.completionTime;
                cpuBusyTime += p.burstTime;

                deallocateMemory(p.pid); //free memory after the process is over
            }

            printMetrics(cpuBusyTime, currentTime); //displays the info to the user
        }

        void runSJF() { //runs 5shortest job first
            int currentTime = 0;
            int completed = 0;
            int cpuBusyTime = 0;
            int n = processes.size();

            List<Process> readyQueue = new ArrayList<>(); //processes ready to run

            while (completed < n) {
                for (Process p : processes) { //adds new process to the ready queue
                    if (p.arrivalTime <= currentTime && !p.isCompleted && !readyQueue.contains(p)) {
                        readyQueue.add(p);
                    }
                }
                readyQueue.sort(Comparator.comparingInt(p -> p.burstTime)); //shortest burst time
                boolean allocated = false;

                for (Process p : readyQueue) {
                    if (allocateMemory(p)) {
                        p.startTime = currentTime;
                        p.completionTime = currentTime + p.burstTime;
                        p.waitingTime = p.startTime - p.arrivalTime;
                        p.turnaroundTime = p.completionTime - p.arrivalTime;
                        p.isCompleted = true;

                        currentTime = p.completionTime;
                        cpuBusyTime += p.burstTime;
                        deallocateMemory(p.pid);
                        readyQueue.remove(p);
                        completed++;
                        allocated = true;
                        break; //run one process
                    }
                }

                if (!allocated) {
                    currentTime++; //waits for new process
                }
            }

            printMetrics(cpuBusyTime, currentTime); //displays the info to the user
        }

        void printMetrics(int cpuTime, int totalTime) { //used to display the perfomance stats of the processes after the chosen scheduling method
            System.out.println("\n--- Process Info ---"); //displays the info to the user
            int totalWT = 0, totalTAT = 0;

            for (Process p : processes) {
                if (p.isCompleted) {
                    totalWT += p.waitingTime;
                    totalTAT += p.turnaroundTime;
                    System.out.printf("PID %d | AT:%d | BT:%d | ST:%d | CT:%d | WT:%d | TAT:%d\n",
                            p.pid, p.arrivalTime, p.burstTime, p.startTime, p.completionTime, p.waitingTime, p.turnaroundTime);
                } //displays the process statistics
            }

            System.out.printf("\nAvg Waiting Time: %.2f\n", (double) totalWT / processes.size()); //displays the average waiting time
            System.out.printf("Avg Turnaround Time: %.2f\n", (double) totalTAT / processes.size()); //displays the average waiting time
            System.out.printf("CPU Usage: %.2f%%\n", (cpuTime * 100.0) / totalTime); //displays the CPU usage percentage
            System.out.printf("Throughput: %.2f processes/unit time\n", (processes.size() * 1.0) / totalTime); //displays the number of processes completed as well total time taken
        }
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in); //scanner for user input

        System.out.println("Enter total memory size: "); //creates an input for  total memory size
        int memSize = sc.nextInt();

        OperatingSystem os = new OperatingSystem(memSize); //OS object for the specific memory size

        System.out.println("Enter number of processes: "); //creates an input for number of processes
        int n = sc.nextInt();

        for (int i = 0; i < n; i++) {
            System.out.printf("Enter arrival time, burst time and memory for Process %d: ", i + 1);
            int at = sc.nextInt(); //arrival time
            int bt = sc.nextInt(); //burst time
            int mem = sc.nextInt(); //memory requirement

            os.addProcess(new Process(i + 1, at, bt, mem)); //adds the process to OS
        }

        System.out.println("Choose Memory Strategy: 1. First-Fit 2. Best-Fit"); //gives the user the option to choose the strategy
        int strategyChoice = sc.nextInt();

        if (strategyChoice == 1) {
            os.setMemoryStrategy("First-Fit"); //first-fit memory
        } else {
            os.setMemoryStrategy("Best-Fit"); //best-fit memory
        }

        boolean exit = false; //controls loop

        while (!exit) { //scheduling options
            System.out.println("\nChoose Scheduling:");
            System.out.println("1. FCFS"); //first come first served
            System.out.println("2. SJF"); //Shortest Job First
            System.out.println("3. Exit"); //Exit the Program

            int choice = sc.nextInt(); //get user choice

            switch (choice) {
                case 1:
                    System.out.println("\nRunning FCFS...");
                    os.runFCFS(); //run First come first served
                    exit = true; //exit after execution
                    break;
                case 2:
                    System.out.println("\nRunning SJF...");
                    os.runSJF(); //run Shortest job first
                    exit = true; //exit after execution
                    break;
                case 3:
                    exit = true; //Invalid choice
                    break;
                default:
                    System.out.println("Invalid choice, try again.");
                    break;
            }
        }

        sc.close(); //closes the scanner
    }
}
