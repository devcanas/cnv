package BIT;

public class Metric {
    int branchPC, totalBranches, branchNumber, loadcount, storecount, fieldloadcount, fieldstorecount;
    String methodName, className;
    StatisticsBranch[] branchInfo;

    public Metric(int total_branches) {
        this.totalBranches = total_branches;
        branchInfo = new StatisticsBranch[totalBranches];
    }

    public int getTotalBranches() { return totalBranches; }

    enum Branch { TAKEN, NOT_TAKEN }
    private int sum(Branch b) {
        int total = 0;
        for (int i = 0; i < branchInfo.length; i++) {
            if (branchInfo[i] != null) {
                total += b.equals(Branch.TAKEN) 
                    ? branchInfo[i].taken_
                    : branchInfo[i].not_taken_;
            }
        }
        return total;
    }

    public int getTotalBranchesTaken() { return sum(Branch.TAKEN); }
    public int getTotalBranchesNotTaken() { return sum(Branch.NOT_TAKEN); }
    public int getFieldLoadCount() { return fieldloadcount; }
    public int getFieldStoreCount() { return fieldstorecount; }
    public int getLoadCount() { return loadcount; }
    public int getStoreCount() { return storecount; }

    public void reset(){
        branchInfo = new StatisticsBranch[totalBranches];
        fieldloadcount = 0;
        fieldstorecount = 0;
        storecount = 0;
        loadcount = 0;
    }

    public void setMethodName(String name){
        methodName = name;
    }

    public void setClassName(String name){
        className = name;
    }

    public void setBranchPC(int pc) {
        this.branchPC = pc;
    }

    public void updateBranchNumber(int n){
        branchNumber = n;
        if (branchInfo[branchNumber] == null) {
            branchInfo[branchNumber] = new StatisticsBranch(className, methodName, branchPC);
        }
    }

    public void updateBranchOutcome(int br_outcome) {
        if (br_outcome == 0) {
            branchInfo[branchNumber].incrNotTaken();
        }
        else {
            branchInfo[branchNumber].incrTaken();
        }
    }

    public void LSCount(int type) {
        if (type == 0)
            loadcount++;
        else
            storecount++;
    }

    public void LSFieldCount(int type) {
        if (type == 0)
            fieldloadcount++;
        else
            fieldstorecount++;
    }

}