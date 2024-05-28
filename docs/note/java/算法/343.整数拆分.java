/*
 * @lc app=leetcode.cn id=343 lang=java
 *
 * [343] 整数拆分
 */

// @lc code=start
class Solution {
    public int integerBreak(int n) {
        // i拆分后乘积最大值为dp[i]
        int[] dp = new int[n + 1];
        if (n < 2) {
            return 0;
        }
        dp[2] = 1;
        for (int i = 3; i <= n; i++) {
            // 存储单次拆分后最大值
            int max = 0;
            for (int j = 1; j < i; j++) {
                // 拆分成2个数
                int temp = j * (i - j);
                // 拆分成大于2个数
                int temp1 = j * dp[i - j];
                // 判断本次拆分结果和上次拆分结果最大值
                max = Math.max(max, Math.max(temp, temp1));

                // // 比较本次拆分最大值max(j*(i-j),j*dp[i-j])和上次最大值max
                // max = Math.max(max, Math.max(j * (i - j), j * dp[i - j]));
            }
            dp[i] = max;
        }
        return dp[n];
    }
}
// @lc code=end
