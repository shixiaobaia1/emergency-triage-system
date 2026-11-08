package org.example.queue.schedule;

import org.example.queue.entity.Patient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 调度测试：读 patients.txt，分别用 1 / 2 / 4 / 8 个医生跑一遍，
 * 对比平均等待时间，直观看到"医生越多，等待越短"。
 * <p>
 * 在 IDEA 里直接运行本类的 main 方法即可。
 */
public class ScheduleTest {

    private static final Pattern LINE = Pattern.compile(
            "(\\w+)\\s+到达\\s+(\\d{1,2}:\\d{2})\\s+时长\\s+(\\d+)min\\s+优先级\\s+(\\d+)");

    public static void main(String[] args) {
        List<Patient> original = loadPatients();
        System.out.println("共读取患者 " + original.size() + " 人");
        System.out.println();

        int[] doctorCounts = {1, 2, 4, 8};
        for (int n : doctorCounts) {
            // 每次调度前重置等待时间，保证独立
            List<Patient> copy = copyAndReset(original);
            Scheduler scheduler = new Scheduler(n);
            scheduler.dispatch(copy);

            System.out.printf("医生数 %d：平均等待 %.1f 分钟，最大等待 %d 分钟%n",
                    n, scheduler.averageWaitTime(copy), maxWait(copy));
            printAssignment(scheduler, 5);   // 每个医生只打印前 5 位患者
            System.out.println();
        }
    }

    /** 打印每个医生接诊的患者（只看前几个，避免刷屏） */
    private static void printAssignment(Scheduler scheduler, int showMax) {
        for (Doctor d : scheduler.getDoctors()) {
            StringBuilder sb = new StringBuilder("  医生" + d.getId() + " 接诊: ");
            List<Patient> handled = d.getHandled();
            int show = Math.min(showMax, handled.size());
            for (int i = 0; i < show; i++) {
                Patient p = handled.get(i);
                sb.append(p.getName())
                  .append("(等").append(p.getWaitTime()).append("分) ");
            }
            if (handled.size() > showMax) {
                sb.append("...共").append(handled.size()).append("人");
            }
            System.out.println(sb);
        }
    }

    private static int maxWait(List<Patient> patients) {
        int max = 0;
        for (Patient p : patients) {
            max = Math.max(max, p.getWaitTime());
        }
        return max;
    }

    /** 复制一份，并把等待时间清零（避免上次调度残留） */
    private static List<Patient> copyAndReset(List<Patient> src) {
        List<Patient> out = new ArrayList<>(src.size());
        for (Patient p : src) {
            out.add(new Patient(p.getId(), p.getName(), p.getArriveTime(),
                    p.getTreatTime(), p.getPriority(), 0));
        }
        return out;
    }

    /** 读 patients.txt：先尝试 classpath，再尝试相对工程路径 */
    private static List<Patient> loadPatients() {
        List<String> lines = readLines();
        List<Patient> list = new ArrayList<>();
        for (String line : lines) {
            Matcher m = LINE.matcher(line.trim());
            if (m.matches()) {
                String name = m.group(1);
                LocalTime arrive = LocalTime.parse(m.group(2));
                int treat = Integer.parseInt(m.group(3));
                int priority = Integer.parseInt(m.group(4));
                int id = Integer.parseInt(name.substring(1));   // P001 -> 1
                list.add(new Patient(id, name, arrive, treat, priority, 0));
            }
        }
        return list;
    }

    private static List<String> readLines() {
        try {
            InputStream is = ScheduleTest.class.getResourceAsStream("/patients.txt");
            if (is != null) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return br.lines().toList();
                }
            }
            return Files.readAllLines(Paths.get("src/main/resources/patients.txt"),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("读取 patients.txt 失败：" + e.getMessage(), e);
        }
    }
}