import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

/*
 * @lc app=leetcode.cn id=1 lang=java
 *
 * [1] 两数之和
 */

// @lc code=start
class Solution {
    // O(n^2)
    public int[] twoSum(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    return new int []{i,j};
                }
            }
        }
        return null;
    }

    // 排序之后双边指针计算O(n) (数组中有同样的数据测试过不了eg:[3,3/n6])
    public int[] twoSumV1(int[] nums, int target) {
        Map<Integer, Integer> valueMap = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            valueMap.put(nums[i], i);
        }
        Arrays.sort(nums);
        int[] values = {};
        int left = 0, right = nums.length - 1;
        while (left < right) {
            int sum = nums[left] + nums[right];
            if (sum == target) {
                values = new int[] { nums[left], nums[right] };
                break;
            } else if (sum < target) {
                left++;
            } else {
                right--;
            }
        }
        if (values.length > 0) {
            int[] res = new int[2];
            for (int i = 0; i < values.length; i++) {
                res[i] = valueMap.get(values[i]);
            }
            return res;
        }
        return null;
    }

    // hash辅助 O(n)
    public int[] twoSumV3(int[] nums, int target) {
        Map<Integer, Integer> valueMap = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int a = target - nums[i];
            if (valueMap.containsKey(a)) {
                return new int[] { valueMap.get(a), i };
            } else {
                valueMap.put(nums[i], i);
            }
        }
        return null;
    }

    public static void main(String[] args) {
        int[] a = { 3,3 };
        System.out.println(Arrays.toString(new Solution().twoSum(a, 6)));
        // System.out.println(Arrays.toString(new Solution().twoSumV2(a, 4)));
        // System.out.println(Arrays.toString(new Solution().twoSumV3(a, 9)));

    }
}

// @lc code=end
