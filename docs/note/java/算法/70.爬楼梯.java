/*
 * @lc app=leetcode.cn id=70 lang=java
 *
 * [70] 爬楼梯
 */

// @lc code=start
class Solution {
    public int climbStairs(int n) {
        // dp数组含义，到达n阶楼梯有dp[i]种方法
        int dp[] = new int[n + 2];
        dp[1] = 1;
        dp[2] = 2;
        if (n <= 2) {
            return dp[n];
        }
        // dp[i]依赖前两个dp[i-1]和dp[i-2]，所以便利顺序为正序
        for (int i = 3; i <= n; i++) {
            // 递推公式
            dp[i] = dp[i - 1] + dp[i - 2];
        }
        return dp[n];
    }
}
// @lc code=end
