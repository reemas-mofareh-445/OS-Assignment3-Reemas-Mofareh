import java.util.LinkedList;
import java.util.Queue;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

// ANSI Color Codes for enhanced terminal output
class Colors {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String CYAN = "\u001B[36m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String BLUE = "\u001B[34m";
    public static final String RED = "\u001B[31m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String WHITE = "\u001B[37m";
    public static final String BRIGHT_WHITE = "\u001B[97m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
}

// ⚠️ SHARED RESOURCES - These need synchronization! ⚠️
class SharedResources {
    // TODO: Students will add synchronization mechanisms here
    // HINT: Use ReentrantLock for mutual exclusion
    // HINT: Use Semaphore for limiting concurrent access
    
    public static int contextSwitchCount = 0;      // Shared counter - NEEDS PROTECTION!
    public static int completedProcessCount = 0;   // Shared counter - NEEDS PROTECTION!
    public static long totalWaitingTime = 0;       // Shared accumulator - NEEDS PROTECTION!
    public static List<String> executionLog = new ArrayList<>();  // Shared list - NEEDS PROTECTION!
    
    // TODO #1: Add a ReentrantLock(s) here to protect critical sections
    // Example: public static final ReentrantLock lock = new ReentrantLock();
    
    // TODO #2: Add a Semaphore to limit concurrent process execution
    // Example: public static final Semaphore cpuSemaphore = new Semaphore(1);
    
    // Method to increment context switch counter
    public static void incrementContextSwitch() {
        // TODO: Protect this critical section with a lock
        // RACE CONDITION: Multiple threads might read and write simultaneously!
        contextSwitchCount++;
    }
    
    // Method to increment completed process counter
    public static void incrementCompletedProcess() {
        // TODO: Protect this critical section with a lock
        completedProcessCount++;
    }
    
    // Method to add waiting time
    public static void addWaitingTime(long time) {
        // TODO: Protect this critical section with a lock
        totalWaitingTime += time;
    }
    
    // Method to log execution
    public static void logExecution(String message) {
        // TODO: Protect this critical section with a lock
        // RACE CONDITION: ArrayList is not thread-safe!
        executionLog.add(message);
    }
}

// Class representing a process that implements Runnable to be run by a thread
class Process implements Runnable {
    private String name;
    private int burstTime;
    private int timeQuantum;
    private int remainingTime;
    private long creationTime;
    private long startTime;
    private long completionTime;
    private int priority;  // From Assignment 1
    
    public Process(String name, int burstTime, int timeQuantum, int priority) {
        this.name = name;
        this.burstTime = burstTime;
        this.timeQuantum = timeQuantum;
        this.remainingTime = burstTime;
        this.priority = priority;
        this.creationTime = System.currentTimeMillis();
        this.startTime = -1;
    }
    
    @Override
    public void run() {
        // TODO #3: Acquire CPU semaphore before executing
        // This ensures only allowed number of processes run simultaneously
        
        try {
            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            }
            
            // Increment context switch counter
            SharedResources.incrementContextSwitch();
            
            int runTime = Math.min(timeQuantum, remainingTime);
            
            String quantumBar = createProgressBar(0, 15);
            String message = "  ▶ " + name + " (Priority: " + priority + ") executing quantum [" + runTime + "ms]";
            System.out.println(Colors.BRIGHT_GREEN + message + Colors.RESET);
            
            // Log execution
            SharedResources.logExecution(name + " started quantum execution");
            
            try {
                int steps = 5;
                int stepTime = runTime / steps;
                
                for (int i = 1; i <= steps; i++) {
                    Thread.sleep(stepTime);
                    int quantumProgress = (i * 100) / steps;
                    quantumBar = createProgressBar(quantumProgress, 15);
                    System.out.print("\r  " + Colors.YELLOW + "⚡" + Colors.RESET + 
                                    " Quantum progress: " + quantumBar);
                }
                System.out.println();
                
            } catch (InterruptedException e) {
                System.out.println(Colors.RED + "\n  ✗ " + name + " was interrupted." + Colors.RESET);
            }
            
            remainingTime -= runTime;
            int overallProgress = (int) (((double)(burstTime - remainingTime) / burstTime) * 100);
            String overallProgressBar = createProgressBar(overallProgress, 20);
            
            System.out.println(Colors.YELLOW + "  ⏸ " + Colors.CYAN + name + Colors.RESET + 
                              " completed quantum " + Colors.BRIGHT_YELLOW + runTime + "ms" + Colors.RESET + 
                              " │ Overall progress: " + overallProgressBar);
            System.out.println(Colors.MAGENTA + "     Remaining time: " + remainingTime + "ms" + Colors.RESET);
            
            if (remainingTime > 0) {
                System.out.println(Colors.BLUE + "  ↻ " + Colors.CYAN + name + Colors.RESET + 
                                  " yields CPU for context switch" + Colors.RESET);
                SharedResources.logExecution(name + " yielded CPU");
            } else {
                completionTime = System.currentTimeMillis();
                long waitingTime = (completionTime - creationTime) - burstTime;
                SharedResources.addWaitingTime(waitingTime);
                SharedResources.incrementCompletedProcess();
                SharedResources.logExecution(name + " completed execution");
                System.out.println(Colors.BRIGHT_GREEN + "  ✓ " + Colors.BOLD + Colors.CYAN + name + 
                                  Colors.RESET + Colors.BRIGHT_GREEN + " finished execution!" + 
                                  Colors.RESET);
            }
            System.out.println();
            
        } finally {
            // TODO #4: Release CPU semaphore here
            // Always release in finally block to prevent deadlocks!
        }
    }
    
    private String createProgressBar(int progress, int width) {
        int filled = (progress * width) / 100;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            if (i < filled) {
                bar.append(Colors.GREEN + "█" + Colors.RESET);
            } else {
                bar.append(Colors.WHITE + "░" + Colors.RESET);
            }
        }
        bar.append("] ").append(progress).append("%");
        return bar.toString();
    }
    
