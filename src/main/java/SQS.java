import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

import java.util.List;


public class SQS {
    private AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
    private String managerQURL;
    private String workersQURL;

    public SQS() {
        CreateQueueRequest managerQReq = new CreateQueueRequest("managerQ3");
        CreateQueueRequest workersQReq = new CreateQueueRequest("workersQ3");
        //CreateQueueRequest userAppQReq = new CreateQueueRequest("userAppQ");


        managerQURL = sqs.createQueue(managerQReq).getQueueUrl();
        workersQURL = sqs.createQueue(workersQReq).getQueueUrl();
    }

    public void setMsgVisibility(String q, String msgReceipt, int timeout){
        String qUrl = getQURL(q);
        sqs.changeMessageVisibility(new ChangeMessageVisibilityRequest(qUrl, msgReceipt,timeout));
    }

    public String makeUserQ(String userID){
        return sqs.createQueue(new CreateQueueRequest(userID)).getQueueUrl();
    }

    public void closeQueues() {
        try {
            closeQ("M");
            closeQ("W");
            //closeQ("U");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void closeQ(String q){
        String qURL = getQURL(q);
        sqs.deleteQueue(qURL);
    }
    // send message to q
    public void sendMessage(String q, String msg) {

        try {
            String qURL = getQURL(q);
            sqs.sendMessage(new SendMessageRequest(qURL, msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // receive message from q
    // If q is empty -> return null, else return first available message
    public Message getMessage(String q) {
        try {
            ReceiveMessageRequest receiveMessageRequest;
            String qURL = getQURL(q);
            receiveMessageRequest = new ReceiveMessageRequest(qURL);
            List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
            if (messages.size() == 0) {
                return null;
            }
            return messages.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean removeMessage(String q, Message msg) {
        try {
            String qURL = getQURL(q);
            String messageRecieptHandle = msg.getReceiptHandle();
            if (sqs.deleteMessage(new DeleteMessageRequest(qURL, messageRecieptHandle)) != null) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getQURL(String q){
        if ("M".equals(q)) {
            return managerQURL;
        } else if ("W".equals(q)) {
            return workersQURL;
        }else {
            return q;
        }
    }

    public void purgeQs() {
        sqs.purgeQueue(new PurgeQueueRequest(workersQURL));
        sqs.purgeQueue(new PurgeQueueRequest(managerQURL));
        //sqs.purgeQueue(new PurgeQueueRequest(userAppQURL));

    }

}

