package org.example.queue.controller;

import org.example.queue.controller.dto.ScheduleResponse;
import org.example.queue.entity.Patient;
import org.example.queue.schedule.Doctor;
import org.example.queue.schedule.Scheduler;
import org.example.queue.service.PatientDataService;
import org.example.queue.service.TextFilePatientSequenceProvider;
import org.example.queue.vo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 多医生并发调度接口。
 * <p>
 * GET /schedule/dispatch?doctorCount=N
 * 返回 N 个医生各自接诊的患者名单，以及平均/最长等待时间。
 * 医生越多，等待越短——前端选不同的 N 就能直观看到差别。
 */
@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    private final TextFilePatientSequenceProvider provider;
    private final PatientDataService patientDataService;

    public ScheduleController(TextFilePatientSequenceProvider provider,
                              PatientDataService patientDataService) {
        this.provider = provider;
        this.patientDataService = patientDataService;
    }

    @GetMapping("/dispatch")
    public Result<ScheduleResponse> dispatch(
            @RequestParam(defaultValue = "4") int doctorCount) {
        if (doctorCount < 1) {
            doctorCount = 1;
        }

        List<Patient> raw = provider.loadRawPatients();
        Scheduler scheduler = new Scheduler(doctorCount);
        scheduler.dispatch(raw);

        ScheduleResponse resp = new ScheduleResponse();
        resp.setDoctorCount(doctorCount);
        resp.setPatientCount(raw.size());
        resp.setAvgWaitTime(Math.round(scheduler.averageWaitTime(raw) * 10) / 10.0);
        resp.setMaxWaitTime(maxWait(raw));

        List<ScheduleResponse.DoctorSchedule> doctorList = new ArrayList<>();
        for (Doctor d : scheduler.getDoctors()) {
            ScheduleResponse.DoctorSchedule ds = new ScheduleResponse.DoctorSchedule();
            ds.setDoctorId(d.getId());
            ds.setHandledCount(d.getHandled().size());
            ds.setPatients(PatientDataService.toVO(d.getHandled()));
            doctorList.add(ds);
        }
        resp.setDoctors(doctorList);
        return Result.success(resp);
    }

    /**
     * 应用医生数量：改变数据源调度用的医生数，并重建折半查找/哈希表的数据。
     * POST /schedule/apply?doctorCount=N
     */
    @PostMapping("/apply")
    public Result<String> apply(@RequestParam(defaultValue = "4") int doctorCount) {
        if (doctorCount < 1) {
            doctorCount = 1;
        }
        provider.setDoctorCount(doctorCount);
        patientDataService.reload();
        return Result.success("已应用 " + doctorCount + " 名医生");
    }

    private int maxWait(List<Patient> patients) {
        int max = 0;
        for (Patient p : patients) {
            max = Math.max(max, p.getWaitTime());
        }
        return max;
    }
}