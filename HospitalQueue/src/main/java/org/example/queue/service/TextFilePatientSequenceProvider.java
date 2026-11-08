package org.example.queue.service;

import org.example.queue.entity.Patient;
import org.example.queue.schedule.Scheduler;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认数据来源：读取 classpath 下 patients.txt，并按"优先级高者先就诊、同级按到达顺序"
 * 仿真出每位患者的实际等待时间，最后按等待时间升序输出（等价于第二阶段堆排序的结果）。
 *
 * patients.txt 行格式：P001 到达 08:00 时长 15min 优先级 3
 */
@Component
public class TextFilePatientSequenceProvider implements PatientSequenceProvider {

    private static final Pattern LINE = Pattern.compile(
            "P(\\d+)\\s*到达\\s*(\\d{2}:\\d{2})\\s*时长\\s*(\\d+)min\\s*优先级\\s*(\\d+)");

    /** 医生数量，默认 4；可调用 setDoctorCount 调整 */
    private int doctorCount = 4;

    public void setDoctorCount(int doctorCount) {
        this.doctorCount = Math.max(1, doctorCount);
    }

    @Override
    public List<Patient> sortedByWaitTime() {
        List<Patient> patients = load();
        // 多医生并发调度计算等待时间（替代原来的单医生仿真）
        new Scheduler(doctorCount).dispatch(patients);
        patients.sort(Comparator.comparingInt(Patient::getWaitTime));
        return patients;
    }

    /**
     * 读取 patients.txt，返回"未计算等待时间"的原始患者列表。
     * 供多医生调度器（schedule.Scheduler）直接使用。
     */
    public List<Patient> loadRawPatients() {
        return load();
    }

    /** 读取 patients.txt */
    private List<Patient> load() {
        List<Patient> patients = new ArrayList<>();
        try {
            ClassPathResource res = new ClassPathResource("patients.txt");
            if (!res.exists()) {
                System.out.println("[PatientSequenceProvider] 未找到 patients.txt，返回空序列");
                return patients;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher m = LINE.matcher(line.trim());
                    if (!m.find()) continue;
                    int id = Integer.parseInt(m.group(1));
                    LocalTime arrive = LocalTime.parse(m.group(2));
                    int treatTime = Integer.parseInt(m.group(3));
                    int priority = Integer.parseInt(m.group(4));
                    patients.add(new Patient(id, "患者" + String.format("%03d", id), arrive,
                            treatTime, priority, 0));
                }
            }
        } catch (Exception e) {
            System.out.println("[PatientSequenceProvider] patients.txt 读取失败: " + e.getMessage());
        }
        return patients;
    }
}
