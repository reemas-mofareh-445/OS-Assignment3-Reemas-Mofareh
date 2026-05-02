# Assignment 3 - Complete Documentation

**Student Name**: Reemas Mofareh
**Student ID**: 445052261
**Date Submitted**: April 26, 2026

---

## 🎥 VIDEO DEMONSTRATION LINK (REQUIRED)


**Video Link**:https://drive.google.com/file/d/16OlfdzWfq5-PEalinGciaZlWbgHMUlpL/view?usp=drivesdk

**Verification**:
- [ ] Link is accessible (tested in incognito mode)
- [ ] Video is 3-5 minutes long
- [ ] Video shows code walkthrough and commits
- [ ] Video has clear audio
- [ ] Uploaded to PERSONAL Gmail (not @std.psau.edu.sa)

---

## Part 1: Development Log (1 mark)

Document your development process with **minimum 3 entries** showing progression:

### Entry 1 - April 22, 2026, 9:30 PM
**What I implemented**: Forked the starter repository on GitHub, made it public, renamed it to `OS-Assignment3-Reemas-Mofareh`, cloned it locally with VS Code, and changed `studentID` from `123456789` to `445052261` in `SchedulerSimulationSync.java`. Made the first commit so the work is anchored to my account from day one.

**Challenges encountered**: GitHub asked for a device verification code on the first sign-in from a new browser, which I had to fetch from my university email before continuing.

**How I solved it**: Opened the @std.psau.edu.sa inbox, copied the 6-digit code, pasted it into the verification page, then completed the fork.

**Testing approach**: Compiled the unmodified code with `javac SchedulerSimulationSync.java` and ran it once to confirm the starter project still builds before adding any synchronization. Verified the printed `Student ID` line shows `445052261`.

**Time spent**: ~30 minutes.

---

### Entry 2 - April 23, 2026, 8:45 PM
**What I implemented**: Task 1 — added `import java.util.concurrent.locks.ReentrantLock;` and **three separate `ReentrantLock` instances** (`contextSwitchLock`, `completedProcessLock`, `waitingTimeLock`) inside `SharedResources`. Wrapped each of `incrementContextSwitch()`, `incrementCompletedProcess()`, and `addWaitingTime()` in a `lock()` / `try` / `finally { unlock() }` block.

**Challenges encountered**: The README presents two design choices (one shared lock vs. one lock per counter). I had to decide which one was a better fit for an assignment that explicitly grades understanding of concurrency, not just correctness.

**How I solved it**: Re-read the "Lock Granularity" section in the README. Because the three counters are completely independent (incrementing `contextSwitchCount` never reads the other two), one lock per counter (fine-grained) gives strictly higher concurrency and is clearly the better answer here. I picked Option B and noted my reasoning so I could repeat it in the video.

**Testing approach**: Recompiled and ran the program. Verified the totals printed at the end match the expected counts (10 completed processes, 16 context switches given the seed `445052261`). No exceptions in the console.

**Time spent**: ~45 minutes.

---

### Entry 3 - April 24, 2026, 9:15 PM
**What I implemented**: Task 2 — added a dedicated `logLock` (a fourth `ReentrantLock`) and wrapped `logExecution()` in the same `lock` / `try` / `finally { unlock }` pattern. Kept it as a separate lock so log writes don't unnecessarily contend with counter updates.