    public void runToCompletion() {
        // TODO: Similar synchronization needed here
        try {
            System.out.println(Colors.BRIGHT_CYAN + "  ⚡ " + Colors.BOLD + Colors.CYAN + name + 
                              Colors.RESET + Colors.BRIGHT_CYAN + " is the last process, running to completion" + 
                              Colors.RESET + " [" + remainingTime + "ms]");
            Thread.sleep(remainingTime);
            remainingTime = 0;
            completionTime = System.currentTimeMillis();
            
            long waitingTime = (completionTime - creationTime) - burstTime;
            SharedResources.addWaitingTime(waitingTime);
            SharedResources.incrementCompletedProcess();
            
            System.out.println(Colors.BRIGHT_GREEN + "  ✓ " + Colors.BOLD + Colors.CYAN + name + 
                              Colors.RESET + Colors.BRIGHT_GREEN + " finished execution!" + Colors.RESET);
            System.out.println();
        } catch (InterruptedException e) {
            System.out.println(Colors.RED + "  ✗ " + name + " was interrupted." + Colors.RESET);
        }
    }
    
    public String getName() {
        return name;
    }
    
    public int getBurstTime() {
        return burstTime;
    }
    
    public int getRemainingTime() {
        return remainingTime;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public boolean isFinished() {
        return remainingTime <= 0;
    }
    
    public long getWaitingTime() {
        if (completionTime > 0) {
            return (completionTime - creationTime) - burstTime;
        }
        return 0;
    }
}

public class SchedulerSimulationSync {
    public static void main(String[] args) {
        // ⚠️ IMPORTANT: Put your student ID here
        int studentID = 445052261;  // ← CHANGE THIS TO YOUR ACTUAL STUDENT ID
        
        Random random = new Random(studentID);
        
        int timeQuantum = 2000 + random.nextInt(4) * 1000;
        int numProcesses = 10 + random.nextInt(11);
        
        Queue<Thread> processQueue = new LinkedList<>();
        Map<Thread, Process> processMap = new HashMap<>();
        List<Process> allProcesses = new ArrayList<>();
        
        // Print simulation header
        System.out.println("\n" + Colors.BOLD + Colors.BRIGHT_CYAN + 
                          "╔═══════════════════════════════════════════════════════════════════════════════════════╗" + 
                          Colors.RESET);
        System.out.println(Colors.BOLD + Colors.BRIGHT_CYAN + "║" + Colors.RESET + 
                          Colors.BG_BLUE + Colors.BRIGHT_WHITE + Colors.BOLD + 
                          "              CPU SCHEDULER SIMULATION WITH SYNCHRONIZATION                       " + 
                          Colors.RESET + Colors.BOLD + Colors.BRIGHT_CYAN + "║" + Colors.RESET);
        System.out.println(Colors.BOLD + Colors.BRIGHT_CYAN + 
                          "╠═══════════════════════════════════════════════════════════════════════════════════════╣" + 
                          Colors.RESET);
        System.out.println(Colors.BOLD + Colors.BRIGHT_CYAN + "║" + Colors.RESET + 
                          Colors.YELLOW + "  ⚙ Processes:     " + Colors.RESET + Colors.BRIGHT_YELLOW + 
                          String.format("%-65s", numProcesses) + 
                          Colors.BOLD + Colors.BRIGHT_CYAN + "║" + Colors.RESET);
        System.out.println(Colors.BOLD + Colors.BRIGHT_CYAN + "║" + Colors.RESET + 
                          Colors.YELLOW + "  ⏱ Time Quantum:  " + Colors.RESET + Colors.BRIGHT_YELLOW + 
                          String.format("%-65s", timeQuantum + "ms") + 
                          Colors.BOLD + Colors.BRIGHT_CYAN + "║" + Colors.RESET);
        System.out.println(Colors.BOLD + Colors.BRIGHT_CYAN + "║" + Colors.RESET + 
                          Colors.YELLOW + "  🔑 Student ID:    " + Colors.RESET + Colors.BRIGHT_YELLOW + 
                          String.format("%-65s", studentID) + 
                          Colors.BOLD + Colors.BRIGHT_CYAN + "║" + Colors.RESET);
        System.out.println(Colors.BOLD + Colors.BRIGHT_CYAN + "║" + Colors.RESET + 
                          Colors.YELLOW + "  🔒 Sync Mode:     " + Colors.RESET + Colors.BRIGHT_YELLOW + 
                          String.format("%-65s", "Mutex Locks & Semaphores") + 
                          Colors.BOLD + Colors.BRIGHT_CYAN + "║" + Colors.RESET);
        System.out.println(Colors.BOLD + Colors.BRIGHT_CYAN + 
                          "╚═══════════════════════════════════════════════════════════════════════════════════════╝" + 
                          Colors.RESET + "\n");
        
        // Create processes with priorities
        for (int i = 1; i <= numProcesses; i++) {
            int burstTime = timeQuantum/2 + random.nextInt(2 * timeQuantum + 1);
            int priority = 1 + random.nextInt(5);  // Priority between 1 and 5
            
            Process process = new Process("P" + i, burstTime, timeQuantum, priority);
            allProcesses.add(process);
            addProcessToQueue(process, processQueue, processMap);
        }
        
        // Start scheduler
        System.out.println(Colors.BOLD + Colors.GREEN + 
                          "╔════════════════════════════════════════════════════════════════════════════════╗" + 
                          Colors.RESET);
        System.out.println(Colors.BOLD + Colors.GREEN + "║" + Colors.RESET + 
                          Colors.BG_GREEN + Colors.WHITE + Colors.BOLD + 
                          "                        ▶  SCHEDULER STARTING  ◀                               " + 
                          Colors.RESET + Colors.BOLD + Colors.GREEN + "║" + Colors.RESET);
        System.out.println(Colors.BOLD + Colors.GREEN + 
                          "╚════════════════════════════════════════════════════════════════════════════════╝" + 
                          Colors.RESET + "\n");
        
        // Scheduler loop
        while (!processQueue.isEmpty()) {
            Thread currentThread = processQueue.poll();
            
            // Display ready queue
            System.out.println(Colors.BOLD + Colors.MAGENTA + "┌─ Ready Queue " + "─".repeat(65) + Colors.RESET);
            System.out.print(Colors.MAGENTA + "│ " + Colors.RESET + Colors.BRIGHT_WHITE + "[" + Colors.RESET);
            int queueCount = 0;
            for (Thread thread : processQueue) {
                Process process = processMap.get(thread);
                if (queueCount > 0) System.out.print(Colors.WHITE + " → " + Colors.RESET);
                System.out.print(Colors.BRIGHT_CYAN + process.getName() + Colors.RESET);
                queueCount++;
            }
            if (queueCount == 0) {
                System.out.print(Colors.YELLOW + "empty" + Colors.RESET);
            }
            System.out.println(Colors.BRIGHT_WHITE + "]" + Colors.RESET);
            System.out.println(Colors.BOLD + Colors.MAGENTA + "└" + "─".repeat(79) + Colors.RESET + "\n");
            
            currentThread.start();
            
            try {
                currentThread.join();
            } catch (InterruptedException e) {
                System.out.println("Main thread interrupted.");
            }
            
            Process process = processMap.get(currentThread);
            
            if (!process.isFinished()) {
                if (!processQueue.isEmpty()) {
                    addProcessToQueue(process, processQueue, processMap);
                } else {
                    System.out.println(Colors.BRIGHT_YELLOW + "  ⚠ " + Colors.CYAN + process.getName() + 
                                      Colors.RESET + Colors.YELLOW + " is the last process → running to completion" + 
                                      Colors.RESET);
                    process.runToCompletion();
                }
            }
        }
        
        // Print statistics
        printStatistics(allProcesses, timeQuantum);
    }
    
