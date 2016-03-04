package uk.dsxt.voting.tests;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Log4j2
public class AWSHelper {

    public static void main(String[] args) {
        AWSHelper awsHelper = new AWSHelper();
        if (args.length < 9) {
            System.out.printf("Usage: <accessKey> <secretKey> <endpoint> <imageId> <instanceType> <subnetId> <subnetId> <securityGroup> <keyName> <instancesCount>%n");
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("aws.endpoint", args[2]);
        properties.setProperty("aws.imageId", args[3]);
        properties.setProperty("aws.instanceType", args[4]);
        properties.setProperty("aws.subnetId", args[5]);
        properties.setProperty("aws.securityGroup", args[6]);
        properties.setProperty("aws.keyName", args[7]);
        awsHelper.start(args[0], args[1], properties, Integer.valueOf(args[8]));
    }

    private AmazonEC2 getConnection(String accessKey, String secretKey, Properties properties) {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonEC2 conn = new AmazonEC2Client(credentials);
        conn.setEndpoint(properties.getProperty("aws.endpoint"));
        return conn;
    }

    public List<String> start(String accessKey, String secretKey, Properties properties, int count) {
        AmazonEC2 conn = getConnection(accessKey, secretKey, properties);
        RunInstancesResult runInstancesResult = runInstances(conn,
                properties.getProperty("aws.imageId"),
                properties.getProperty("aws.instanceType"),
                count,
                properties.getProperty("aws.subnetId"),
                properties.getProperty("aws.securityGroup"),
                properties.getProperty("aws.keyName"));
        return runInstancesResult.getReservation().getInstances().stream().map(Instance::getInstanceId).collect(Collectors.toList());
    }

    private RunInstancesResult runInstances(AmazonEC2 conn, String imageId, String instanceType, int count,
                                            String subnetId, String securityGroup, String keyName) {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withImageId(imageId)
                .withInstanceType(instanceType)
                .withMinCount(count)
                .withMaxCount(count)
                .withSubnetId(subnetId)
                .withSecurityGroups(securityGroup)
                .withKeyName(keyName);
        return conn.runInstances(runInstancesRequest);
    }

    public void stop(String accessKey, String secretKey, Properties properties, List<String> instancesForTerminate) {
        AmazonEC2 conn = getConnection(accessKey, secretKey, properties);
        TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest(instancesForTerminate);
        conn.terminateInstances(terminateInstancesRequest);
    }

}
