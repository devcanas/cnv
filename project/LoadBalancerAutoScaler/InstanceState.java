package LoadBalancerAutoScaler;

import com.sun.net.httpserver.HttpExchange;
import java.util.List;
import java.util.ArrayList;

public class InstanceState {
    float computationLeft;
    List<HttpExchange> pendingRequests = new ArrayList<>();
    boolean toTerminate = false;

    public InstanceState(){
        this.computationLeft = 0;
    }

    public void addRequest(HttpExchange t){
        pendingRequests.add(t);
    }

    public void removeRequest(HttpExchange t){
        pendingRequests.remove(t);
    }

    public void addComputedRequestLoad(float load) {
        computationLeft += load;
    }

    public void removeComputedRequestLoad(float load) {
        computationLeft -= load;
    }

    public List<HttpExchange> getPendingRequestList(){
        return this.pendingRequests;
    }

    public int getPendingRequestListSize(){
        return this.pendingRequests.size();
    }

    public float getComputationLeft() {
        return this.computationLeft;
    }

    public void markAsToBeTerminated(){
        this.toTerminate = true;
    }

    public boolean isToTerminate(){
        return this.toTerminate;
    }

}