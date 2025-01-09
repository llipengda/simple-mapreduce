package site.pdli.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import site.pdli.Config;
import site.pdli.Reducer;
import site.pdli.common.ContextImpl;
import site.pdli.common.Tuple;
import site.pdli.common.partitioner.HashcodePartitioner;
import site.pdli.messaging.Worker.TaskType;
import site.pdli.utils.FileUtil;
import site.pdli.worker.WorkerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReduceTask extends Task {
    private final Reducer<Object, Object, Object, Object> reducer;

    private static final Logger log = LoggerFactory.getLogger(ReduceTask.class);

    @SuppressWarnings("FieldMayBeFinal")
    private Map<Object, List<Object>> map = new HashMap<>();

    @SuppressWarnings("FieldMayBeFinal")
    private WorkerContext ctx;

    @SuppressWarnings("unchecked")
    public ReduceTask(TaskInfo taskInfo, WorkerContext ctx) {
        super(taskInfo, ctx);

        this.ctx = ctx;

        if (ctx.getContext() != null) {
            map = (HashMap<Object, List<Object>>) ctx.getContext();
        }

        Class<? extends Reducer<?, ?, ?, ?>> reducerClass = Config.getInstance()
            .getReducerClass();
        try {
            reducer = (Reducer<Object, Object, Object, Object>) reducerClass.getDeclaredConstructor()
                .newInstance();
        } catch (Exception e) {
            log.error("Error creating reducer", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doExecute() {
        log.info("Executing ReduceTask");

        if (taskInfo.getTaskType() == TaskType.REDUCE_READ) {
            processRead();
            ctx.setContext(map);
        } else if (taskInfo.getTaskType() == TaskType.REDUCE) {
            processReduce();
        }
    }

    private void processRead() {
        var inputFiles = taskInfo.getInputFiles();

        for (var file : inputFiles) {
            FileUtil.readRemote(file)
                .lines()
                .map(String::getBytes)
                .map(Tuple::fromBytes)
                .forEach(tuple -> {
                    var key = tuple.key();
                    var value = tuple.value();

                    map.putIfAbsent(key, new ArrayList<>());
                    map.get(key)
                        .add(value);
                });
        }
    }

    private void processReduce() {
        var outputDir = Config.getInstance()
            .getOutputDir()
            .getPath();
        var context = new ContextImpl<>(new HashcodePartitioner(), outputDir);

        for (var entry : map.entrySet()) {
            reducer.reduce(entry.getKey(), entry.getValue(), context);
        }

        context.close();
    }
}
