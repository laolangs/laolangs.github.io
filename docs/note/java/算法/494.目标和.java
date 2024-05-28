/*
 * @lc app=leetcode.cn id=494 lang=java
 *
 * [494] 目标和
 */

// @lc code=start
class Solution {
    public int findTargetSumWays(int[] nums, int target) {
        int sum = 0;
        for (int num : nums) {
            sum += num;
        }
        // 如果数组元素的和小于目标值或者目标值和数组元素的和不是偶数，直接返回0
        if (sum < target || (sum + target) % 2 != 0) {
            return 0;
        }
        // target为负数导致计算的列数为负
        int capacity = (sum + Math.abs(target))/2;;
        int n = nums.length;
        // dp[i][j]表示前i个元素中，选取若干个元素使得它们的和等于j的方法数
        int[][] dp = new int[n + 1][capacity + 1];
        dp[0][0] = 1;
        for (int i = 1; i <= n; i++) {
            for (int j = 0; j <= capacity; j++) {
                if (j >= nums[i - 1]) {
                    // 不放i:dp[i-1][j] 放i:dp[i-1][j-nums[i-1]]
                    // dp[i-1][j-nums[i]] 表示在前 i-1 个元素中，选取若干个元素使得它们的和等于 j-nums[i] 的方法数。
                    // 如果我们在前 i-1 个元素中选取若干个元素使得它们的和等于 j-nums[i]，然后再加上第 i 个元素 nums[i]，就可以得到和为 j 的集合。
                    dp[i][j] = dp[i - 1][j] + dp[i - 1][j - nums[i - 1]];

                } else {
                    dp[i][j] = dp[i - 1][j];
                }
            }

        }
        return dp[n][capacity];
    }
}
// @lc code=end
