import com.amazonaws.services.sqs.model.Message;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class worker {
    public static void main(String[] args){
        SQS queues = new SQS();

        while(true){
            Message msg = queues.getMessage("W");
            if(msg == null) {
                continue;
            }

            System.out.println("Recieved MSG:");
            System.out.println(msg.getBody());

            // msg is not null -> msg is "new PDF task ..."

            String body = msg.getBody();
            String[] parts = body.split("\n");
            String[] split = parts[2].split("\t");

            String userApp = parts[1];
            String operation = split[0];
            String oldURL = split[1];
            String outputURL, key;

            try {
                String filename = utilities.extractFileName(oldURL);

                FileUtils.copyURLToFile(new URL(oldURL), new File(filename+".pdf"));

                if (operation.equals("ToImage")) {
                    queues.setMsgVisibility("W", msg.getReceiptHandle(), 180);
                    utilities.toImg(filename);
                    filename = filename+".png";
                } else if (operation.equals("ToHTML")) {
                    utilities.toHtml(filename);
                    filename = filename+".html";
                } else if (operation.equals("ToText")) {
                    utilities.toText(filename);
                    filename = filename+".txt";
                } else {
                    // operation is not valid
                    outputURL = "Operation is not valid";
                    queues.sendMessage("M", "done PDF task\n"+userApp+"\n"+oldURL+"\n"+outputURL+"\n"+operation);
                    queues.removeMessage("W", msg);
                    continue;
                }

                key = S3.uploadFile(new File(filename));
                outputURL = S3.getFileURL(key);


                System.out.println("Finished converting");
                System.out.println("Sending manager queue done PDF task");


                queues.sendMessage("M", "done PDF task\n"+userApp+"\n"+oldURL+"\n"+outputURL+"\n"+operation);
                queues.removeMessage("W", msg);
            }
            catch(IOException ioe){
                outputURL = "Download failed, invalid URL";
                queues.sendMessage("M", "done PDF task\n"+userApp+"\n"+oldURL+"\n"+outputURL+"\n"+operation);
                queues.removeMessage("W", msg);
            }
            catch(Exception e){
                if(e.getMessage().equals("")) {
                    outputURL = "Unknown error occurred";
                }
                else{
                    outputURL = e.getMessage();
                }
                queues.sendMessage("M", "done PDF task\n"+userApp+"\n"+oldURL+"\n"+outputURL+"\n"+operation);
                queues.removeMessage("W", msg);
            }
        }


    }

}
