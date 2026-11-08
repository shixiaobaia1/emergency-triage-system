package org.example.queue.structure;

//相关类
import org.example.queue.entity.Patient;
import org.example.queue.structure.HashTable.Key;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class MainTest {
    // 数据文件路径
    private static final String txtPath = "patients.txt";
    // 全部病人
    private static List<Patient> patientList = new ArrayList<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // 第一步：一次性读取全部病人数据
        loadPatientsOnce();
        if(patientList.isEmpty()){
            System.out.println("没有读取到病人数据，程序退出");
            scanner.close();
            return;
        }
        Patient[] patientArr = patientList.toArray(new Patient[0]);
        System.out.println("已加载病人总数：" + patientArr.length);

        HashTable hashTable = null;

        while(true){
            System.out.println("\n========== 系统主菜单 ==========");
            System.out.println("1 — 设置哈希表参数（表长m、哈希关键字）并重建哈希表");
            System.out.println("2 — 执行交互查询（哈希+二分对比）");
            System.out.println("0 — 退出程序");
            System.out.print("请选择：");

            if(!scanner.hasNextInt()){
                scanner.next();
                System.out.println("请输入数字！");
                continue;
            }
            int op = scanner.nextInt();
            if(op == 0){
                System.out.println("程序结束");
                break;
            }else if(op == 1){
                // 【运行时输入哈希表长度、选择关键字，动态构建哈希表】
                hashTable = buildHashTableByInput(scanner);
            }else if(op == 2){
                if(hashTable == null){
                    System.out.println("⚠️请先选择【1】设置哈希表参数，构建哈希表！");
                    continue;
                }
                // 进入查询交互
                runQueryMenu(scanner,hashTable,patientArr);
            }else{
                System.out.println("无效选项");
            }
        }
        scanner.close();
    }

    /**
     * 控制台输入m，选择关键字，动态创建哈希表，并把全部数据插入
     */
    private static HashTable buildHashTableByInput(Scanner scanner){
        System.out.println("\n------ 设置哈希表参数 ------");
        int m;
        while(true){
            System.out.print("请输入哈希表长度m(m≥2)：");
            if(!scanner.hasNextInt()){
                scanner.next();
                System.out.println("必须输入整数！");
                continue;
            }
            m = scanner.nextInt();
            if(m >= 2) break;
            System.out.println("m必须大于等于2，请重新输入！");
        }

        Key hashKey;
        while(true){
            System.out.println("选择哈希关键字：1=就诊ID   2=治疗时长treatTime");
            System.out.print("输入选择：");
            if(!scanner.hasNextInt()){
                scanner.next();
                System.out.println("输入数字1或2");
                continue;
            }
            int sel = scanner.nextInt();
            if(sel == 1){
                hashKey = Key.ID;
                break;
            }else if(sel == 2){
                hashKey = Key.TreatTime;
                break;
            }else{
                System.out.println("只能选1或2");
            }
        }

        // 新建哈希表，批量插入全部病人
        HashTable ht = new HashTable(m,hashKey);
        int insertCnt = 0;
        for(Patient p : patientList){
            try {
                ht.insert(p);
                insertCnt++;
            }catch (IllegalStateException e){
                System.out.println("⚠️插入异常：" + e.getMessage());
                break;
            }
        }
        System.out.println("哈希表构建完成，成功插入"+insertCnt+"条");
        ht.printSummary();
        return ht;
    }

    /**
     * 查询子菜单：同时哈希+二分
     */
    private static void runQueryMenu(Scanner scanner,HashTable hashTable,Patient[] patientArr){
        Key nowHashKey = hashTable.getKey();
        System.out.println("\n==== 查询模式，当前哈希关键字："+nowHashKey.getLabel()+" ====");
        System.out.println("输入 -1 返回上一级菜单");

        // 根据当前哈希关键字，把二分数组排序
        java.util.function.ToIntFunction<Patient> binKeyFunc;
        if(nowHashKey == Key.ID){
            binKeyFunc = BinarySearch.KeyID;
            Arrays.sort(patientArr, Comparator.comparingInt(BinarySearch.KeyID));
        }else{
            binKeyFunc = BinarySearch.KeyTreatTime;
            Arrays.sort(patientArr, Comparator.comparingInt(BinarySearch.KeyTreatTime));
        }
        BinarySearch.upsort(patientArr,binKeyFunc, nowHashKey.getLabel());
        BinarySearch.printDecisionTree(patientArr);
        double theoryASL = BinarySearch.ASLsuc(patientArr);
        double realASL = BinarySearch.ASL(patientArr, binKeyFunc);
        System.out.printf("二分查找｜理论ASL成功=%.2f，实测ASL成功=%.2f%n",theoryASL,realASL);

        while(true){
            System.out.print("\n请输入要查找的关键字值(按-1返回上一层菜单)：");
            if(!scanner.hasNextInt()){
                scanner.next();
                System.out.println("输入数字！");
                continue;
            }
            int keyVal = scanner.nextInt();
            if(keyVal == -1){
                System.out.println("返回上层菜单");
                break;
            }

            SearchResult<Patient> hashRes = hashTable.search(keyVal);
            SearchResult<Patient> binRes;
            if(nowHashKey == Key.ID){
                binRes = BinarySearch.searchByID(patientArr,keyVal);
            }else{
                binRes = BinarySearch.searchByTreatTime(patientArr,keyVal);
            }

            System.out.println("----------------------------------------");
            System.out.println("查询关键字 = " + keyVal);
            if(hashRes.isFound()){
                System.out.printf("【哈希查找】成功｜比较%d次｜槽位%d｜%s%n",
                        hashRes.getCount(),hashRes.getIndex(),hashRes.getData());
            }else{
                System.out.printf("【哈希查找】失败｜比较%d次%n",hashRes.getCount());
            }
            if(binRes.isFound()){
                System.out.printf("【二分查找】成功｜比较%d次｜数组下标%d｜%s%n",
                        binRes.getCount(),binRes.getIndex(),binRes.getData());
            }else{
                System.out.printf("【二分查找】失败｜比较%d次%n",binRes.getCount());
            }
            System.out.println("----------------------------------------");
        }
    }

    /**
     * 只执行一次读取txt，把所有病人存入全局list
     */
    private static void loadPatientsOnce() {
        patientList.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(txtPath))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if(line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length != 7) {
                    System.out.printf("【跳过第%d行格式错误】%s%n", lineNo, line);
                    continue;
                }
                try {
                    String idStr = parts[0].substring(1);
                    int id = Integer.parseInt(idStr);
                    LocalTime arrive = LocalTime.parse(parts[2]);
                    String treatStr = parts[4].replace("min", "");
                    int treatTime = Integer.parseInt(treatStr);
                    int priority = Integer.parseInt(parts[6]);
                    Patient p = new Patient(id, parts[0], arrive, treatTime, priority, 0);
                    patientList.add(p);
                } catch (Exception e) {
                    System.out.printf("【解析异常，第%d行】%s%n", lineNo, e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件异常："+e.getMessage());
        }
    }
}