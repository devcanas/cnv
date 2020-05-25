package LoadBalancerAutoScaler.AutoScalerUtils;

import LoadBalancerAutoScaler.Main;
import LoadBalancerAutoScaler.InstanceState;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

import java.util.Map;

public class InstanceManager {

    public static void newInstance()
    {
        System.out.println("Starting a new instance.");
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId("ami-0fbea6706efb7b85c")
                .withInstanceType("t2.micro")
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName("CNV-2020")
                .withSecurityGroupIds("sg-00269700b432f948a")
                .withSubnetId("subnet-59807c78")
                .withMonitoring(true);
        RunInstancesResult runInstancesResult = Main.ec2.runInstances(runInstancesRequest);

        //Adding the instance to hashset of instances
        Main.instances.put(runInstancesResult.getReservation().getInstances().get(0), new InstanceState());
    }

    public static void signalTermination(String instanceId){
        for(Map.Entry<Instance, InstanceState> entry : Main.instances.entrySet()){
            if(entry.getKey().getInstanceId().equals(instanceId)) {
                entry.getValue().markAsToBeTerminated();
            }
        }
    }

    public static void terminateInstance(String instanceId){
        System.out.println("Terminating instance with id: " + instanceId);
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instanceId);
        Main.ec2.terminateInstances(termInstanceReq);

        //Removing the instance from the hashset of instances
        for(Map.Entry<Instance, InstanceState> entry : Main.instances.entrySet()){
            if(entry.getKey().getInstanceId().equals(instanceId))
                Main.instances.remove(entry.getKey());
        }
    }

}