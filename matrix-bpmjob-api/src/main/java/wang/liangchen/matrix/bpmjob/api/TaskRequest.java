package wang.liangchen.matrix.bpmjob.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Liangchen.Wang 2023-03-20 14:29
 */
public class TaskRequest {
    private final Long taskId;
    private List<StackTraceElementRequest> stackTrace;

    public TaskRequest(Long taskId) {
        this(taskId, null);
    }

    public TaskRequest(Long taskId, Throwable throwable) {
        this.taskId = taskId;
        if (null == throwable) {
            stackTrace = Collections.emptyList();
            return;
        }
        StackTraceElement[] throwableStackTrace = throwable.getStackTrace();
        stackTrace = new ArrayList<>(throwableStackTrace.length);
        for (StackTraceElement stackTraceElement : throwableStackTrace) {
            StackTraceElementRequest stackTraceElementRequest = new StackTraceElementRequest();
            stackTraceElementRequest.setDeclaringClass(stackTraceElement.getClassName());
            stackTraceElementRequest.setMethodName(stackTraceElement.getMethodName());
            stackTraceElementRequest.setFileName(stackTraceElement.getFileName());
            stackTraceElementRequest.setLineNumber(stackTraceElement.getLineNumber());
            stackTrace.add(stackTraceElementRequest);
        }
    }

}
