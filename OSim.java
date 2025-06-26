import java.util.*; //import Java utilities

public class OSim { //Main class for the Operating System

    static class Process { //Process info stored here

        int pid; //process ID
        int arrivalTime; //when process comes in
        int burstTime; //how long process needs CPU
        int remainingTime; //time left (used in RR before)
        int memoryRequired; //memory needed
        int startTime = -1; //when process starts
        int completionTime = -1; //when process finishes
        int waitingTime = 0; //time waiting in queue
        int turnaroundTime = 0; //total time in system
        boolean isCompleted = false; //true if done

        Process(int pid, int arrivalTime, int burstTime, int memoryRequired) {
            this.pid = pid;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.remainingTime = burstTime; //not used now
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
        String memoryStrategy = "First-Fit"; //default memory method

        OperatingSystem(int memorySize) {
            this.totalMemory = memorySize;
            this.memory = new ArrayList<>();
            this.memory.add(new MemoryBlock(0, memorySize)); //start with one free block
            this.processes = new ArrayList<>();
        }

        void setMemoryStrategy(String strategy) {
            this.memoryStrategy = strategy; //set fit method
        }

        void addProcess(Process p) {
            processes.add(p); //add new process
        }

        boolean allocateMemory(Process p) {
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
                            best = block; //choose smallest that fits
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

        void deallocateMemory(int pid) {
            for (MemoryBlock block : memory) {
                if (block.allocatedTo == pid) {
                    block.isFree = true;
                    block.allocatedTo = -1;
                    break; //memory released
                }
            }
        }

        void runFCFS() {
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

                deallocateMemory(p.pid); //free memory
            }

            printMetrics(cpuBusyTime, currentTime); //show final info
        }

        void runSJF() {
            int currentTime = 0;
            int completed = 0;
            int cpuBusyTime = 0;
            int n = processes.size();

            List<Process> readyQueue = new ArrayList<>(); //processes ready to run

            while (completed < n) {
                for (Process p : processes) {
                    if (p.arrivalTime <= currentTime && !p.isCompleted && !readyQueue.contains(p)) {
                        readyQueue.add(p); //add to queue
                    }
                }

                readyQueue.sort(Comparator.comparingInt(p -> p.burstTime)); //shortest first
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
                    currentTime++; //wait for new process
                }
            }

            printMetrics(cpuBusyTime, currentTime); //show final info
        }

        void printMetrics(int cpuTime, int totalTime) {
            System.out.println("\n--- Process Info ---");
            int totalWT = 0, totalTAT = 0;

            for (Process p : processes) {
                if (p.isCompleted) {
                    totalWT += p.waitingTime;
                    totalTAT += p.turnaroundTime;
                    System.out.printf("PID %d | AT:%d | BT:%d | ST:%d | CT:%d | WT:%d | TAT:%d\n",
                            p.pid, p.arrivalTime, p.burstTime, p.startTime, p.completionTime, p.waitingTime, p.turnaroundTime);
                }
            }

            System.out.printf("\nAvg Waiting Time: %.2f\n", (double) totalWT / processes.size());
            System.out.printf("Avg Turnaround Time: %.2f\n", (double) totalTAT / processes.size());
            System.out.printf("CPU Usage: %.2f%%\n", (cpuTime * 100.0) / totalTime);
            System.out.printf("Throughput: %.2f processes/unit time\n", (processes.size() * 1.0) / totalTime);
        }
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in); //input reader

        System.out.println("Enter total memory size: "); //memory size
        int memSize = sc.nextInt();

        OperatingSystem os = new OperatingSystem(memSize); //new OS

        System.out.println("Enter number of processes: "); //number of processes
        int n = sc.nextInt();

        for (int i = 0; i < n; i++) {
            System.out.printf("Enter arrival time, burst time and memory for Process %d: ", i + 1);
            int at = sc.nextInt();
            int bt = sc.nextInt();
            int mem = sc.nextInt();

            os.addProcess(new Process(i + 1, at, bt, mem)); //add process
        }

        System.out.println("Choose Memory Strategy: 1. First-Fit 2. Best-Fit");
        int strategyChoice = sc.nextInt();

        if (strategyChoice == 1) {
            os.setMemoryStrategy("First-Fit"); //first-fit memory
        } else {
            os.setMemoryStrategy("Best-Fit"); //best-fit memory
        }

        boolean exit = false;

        while (!exit) {
            System.out.println("\nChoose Scheduling:");
            System.out.println("1. FCFS");
            System.out.println("2. SJF");
            System.out.println("3. Exit");

            int choice = sc.nextInt();

            switch (choice) {
                case 1:
                    System.out.println("\nRunning FCFS...");
                    os.runFCFS(); //run FCFS
                    exit = true; //exit after done
                    break;
                case 2:
                    System.out.println("\nRunning SJF...");
                    os.runSJF(); //run SJF
                    exit = true; //exit after done
                    break;
                case 3:
                    exit = true; //user exits
                    break;
                default:
                    System.out.println("Invalid choice, try again.");
                    break;
            }
        }

        sc.close(); //close input
    }
}


