package com.phuna.amazonecs;

import javax.annotation.Nonnull;
import hudson.Extension;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.NodeProvisioner.*;
import hudson.slaves.CloudProvisioningListener;
import hudson.model.LoadStatistics.LoadStatisticsSnapshot;


@Extension
public class ECSProvisioningStrategy extends NodeProvisioner.Strategy {
    
    /** The strategy we use to launch a new container */
    @Nonnull
    @Override
    public StrategyDecision apply(@Nonnull StrategyState state) {
	if (state.getSnapshot().getQueueLength() == 0) {
	    return StrategyDecision.PROVISIONING_COMPLETED;
	}
	
	Cloud cloud = null;
	for (Cloud c : Jenkins.getInstance().clouds) {
	    if (c.canProvision(state.getLabel())) {
		cloud = c;
		break;
	    }
	}
	workloadToProvision = 1;

	/** we will want to create a cloudprovisioninglistener that checks to see if a new container can
	 *  be provisioned on any of the container instances in this cloud without hitting the memory or
	 *  CPU limits of the instance. */
	
	for (CloudProvisioningListener cl : CloudProvisioningListener.all()) {
	    if (cl.canProvision(c, state.getLabel(), workloadToProvision) != null) {
		return StrategyDecision.CONSULT_REMAINING_STRATEGIES;
	    }
	}
	Collection<PlannedNode> additionalCapacities =
	    c.provision(state.getLabel(), workloadToProvision);

	for (CloudProvisioningListener cl : CloudProvisioningListener.all()) {
	    cl.onStarted(c, state.getLabel(), additionalCapacities);
	}

	for (PlannedNode ac : additionalCapacities) {
	    LOGGER.log(Level.INFO, "Started provisioning {0} from {1} with {2,number,integer} "
		       + "executors.",
		       new Object[]{ac.displayName, c.name, ac.numExecutors});
	}

	state.recordPendingLaunches(additionalCapacities);
	return StrategyDecison.CONSULT_REMAINING_STRATEGIES;
    }

}