    public static void addProcessToQueue(Process process, Queue<Thread> processQueue, 
                                        Map<Thread, Process> processMap) {
        Thread thread = new Thread(process);
        processQueue.add(thread);
        processMap.put(thread, process);
        
        System.out.println(Colors.BLUE + "  ➕ " + Colors.BOLD + Colors.CYAN + process.getName() + 
                          Colors.RESET + Colors.BLUE + " (Priority: " + process.getPriority() + ")" +
                          " added to ready queue" + Colors.RESET + 
                          " │ Burst time: " + Colors.YELLOW + process.getBurstTime() + "ms" + 
                          Colors.RESET);
    }
    
    public static void printStatistics(List<Process> processes, int timeQuantum) {
        System.out.println(Colors.BOLD + Colors.BRIGHT_GREEN + 
                          "╔════════════════════════════════════════════════════════════════════════════════╗" + 
                          Colors.RESET);
        System.out.println(Colors.BOLD + Colors.BRIGHT_GREEN + "║" + Colors.RESET + 
                          Colors.BG_GREEN + Colors.WHITE + Colors.BOLD + 
                          "                     ✓  ALL PROCESSES COMPLETED  ✓                            " + 
                          Colors.RESET + Colors.BOLD + Colors.BRIGHT_GREEN + "║" + Colors.RESET);
        System.out.println(Colors.BOLD + Colors.BRIGHT_GREEN + 
                          "╚════════════════════════════════════════════════════════════════════════════════╝" + 
                          Colors.RESET + "\n");
        
        // Print synchronization statistics
        System.out.println(Colors.BOLD + Colors.BRIGHT_CYAN + "═══ Synchronization Statistics ═══" + Colors.RESET);
        System.out.println(Colors.YELLOW + "Total Context Switches: " + Colors.BRIGHT_YELLOW + 
                          SharedResources.contextSwitchCount + Colors.RESET);
        System.out.println(Colors.YELLOW + "Total Completed Processes: " + Colors.BRIGHT_YELLOW + 
                          SharedResources.completedProcessCount + Colors.RESET);
        System.out.println(Colors.YELLOW + "Total Waiting Time: " + Colors.BRIGHT_YELLOW + 
                          SharedResources.totalWaitingTime + "ms" + Colors.RESET);
        System.out.println(Colors.YELLOW + "Average Waiting Time: " + Colors.BRIGHT_YELLOW + 
                          (SharedResources.totalWaitingTime / processes.size()) + "ms" + Colors.RESET);
        System.out.println();
        
        // Print process summary table
        System.out.println(Colors.BOLD + Colors.BRIGHT_CYAN + "═══ Process Summary Table ═══" + Colors.RESET);
        System.out.println(Colors.BOLD + String.format("%-10s %-12s %-12s %-12s", 
                          "Process", "Priority", "Burst Time", "Waiting Time") + Colors.RESET);
        System.out.println("─".repeat(48));
        
        for (Process p : processes) {
            System.out.println(String.format("%-10s %-12d %-12d %-12d", 
                              p.getName(), p.getPriority(), p.getBurstTime(), p.getWaitingTime()));
        }
        System.out.println();
        
        // Show execution log summary
        System.out.println(Colors.BOLD + Colors.BRIGHT_CYAN + "═══ Execution Log Summary ═══" + Colors.RESET);
        System.out.println(Colors.YELLOW + "Total log entries: " + Colors.BRIGHT_YELLOW + 
                          SharedResources.executionLog.size() + Colors.RESET);
        System.out.println();
    }
}
