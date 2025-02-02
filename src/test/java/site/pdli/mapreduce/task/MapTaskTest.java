package site.pdli.mapreduce.task;

import org.junit.*;
import site.pdli.mapreduce.Config;
import site.pdli.mapreduce.example.WordCountMapper;
import site.pdli.mapreduce.messaging.Worker;
import site.pdli.mapreduce.utils.FileUtil;
import site.pdli.mapreduce.worker.WorkerContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MapTaskTest {
    @Before
    public void setUp() throws IOException {
        Config.getInstance()
            .setMapperClass(WordCountMapper.class);
        Config.getInstance()
            .setNumReducers(2);
        Config.getInstance()
            .setOutputDir(new File("out"));

        var content1 = "haha this is content1\nanother line";
        var content2 = "haha this is content2\nanother line\nyet another line";

        FileUtil.writeLocal("input1", content1.getBytes());
        FileUtil.writeLocal("input2", content2.getBytes());
    }

    @After
    public void tearDown() throws IOException {
        FileUtil.del("input1");
        FileUtil.del("input2");
        FileUtil.del("tmp");
    }

    @Ignore
    @Test
    public void testMapTask() {
        try (var task = Task.createTask(
            new TaskInfo("task1", Worker.TaskType.MAP,
                List.of("localhost:10001://input1", "localhost:10002://input2")),
            new WorkerContext("localhost", 10000))) {
            task.execute();
            task.join();

            afterExecute(task.getTaskInfo());
        }
    }

    public void afterExecute(TaskInfo taskInfo) {
        var files = taskInfo.getOutputFiles();

        String part0;
        String part1;

        try {
            part0 = FileUtil.readLocal(FileUtil.getFileName(files.get(0)));
            part1 = FileUtil.readLocal(FileUtil.getFileName(files.get(1)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals("""
            "haha"|"1"
            "this"|"1"
            "is"|"1"
            "content1"|"1"
            "line"|"1"
            "haha"|"1"
            "this"|"1"
            "is"|"1"
            "line"|"1"
            "yet"|"1"
            "line"|"1"
            """, part0);

        Assert.assertEquals("""
            "another"|"1"
            "content2"|"1"
            "another"|"1"
            "another"|"1"
            """, part1);

    }
}
