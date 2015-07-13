package hudson.slaves;

import java.util.logging.Logger;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import hudson.Extension;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.NodeProvisioner.*;
import hudson.slaves.CloudProvisioningListener;
import hudson.slaves.Cloud;
import hudson.model.LoadStatistics.LoadStatisticsSnapshot;
import jenkins.model.Jenkins;

import com.phuna.amazonecs.EcsCloud;
import com.phuna.amazonecs.AWSUtils;

import com.amazonaws.services.ecs.model.DescribeTaskDefinitionResult;

import java.util.Collection;


@Extension
public class ECSProvisioningStrategy extends NodeProvisioner.Strategy {

    private static final Logger logger = Logger.getLogger(NodeProvisioner.class.getName());
    
    /** The strategy we use to launch a new container */
    @Nonnull
    @Override
    public StrategyDecision apply(@Nonnull StrategyState state) {
	if (state.getSnapshot().getQueueLength() == 0) {
	    return StrategyDecision.PROVISIONING_COMPLETED;
	}

	logger.info("Trying to find a cloud to provision for: "+state.getLabel().getDisplayName());
	EcsCloud cloud = null;
	for (Cloud c : Jenkins.getInstance().clouds) {
	    if (c.canProvision(state.getLabel()) && (c instanceof EcsCloud)) {
	    	logger.info("Found cloud: "+c.getDisplayName()+" for "+state.getLabel().getDisplayName());
			cloud = (EcsCloud) c;
			break;
	    }
	}

	DescribeTaskDefinitionResult result = AWSUtils.describeTaskDefinition(cloud, state.getLabel());
	
	int workloadToProvision = result.getTaskDefinition().getContainerDefinitions().size();

	/** we will want to create a cloudprovisioninglistener that checks to see if a new container can
	 *  be provisioned on any of the container instances in this cloud without hitting the memory or
	 *  CPU limits of the instance. */
	
	for (CloudProvisioningListener cl : CloudProvisioningListener.all()) {
	    if (cl.canProvision(cloud, state.getLabel(), workloadToProvision) != null) {
			return StrategyDecision.CONSULT_REMAINING_STRATEGIES;
	    }
	}
	Collection<PlannedNode> additionalCapacities =
	    cloud.provision(state.getLabel(), workloadToProvision);

	for (CloudProvisioningListener cl : CloudProvisioningListener.all()) {
	    cl.onStarted(cloud, state.getLabel(), additionalCapacities);
	}

	for (PlannedNode ac : additionalCapacities) {
	    logger.log(Level.INFO, "Started provisioning {0} from {1} with {2,number,integer} "
		       + "executors.",
		       new Object[]{ac.displayName, cloud.name, ac.numExecutors});
	}

	logger.info(String.format(
		"Additional Planned Capcity: %d\nPlanned Capacity: %dAvailable Executors: %d"
		+"\nBusy Executors: %d\nConnecting Executors: %d\nDefined Executors: %d\n Idle Executors: %d\n"
		+"Online Executors: %d\n Queue Length: %d", 
		state.getAdditionalPlannedCapacity(), state.getPlannedCapacityLatest(), state.getAvailableExecutorsLatest(), 
		state.getBusyExecutorsLatest(), state.getConnectingExecutorsLatest(), state.getDefinedExecutorsLatest(), 
		state.getIdleExecutorsLatest(), state.getOnlineExecutorsLatest(), state.getQueueLengthLatest()
		)
	);

	state.recordPendingLaunches(additionalCapacities);
	return StrategyDecision.PROVISIONING_COMPLETED;
    }

}
