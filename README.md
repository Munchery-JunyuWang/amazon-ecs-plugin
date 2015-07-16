amazon-ecs-plugin
=================

Jenkins Cloud Plugin for Amazon ECS (EC2 Container Service)

Use Amazon ECS plugin to dynamically provision a slave on Amazon ECS, run a single build on it, then tear down that slave.

Configuration
------------------
### Global
- On your configuration page, scroll down and click 'Add a New Cloud', and then click 'Amazon ECS'
- Give the cloud a name, and input your Amazon Key Id and Secret Access Key, and the region in which your cluster is running.
  - Right now, each cloud only supports clusters in one region, but could easily be extended to support clusters in multiple regions.
- Input the cluster name, which defaults to 'default'
- Specify an AWS Auto Scaling Group, if you use one on your cluster. This is used right now to start up an instance in the cluster if none are running when you try to start a job.
- As in the Auto Scaling Group, if you would like to set a timeout on the first instance to launch in your cluster, set one here. The default is really really large.
- Tap the Test Connection button to make sure you are authenticating and connecting to the right region. Don't be too alarmed if the number of connected instances seems high, since it looks at instances on every cluster in the region, including some that might be duplicates of each other.



### Task Definitions
- Add Task Definitions on your AWS account, which are detailed at http://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_defintions.html
- Copy the **full** ARN into the task definition ARN field
- Labels are how you tell your jobs where to run. Under each job that should be run on ECS, specify the label so that it will be run on a container built using this task definition
- Specify a workspace directory on the slave if you wish (the default is /home/jenkins)
- add a Container Start Timeout (in seconds), which defines how long the plugin waits for your image to be started before giving up. The default never gives up.
- Add the number of containers you want spawned for this task defintion. One is the default, and the only value that has currently been tested.


## Gotchas

This is a very raw plugin, with lots of room for improvement and refinement. There are a lot of moving parts. The most important ones to worry about are setting up docker images, configuring your Amazon cluster with task defintions, launch configurations, and auto scaling groups. If you get those figured out, this service should actually work pretty well, and has a lot of room for growth. Below are a few things that may not be apparent at first, but should save you some headaches.
- The containers you launch need to have an SSH server running in order for Jenkins to connect to them, so make sure you use a docker image that does run an ssh server.
- Many commands you normally run on the command line require that you are in a login shell. Unfortunately, it doesn't seem like SSHing in from Jenkins opens a login shell, so you may have to prepend your commands with something like "/bin/bash -l -c" in order for them to work correctly.
- As docker evolves, so will Amazon's Container Service, so this plugin is not something to be installed and forgotten about, since most likely it will require significant changes in the coming months.
