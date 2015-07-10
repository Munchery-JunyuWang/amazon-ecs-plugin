package com.phuna.amazonecs;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.Container;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.NetworkBinding;
import com.amazonaws.services.ecs.model.RunTaskRequest;
import com.amazonaws.services.ecs.model.RunTaskResult;
import com.amazonaws.services.ecs.model.StopTaskRequest;
import com.amazonaws.services.ecs.model.Task;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container.Port;

public class AWSUtils {
	private static final Logger logger = Logger.getLogger(AWSUtils.class
			.getName());

	// public static AmazonECSClient getEcsClient() {
	// AmazonECSClient client = null;
	// if (EcsCloud.getAwsCredentials() != null) {
	// client = new AmazonECSClient(EcsCloud.getAwsCredentials());
	// } else {
	// client = new AmazonECSClient();
	// }
	//
	// String endpoint = System.getenv("AWS_ECS_ENDPOINT");
	// if (endpoint == null || "".equals(endpoint)) {
	// endpoint = Constants.AWS_ECS_ENDPOINT;
	// }
	// client.setEndpoint(endpoint);
	// return client;
	// }
	//
	// public static AmazonECSClient getEcsClient(String accessKeyId,
	// String secretAccessKey) {
	// AmazonECSClient client = null;
	// client = new AmazonECSClient(new BasicAWSCredentials(accessKeyId,
	// secretAccessKey));
	//
	// String endpoint = System.getenv("AWS_ECS_ENDPOINT");
	// if (endpoint == null || "".equals(endpoint)) {
	// endpoint = Constants.AWS_ECS_ENDPOINT;
	// }
	// client.setEndpoint(endpoint);
	// return client;
	// }

	// public static AmazonEC2Client getEc2Client() {
	// AmazonEC2Client client = null;
	// if (EcsCloud.getAwsCredentials() != null) {
	// client = new AmazonEC2Client(EcsCloud.getAwsCredentials());
	// } else {
	// client = new AmazonEC2Client();
	// }
	//
	// String endpoint = System.getenv("AWS_EC2_ENDPOINT");
	// if (endpoint == null || "".equals(endpoint)) {
	// endpoint = Constants.AWS_EC2_ENDPOINT;
	// }
	// client.setEndpoint(endpoint);
	// return client;
	// }

        public static Container getContainer(EcsCloud cloud, String taskArn) {
	        DescribeTasksResult dtrr = AWSUtils.describeTasks(cloud,
								  taskArn);
		if (dtrr.getTasks().size() == 0
		    || dtrr.getTasks().get(0).getContainers().size() == 0) {
		    throw new RuntimeException("No container found for task ARN: "
					       + taskArn);
		}
		return dtrr.getTasks().get(0).getContainers().get(0);
	}

	public static boolean waitForContainer(EcsCloud cloud,
						 String taskArn,
						 int timeout,
						 String status) {
		Container ctn = null;
		int count = 0;
		do {
			ctn = AWSUtils.getContainer(cloud, taskArn);
			if (ctn.getLastStatus().equalsIgnoreCase(status)) {
				return true;
			}
			if (count % 5 == 0) {
			    logger.info("Wait for container's " + status);
			}
			try {
				Thread.sleep(Constants.WAIT_TIME_MS);
			} catch (InterruptedException e) {
				// No-op
			}
			timeout -= Constants.WAIT_TIME_MS;
			count++;
		} while (timeout > 0);
		return false;
	}

	public static int getContainerExternalSSHPort(EcsCloud cloud,
			String taskArn) {
		Container ctn = AWSUtils.getContainer(cloud, taskArn);
		List<NetworkBinding> nbs = ctn.getNetworkBindings();
		logger.info("Network binding size = " + nbs.size());
		int port = -1;
		for (NetworkBinding nb : nbs) {
			logger.info("Container's binding: host = " + nb.getBindIP()
					+ ", port = " + nb.getHostPort());
			if (nb.getContainerPort() == Constants.SSH_PORT) {
				port = nb.getHostPort();
				break;
			}
		}
		if (port == -1) {
			throw new RuntimeException(
					"Cannot find external mapped port for SSH");
		}
		return port;
	}

	public static DescribeTasksResult describeTasks(EcsCloud cloud,
			String... tasksArn) {
		DescribeTasksRequest dtr = new DescribeTasksRequest();
		dtr.setCluster(cloud.getCluster());
		List<String> taskArnList = new ArrayList<String>();
		for (int i = 0; i < tasksArn.length; i++) {
			taskArnList.add(tasksArn[i]);
		}
		dtr.setTasks(taskArnList);
		return cloud.getEcsClient().describeTasks(dtr);
	}

	public static DescribeContainerInstancesResult describeContainerInstances(
			EcsCloud cloud, String... containerInstancesArn) {
		DescribeContainerInstancesRequest dcir = new DescribeContainerInstancesRequest();
		dcir.setCluster(cloud.getCluster());
		List<String> containerInstanceArnList = new ArrayList<String>();
		for (int i = 0; i < containerInstancesArn.length; i++) {
			containerInstanceArnList.add(containerInstancesArn[i]);
		}
		dcir.setContainerInstances(containerInstanceArnList);
		return cloud.getEcsClient().describeContainerInstances(dcir);
	}

