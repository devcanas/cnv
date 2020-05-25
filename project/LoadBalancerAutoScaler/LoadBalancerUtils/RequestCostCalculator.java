package LoadBalancerAutoScaler.LoadBalancerUtils;

import LoadBalancerAutoScaler.LoadBalancerUtils.MetricItem;
import LoadBalancerAutoScaler.LoadBalancerUtils.MetricLogger;
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

    public static float computeRequestLoad(String strategy,String maxUnassignedEntries,String puzzleLines, String puzzleColumns, String puzzleName)
    {
        updateMaxValue();

        // get metrics for similar requests
        MetricItem item = new MetricItem();
        item.setSolverStrategy(strategy);
        item.setUnassigned(Integer.parseInt(maxUnassignedEntries));
        item.setNCol(Integer.parseInt(puzzleLines));
        item.setNLin(Integer.parseInt(puzzleColumns));
        List<MetricItem> similarItems = MetricLogger.getInstance().getLogsSimilarTo(item);

        float branchesTakenPredicted = 1, branchesNotTakenPredicted = 1, fieldLoadCountPredicted = 1;
        float fieldStoreCountPredicted = 1, loadCountPredicted = 1, storeCountPredicted = 1;

        for (MetricItem i : similarItems) {
            branchesTakenPredicted += i.getBranchesTaken();
            branchesNotTakenPredicted += i.getBranchesNotTaken();
            fieldLoadCountPredicted += i.getFieldLoadCount();
            fieldStoreCountPredicted += i.getFieldStoreCount();
            loadCountPredicted += i.getLoadCount();
            storeCountPredicted += i.getStoreCount();
        }

        int count = similarItems.size() > 0 ? similarItems.size() : 1;
        float branchesTaken = normalized(branchesTakenPredicted / count, maxBranchesTaken);
        float branchesNotTaken = normalized(branchesNotTakenPredicted / count, maxBranchesNotTaken);
        float fieldLoadCount = normalized(fieldLoadCountPredicted / count, maxFieldLoadCount);
        float fieldStoreCount = normalized(fieldStoreCountPredicted / count, maxFieldStoreCount);
        float loadCount = normalized(loadCountPredicted / count, maxLoadCount);
        float storeCount = normalized(storeCountPredicted / count, maxStoreCount);
        
        float totalLoad = (branchesTaken + branchesNotTaken + fieldLoadCount + fieldLoadCount + loadCount + storeCount) / 6;

        return totalLoad / 100;
    }

    public static float normalized(float value, float maxValue) {
        return (value * 100) / maxValue;
    }
}