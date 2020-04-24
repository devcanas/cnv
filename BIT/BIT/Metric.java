package BIT;

public class Metric {
    int totalBranches, branchPC, branchNumber, loadcount, storecount, fieldloadcount, fieldstorecount;
    String methodName, className;
    StatisticsBranch[] branchInfo;

    public Metric(int total_branches) {
        this.totalBranches = total_branches;
        branchInfo = new StatisticsBranch[totalBranches];
    }

    public int getTotalBranches() { return totalBranches; }

    public void reset(){
        branchInfo = new StatisticsBranch[totalBranches];
        fieldloadcount = 0;
        fieldstorecount = 0;
        storecount = 0;
        loadcount = 0;
    }

    public String toString(){
        String result = "Branch summary:\n";
        result += "CLASS NAME" + '\t' + "METHOD" + '\t' + "PC" + '\t' + "TAKEN" + '\t' + "NOT_TAKEN\n";
        result += branchInfo.length + "\n";

        for (int i = 0; i < branchInfo.length; i++) {
            if (branchInfo[i] != null) {
                result += branchInfo[i].print();
            }
        }

        result += "Load Store summary:\n";
        result += "Field load:    " + fieldloadcount + '\n';
        result += "Field store:   " + fieldstorecount + '\n';
        result += "Regular load:  " + loadcount + '\n';
        result += "Regular store: " + storecount + '\n';

        return result;
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