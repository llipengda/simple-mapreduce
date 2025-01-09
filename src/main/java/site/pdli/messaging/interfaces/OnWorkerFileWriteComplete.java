package site.pdli.messaging.interfaces;

import java.util.List;

@FunctionalInterface
public interface OnWorkerFileWriteComplete {
    void call(String workerId, List<String> outputFiles);
}
