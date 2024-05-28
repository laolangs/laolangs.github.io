/*
 * @lc app=leetcode.cn id=474 lang=java
 *
 * [474] 一和零
 */

// @lc code=start
class Solution {
    public int findMaxForm(String[] strs, int m, int n) {
        int l = strs.length;
        // dp[i][j][k] 表示在前i个字符串中，使用j个0和k个1的情况下最多可以得到的字符串数量
        int[][][] dp = new int[l + 1][m + 1][n + 1];
        for (int i = 1; i < l + 1; i++) {
            int zero = 0;
            int one = 0;
            for (int j = 0; j < strs[i - 1].length(); j++) {
                if (strs[i - 1].charAt(j) == '0') {
                    zero++;
                } else {
                    one++;
                }
            }
            for (int j = 0; j < m + 1; j++) {
                for (int k = 0; k < n + 1; k++) {
                    dp[i][j][k] = dp[i - 1][j][k];
                    if (j >= zero && k >= one) {
                        dp[i][j][k] = Math.max(dp[i][j][k], dp[i - 1][j - zero][k - one] + 1);
                    }
                }
            }
        }
        return dp[l][m][n];

    }
}
// @lc code=end

