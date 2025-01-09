package site.pdli.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import site.pdli.Config;
import site.pdli.utils.SSHUtil;
import site.pdli.worker.Master;
import site.pdli.worker.Worker;

@SuppressWarnings("FieldMayBeFinal")
public class RemoteRunner implements Runner {
    // master only
    private Master master;

    // workers only
    private Worker worker;
    private String workerId;
    private Integer workerPort = null;
    private String masterHost;
    private Integer masterPort = null;

    // both
    private Config config = Config.getInstance();
    private boolean isMaster;
    protected static Logger log = LoggerFactory.getLogger(RemoteRunner.class);

    public RemoteRunner(String[] args) {
        if (args.length == 0) {
            isMaster = true;
        } else {
            isMaster = false;
            workerId = args[0];
            workerPort = Integer.parseInt(args[1]);
            masterHost = args[2];
            masterPort = Integer.parseInt(args[3]);
        }
    }

    // master only
    protected void startMaster() {
        assert worker == null;
        assert workerId == null;
        assert workerPort == null;
        assert masterHost == null;
        assert masterPort == null;


        try {
            master = new Master("master", config.getMasterPort());

            for (var worker : config.getWorkers()) {
                master.addWorker(worker.getHost(), worker.getPort());
            }

            master.start();
            master.splitInput();

            startWorkers();
            Thread.sleep(2000);

            master.assignMapTask();
        } catch (Exception e) {
            log.error("Error in master", e);
            System.exit(1);
        }
    }

    // master only
    protected void startWorkers() {
        assert worker == null;
        assert workerId == null;
        assert workerPort == null;
        assert masterHost == null;
        assert masterPort == null;

        var mappers = master.getMappers();
        var reducers = master.getReducers();

        for (var entry : mappers.entrySet()) {
            var id = entry.getKey();
            var ctx = entry.getValue();

            SSHUtil.copyToRemote(ctx.getHost(), config.getJarPath(), config.getJarPath());
            SSHUtil.execCommandOnRemote(ctx.getHost(), "java -cp " + config.getJarPath() + " " + config.getMainClass()
                .getName() + " " + id + " " + ctx.getPort() + " " + master.getHost() + " " + master.getPort());
        }

        for (var entry : reducers.entrySet()) {
            var id = entry.getKey();
            var ctx = entry.getValue();

            SSHUtil.copyToRemote(ctx.getHost(), config.getJarPath(), config.getJarPath());
            SSHUtil.execCommandOnRemote(ctx.getHost(), "java -cp " + config.getJarPath() + " " + config.getMainClass()
                .getName() + " " + id + " " + ctx.getPort() + " " + master.getHost() + " " + master.getPort());
        }
    }


    // workers only
    private void startWorker() {
        assert master == null;

        try {
            worker = new Worker(workerId, workerPort, masterHost, masterPort);
            worker.start();
            Thread.sleep(2000);
        } catch (Exception e) {
            log.error("Error in worker", e);
            System.exit(1);
        }
    }

    @Override
    public void run() {
        if (isMaster) {
            startMaster();
        } else {
            startWorker();
        }
    }

    @Override
    public void waitForCompletion() {
        if (isMaster) {
            master.block();
            master.close();
        } else {
            worker.block();
            worker.close();
        }
    }
}
