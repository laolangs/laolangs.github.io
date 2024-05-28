/*
 * @lc app=leetcode.cn id=746 lang=java
 *
 * [746] 使用最小花费爬楼梯
 */

// @lc code=start
class Solution {
    public int minCostClimbingStairs(int[] cost) {
        // 到达i阶需要的最小花费为dp[i]
        int[] dp = new int[10001];
        dp[0] = 0;
        dp[1] = 0;
        // 楼顶在cost.lenth位置
        for (int i = 2; i <= cost.length; i++) {
            dp[i] = Math.min(dp[i - 1] + cost[i - 1], dp[i - 2] + cost[i - 2]);
        }

        return dp[cost.length];

    }
}
// @lc code=end
