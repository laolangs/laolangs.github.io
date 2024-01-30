/*
 * @lc app=leetcode.cn id=206 lang=java
 *
 * [206] 反转链表
 */

// @lc code=start
/**
 * Definition for singly-linked list.
 * public class ListNode {
 *     int val;
 *     ListNode next;
 *     ListNode() {}
 *     ListNode(int val) { this.val = val; }
 *     ListNode(int val, ListNode next) { this.val = val; this.next = next; }
 * }
 * 
 * 思考方式 通过两个节点来控制，调整链表的指向，双指针后移
 * 1➡️2➡️3➡️NULL
 * 
 * pre   current
 * null  1>2>3>NULL
 * 
 * 循环执行结果
 * pre              current
 * null<1           2>3>NULL
 * null<1<2         3>NULL
 * null<1<2<3       NULL
 * 
 * 
 */
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode current = head;
        ListNode pre = null;
        while (current != null) {
            ListNode temp = current.next;
            // 反转指针指向
            current.next = pre;
            // 指针移动
            pre = current;
            current = temp;
        }
        return pre;
    }
}
// @lc code=end

