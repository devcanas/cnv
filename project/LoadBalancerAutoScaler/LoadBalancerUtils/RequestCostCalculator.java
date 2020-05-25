package LoadBalancerAutoScaler.LoadBalancerUtils;

public class RequestCostCalculator {
    
    private static int maxBranchesTaken;
    private static int maxBranchesNotTaken;
    private static int maxFieldLoadCount;
    private static int maxFieldStoreCount;
    private static int maxLoadCount;
    private static int maxStoreCount;
    
    public void updateMaxValue(){
        this.maxBranchesTaken = 5;
        this.maxBranchesNotTaken = 5;
        this.maxFieldLoadCount = 5;
        this.maxFieldStoreCount = 5;
        this.maxLoadCount = 5;
        this.maxStoreCount = 5;
        //Request Dynamo Db and Update
    }

    public static float computeRequestLoad(String strategy,String maxUnassignedEntries,String puzzleLines, String puzzleColumns, String puzzleName){
        this.updateMaxValue();
        float branchesTaken = (1 * 100) / maxBranchesTaken;
        float branchesNotTaken = (1 * 100) / maxBranchesNotTaken;
        float fieldLoadCount = (1 * 100) / maxFieldLoadCount;
        float fieldStoreCount = (1 * 100) / maxFieldStoreCount;
        float loadCount = (1 * 100) / maxLoadCount;
        float storeCount = (1 * 100) / maxStoreCount;
        
        float totalLoad = (branchesTaken + branchesNotTaken + fieldLoadCount + fieldLoadCount + loadCount + storeCount) / 6;

        return totalLoad / 100;
    }
    
    
}