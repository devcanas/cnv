package LoadBalancerAutoScaler;

import LoadBalancerAutoScaler.AutoScalerUtils.*;
import LoadBalancerAutoScaler.Main;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AutoScaler {

    public static void start()
    {
        // Starts health check thread
        HealthCheckThread healthCheckThread = new HealthCheckThread();
        healthCheckThread.start();

        // Start scalling pollicies thread
        ScallingPolliciesThread scallingPolliciesThread = new ScallingPolliciesThread();
        scallingPolliciesThread.start();
    }

    /*
        Pings all running instances (except the Load Balancer and Auto Scaler instance) every 30 seconds.
        If health check fails -> terminates instance, send the pending requests to other instances
     */
    static class HealthCheckThread extends Thread
    {
        Instance currentInstance = null;
        InstanceState currentInstanceState = null;
        public HashMap<String, Integer> instanceFailures = new HashMap<>();

        public void run() {
            while(true){
                try{
                    for (Map.Entry<Instance, InstanceState> entry : Main.instances.entrySet()) {
                        currentInstance = entry.getKey();
                        currentInstanceState = entry.getValue();
                        if(currentInstance.getPublicDnsName().equals("")){
                            DescribeInstancesResult describeInstancesResult = Main.ec2.describeInstances();
                            List<Reservation> reservations = describeInstancesResult.getReservations();
                            for (Reservation reservation : reservations) {
                                for (Instance instance : reservation.getInstances()) {
                                    if(instance.getInstanceId().equals(currentInstance.getInstanceId())){
                                        InstanceState is = Main.instances.remove(currentInstance);
                                        Main.instances.put(instance, is);
                                    }
                                }
                            }
                        }
                        URL url = new URL("http://" + currentInstance.getPublicDnsName() +":8000/ping");
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("GET");
                        con.setConnectTimeout(5000); //set timeout to 5 seconds
                        int status = con.getResponseCode();
                        if(status != 200){
                            System.out.println("Failed");
                            //Check if instance is runnning
                            //Terminate Instance
                            //Forward pending requests
                            //Remove instance from this.instances
                        }
                        instanceFailures.put(currentInstance.getInstanceId(), 0);
                        System.out.println("Instance : " + currentInstance.getInstanceId() + " is okay.");
                    }
                    Thread.sleep(30000);
                }catch (Exception e){
                    System.out.println("Instance : " + currentInstance.getInstanceId() + " has failed.");
                    instanceFailures.put(currentInstance.getInstanceId(), instanceFailures.get(currentInstance.getInstanceId()) + 1);
                    if(instanceFailures.get(currentInstance.getInstanceId()) >= 2){
                        InstanceManager.terminateInstance(currentInstance.getInstanceId());
                    }
                    //e.printStackTrace();
                }
            }
        }
    }
    /*
        Requests all running instances for its CPU Utilization (except the Load Balancer and Auto Scaler instance)
        every 60 seconds.
        Metrics:
            Cpu Utilization from the last 3 minutes
            Datapoints with period 1 minute
        Average CPU Utilization of an instance:
            50 % for AWS CPU Utilization value
            50 % for pending requests costs
     */
    static class ScallingPolliciesThread extends Thread
    {
        private static final int MINIMUM_INSTANCES = 1;
        private static final int MAXIMUM_INSTANCES = 3;

        public void run(){
            while(true) {
                try {
                    double instanceTotal = 0;
                    int instanceCount = 0;
                    long offsetInMilliseconds = 1000 * 60 * 3;
                    Dimension instanceDimension = new Dimension();
                    instanceDimension.setName("InstanceId");
                    for (Map.Entry<Instance, InstanceState> entry : Main.instances.entrySet()) {
                        String name = entry.getKey().getInstanceId();
                        System.out.println("Evaluating Instance: " + name);
                        // If an instance to be terminated as finished its pending requests -> terminate it and discard its metrics
                        if(entry.getValue().isToTerminate() && entry.getValue().getComputationLeft() == 0) {
                            InstanceManager.terminateInstance(name);
                            instanceCount--;
                            continue;
                        }
                        instanceDimension.setValue(name);
                        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                                .withStartTime(new Date(new Date().getTime() - offsetInMilliseconds))
                                .withNamespace("AWS/EC2")
                                .withPeriod(60)
                                .withMetricName("CPUUtilization")
                                .withStatistics("Average")
                                .withDimensions(instanceDimension)
                                .withEndTime(new Date());
                        GetMetricStatisticsResult getMetricStatisticsResult =
                                Main.cloudWatch.getMetricStatistics(request);
                        List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();
                        double instanceCPUUtilization = 0;
                        for (Datapoint dp : datapoints) {
                            instanceCount++;
                            instanceCPUUtilization += dp.getAverage();
                        }
                        // If there are not any datapoints of the instance yet assume a CPU Utilization of 50% to prevent
                        // auto scaler from terminating instances.
                        if(datapoints.size() == 0) {
                            instanceCPUUtilization += 50;
                            instanceCount = 1;
                        }
                        // Normalized the computation left to percentage
                        float instancePendingRequestCost = entry.getValue().getComputationLeft() * (float) 100 / 6;

                        instanceCPUUtilization = (instanceCPUUtilization/instanceCount + instancePendingRequestCost)/2;
                        System.out.println("Cpu Utilization for instance: " + name + " is : " + instanceCPUUtilization);

                        // Total for all instances
                        instanceTotal += instanceCPUUtilization;
                    }
                    // Average of all instances
                    if(Main.instances.size() >= MINIMUM_INSTANCES){
                        instanceTotal = instanceTotal/Main.instances.size();
                        System.out.println("Total Instances CPU Utilization: " + instanceTotal);
                    }
                    // If 0 instances running or less than 3 and overall cpu utlization higher than 70 -> launch new instance
                    // Else if 3 instances running or more than 1 and overall cpu utilization lower than 40 -> signal instance termination
                    if(Main.instances.size() < MINIMUM_INSTANCES || (instanceTotal >= 70 && Main.instances.size() < MAXIMUM_INSTANCES)){
                        InstanceManager.newInstance();
                    }else if(Main.instances.size() > MAXIMUM_INSTANCES || (instanceTotal <= 40 && Main.instances.size() > MINIMUM_INSTANCES)) {
                        InstanceManager.signalTermination(LoadBalancer.getLessLoadedInstance().getInstanceId());
                    }
                    Thread.sleep(60000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}