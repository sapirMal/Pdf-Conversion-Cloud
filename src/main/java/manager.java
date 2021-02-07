import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.util.IOUtils;


import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Thread.sleep;

public class manager {
    public static void main(String[] args){
        System.out.println("Manager RUNNING");
        SQS queues = new SQS();
        int workerCount = 0, tmp, userCount=0, msgCount;
        boolean terminate = false;
        boolean lastUser = false;
        String userIdToTerminate = "";

        List<userHandler> userApplications = new LinkedList<userHandler>();

        try {
            while (true) {
                Message msg = queues.getMessage("M");
                if (msg == null){
                    sleep(1000);
                    continue;
                }
                System.out.println("*******************************");
                System.out.println("Recieved MSG:");
                System.out.println(msg.getBody());
                System.out.println("*******************************");

                String body = msg.getBody();
                String taskType = body.substring(0, body.indexOf('\n'));

                if (taskType.equals("new task")) {
                    body = msg.getBody().substring(body.indexOf('\n') + 1);

                    if (body.split("\n")[0].equals("terminate")) {
                        terminate = true;
                        userIdToTerminate = body.split("\n")[1];
                        if(userApplications.size() > 0 &&
                           userApplications.get(userApplications.size()-1).getId().equals(userIdToTerminate)){
                            lastUser = true;

                        }
                        // DO NOT ACCEPT MORE INPUT FILES

                        // Wait for workers to finish ~ WQ=empty -> close all workers
                        // generate response message -> export summary to userApplication
                    } else { //body = n\n<key of input file>\n<user ID>
                        if(terminate && lastUser) continue;

                        tmp = workerCount;
                        msgCount = 0;

                        String[] parts = body.split("\n");

                        System.out.println(Arrays.toString(parts));
                        int n = Integer.parseInt(parts[0]);
                        String key = parts[1];
                        String userID = parts[2];
                        String userQUrl = parts[3];


                        if(userID.equals(userIdToTerminate)){
                            lastUser = true;
                        }

                        S3Object inputFile = S3.downloadFile(key);
                        String inputData = IOUtils.toString(inputFile.getObjectContent());
                        String toSend;
                        while(!inputData.equals("")){
                            if(msgCount % n == 0){
                                tmp--;
                                if(tmp<=0){
                                    //create new worker, workerCount++
                                    EC2.runMachines(1, 1, "worker", Integer.toString(workerCount));
                                    if(workerCount<19) {
                                        workerCount++;
                                    }
                                }
                            }
                            int index = inputData.indexOf('\n');
                            if(index == -1){
                                toSend = inputData;
                                inputData = "";

                            }
                            else {
                                toSend = inputData.substring(0, index);
                                inputData = inputData.substring(index+1);
                            }
                            System.out.println("TOSEND::"+toSend);
                            toSend = "new PDF task\n" + userCount + "\n" + toSend;
                            queues.sendMessage("W", toSend);
                            msgCount++;
                        }
                        userHandler userApp = new userHandler(userID, msgCount, "", userQUrl);
                        userApplications.add(userApp);
                        userCount++;
                    }

                } else if (taskType.equals("done PDF task")) {
                    String[] parts = body.split("\n");
                    int userAppIndex = Integer.parseInt(parts[1]);
                    String oldURL = parts[2];
                    String outputURL = parts[3]; //Could also be description of the error the worker experienced
                    String operation = parts[4];
                    String toExport = operation + ": " + oldURL + " " + outputURL + "\n";
                    userHandler userApp = userApplications.get(userAppIndex);
                    userApp.setAcc(userApp.getAcc() + toExport);
                    userApp.setActiveMsgs(userApp.getActiveMsgs() - 1);

                    if (userApp.getActiveMsgs() == 0) {
                        System.out.println("********************** PRINTING ACC");
                        System.out.println(userApp.getAcc());
                        System.out.println("********************** Finished ACC");

                        String fname = "summary" + userApp.getId();
                        utilities.stringToText(fname, userApp.getAcc());
                        File f = new File(fname + ".txt");
                        String URL = S3.getFileURL(S3.uploadFile(f));
                        queues.sendMessage(userApp.getQurl(), "done task\n" + userApp.getId() + "\n" + URL);

                    }
                }


                if(terminate && lastUser){
                    // iterate over all userapps, wait untill all active messages = 0
                    boolean over = true;
                    for (userHandler userApp : userApplications){
                        if(userApp.getActiveMsgs() > 0) {
                            over = false;
                        }
                    }

                    if (!over){
                        queues.removeMessage("M", msg);
                        continue;
                    }
                    System.out.println();
                    // close all workers
                    EC2.closeWorkers();

                    queues.closeQueues();

                    // Last queue is UserApp queue and it remains unclosed for the event which
                    // multiple users wait for an answer and  there's no guarantee which user will receive
                    // his response message last.

                    // close manager, removing message is redundant as we closed the manager Q above.
                    //queues.removeMessage("M", msg);
                    EC2.closeManager();
                    return;
                }

                queues.removeMessage("M", msg);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
