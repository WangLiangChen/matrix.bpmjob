package wang.liangchen.matrix.bpmjob.console.controller;

import jakarta.inject.Inject;
import org.springframework.web.bind.annotation.*;
import wang.liangchen.matrix.bpmjob.service.TriggerCreateService;
import wang.liangchen.matrix.bpmjob.service.TriggerRequest;

/**
 * @author Liangchen.Wang 2023-01-18 17:57
 */
@RestController
@RequestMapping("/trigger")
public class TriggerController {
    private final TriggerCreateService service;

    @Inject
    public TriggerController(TriggerCreateService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public void create(@RequestBody TriggerRequest triggerRequest) {
        this.service.createTrigger(triggerRequest);
    }

    @GetMapping("/disable")
    public void disable(@RequestParam Long triggerId) {
        this.service.disableTrigger(triggerId);
    }

    @GetMapping("/enable")
    public void enable(@RequestParam Long triggerId) {
        this.service.enableTrigger(triggerId);
    }
}
