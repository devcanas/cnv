Our Instrumentation class CPUUsage instruments the solver classes in order to obtain both branch outcome statistics and 
load_store information. This class has a HashMap with the ThreadId as key and a Metric object as value. The threadId is 
the identifier of the thread responsible for the request we are getting the metrics from and the Metric object will store
all variables and perform the necessary operations to get the correct metrics. This way we can store the metrics from each
thread in its own without ever being after by other running threads collection their own metrics.  

In order to obtain the metrics, each WebServer executor launches an instance of the CPUUsage class as it receives a request.
After handling the request it calls a CPUUsage method to log the metrics to the log.txt and resets all counters.

Regarding the Load Balancer it has the following configurations:
name: CNV-project-LB
port: 80
Subnets: us-east-1a
Healthcheck
- ping port: 8000
- ping path: /sudoku

Auto Scaling Group
name: CNV-project-group
subnet: us-east-1a
Load balancing: Receive traffic from one or more load balancers 
Health Check Type: ELB
Health Check Grace Period: 60 seconds
Monitoring: Enable CloudWatch detailed monitoring 
Scaling Policies
- Scale between 1 and 2 instances
- Increase group size:
    - Add 1 capacity group when CPUUtilization >= 40% for more than 1 minute
    - Instances need 300 seconds to warm up after each step
- Decrease group size:
    - Remove 1 capacity group when CPUUtilization <= 20% for more than 1 minute
