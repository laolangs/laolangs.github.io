import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * @lc app=leetcode.cn id=15 lang=java
 *
 * [15] 三数之和
 */

// @lc code=start
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        if (nums.length < 3) {
            return res;
        }
        Arrays.sort(nums);
        for (int a = 0; a < nums.length - 2; a++) {
            int i = a + 1;
            int j = nums.length - 1;
            if (a > 0 && nums[a] == nums[a - 1]) {
                continue;
            }
            while (i < j) {
                int sum = nums[a] + nums[i] + nums[j];
                if (sum > 0) {
                    while (i < j && nums[j] == nums[j - 1]) {
                        j--;
                    }
                    j--;
                } else if (sum < 0) {
                    while (i < j && nums[i] == nums[i + 1]) {
                        i++;
                    }
                    i++;
                } else {
                    res.add(Arrays.asList(nums[a], nums[i], nums[j]));
                    while (i < j && nums[i] == nums[i + 1]) {
                        i++;
                    }
                    while (i < j && nums[j] == nums[j - 1]) {
                        j--;
                    }
                    i++;
                    j--;
                }
            }
        }
        return res;
    }
}
// @lc code=end
