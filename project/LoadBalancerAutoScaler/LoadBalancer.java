package LoadBalancerAutoScaler;

import LoadBalancerAutoScaler.LoadBalancerUtils.*;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

class LoadBalancer implements HttpHandler {

    private static final int MAX_PUZZLE_LINES = 25;
    private static final int MAX_PUZZLE_COLUMNS = 25;

    @Override
    public void handle(HttpExchange t) throws IOException {

        System.out.println("New request received: " + t.getRequestHeaders());

        ArrayList<String> newArgs = Parser.parseRequestParams(t.getRequestURI().getQuery());

        String strategy = newArgs.get(0);
        String maxUnassignedEntries = newArgs.get(1);
        String puzzleLines = newArgs.get(2);
        String puzzleColumns = newArgs.get(3);
        String puzzleName = newArgs.get(4);

        if(Integer.parseInt(puzzleLines) >= MAX_PUZZLE_LINES || Integer.parseInt(puzzleColumns) >= MAX_PUZZLE_COLUMNS){
            RequestForwarder.invalidRequest(t);
            return;
        }

        System.out.println("Strategy: " + strategy);
        System.out.println("maxUnassignedEntries: " + maxUnassignedEntries);
        System.out.println("puzzleLines: " + puzzleLines);
        System.out.println("puzzleColumns: " + puzzleColumns);
        System.out.println("puzzleName: " + puzzleName);

        float load = RequestCostCalculator.computeRequestLoad(strategy, maxUnassignedEntries, puzzleLines, puzzleColumns, puzzleName);
        System.out.println(load);

        try {
            Instance chosenInstance = getLessLoadedInstance();
            InstanceState is = Main.instances.get(chosenInstance);
            is.addRequest(t);
            is.addComputedRequestLoad(load);
            boolean success = RequestForwarder.forwardRequest(t, chosenInstance.getPublicDnsName());
            if(success) {
                Main.instances.get(chosenInstance).removeRequest(t);
                is.removeComputedRequestLoad(load);
            }
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    }

    public static Instance getLessLoadedInstance(){
        Instance chosenInstance = null;
        //Max number of computation left per thread is 6
        float lowest = 10;
        for(Map.Entry<Instance, InstanceState> entry : Main.instances.entrySet()){
            System.out.println(entry.getKey().getInstanceId() + ": " + entry.getValue().getComputationLeft());
            if(!entry.getValue().isToTerminate() && entry.getValue().getComputationLeft() <= lowest){
                chosenInstance = entry.getKey();
                lowest = entry.getValue().getComputationLeft();
            }
        }
        System.out.println("Instance chosen has id: " + chosenInstance.getInstanceId() + " and computation left (before the request): " + Main.instances.get(chosenInstance).getComputationLeft());
        return chosenInstance;

    }
}