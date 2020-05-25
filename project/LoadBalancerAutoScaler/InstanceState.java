package LoadBalancerAutoScaler;

import com.sun.net.httpserver.HttpExchange;
import java.util.List;
import java.util.ArrayList;

public class InstanceState {
    int computationLeft;
    List<HttpExchange> pendingRequests = new ArrayList<>();
    boolean toTerminate = false;

    public InstanceState(){
        this.computationLeft = 0;
    }

    public void addRequest(HttpExchange t){
        pendingRequests.add(t);
        computationLeft++;
    }

    public void removeRequest(HttpExchange t){
        pendingRequests.remove(t);
        computationLeft--;
    }

    public List<HttpExchange> getPendingRequestList(){
        return this.pendingRequests;
    }

    public int getPendingRequestListSize(){
        return this.pendingRequests.size();
    }

    public int getComputationLeft() {
        return this.computationLeft;
    }

    public void markAsToBeTerminated(){
        this.toTerminate = true;
    }

    public boolean isToTerminate(){
        return this.toTerminate;
    }

}