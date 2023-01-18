package wang.liangchen.matrix.bpmjob.domain.task;

import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import wang.liangchen.matrix.bpmjob.domain.host.Host;
import wang.liangchen.matrix.bpmjob.domain.trigger.Wal;
import wang.liangchen.matrix.framework.data.dao.StandaloneDao;
import wang.liangchen.matrix.framework.data.dao.criteria.Criteria;
import wang.liangchen.matrix.framework.data.dao.criteria.UpdateCriteria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Liangchen.Wang
 */
@Service
public class TaskManager {
    private final StandaloneDao repository;

    @Inject
    public TaskManager(StandaloneDao repository) {
        this.repository = repository;
    }

    public int createTask(Wal wal, Host host) {
        Byte shardingNumber = wal.getShardingNumber();
        shardingNumber = shardingNumber == 0 ? 1 : shardingNumber;
        List<Task> tasks = new ArrayList<>();
        for (byte i = 0; i < shardingNumber; i++) {
            Task task = Task.newInstance();
            task.setParentId(0L);
            task.setWalId(wal.getWalId());
            task.setHostId(host.getHostId());
            task.setTriggerId(wal.getTriggerId());
            task.setExpectedHost(wal.getHostLabel());
            task.setActualHost(host.getHostLabel());
            task.setTaskGroup(wal.getWalGroup());
            task.setTriggerParams(wal.getTriggerParams());
            task.setTaskParams(wal.getTaskParams());

            task.setShardingNumber(i);

            LocalDateTime now = LocalDateTime.now();
            task.setCreateDatetime(now);
            task.setAssignDatetime(now);
            task.setAckDatetime(now);
            task.setCompleteDatetime(now);
            task.setProgress((short) 0);
            task.setState(TaskState.UNASSIGNED.getState());
            tasks.add(task);
        }
        return repository.insert(tasks);
    }

    public int delete(Long taskId) {
        Task entity = Task.newInstance();
        entity.setTaskId(taskId);
        return repository.delete(entity);
    }

    public int update(Task entity) {
        return repository.update(entity);
    }

    public Task byKey(Long taskId, String... resultColumns) {
        return repository.select(Criteria.of(Task.class)
                .resultColumns(resultColumns)
                ._equals(Task::getTaskId, taskId)
        );
    }

    public int stateTransfer(Long taskId, Byte to, Byte... from) {
        Task entity = Task.newInstance();
        entity.setState(to);
        UpdateCriteria<Task> updateCriteria = UpdateCriteria.of(entity)
                ._equals(Task::getTaskId, taskId)
                ._in(Task::getState, from);
        return repository.update(updateCriteria);
    }

    public List<Task> byStates(Short... states) {
        return repository.list(Criteria.of(Task.class)._in(Task::getState, states));
    }
}