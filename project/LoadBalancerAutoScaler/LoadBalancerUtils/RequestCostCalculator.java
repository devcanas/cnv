package LoadBalancerAutoScaler.LoadBalancerUtils;

import pt.ulisboa.tecnico.cnv.server.MetricItem;
import pt.ulisboa.tecnico.cnv.server.MetricLogger;
import java.util.List;

public class RequestCostCalculator {
    
    private static int maxBranchesTaken = 5;
    private static int maxBranchesNotTaken = 5;
    private static int maxFieldLoadCount = 5;
    private static int maxFieldStoreCount = 5;
    private static int maxLoadCount = 5;
    private static int maxStoreCount = 5;
    
    public static void updateMaxValue(){
        List<MetricItem> metrics = MetricLogger.getInstance().getLogs();
        for (MetricItem metric : metrics) {
            if (maxBranchesTaken < metric.getBranchesTaken()) {
                maxBranchesTaken = metric.getBranchesTaken();
            }
            if (maxBranchesNotTaken < metric.getBranchesNotTaken()) {
                maxBranchesNotTaken = metric.getBranchesNotTaken();
            }
            if (maxFieldLoadCount < metric.getFieldLoadCount()) {
                maxFieldLoadCount = metric.getFieldLoadCount();
            }
            if (maxFieldStoreCount < metric.getFieldStoreCount()) {
                maxFieldStoreCount = metric.getFieldStoreCount();
            }
            if (maxLoadCount < metric.getLoadCount()) {
                maxLoadCount = metric.getLoadCount();
            }
            if (maxStoreCount < metric.getStoreCount()) {
                maxStoreCount = metric.getStoreCount();
            }
        }
    }

    public static float computeRequestLoad(String strategy,String maxUnassignedEntries,String puzzleLines, String puzzleColumns, String puzzleName){
        updateMaxValue();
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