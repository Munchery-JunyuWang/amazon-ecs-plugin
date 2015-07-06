package com.phuna.amazonecs;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class CommonUtils {
	private static final Logger logger = Logger.getLogger(CommonUtils.class
			.getName());

	public static boolean isPortAvailable(String host, int port) {
		Socket socket = null;
		boolean available = false;
		try {
			socket = new Socket(host, port);
			available = true;
		} catch (IOException e) {
			logger.info("Catch IOException 1st");
			logger.info(e.getMessage());
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					logger.info("Catch IOException 2nd");
					logger.info(e.getMessage());
				}
			}
		}
		logger.info("isPortAvailable: " + available);
		return available;
	}

        public static boolean waitForPort(String host, int port, int containerStartTimeout) {
        	logger.info("Entering waitForPort");
     	    while (containerStartTimeout > 0) {
     	    	logger.info("Container Timeout: "+Integer.toString(containerStartTimeout));
				if (isPortAvailable(host, port)) {
					logger.info("waitForPort: Returning true.");
					return true;
				}

				try {
					Thread.sleep(Constants.WAIT_TIME_MS);
				} catch (InterruptedException e) {
					logger.info("Thread interrupt.");
				}
				containerStartTimeout -= Constants.WAIT_TIME_MS;
			}
			logger.info("waitForPort: Returning false");
			return false;
	}

	public static Map<String, List<Integer>> parsePorts(String waitPorts)
			throws IllegalArgumentException, NumberFormatException {
		Map<String, List<Integer>> containers = new HashMap<String, List<Integer>>();
		String[] containerPorts = waitPorts.split(System
				.getProperty("line.separator"));
		for (String container : containerPorts) {
			String[] idPorts = container.split(" ", 2);
			if (idPorts.length < 2)
				throw new IllegalArgumentException("Cannot parse " + idPorts
						+ " as '[conainerId] [port1],[port2],...'");
			String containerId = idPorts[0].trim();
			String portsStr = idPorts[1].trim();

			List<Integer> ports = new ArrayList<Integer>();
			for (String port : portsStr.split(",")) {
				ports.add(new Integer(port));
			}
			containers.put(containerId, ports);
		}
		return containers;
	}

}
