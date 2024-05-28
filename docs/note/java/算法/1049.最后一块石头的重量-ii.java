/*
 * @lc app=leetcode.cn id=1049 lang=java
 *
 * [1049] 最后一块石头的重量 II
 */

// @lc code=start
class Solution {
    public int lastStoneWeightII(int[] stones) {
        int sum = 0;
        for (int i = 0; i < stones.length; i++) {
            sum += stones[i];
        }
        int capacity = sum / 2;
        // 装满容量为 capacity 的最大重量为dp[capacity]
        int[] dp = new int[capacity + 1];
        for (int i = 0; i < stones.length; i++) {
            for (int j = capacity; j >= stones[i]; j--) {
                dp[j] = Math.max(dp[j], dp[j - stones[i]] + stones[i]);
            }
        }
        int last = sum - dp[capacity];
        return Math.abs(last - dp[capacity]);
    }
}
// @lc code=end
