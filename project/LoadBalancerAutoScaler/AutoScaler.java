package LoadBalancerAutoScaler;

import LoadBalancerAutoScaler.Main;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class AutoScaler {

    public static void newInstance()
    {
        System.out.println("Starting a new instance.");
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId("ami-02f0faa2adb9c6f64")
                .withInstanceType("t2.micro")
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName("CNV-2020")
                .withSecurityGroups("CNV-ssh+http");
        RunInstancesResult runInstancesResult = Main.ec2.runInstances(runInstancesRequest);

        //Adding the instance to hashset of instances
        Main.instances.add(runInstancesResult.getReservation().getInstances().get(0));
    }

    public static void terminateInstance(String instanceId){
        System.out.println("Terminating instance with id: " + instanceId);
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instanceId);
        Main.ec2.terminateInstances(termInstanceReq);

        //Removing the instance from the hashset of instances
        for(Instance instance: Main.instances){
            if(instance.getInstanceId().equals(instanceId))
                Main.instances.remove(instance);
        }
    }
}