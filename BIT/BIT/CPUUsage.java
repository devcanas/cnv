package BIT;

import BIT.highBIT.*;
import java.io.*;
import java.util.*;

public class CPUUsage {
    private static PrintStream out = null;
    private static int i_count = 0, b_count = 0, m_count = 0;
    private static Map<Long, Metric> metrics = new HashMap<Long, Metric>();
    private static Metric m;

    public static void main(String argv[]) {
        File file_in = new File(argv[0]);
        String infilenames[] = file_in.list();

        int total = 0;
        int k = 0;

        for (int i = 0; i < infilenames.length; i++) {
            String infilename = infilenames[i];
            if (infilename.endsWith(".class")) {
                // create class info object
                ClassInfo ci = new ClassInfo(argv[0] + System.getProperty("file.separator") + infilename);

                // loop through all the routines
                // see java.util.Enumeration for more information on Enumeration class
                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    InstructionArray instructions = routine.getInstructionArray();

                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        Instruction instr = (Instruction) instructions.elementAt(bb.getEndAddress());
                        short instr_type = InstructionTable.InstructionTypeTable[instr.getOpcode()];
                        if (instr_type == InstructionTable.CONDITIONAL_INSTRUCTION) {
                            total++;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < infilenames.length; i++) {
            String infilename = infilenames[i];
            if (infilename.endsWith(".class")) {
                ClassInfo ci = new ClassInfo(argv[0] + System.getProperty("file.separator") + infilename);

                ci.addBefore("BIT/CPUUsage", "branchInit", new Integer(total));
                ci.addBefore("BIT/CPUUsage", "setBranchClassName", ci.getClassName());

                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    routine.addBefore("BIT/CPUUsage", "setBranchMethodName", routine.getMethodName());
                    InstructionArray instructions = routine.getInstructionArray();
                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        Instruction instr = (Instruction) instructions.elementAt(bb.getEndAddress());
                        int opcode=instr.getOpcode();
                        if (opcode == InstructionTable.getfield)
                            instr.addBefore("BIT/CPUUsage", "LSFieldCount", new Integer(0));
                        else if (opcode == InstructionTable.putfield)
                            instr.addBefore("BIT/CPUUsage", "LSFieldCount", new Integer(1));
                        else{
                            short instr_type = InstructionTable.InstructionTypeTable[instr.getOpcode()];
                            if (instr_type == InstructionTable.CONDITIONAL_INSTRUCTION) {
                                instr.addBefore("BIT/CPUUsage", "setBranchPC", new Integer(instr.getOffset()));
                                instr.addBefore("BIT/CPUUsage", "updateBranchNumber", new Integer(k));
                                instr.addBefore("BIT/CPUUsage", "updateBranchOutcome", "BranchOutcome");
                                k++;
                            }else if (instr_type == InstructionTable.LOAD_INSTRUCTION) {
                                instr.addBefore("BIT/CPUUsage", "LSCount", new Integer(0));
                            }
                            else if (instr_type == InstructionTable.STORE_INSTRUCTION) {
                                instr.addBefore("BIT/CPUUsage", "LSCount", new Integer(1));
                            }
                        }
                    }
                }
            }
        }
    }

    public synchronized void reset(){
        metrics.get(Thread.currentThread().getId()).reset();
    }

    public synchronized String toString(){
        long id = Thread.currentThread().getId();
        String result = "\nThread: \t" + id + "\n";
        result += metrics.get(id).toString();
        return result;
    }

    public static synchronized void setBranchClassName(String name)
    {
        Metric m = metrics.get(Thread.currentThread().getId());
        m.setClassName(name);
    }

    public static synchronized void setBranchMethodName(String name)
    {
        Metric m = metrics.get(Thread.currentThread().getId());
        m.setMethodName(name);
    }

    public static synchronized void setBranchPC(int pc)
    {
        Metric m = metrics.get(Thread.currentThread().getId());
        m.setBranchPC(pc);
    }

    public static synchronized void branchInit(int n)
    {
        Metric m = metrics.get(Thread.currentThread().getId());
        if(m == null){
            m = new Metric(n);
            metrics.put(Thread.currentThread().getId(), m);
        }
    }

    public static synchronized void updateBranchNumber(int n)
    {
        Metric m = metrics.get(Thread.currentThread().getId());
        m.updateBranchNumber(n);
    }

    public static synchronized void updateBranchOutcome(int br_outcome)
    {
        Metric m = metrics.get(Thread.currentThread().getId());
        m.updateBranchOutcome(br_outcome);
    }

    public static synchronized void LSFieldCount(int type)
    {
        Metric m = metrics.get(Thread.currentThread().getId());
        m.LSFieldCount(type);
    }

    public static synchronized void LSCount(int type)
    {
        Metric m = metrics.get(Thread.currentThread().getId());
        m.LSCount(type);
    }
}

