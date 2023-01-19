package wang.liangchen.matrix.bpmjob.domain.trigger;

import jakarta.inject.Inject;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import wang.liangchen.matrix.bpmjob.domain.host.Host;
import wang.liangchen.matrix.bpmjob.domain.trigger.enumeration.TriggerState;
import wang.liangchen.matrix.bpmjob.domain.trigger.enumeration.WalState;
import wang.liangchen.matrix.framework.commons.exception.MatrixInfoException;
import wang.liangchen.matrix.framework.data.dao.StandaloneDao;
import wang.liangchen.matrix.framework.data.dao.criteria.Criteria;
import wang.liangchen.matrix.framework.data.dao.criteria.DeleteCriteria;
import wang.liangchen.matrix.framework.data.dao.criteria.EntityGetter;
import wang.liangchen.matrix.framework.data.dao.criteria.UpdateCriteria;
import wang.liangchen.matrix.framework.data.dao.entity.JsonField;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author Liangchen.Wang
 */
@Service
public class TriggerManager {
    private final StandaloneDao repository;

    @Inject
    public TriggerManager(StandaloneDao repository) {
        this.repository = repository;
    }

    public void createTrigger(Trigger trigger) {
        String triggerExpression = trigger.getTriggerExpression();
        // validate and resolve cron expression
        CronExpression cronExpression;
        try {
            cronExpression = CronExpression.parse(triggerExpression);
        } catch (Exception e) {
            throw new MatrixInfoException("The expression '{}' is invalid.", triggerExpression);
        }
        trigger.setState(TriggerState.NORMAL);
        trigger.initializeFields();
        this.repository.insert(trigger);
        createTriggerInstant(trigger.getTriggerId(), cronExpression);
    }

    public boolean enableTrigger(Long triggerId) {
        Trigger entity = Trigger.newInstance();
        entity.setState(TriggerState.NORMAL);

        UpdateCriteria<Trigger> updateCriteria = UpdateCriteria.of(entity)
                ._equals(Trigger::getTriggerId, triggerId)
                ._equals(Trigger::getState, TriggerState.SUSPENDED);
        int rows = this.repository.update(updateCriteria);
        if (1 == rows) {
            Trigger trigger = this.selectTrigger(triggerId, Trigger::getTriggerExpression);
            this.createTriggerInstant(triggerId, CronExpression.parse(trigger.getTriggerExpression()));
        }
        return true;
    }

    public boolean disableTrigger(Long triggerId) {
        Trigger entity = Trigger.newInstance();
        entity.setState(TriggerState.SUSPENDED);

        UpdateCriteria<Trigger> updateCriteria = UpdateCriteria.of(entity)
                ._equals(Trigger::getTriggerId, triggerId)
                ._equals(Trigger::getState, TriggerState.NORMAL);
        int rows = this.repository.update(updateCriteria);
        if (1 == rows) {
            this.deleteTriggerInstant(triggerId);
        }
        return true;
    }

    private int createTriggerInstant(Long triggerId, CronExpression cronExpression) {
        TriggerTime triggerTime = TriggerTime.newInstance();
        triggerTime.setTriggerId(triggerId);
        triggerTime.setTriggerInstant(cronExpression.next(LocalDateTime.now()));
        return this.repository.insert(triggerTime);
    }

    private int deleteTriggerInstant(Long triggerId) {
        DeleteCriteria<TriggerTime> deleteCriteria = DeleteCriteria.of(TriggerTime.class)._equals(TriggerTime::getTriggerId, triggerId);
        return this.repository.delete(deleteCriteria);
    }

    public Trigger selectTrigger(Long triggerId, EntityGetter<Trigger>... resultFields) {
        return repository.select(Criteria.of(Trigger.class)
                .resultFields(resultFields)
                ._equals(Trigger::getTriggerId, triggerId));
    }

    public List<TriggerTime> eligibleTriggerTimes(LocalDateTime duration, int batchSize) {
        Criteria<TriggerTime> criteria = Criteria.of(TriggerTime.class)
                ._lessThan(TriggerTime::getTriggerInstant, duration)
                .pageSize(batchSize).pageNumber(1);
        return this.repository.list(criteria);
    }

    public boolean renewTriggerInstant(Long triggerId, LocalDateTime triggerInstant, LocalDateTime nextTriggerInstant) {
        TriggerTime triggerTime = TriggerTime.newInstance();
        triggerTime.setTriggerInstant(nextTriggerInstant);

        UpdateCriteria<TriggerTime> updateCriteria = UpdateCriteria.of(triggerTime)
                ._equals(TriggerTime::getTriggerId, triggerId)
                ._equals(TriggerTime::getTriggerInstant, triggerInstant);
        int rows = repository.update(updateCriteria);
        return 1 == rows;
    }

    public List<Long> eligibleWalIds(LocalDateTime duration, int batchSize) {
        Criteria<Wal> criteria = Criteria.of(Wal.class)
                .resultFields(Wal::getWalId)
                ._lessThan(Wal::getTriggerDatetime, duration)
                .pageSize(batchSize).pageNumber(1);
        List<Wal> wals = this.repository.list(criteria);
        return wals.stream().map(Wal::getWalId).collect(Collectors.toList());
    }

    public Wal selectWal(Long walId, EntityGetter<Wal>... resultFields) {
        return repository.select(Criteria.of(Wal.class)
                .resultFields(resultFields)
                ._equals(Wal::getWalId, walId));
    }

    public Wal createWal(Trigger trigger, LocalDateTime triggerInstant, JsonField taskParams, Host host) {
        Wal wal = Wal.newInstance();
        wal.setTriggerId(trigger.getTriggerId());
        wal.setHostId(host.getHostId());
        wal.setHostLabel(host.getHostLabel());
        wal.setWalGroup(trigger.getTriggerGroup());
        wal.setTriggerParams(trigger.getTriggerParams());
        wal.setShardingNumber(trigger.getShardingNumber());

        wal.setTaskParams(taskParams);
        wal.setTriggerDatetime(triggerInstant);

        LocalDateTime now = LocalDateTime.now();
        wal.setCreateDatetime(now);
        wal.setScheduleDatetime(now);
        wal.setState(WalState.ACQUIRED.getState());
        this.repository.insert(wal);
        return wal;
    }

    public int deleteWal(Long walId) {
        return this.repository.delete(DeleteCriteria.of(Wal.class)._equals(Wal::getWalId, walId));
    }
}