	public static DescribeInstancesResult describeInstances(
			EcsCloud cloud, String... ec2InstanceIds) {
		DescribeInstancesRequest dir = new DescribeInstancesRequest();
		List<String> ec2InstanceIdList = new ArrayList<String>();
		for (int i = 0; i < ec2InstanceIds.length; i++) {
			ec2InstanceIdList.add(ec2InstanceIds[i]);
		}
		dir.setInstanceIds(ec2InstanceIdList);
		return cloud.getEc2Client().describeInstances(dir);
	}

	public static DescribeInstancesResult describeInstancesOfTask(
			EcsCloud cloud, String taskArn) {
		DescribeTasksResult dtrr = AWSUtils.describeTasks(cloud, taskArn);
		if (dtrr.getTasks().size() == 0) {
			throw new RuntimeException("No task found for task ARN: " + taskArn);
		}
		String containerInstanceArn = dtrr.getTasks().get(0)
				.getContainerInstanceArn();

		DescribeContainerInstancesResult dcirr = AWSUtils
				.describeContainerInstances(cloud, containerInstanceArn);
		if (dcirr.getContainerInstances().size() == 0) {
			throw new RuntimeException(
					"No container instances found for task ARN: " + taskArn);
		}
		String ec2InstanceId = dcirr.getContainerInstances().get(0)
				.getEc2InstanceId();

		return AWSUtils.describeInstances(cloud, ec2InstanceId);
	}

	public static String getTaskContainerPrivateAddress(
			EcsCloud cloud, String taskArn) {
		DescribeInstancesResult dirr = AWSUtils.describeInstancesOfTask(
				cloud, taskArn);
		if (dirr.getReservations().size() == 0
				|| dirr.getReservations().get(0).getInstances().size() == 0) {
			throw new RuntimeException("No EC2 instance found for task ARN: "
					+ taskArn);
		}
		return dirr.getReservations().get(0).getInstances().get(0)
				.getPrivateIpAddress();
	}

	public static String getTaskContainerPublicAddress(
			EcsCloud cloud, String taskArn) {
		DescribeInstancesResult dirr = AWSUtils.describeInstancesOfTask(
				cloud, taskArn);
		if (dirr.getReservations().size() == 0
				|| dirr.getReservations().get(0).getInstances().size() == 0) {
			throw new RuntimeException("No EC2 instance found for task ARN: "
					+ taskArn);
		}
		return dirr.getReservations().get(0).getInstances().get(0)
				.getPublicIpAddress();
	}

	public static void cleanUpTasks(EcsCloud cloud, RunTaskResult rtr) {
		logger.info("*** Cleanup tasks");
		StopTaskRequest str = null;
		if (rtr.getTasks().size() != 0) {
			List<Task> tasks = rtr.getTasks();
			for (Task task : tasks) {
				str = new StopTaskRequest();
				str.setCluster(cloud.getCluster());
				str.setTask(task.getTaskArn());
				cloud.getEcsClient().stopTask(str);
			}
		}
	}

        public static void startContainerInstance(EcsCloud cloud) {
	        SetDesiredCapacityRequest request = new SetDesiredCapacityRequest()
		    .withAutoScalingGroupName(cloud.getAutoScalingGroupName())
		    .withDesiredCapacity(Constants.CLUSTER_INITIALIZATION_SIZE);
		
		AmazonAutoScalingClient client = cloud.getAutoScalingClient();
		client.setDesiredCapacity(request);
	}

        public static RunTaskResult startTask(EcsCloud cloud, String taskArn) {
	        RunTaskRequest request = new RunTaskRequest()
		    .withCluster(cloud.getCluster())
		    .withTaskDefinition(taskArn);
		AmazonECSClient client = cloud.getEcsClient();
		return client.runTask(request);
	}
    
        public static void stopTask(EcsCloud cloud, String taskArn, boolean sameVPC) {
  	        Container ctn = AWSUtils.getContainer(cloud, taskArn);
		logger.info("Found container for task: task = " + taskArn
			    + ", container name = " + ctn.getName()
			    + ", container status = " + ctn.getLastStatus());
		String host = "";
		if (sameVPC) {
			host = AWSUtils.getTaskContainerPrivateAddress(cloud, taskArn);
		} else {
			host = AWSUtils.getTaskContainerPublicAddress(cloud, taskArn);
		}
		logger.info("Docker host to delete container = " + host);

		int externalPort = AWSUtils.getContainerExternalSSHPort(cloud, taskArn);
		logger.info("Container's mapped external SSH port = " + externalPort);

		StopTaskRequest str = new StopTaskRequest();
		str.setCluster(cloud.getCluster());
		str.setTask(taskArn);
		cloud.getEcsClient().stopTask(str);

		if (!AWSUtils.waitForContainer(cloud, taskArn, Constants.CONTAINER_STOP_TIMEOUT, "STOPPED")) {
		    logger.warning("Container did not stop in 60 seconds, either the grace period is really long or SIGKILL did nothing.");
		}
	}
}
