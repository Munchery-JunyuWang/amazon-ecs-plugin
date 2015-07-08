package com.phuna.amazonecs;

import hudson.Extension;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.NodeProvisioner.*;

@Extension
public class ECSProvisioningStrategy extends NodeProvisioner.Strategy {
    
    /** The strategy we use to launch a new container */
    @Override
    public StrategyDecision apply(StrategyState state) {
	

    }

}