**Challenges encountered**: I first wondered whether I could just reuse one of the counter locks. After reading the assignment again I realised the log is a different shared resource (`ArrayList`) with a different access pattern (it's hit far more often), so giving it its own lock is the cleaner design.

**How I solved it**: Added `public static final ReentrantLock logLock = new ReentrantLock();` and used it only inside `logExecution()`. This way, two threads can update a counter and the log "in parallel" instead of serialising on a single global lock.

**Testing approach**: Ran the program 3 times in a row. The "Total log entries" line printed exactly 32 every time, and there was no `ConcurrentModificationException` in the output.

**Time spent**: ~30 minutes.

---

### Entry 4 - April 25, 2026, 8:30 PM
**What I implemented**: Task 3 — added `import java.util.concurrent.Semaphore;` and a binary `Semaphore cpuSemaphore = new Semaphore(1);`. Wrapped the body of `Process.run()` and `Process.runToCompletion()` so each thread does `cpuSemaphore.acquire()` before doing any work and always `cpuSemaphore.release()` in a `finally` block.

**Challenges encountered**: `acquire()` throws `InterruptedException`. If I let the exception escape I would acquire a permit and then leak it. I also had to decide what to do for `runToCompletion()`, which has its own `try/catch (InterruptedException)` already.

**How I solved it**: For `run()` I used a small outer `try/catch` around `acquire()` so an interrupt is restored (`Thread.currentThread().interrupt()`) and we return without entering the protected block (so we never release a permit we did not acquire). For `runToCompletion()` I added a boolean `acquired` flag and only released the permit in `finally` if `acquired == true`. This guarantees no permit leak.

**Testing approach**: Ran the program 5 times in a row. All five runs produced identical deterministic counts (16 context switches, 10 completions, 32 log entries). The total waiting time fluctuates by less than 0.2% across runs, which is just the natural jitter from `Thread.sleep`.

**Time spent**: ~50 minutes.

---

### Entry 5 - April 26, 2026, 7:00 PM
**What I implemented**: Filled `ASSIGNMENT_DOCUMENTATION.md` with all six parts, recorded the development log, the lock-granularity rationale, the testing evidence from the 5 runs, and the reflection. Re-read the final checklist in `README.md` and confirmed every item.

**Challenges encountered**: Making sure my answers actually reflected what I wrote in the code (and the design choices I made) instead of being generic textbook descriptions of synchronization.

**How I solved it**: For each part I went back to the source file, read the exact lines I added, and described them. The "Code snippet" subsections are copied directly from `SchedulerSimulationSync.java` so the documentation and the code can never disagree.

**Testing approach**: Ran the program one last time after the documentation was complete to make sure I hadn't accidentally touched the Java file. Output unchanged.

**Time spent**: ~60 minutes.

---

## Part 2: Technical Questions (1 mark)

### Question 1: Race Conditions
**Q**: Identify and explain TWO race conditions in the original code. For each:
- What shared resource is affected?
- Why is concurrent access a problem?
- What incorrect behavior could occur?

**Your Answer**:

**Race 1 — `contextSwitchCount++` (also `completedProcessCount++` and `totalWaitingTime += time`).** The shared resource is the `static int contextSwitchCount` field in `SharedResources`. The `++` operator looks atomic but it is actually three steps: read the current value, add one, and write the result back. If two threads run `incrementContextSwitch()` at almost the same time, both can read the same old value (say `5`), both add one, and both write `6`. The net effect is one increment instead of two — the counter is silently smaller than it should be. Across many context switches this leads to the "Total Context Switches" statistic reporting fewer events than really happened, which would make later analysis (e.g., turnaround vs. throughput) wrong.

**Race 2 — `executionLog.add(message)` on a plain `ArrayList`.** The shared resource is `static List<String> executionLog = new ArrayList<>()`. `ArrayList` is documented as **not thread-safe**: it has an internal `size` field and an internal `Object[] elementData` array that are mutated together but without any locking. If two threads call `add()` simultaneously, possible incorrect behaviours are: (a) one element overwrites another at the same index, (b) `size` ends up smaller than the number of `add` calls, (c) the iterator used elsewhere throws `ConcurrentModificationException`, or (d) an `ArrayIndexOutOfBoundsException` is thrown during an internal grow. None of these are acceptable for a correctness-critical log.

---

### Question 2: Locks vs Semaphores
**Q**: Explain the difference between ReentrantLock and Semaphore. Where did you use each in your code and why?

**Your Answer**:

A **`ReentrantLock`** is a binary mutual-exclusion primitive: at any moment either nobody holds it or **exactly one** thread holds it, and that thread can re-acquire it (re-entrant). Its purpose is "protect this critical section so only one thread at a time can run it". A **`Semaphore(N)`** holds `N` permits; up to `N` threads may pass through `acquire()` concurrently and each one must call `release()` to return its permit. A semaphore is a **counting** synchroniser used for *resource pooling* / *admission control*; it is not necessarily owned by a single thread (one thread can `acquire()` and a different thread can `release()`).

In my code I used a `ReentrantLock` for every shared **piece of in-memory state** that needs mutual exclusion: `contextSwitchLock`, `completedProcessLock`, `waitingTimeLock` for the three independent counters, and `logLock` for the `ArrayList`. Mutual exclusion is the right tool there because these are all "critical section" updates that must look atomic. I used a `Semaphore(1)` (`cpuSemaphore`) at a different level: it represents a **virtual single-CPU resource** that processes compete for. I `acquire()` it before a process starts its quantum and `release()` it in `finally`. Conceptually this is admission control rather than data protection, which is exactly what semaphores were designed for.

---

### Question 3: Deadlock Prevention
**Q**: What is deadlock? Explain TWO prevention techniques and what you did to prevent deadlocks in your code.

**Your Answer**:

A **deadlock** is a state in which two or more threads are each waiting forever for a resource that another waiting thread holds, so none of them can ever proceed. The classic four conditions (Coffman) — mutual exclusion, hold-and-wait, no preemption, and circular wait — must all hold for a deadlock to be possible.

**Prevention technique 1 — always release locks in a `finally` block.** If an exception is thrown inside a critical section and the `unlock()` call is *after* the section, the lock is leaked: every other thread waiting for it will block forever, which is effectively a deadlock. Using `try { ... } finally { lock.unlock(); }` guarantees the lock is released on every code path, including exceptional ones.

**Prevention technique 2 — avoid hold-and-wait / enforce a fixed lock ordering.** When code might need more than one lock, all threads must take them in the same global order so a circular wait is impossible. Equivalently, you can keep critical sections short enough that you never hold one lock while trying to grab another.

**What I did in this code:**
1. Every `lock()` call is matched by an `unlock()` in a `finally` block — see `incrementContextSwitch()`, `incrementCompletedProcess()`, `addWaitingTime()`, and `logExecution()`.
2. The semaphore is also released in a `finally` block. In `runToCompletion()` I use a boolean `acquired` guard so I only release a permit I actually hold (this also covers the case where `acquire()` is interrupted before returning).
3. I never hold two locks at the same time. Each helper method takes one lock, does the smallest possible amount of work, and releases it. With at most one lock held per thread there cannot be a circular-wait, so no deadlock is possible by construction.
4. On `InterruptedException` from `acquire()` I restore the interrupt flag (`Thread.currentThread().interrupt()`) and return without entering the protected block, so I never call a `release()` for a permit I did not acquire.

---

### Question 4: Lock Granularity Design Decision
**Q**: For Task 1 (protecting the three counters), explain your lock design choice:
- Did you use ONE lock for all three counters (coarse-grained) OR separate locks for each counter (fine-grained)?
- Explain WHY you made this choice
- What are the trade-offs between the two approaches?
- Given that the three counters are independent, which approach provides better concurrency and why?

**Your Answer**:

I chose **fine-grained locking** — three independent locks, one per counter (`contextSwitchLock`, `completedProcessLock`, `waitingTimeLock`).

**Why:** the three counters are *semantically independent*. Updating `contextSwitchCount` never reads or modifies `completedProcessCount` or `totalWaitingTime`. With a single shared lock, a thread that only wants to bump the context-switch counter would have to wait for any other thread that happens to be bumping the completed-process counter, even though those operations cannot possibly conflict at the data level. That waiting is pure overhead — artificial contention created by the lock, not by the data.

**Trade-offs:**
- *Coarse-grained (single lock)* — fewer lines of code and only one object to reason about, so it is simpler. But it creates an artificial bottleneck: at most one counter update per moment in the whole program, regardless of which counter it is. With many threads this serialises updates that did not need to be serialised.
- *Fine-grained (one lock per counter)* — slightly more code and three objects to remember, but threads updating *different* counters can now run truly in parallel. There is also a tiny bit more memory (three lock objects instead of one), which is irrelevant here.

Because the counters are independent, fine-grained locking gives strictly **better concurrency** with no correctness penalty — it never makes the program slower, only sometimes faster. It also matches the standard industry guidance: "use the smallest lock that still protects the invariant." That is why I picked it, and that is what I show in the video.

---

## Part 3: Synchronization Analysis (1 mark)

### Critical Section #1: Counter Variables

**Which variables**: `SharedResources.contextSwitchCount` (`int`), `SharedResources.completedProcessCount` (`int`), and `SharedResources.totalWaitingTime` (`long`).

**Why they need protection**: All three are mutated by `++` or `+=` from many `Process` threads concurrently. These operations are read-modify-write and not atomic. Without a lock, two threads can read the same old value, both compute the same new value, and one update is silently lost. Over many context switches and completions this would corrupt the final statistics.

**Synchronization mechanism used**: One **`ReentrantLock` per counter** (fine-grained), each used with the standard `lock() / try / finally { unlock() }` pattern. Three independent locks because the three counters are independent — see Question 4 for the design rationale.

**Code snippet**:
```java
public static final ReentrantLock contextSwitchLock   = new ReentrantLock();
public static final ReentrantLock completedProcessLock = new ReentrantLock();
public static final ReentrantLock waitingTimeLock     = new ReentrantLock();

public static void incrementContextSwitch() {
    contextSwitchLock.lock();
    try {
        contextSwitchCount++;
    } finally {
        contextSwitchLock.unlock();
    }
}

public static void incrementCompletedProcess() {
    completedProcessLock.lock();
    try {
        completedProcessCount++;
    } finally {
        completedProcessLock.unlock();
    }
}

public static void addWaitingTime(long time) {
    waitingTimeLock.lock();
    try {
        totalWaitingTime += time;
    } finally {
        waitingTimeLock.unlock();
    }
}
```

**Justification**: `ReentrantLock` is the textbook tool for protecting a shared variable against lost-update races, and using one lock per counter avoids artificial contention between unrelated updates. The `finally` block guarantees the lock is released even if an exception is thrown.

---

### Critical Section #2: Execution Log

**What resource**: `SharedResources.executionLog`, a `java.util.ArrayList<String>` shared by every `Process` thread.

**Why it needs protection**: `ArrayList` is explicitly **not thread-safe**. `add()` reads and writes both the internal `size` field and the `Object[]` backing array; concurrent calls can lose entries, overwrite slots, throw `ArrayIndexOutOfBoundsException` during a grow, or trigger `ConcurrentModificationException` if any thread iterates the list at the same time.

**Synchronization mechanism used**: A dedicated **`ReentrantLock` named `logLock`**, separate from the counter locks so log writes do not contend with counter updates.

**Code snippet**:
```java
public static final ReentrantLock logLock = new ReentrantLock();

public static void logExecution(String message) {
    logLock.lock();
    try {
        executionLog.add(message);
    } finally {
        logLock.unlock();
    }
}
```

**Justification**: Mutual exclusion turns the unsafe `add()` into a critical section that exactly one thread enters at a time, which is the minimum that makes `ArrayList` safe to use here. (`Collections.synchronizedList` would also work, but a `ReentrantLock` here keeps the code symmetric with the counter sections and makes the protection explicit at the call site, which is easier to explain in the video.)

---

### Critical Section #3: CPU Semaphore

**Purpose of semaphore**: To model a single-CPU machine where only **one** process can be "executing on the CPU" at a time. Threads queue up and are admitted one at a time, which produces deterministic, easy-to-read output and matches the round-robin scheduler model from Assignment 1.

**Number of permits and why**: **1 permit** (binary semaphore). One permit = "one CPU available". Increasing the count to 2 (as the README suggests trying) lets two processes interleave their print output and would simulate a dual-core CPU; that is useful as an experiment but not the model the assignment is asking for.

**Where implemented**: Inside `Process.run()` and `Process.runToCompletion()`. The permit is acquired before any work begins and released in a `finally` block so it is returned even if an exception is thrown mid-quantum.

**Code snippet**:
```java
public static final Semaphore cpuSemaphore = new Semaphore(1);

@Override
public void run() {
    try {
        SharedResources.cpuSemaphore.acquire();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
    }
    try {
        // ... full quantum execution, logging, statistics ...
    } finally {
        SharedResources.cpuSemaphore.release();
    }
}

public void runToCompletion() {
    boolean acquired = false;
    try {
        SharedResources.cpuSemaphore.acquire();
        acquired = true;
        // ... run to completion ...
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        if (acquired) {
            SharedResources.cpuSemaphore.release();
        }
    }
}
```

**Effect on program behavior**: With `Semaphore(1)`, every quantum's progress bar prints cleanly without being shredded by another thread's output, every "started quantum / yielded CPU / completed execution" log line appears in the right order, and the final statistics are reproducible run after run. The `acquired` flag guarantees the program never releases a permit it did not actually obtain, which would otherwise let the semaphore drift above 1 permit and break the single-CPU model.

---

## Part 4: Testing and Verification (2 marks)

### Test 1: Consistency Check
**What I tested**: Whether the program produces the same statistics every time it is run, which is the headline behaviour we expect once race conditions are fixed.

**Testing procedure**:
```powershell
javac SchedulerSimulationSync.java
java -cp . SchedulerSimulationSync > run1.log
java -cp . SchedulerSimulationSync > run2.log
java -cp . SchedulerSimulationSync > run3.log
java -cp . SchedulerSimulationSync > run4.log
java -cp . SchedulerSimulationSync > run5.log
```

**Results**:

| Run | Total Context Switches | Completed Processes | Total Log Entries | Total Waiting Time | Average Waiting Time |
|-----|-----------------------:|--------------------:|------------------:|-------------------:|---------------------:|
| 1   | 16                     | 10                  | 32                | 287209 ms          | 28720 ms             |
| 2   | 16                     | 10                  | 32                | 286947 ms          | 28694 ms             |
| 3   | 16                     | 10                  | 32                | 287154 ms          | 28715 ms             |
| 4   | 16                     | 10                  | 32                | 287166 ms          | 28716 ms             |
| 5   | 16                     | 10                  | 32                | 287290 ms          | 28729 ms             |

The three deterministic counts (context switches, completed processes, log entries) are **identical across all five runs**. Total waiting time varies by less than ±0.2% — this is wall-clock jitter from `Thread.sleep`, not a synchronization bug, because the calculation is `(completionTime - creationTime) - burstTime` and `completionTime` is read from `System.currentTimeMillis()`.

**Why synchronization is necessary**: Without the locks, four shared resources can race:
1. `contextSwitchCount++`, `completedProcessCount++`, `totalWaitingTime +=` are all non-atomic read-modify-write operations. Concurrent threads can lose updates and the printed totals would be smaller than the true values.
2. `executionLog.add(...)` on a plain `ArrayList` is not thread-safe and can corrupt the internal `size`/array, throw `ArrayIndexOutOfBoundsException` during a grow, or throw `ConcurrentModificationException`.
3. Without the binary semaphore, multiple `Process` threads would print to `System.out` interleaved, making the run trace unreadable.

These bugs may not appear on every run (race conditions are *probabilistic*), which is why the assignment specifically asks for "consistent results across multiple runs" — that is the empirical signal that they are fixed.

**Conclusion**: All deterministic outputs are identical across 5 runs. Synchronization works.

---

### Test 2: Exception Testing
**What I tested**: That no `ConcurrentModificationException`, `ArrayIndexOutOfBoundsException`, or `IllegalMonitorStateException` is ever thrown.

**Testing procedure**: Inspected each of `run1.log`–`run5.log` for stack traces and the strings `Exception` / `Error`. Also visually scrolled through the console output of one run.

**Results**: Zero exceptions in all 5 logs. The program terminates cleanly with the "ALL PROCESSES COMPLETED" banner every time.

**What this proves**: The `logLock` correctly serialises writes to the `ArrayList`, and every `lock()` call is matched by an `unlock()` (no `IllegalMonitorStateException`, which would be thrown if a thread tried to release a lock it did not own).

---

### Test 3: Correctness Verification
**What I tested**: That the final statistics are not just *consistent* but also *correct*.

**Expected values** (with seed `studentID = 445052261`):
- Number of processes: `10 + random.nextInt(11)` with the seed → **10 processes**.
- Each process appears in the log at least twice: one "started quantum execution" and one "yielded CPU" / "completed execution". Therefore `log entries ≈ context switches + completed processes` = `16 + 10 = 26`. The actual value is 32 because some processes go through several quanta and produce multiple "started + yielded" pairs.
- All 10 processes must finish: completed processes = 10, no negative remaining time.

**Actual values**: Context Switches = 16, Completed Processes = 10, Log Entries = 32, all non-negative waiting times in the per-process table. Matches the expectation.

**Analysis**: The sanity-check identity `(context switches) + (completed processes) ≤ log entries` holds (`26 ≤ 32`), which is exactly what the README's "Test 2: Check log count" predicts. Numbers are correct.

---

### Test 4: Different Scenarios
**Scenario tested**: Temporarily changed `Semaphore(1)` to `Semaphore(2)` and re-ran the program (then reverted before committing).

**Purpose**: To confirm that the semaphore really is what serialises CPU usage, and to see what a "two-CPU" scheduler looks like.

**Results**: With two permits, two processes start their quantum at the same time, the progress-bar lines from the two threads interleave on the console, and the totals at the end are still correct (context switches and completed processes are still atomic because they are protected by the counter locks, not by the semaphore).

**What I learned**: The semaphore controls **admission to the CPU**, not **data consistency**. The correctness of the counters comes from the `ReentrantLock`s; even when two threads are running simultaneously, the locks still keep the shared counters and the log internally consistent. This made the boundary between Task 1/2 (mutual exclusion) and Task 3 (resource control) very clear in my mind.

---

## Part 5: Reflection and Learning

### What I learned about synchronization:

The biggest insight for me was realising that operations I always thought of as a single instruction — like `count++` — are actually three steps under the hood (read, add, write), and that gap is exactly where race conditions live. Once you see that, the rest of the topic becomes very concrete: a *critical section* is the smallest piece of code that has to look atomic from outside, and a synchronization primitive is just a way to keep two threads from being inside the same critical section at the same time. I also learned that `ReentrantLock` and `Semaphore` are not interchangeable even though they "feel" similar — a lock is **owned** by one thread for mutual exclusion, while a semaphore tracks **permits** for resource admission and does not have to be released by the same thread that acquired it. The single most important habit I picked up is putting `unlock()` and `release()` inside `finally` blocks — without that, one exception inside a critical section can hang every other thread forever, which is functionally a deadlock. Finally, choosing **lock granularity** (one big lock vs. one lock per resource) turned out to be a real engineering decision: too coarse and you serialise things that did not need to be serialised; too fine and the code gets harder to reason about.

---

### Real-world applications:

Give TWO examples where synchronization is critical:

**Example 1**: **Bank account transfers.** When two customers are simultaneously transferring money out of the same joint account, the bank's backend reads the balance, subtracts the amount, and writes the new balance — exactly the same read-modify-write pattern as `count++`. Without a lock around the account record, two concurrent withdrawals could each read a balance of `1000`, each subtract `800`, and each write `200`, allowing the customer to withdraw `1600` from a `1000` account. Real banking systems use database-level row locks (or optimistic concurrency control) to make this impossible.

**Example 2**: **Airline seat reservation systems.** When the last seat on a flight is being booked by two passengers from different cities at the same instant, the booking service must guarantee that only one of them gets the seat. This is conceptually the same as my binary `Semaphore(1)`: the seat is a shared resource with exactly one permit, and only one thread/transaction may "acquire" it. Without synchronization the airline would oversell the flight, which is exactly the kind of correctness failure synchronization is designed to prevent.

---

### How I would explain synchronization to others:

Imagine the shared counters and the log are a **single whiteboard** in a classroom, and each `Process` thread is a student who needs to write on it. In Assignment 1 there was only one student writing at a time, so nothing could go wrong. In Assignment 3 several students rush to the whiteboard at once: two of them grab the marker simultaneously, both read "5", both write "6", and one increment is lost — that is a *race condition*. A `ReentrantLock` is the **marker itself**: only the student holding the marker may write, and they must put it back when they are done (`finally { unlock(); }`). A `Semaphore(1)` is a **chair in front of the whiteboard**: there is one chair, and only the student sitting in it can work; everyone else queues behind. The reason I used three separate markers for the three counters (instead of one) is the same reason a real classroom would have multiple whiteboards if the topics were unrelated — you do not want a student updating the attendance list to block a different student updating the homework list, because those two activities can safely happen at the same time. That is the whole idea: figure out what your shared resource is, give it a marker, and make sure the marker always goes back.

---

## Part 6: GitHub Repository Information

**Repository URL**: https://github.com/reemas-mofareh-445/OS-Assignment3-Reemas-Mofareh

**Number of commits**: 6 total (4 of them are the meaningful task commits required by the assignment; 2 are the upstream "first commit" inherited from the fork).

**Commit messages**:
1. `Set student ID: 445052261` — initial change after forking, anchors all subsequent work to my account.
2. `Task 1 (445052261): Added fine-grained ReentrantLock for counter protection` — three independent `ReentrantLock`s (one per counter) wrapping the increment helpers in `try/finally`.
3. `Task 2 (445052261): Added ReentrantLock for execution log` — dedicated `logLock` protecting `executionLog.add(...)`.
4. `Task 3 (445052261): Implemented binary semaphore for CPU control in run() and runToCompletion()` — `Semaphore(1)` acquired before each quantum and released in `finally`, with an `acquired` guard so no permit is leaked on `InterruptedException`.

(A 5th commit will add this completed `ASSIGNMENT_DOCUMENTATION.md`.)

---

## Summary

**Total time spent on assignment**: ~3.5 hours total — roughly 30 min setup (fork/clone/student ID), 45 min Task 1, 30 min Task 2, 50 min Task 3 (the `InterruptedException` + permit-leak handling took the longest), 60 min documentation, and the rest on testing/verification.

**Key takeaways**:
1. `count++` is **not atomic**. Any shared mutable state touched by multiple threads needs a synchronization primitive — there are no exceptions.
2. **`ReentrantLock` for mutual exclusion, `Semaphore` for resource admission.** They look similar but solve different problems, and using the wrong one is a design smell even when it happens to work.
3. **Always release in `finally`.** A leaked lock or a leaked permit is just as bad as a deadlock, and the bug only shows up when an exception is thrown.

**Most challenging aspect**: Handling `InterruptedException` correctly inside `Process.runToCompletion()`. My first version released the semaphore unconditionally in `finally`, which is wrong because if `acquire()` is interrupted before returning, no permit was actually obtained — releasing one anyway would let the semaphore drift above 1 and silently break the single-CPU model. Adding the boolean `acquired` flag fixed it cleanly.

**What I'm most proud of**: Choosing **fine-grained locking** for Task 1 instead of a single big lock. The README explicitly listed both options and asked us to justify our choice, and I am proud that my justification (independent counters → independent locks → no artificial contention) is the same reasoning real concurrent-programming codebases use.

---

**End of Documentation**
