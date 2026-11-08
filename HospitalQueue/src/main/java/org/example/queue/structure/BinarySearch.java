package org.example.queue.structure;

import org.example.queue.entity.Patient;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToIntFunction;

public class BinarySearch {

    //取出病人id号，可用于查询
    public static final ToIntFunction<Patient> KeyID=Patient::getId;
    //取出病人治疗时长，可用于查询（队友原实现，MainTest 命令行使用）
    public static final ToIntFunction<Patient> KeyTreatTime=Patient::getTreatTime;
    //取出病人等待时间，可用于查询（网页"等待时间"关键字使用）
    public static final ToIntFunction<Patient> KeyWaitTime=Patient::getWaitTime;

    public BinarySearch(){}

    public static SearchResult<Patient> searchByID(Patient[] arr,int id){
        return search(arr,id,KeyID);
    }

    public static SearchResult<Patient> searchByTreatTime(Patient[] arr,int TreatTime){
        return search(arr,TreatTime,KeyTreatTime);
    }




    /**
     * 折半查找，数组按id升序排列
     * @param arr 有序Patient数组
     * @param keyof 找出待查关键字位置，即可使用同一段代码完成id和等待时间的查找
     * @para  key  要查找的id/时间
     * @return 返回比较次数，找不到返回-1
     */

    public static SearchResult<Patient> search(Patient[] arr, int key,ToIntFunction<Patient>keyof){
        int left=0;
        int right=arr.length-1;
        int count=0;
        while (left<=right){
            int mid=(left+right)/2;
            int midkey=keyof.applyAsInt(arr[mid]);
            count++;
            if(midkey==key)
                return new SearchResult<>(mid,count,arr[mid]);
            else if(midkey<key)
                left=mid+1;
            else
                right=mid-1;
        }
        return SearchResult.miss(count);
    }

    /**
     * 检验查找前是否为升序序列
     * @param arr
     * @return
     */
    public static void upsort(Patient[] arr,ToIntFunction<Patient> keyof,String keyname){
        for(int i=1;i<arr.length;i++){
            int pre=keyof.applyAsInt(arr[i-1]);
            int cur=keyof.applyAsInt(arr[i]);
            if(pre>cur){
                System.out.println("未按升序排列");
            }
        }
    }

    /** 校验序列是否按关键字升序（排序键与查找键不一致时抛异常，防止静默算错） */
    public static void validateSorted(Patient[] arr, ToIntFunction<Patient> keyof, String keyname) {
        for (int i = 1; i < arr.length; i++) {
            if (keyof.applyAsInt(arr[i - 1]) > keyof.applyAsInt(arr[i])) {
                throw new IllegalStateException("序列未按【" + keyname + "】升序排列，排序键与查找键不一致");
            }
        }
    }

    /*
    平均查找长度ASL成功&&失败
     */
    public static double ASLsuc(Patient[] arr){
        Stats s=stats(arr.length);
        return (double)s.sucSum/arr.length;
    }

    public static double ASLfail(Patient[] arr){
        Stats s=stats(arr.length);
        return (double)s.failSum/s.externalCount;
    }

    /*
    暴力ASL，可以与ASLsuc交叉验证，证明树推导值==实测值
     */

    public static double ASL(Patient[] arr,ToIntFunction<Patient> keyof){
        int totcnt=0;
        int n=arr.length;
        for(Patient p : arr){
                totcnt+=search(arr,keyof.applyAsInt(p),keyof).getCount();
        }
        return totcnt*1.0/n;
    }

    /** 打印判定树层分布表与 ASL 推导过程 */
    public static void printDecisionTree(Patient[] arr) {
        Stats s = stats(arr.length);

        System.out.println("---- 折半查找判定树 (n=" + arr.length + ", 树高=" + s.maxLevel + ") ----");
        System.out.printf("%-8s%-12s%-16s%n", "层数", "结点个数", "层数×个数");
        for (Map.Entry<Integer, Integer> e : s.levelCount.entrySet()) {
            System.out.printf("%-10d%-14d%-16d%n",
                    e.getKey(), e.getValue(), e.getKey() * e.getValue());
        }
        System.out.println("内部结点(查找成功位置) = " + arr.length);
        System.out.println("外部结点(查找失败位置) = " + s.externalCount + "  (= n+1)");
        System.out.printf("ASL(成功) = %d/%d = %.2f 次%n",
                s.sucSum, arr.length, (double) s.sucSum / arr.length);
        System.out.printf("ASL(失败) = %d/%d = %.2f 次%n",
                s.failSum, s.externalCount, (double) s.failSum / s.externalCount);
    }

    /** 判定树统计结果（对外输出用，字段与 Controller 对齐） */
    public static final class DecisionTreeStats {
        public final double aslSuc;      // 查找成功平均查找长度
        public final double aslFail;     // 查找失败平均查找长度
        public final int treeHeight;     // 判定树高度
        public final Map<Integer, Integer> levelCounts;  // 各层结点数

        public DecisionTreeStats(double aslSuc, double aslFail, int treeHeight,
                                 Map<Integer, Integer> levelCounts) {
            this.aslSuc = aslSuc;
            this.aslFail = aslFail;
            this.treeHeight = treeHeight;
            this.levelCounts = levelCounts;
        }
    }

    private static final class Stats{
        long sucSum;
        long failSum;
        long externalCount;
        int maxLevel;
        final Map<Integer,Integer> levelCount=new TreeMap<>();
    }

    private static Stats stats(int n){
        Stats s=new Stats();
        walk(0,n-1,1,s);
        return s;
    }

    /** 计算 n 个互异关键字的判定树统计，供 /binary/stats 等接口返回 */
    public static DecisionTreeStats decisionTreeStats(int n) {
        Stats s = stats(n);
        return new DecisionTreeStats(
                (double) s.sucSum / n,
                (double) s.failSum / s.externalCount,
                s.maxLevel,
                s.levelCount);
    }

    private static void walk(int low, int high, int level, Stats s) {
        if (low > high) {
            s.failSum += level - 1;
            s.externalCount++;
            return;
        }
        int mid = (low + high) >>> 1;
        s.sucSum += level;
        s.maxLevel = Math.max(s.maxLevel, level);
        s.levelCount.merge(level, 1, Integer::sum);
        walk(low, mid - 1, level + 1, s);
        walk(mid + 1, high, level + 1, s);
    }
}
