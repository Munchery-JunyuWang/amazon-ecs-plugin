package com.phuna.amazonecs;

import java.lang.Integer;

public class Constants {
	public static final int WAIT_TIME_MS = 1000;
        public static final int CONTAINER_START_TIMEOUT = Integer.MAX_VALUE;
        public static final int CONTAINER_STOP_TIMEOUT = 60000;
	public static final int SSH_PORT = 22;
        public static final int CLUSTER_INITIALIZATION_SIZE = 1;
        public static final int CONTAINER_INSTANCE_LAUNCH_TIMEOUT = Integer.MAX_VALUE;
}
