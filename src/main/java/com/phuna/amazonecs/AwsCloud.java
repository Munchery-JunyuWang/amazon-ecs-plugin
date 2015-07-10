package com.phuna.amazonecs;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;

public interface AwsCloud {
	AmazonECSClient getEcsClient();
	AmazonEC2Client getEc2Client();
        AmazonAutoScalingClient getAutoScalingClient();
}
