import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.util.Base64;

import java.util.LinkedList;
import java.util.List;

public class EC2 {
    private static AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
    private static List<Instance> runningInstances = new LinkedList<Instance>();

    public static List<Instance> getActiveInstances() {
        DescribeInstancesResult result = ec2.describeInstances();
        for (int i = 0; i < result.getReservations().size(); i++) {
            List<Instance> reservationInstances = result.getReservations().get(i).getInstances();
            for (Instance inst : reservationInstances) {
                if ((inst.getState().getName().equals("running") ||
                        inst.getState().getName().equals("pending"))
                        && !runningInstances.contains(inst)) {
                    runningInstances.add(inst);
                }

            }
        }
        return runningInstances;
    }


    public static List<Instance> runMachines(int min, int max, String key, String value){
        List<Instance> instances = null;
        try {
            String imageId = "";
            String userData = "";
            String firstLine = "#!/bin/bash\r\n";
            if(key.equals("manager")){
                imageId = "ami-02d6b8ccf9c9cbe20"; //Image with java, maven, the jar file of the manager
                userData = firstLine + "java -jar /home/ec2-user/manager.jar\r\n";
            }
            else if(key.equals("worker")){
                imageId = "ami-0fa2497f50bf0ce72"; //Image with java, maven, the updated jar file of the worker
                userData = firstLine + "java -jar  /home/ec2-user/worker.jar\r\n";
            }

            RunInstancesRequest request = new RunInstancesRequest(imageId, min, max);
            String encoded = Base64.encodeAsString(userData.getBytes());
            request.withUserData(encoded)
            .withIamInstanceProfile(new IamInstanceProfileSpecification().withName("Admin"))
            .withSecurityGroupIds("sg-05d41e32a3e33a11b")
            .setKeyName("ssh_login");
            request.setInstanceType(InstanceType.T2Micro.toString());

            //request.setUserData(Base64.encodeAsString(userData.getBytes()));//Base64.getEncoder().encodeToString(userData.getBytes()));

            Tag t = new Tag(key, value);
            List<Tag> tags = new LinkedList<Tag>();
            tags.add(t);
            List<TagSpecification> specifications = new LinkedList<TagSpecification>();
            TagSpecification tagspec = new TagSpecification();
            tagspec.setTags(tags);
            tagspec.setResourceType("instance");

            specifications.add(tagspec);

            request.setTagSpecifications(specifications);
            instances = ec2.runInstances(request).getReservation().getInstances();
            //System.out.println("**********");
            //System.out.println("runMachines - "+instances);

            runningInstances = getActiveInstances();
            //runningInstances = result.getReservations().get(0).getInstances();

            System.out.println("runningInstances length = "+runningInstances.size());
            //System.out.println("runningInstances - "+runningInstances);
            System.out.println("**********");

        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
        return instances;
    }


    private static boolean closeMachinesByTag(String tag){

        List <String> machinesIdByTag = new LinkedList<String>();

        for (Instance machine : runningInstances){
            if(machine.getTags().get(0).getKey().equals(tag)) {
                machinesIdByTag.add(machine.getInstanceId());
            }
        }

        System.out.println("running instances:");
        for (Instance inst : runningInstances){
            System.out.print("<" + inst.getTags().get(0) +", "+inst.getInstanceId()+ "> ");
        }
        System.out.println("machinesIdByTag: "+ machinesIdByTag);
        for (String id : machinesIdByTag){
            System.out.print("<" + id +"> ");
        }
        if (machinesIdByTag.size() > 0) {
            return ec2.terminateInstances(new TerminateInstancesRequest(machinesIdByTag)) != null;
        }
        return true;
    }

    public static boolean closeManager(){
        System.out.println("Trying to close manager");
        if(closeMachinesByTag("manager")){
            System.out.println("Manager closed!");
            return true;
        }
        System.out.println("Closing manager failed!");
        return false;
    }

    public static boolean closeWorkers(){
        System.out.println("Trying to close all workers");
        if(closeMachinesByTag("worker")){
            System.out.println("All workers closed!");
            return true;
        }
        System.out.println("Closing all workers failed!");
        return false;
    }

}
