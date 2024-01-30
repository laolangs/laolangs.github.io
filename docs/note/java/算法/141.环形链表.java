/*
 * @lc app=leetcode.cn id=141 lang=java
 *
 * [141] 环形链表
 */

// @lc code=start
/**
 * Definition for singly-linked list.
 * class ListNode {
 * int val;
 * ListNode next;
 * ListNode(int x) {
 * val = x;
 * next = null;
 * }
 * }
 * 想象学校操场，跑得快的跟跑的慢的一定会在某一刻相遇
 * 思路 快慢指针，快指针每次走两步，慢指针每次走一步，如果快指针走到了尾部，说明没有环，否则有环
 */
public class Solution {
    public boolean hasCycle(ListNode head) {
        ListNode fast = head, slow = head;
        while (fast != null && fast.next != null) {
            // 快指针走两步，慢指针走一步
            fast = fast.next.next;
            slow = slow.next;
            // 快慢指针相遇，说明有环
            if (fast == slow) {
                return true;
            }
        }
        // 不包含环
        return false;

    }
}
// @lc code=end
