package org.example.queue.structure;
import java.time.LocalTime;
import java.util.function.ToIntFunction;
import org.example.queue.entity.Patient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 哈希函数：H(key) = key % p，p 为不大于表长 m 的最大质数；
 *除留取余法&线性探测法处理冲突
 * 冲突处理：Hi = (H(key) + i) % m，i = 1, 2, 3, ...
 */

public class HashTable {

    /** 选择用ID还是treattime查找*/
    public enum Key {
        ID("就诊编号", Patient::getId),
        TreatTime("治疗时长", Patient::getTreatTime),
        WaitTime("等待时间", Patient::getTotalWaitTime);

        //显示名字
        private final String label;
        //提取器，方法调用Patient::get___，输入patient,就会返回对应关键字
        private final ToIntFunction<Patient> extractor;

        //全参构造
        Key(String label, ToIntFunction<Patient> extractor) {
            this.label=label;
            this.extractor=extractor;
        }

        public String getLabel() {
            return label;
        }

        //对外调用，拿到病人的对应关键字值
        public int of(Patient p) {
            return extractor.applyAsInt(p);
        }
    }

    //成员变量
    private final int m;                    // 表长
    private final int p;                    // 除数：不大于 m 的最大质数
    private final Key key;                  // 关键字(ID or TreatTime)
    private final Patient[] table;          // 底层数组，存放Patient，null 表示空槽
    private final int[] probeCompares;      // 每个槽位存入时的比较次数
    private int size;                       // 已存元素个数
    private int collisionCount;             // 发生过冲突的元素个数

    //构造方法——初始化
    public HashTable(int m) {
        this(m, Key.ID);
    }

    public HashTable(int m, Key key) {

        //表长必须大于2，不然就抛出异常
        if (m < 2) {
            throw new IllegalArgumentException("表长必须 ≥ 2");
        }
        this.m = m;
        //调用maxPrimeNotGreaterThan找到最大质数
        this.p = maxPrimeNotGreaterThan(m);
        this.key = key;
        this.table = new Patient[m];
        this.probeCompares = new int[m];
    }

    //访问器
    public int getM() {
        return m;
    }
    public int getP() {
        return p;
    }
    public Key getKey() {
        return key;
    }
    public int size() {
        return size;
    }
    public int getCollisionCount() {
        return collisionCount;
    }

    /*
    * 装填因子 α = n / m
    * n是size，就是已经装入的元素，m是总共有的槽位， α的值越大，越容易发生冲突
    */
    public double loadFactor() {
        return (double) size / m;
    }

    /** 最长探测长度 */
    public int maxProbeLength() {
        int max = 0;
        for (int c : probeCompares) {
            max = Math.max(max, c);
        }
        return max;
    }

    /** 除留余数法 H(k)=k mod p */
    private int hash(int k) {
        return k % p;
    }


    /**
     * 插入一个患者。
     *
     * @return 实际存入的槽位下标；若该关键字已存在，返回原槽位
     */
    public int insert(Patient patient) {
        //拿到当前病人的关键字id or treattime
        int k=key.of(patient);
        //计算原始位置
        int home=hash(k);
        //线性探测
        for (int d=0; d<m; d++) {
            int slot=(home+d)%m;
            if (table[slot]==null) {
                table[slot]=patient;
                probeCompares[slot]=d+1;     // 比较次数 = 冲突次数 + 1
                size++;
                //发生过冲突
                if (d>0) {
                    collisionCount++;
                }
                return slot;
            }
            if (key.of(table[slot])==k) {
                return slot;                      // 同一关键字不重复插入
            }
        }
        throw new IllegalStateException(
                "哈希表已满，装填因子不能达到 1.00（当前 n=" + size + ", m=" + m + "）");
    }

    /**
     * 按关键字查找。探到空槽即失败，空槽本身也算 1 次比较。
     */
    public SearchResult<Patient> search(int k) {
        int home = hash(k);
        for (int d = 0; d < m; d++) {
            int slot = (home + d) % m;
            Patient patient = table[slot];
            if (patient == null) {
                return SearchResult.miss(d + 1);
            }
            if (key.of(patient) == k) {
                return new SearchResult<>(slot, d + 1, patient);
            }
        }
        return SearchResult.miss(m);
    }

    /** 查找成功 ASL*/
    public double aslSuc() {
        long total = 0;
        for (int i = 0; i < m; i++) {
            if (table[i] != null) {
                total += probeCompares[i];
            }
        }
        return (double) total / size;
    }

