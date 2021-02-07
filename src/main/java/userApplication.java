import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static java.lang.Thread.sleep;

public class userApplication {
    public static void main(String[] args){
        /*
        User starts the application and supplies as input a
        file with URLs of PDF files together with operations to perform on them,
        n integer n stating how many PDF files per worker,
        and an optional argument terminate,
        if received the local application sends a terminate message to the Manager.
        */

        // run cmd line: java -jar myjar.jar INPUT OUTPUT N [terminate]
        String id = UUID.randomUUID().toString();
        File inFile = new File(args[0]);
        String outputFilename = args[1];
        if(outputFilename.contains(".html")){
            outputFilename = outputFilename.substring(0, outputFilename.length()-5);
        }


        String nWorkLoad = args[2];
        boolean terminate = false;
        if(args.length > 3){
            terminate = true;
        }

        //check manager node existance
        if(!checkManager()){
            EC2.runMachines(1, 1, "manager", "whatever");
        }
        //Upload input file to S3
        //Verify a bucket exists
        S3.createBucket();
        String inFileKey = S3.uploadFile(inFile);

        //Send message noting the location of inFile in S3
        SQS queues = new SQS();
        String qurl = queues.makeUserQ(id);

        /*
        System.out.println("*******************************");
        System.out.println("WRITING NEW MESSAGE:");
        System.out.println("new task\n"+nWorkLoad+"\n"+inFileKey+"\n"+id);
        System.out.println("*******************************");

         */

        queues.sendMessage("M", "new task\n"+nWorkLoad+"\n"+inFileKey+"\n"+id+"\n"+qurl);
        if(terminate) {
            /*
            System.out.println("*******************************");
            System.out.println("WRITING NEW MESSAGE:");
            System.out.println("new task\nterminate\n"+id);
            System.out.println("*******************************");

             */

            queues.sendMessage("M", "new task\nterminate\n"+id);
        }
        Message msg;
        while(true){
            msg = queues.getMessage(qurl);
            if(msg == null)
                try {
                    sleep(1);
                    continue;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            String[] parts = msg.getBody().split("\n");



            if(parts[0].equals("done task")){
                if (parts[1].equals(id)) {
                    break;
                }
            }
        }

        S3Object summary = S3.downloadFile("summary"+id+".txt");

        try {
            utilities.stringToHTML(outputFilename, IOUtils.toString(summary.getObjectContent()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        queues.removeMessage(qurl, msg);
        try {
            queues.closeQ(qurl);
        }
        catch(Exception e){
            e.printStackTrace();
        }


        System.out.println("Done! output file named: "+outputFilename+".html will be found at current working directory");
    }
    // ATM: ouputfilename is not unique, 2 users or more can choose the same output filename,
    // can result in errors when saving the file to s3 (same files)
    // possible solution: add the userID to the output filename as filename+id.pdf
    // we did not implement this because the output filename requirement

    private static boolean checkManager(){
        List<Instance> activeInstances = EC2.getActiveInstances();
        for (Instance i : activeInstances){
            List<Tag> tags = i.getTags();
            for (Tag t : tags){
                if(t.getKey().equals("manager")){
                    return true;
                }
            }
        }
        return false;
    }


}

