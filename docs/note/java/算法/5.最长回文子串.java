/*
 * @lc app=leetcode.cn id=5 lang=java
 *
 * [5] 最长回文子串
 */

// @lc code=start
class Solution {
    public String longestPalindrome(String s) {
        String result = "";
        for (int i = 0; i < s.length(); i++) {
            // 判断以s[i]为中心的回文
            String s1 = palindrome(s, i, i);
            // 判断以s[i]和s[i+1]为中心的回文
            String s2 = palindrome(s, i, i + 1);
            if (s1.length() > result.length()) {
                result = s1;
            }
            if (s2.length() > result.length()) {
                result = s2;
            }
        }
        return result;
    }

    public String palindrome(String s, int l, int r) {
        while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) {
            l--;
            r++;
        }
        return s.substring(l + 1, r);
    }
}
// @lc code=end
