/*
 * @lc app=leetcode.cn id=11 lang=java
 *
 * 给定一个长度为 n 的整数数组 height 。有 n 条垂线，第 i 条线的两个端点是 (i, 0) 和 (i, height[i]) 。
 * 找出其中的两条线，使得它们与 x 轴共同构成的容器可以容纳最多的水。
 * 返回容器可以储存的最大水量。
 * 
 * [11] 盛最多水的容器
 * 
 * 如果移动较低的那一边，那条边可能会变高，使得矩形的高度变大，进而就「有可能」使得矩形的面积变大；
 * 相反，如果移动较高的那一边，矩形的高度是无论如何都不会变大的，所以不可能使矩形的面积变得更大。
 */

// @lc code=start
class Solution {
    public int maxArea(int[] height) {
        int i = 0, j = height.length - 1, s = 0;
        while (i < j) {
            // [left, right] 之间的矩形面积
            s = Math.max(s, Math.min(height[j], height[i]) * (j - i));
            // 双指针技巧，移动较低的一边
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
