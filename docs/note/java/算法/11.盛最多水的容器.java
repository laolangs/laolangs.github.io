/*
 * @lc app=leetcode.cn id=11 lang=java
 *
 * [11] 盛最多水的容器
 */

// @lc code=start
class Solution {
    public int maxArea(int[] height) {
        int i = 0, j = height.length - 1, s = 0;
        while (i < j) {
            s = Math.max(s, Math.min(height[j], height[i]) * (j - i));
            if (height[j] < height[i]) {
                j--;
            } else {
                i++;
            }
        }

        return s;

    }
}
// @lc code=end
