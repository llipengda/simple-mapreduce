package site.pdli.example;

import site.pdli.Config;
import site.pdli.runner.LocalRunner;
import site.pdli.runner.Runner;

import java.io.File;

public class WordCount {
    public static void main(String[] args) {
        var config = Config.getInstance();

        config.setMapperClass(WordCountMapper.class);
        config.setReducerClass(WordCountReducer.class);

        config.addWorker("localhost");
        config.addWorker("localhost");
        config.addWorker("localhost");
        config.addWorker("localhost");
        config.addWorker("localhost");
        config.setUsingLocalFileSystemForLocalhost(false);

        config.setNumReducers(2);
        config.setSplitChunkSize(100 * 1000);

        config.setInputFile(new File(args[0]));
        config.setOutputDir(new File(args[1]));

        Runner runner = new LocalRunner();

        runner.run();
        runner.waitForCompletion();
    }
}
