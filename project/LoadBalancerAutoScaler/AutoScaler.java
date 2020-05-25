package LoadBalancerAutoScaler;

import LoadBalancerAutoScaler.Main;
import LoadBalancerAutoScaler.AutoScalerUtils.*;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import java.util.Map;
import java.util.Date;
import java.util.List;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

public class AutoScaler {

    public static void start() {
        HealthCheckThread healthCheckThread = new HealthCheckThread();
        healthCheckThread.start();
        ScallingPolliciesThread scallingPolliciesThread = new ScallingPolliciesThread();
        scallingPolliciesThread.start();
    }

    static class HealthCheckThread extends Thread {
        public void run() {
            while(true){
                try{
                    for (Map.Entry<Instance, InstanceState> entry : Main.instances.entrySet()) {
                        if(entry.getKey().getPublicDnsName().equals("")){
                            continue;
                        }
                        if(entry.getKey().getTags().size() > 0){
                            boolean skipInstance = false;
                            for(Tag tag: entry.getKey().getTags()){
                                if(tag.getKey().equals("Name") && tag.getValue().equals("Load Balancer"))
                                    skipInstance = true;
                            }
                            if(skipInstance)
                                continue;
                        }
                        URL url = new URL("http://" + entry.getKey().getPublicDnsName() +":8000/ping");
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("GET");
                        int status = con.getResponseCode();
                        if(status != 200){
                            //Check if instance is runnning
                            //Stop instance if its not running
                            //Remove instance from this.instances
                            //See if there needs to be another instance running
                            //Launches new instance if needed
                        }
                    }
                    Thread.sleep(60000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    static class ScallingPolliciesThread extends Thread
    {
        private static final int MINIMUM_INSTANCES = 1;
        private static final int MAXIMUM_INSTANCES = 3;

        public void run(){
            while(true) {
                try {
                    Thread.sleep(60000);
                    double instanceTotal = 0;
                    long offsetInMilliseconds = 1000 * 60 * 3;
                    Dimension instanceDimension = new Dimension();
                    instanceDimension.setName("InstanceId");
                    for (Map.Entry<Instance, InstanceState> entry : Main.instances.entrySet()) {
                        String name = entry.getKey().getInstanceId();
                        System.out.println("Instance: " + name);
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
                        int aux = datapoints.size();
                        for (Datapoint dp : datapoints) {
                            System.out.println("Average Value " + dp.getAverage());
                            instanceCPUUtilization += dp.getAverage();
                        }
                        if(datapoints.size() == 0) {
                            instanceCPUUtilization += 50;
                            aux = 1;
                        }
                        instanceCPUUtilization = (instanceCPUUtilization/aux + (entry.getValue().getPendingRequestListSize() * 18.3))/2;
                        instanceTotal += instanceCPUUtilization;
                        System.out.println("Cpu Utilization for instance: " + name + " is : " + instanceCPUUtilization);
                        if(entry.getValue().isToTerminate() && entry.getValue().getPendingRequestListSize() == 0) {
                            InstanceManager.terminateInstance(name);
                        }
                    }
                    instanceTotal = instanceTotal/Main.instances.size();
                    if(Main.instances.size() < MINIMUM_INSTANCES || (instanceTotal >= 70 && Main.instances.size() < MAXIMUM_INSTANCES)){
                        InstanceManager.newInstance();
                    }else if(Main.instances.size() > MAXIMUM_INSTANCES || (instanceTotal <= 40 && Main.instances.size() > MINIMUM_INSTANCES)) {
                        InstanceManager.signalTermination(LoadBalancer.getLessLoadedInstance().getInstanceId());
                    }
                    System.out.println("Total Instances CPU Utilization: " + instanceTotal);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}