    /**
     * 查找失败 ASL
     * 除留余数法只会产生 p 种哈希地址（0 .. p-1）
     */
    public double aslFail() {
        long total = 0;
        for (int h = 0; h < p; h++) {
            int compares = 0;
            for (int d = 0; d < m; d++) {
                compares++;
                if (table[(h + d) % m] == null) {
                    break;
                }
            }
            total += compares;
        }
        return (double) total / p;
    }

    //打印哈希表：槽位 关键字 比较次数
    public void printStorage() {
        System.out.println("---- 哈希表存储结构（关键字=" + key.getLabel()
                + ", m=" + m + ", p=" + p + ", n=" + size
                + ", α=" + String.format("%.2f", loadFactor())
                + ", 冲突元素=" + collisionCount
                + ", 最长探测=" + maxProbeLength() + "次） ----");
        int columns = 4;
        for (int i = 0; i < m; i++) {
            if (table[i] == null) {
                System.out.printf("[%3d]  --              ", i);
            } else {
                System.out.printf("[%3d] %4d (%2d次)     ", i, key.of(table[i]), probeCompares[i]);
            }
            if ((i + 1) % columns == 0 || i == m - 1) {
                System.out.println();
            }
        }
    }

    //打印统计内容
    //上下都是测试类才需要的，做了页面就可以不用调用测试类
    public void printSummary() {
        System.out.printf("哈希表(关键字=%s, m=%d, p=%d, n=%d, α=%.2f): "
                        + "ASL成功=%.2f 次, ASL失败=%.2f 次, 冲突元素=%d, 最长探测=%d 次%n",
                key.getLabel(), m, p, size, loadFactor(),
                aslSuc(), aslFail(), collisionCount, maxProbeLength());
    }

    /** 单个槽位快照（供 Controller 返回完整存储结构） */
    public static final class SlotSnapshot {
        public final int slot;
        public final Integer key;
        public final Integer probe;
        public final Patient patient;
        SlotSnapshot(int slot, Integer key, Integer probe, Patient patient) {
            this.slot = slot;
            this.key = key;
            this.probe = probe;
            this.patient = patient;
        }
    }

    /** 输出完整存储结构：每个槽位的下标、关键字、探测次数、患者 */
    public java.util.List<SlotSnapshot> getSlotEntries() {
        java.util.List<SlotSnapshot> list = new java.util.ArrayList<>();
        for (int i = 0; i < m; i++) {
            if (table[i] == null) {
                list.add(new SlotSnapshot(i, null, null, null));
            } else {
                list.add(new SlotSnapshot(i, key.of(table[i]), probeCompares[i], table[i]));
            }
        }
        return list;
    }

    //求最大质数
    public static int maxPrimeNotGreaterThan(int n) {
        for (int x = n; x >= 2; x--) {
            if (isPrime(x)) {
                return x;
            }
        }
        return 2;
    }

    private static boolean isPrime(int x) {
        if (x < 2) {
            return false;
        }
        for (int i = 2; (long) i * i <= x; i++) {
            if (x % i == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从txt批量加载病人
     * @param filePath txt文件路径
     */
    public void loadFromTxt(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty()) continue;

                // 按空白分割
                String[] parts = line.split("\\s+");
                // 固定格式，每行7个单元
                if (parts.length !=7){
                    System.out.printf("【跳过第%d行，格式错误】%s%n", lineNo, line);
                    continue;
                }
                try {
                    // parts[0] = P001 去掉P，取数字
                    String idStr = parts[0].substring(1);
                    int id = Integer.parseInt(idStr);

                    // parts[2] = 08:00 到达时间
                    LocalTime arriveTime = LocalTime.parse(parts[2]);

                    // parts[4] =15min 去掉min
                    String treatStr = parts[4].replace("min","");
                    int treatTime = Integer.parseInt(treatStr);

                    // parts[6] =优先级数字
                    int priority = Integer.parseInt(parts[6]);

                    // name用P001，waitTime=0
                    Patient patient = new Patient(id, parts[0], arriveTime, treatTime, priority);
                    int slot = insert(patient);
                    System.out.printf("插入 %s → 槽位[%d]%n",patient,slot);

                }catch (Exception e){
                    System.out.printf("【解析失败 第%d行】%s，错误信息：%s%n",lineNo,line,e.getMessage());
                }
            }
            System.out.println("\n==== 文件读取完毕 ====");
        } catch (IOException e) {
            System.err.println("读取文件IO异常：" + e.getMessage());
        }
    }
}