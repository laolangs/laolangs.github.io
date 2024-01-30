/*
 * @lc app=leetcode.cn id=142 lang=java
 *
 * [142] 环形链表 II
 */

// @lc code=start
/**
 * Definition for singly-linked list.
 * class ListNode {
 *     int val;
 *     ListNode next;
 *     ListNode(int x) {
 *         val = x;
 *         next = null;
 *     }
 * }
 * 给定一个链表的头节点  head ，返回链表开始入环的第一个节点。 如果链表无环，则返回 null。
 * 如果链表中有某个节点，可以通过连续跟踪 next 指针再次到达，则链表中存在环。
 * 为了表示给定链表中的环，评测系统内部使用整数 pos 来表示链表尾连接到链表
 * 中的位置（索引从 0 开始）。如果 pos 是 -1，则在该链表中没有环。
 * 注意：pos 不作为参数进行传递，仅仅是为了标识链表的实际情况。不允许修改 链表。
 * 
 * 解题思路：
 *  假如快慢指针相遇，满指针走k步，快指针走了2k步，k=N*环链长度
 *  假如相遇节点距环节点距离为m步，从头节点出发 走k-m步走到入口，相遇点再走k-m步也刚好走到入口点
 *  头节点-------------------入口点--------相遇点-----------
 *   |                        |              |
 *   0                        m              k
 *   ----------(k-m)-----------
 *                            -------------(k-m)---------
 *                            环节点从相遇点k开始走，走了k-m步到入口点
 * 
 */
public class Solution {
    public ListNode detectCycle(ListNode head) {
        ListNode fast = head,slow = head;
        while(fast!= null && fast.next != null){
            fast = fast.next.next;
            slow = slow.next;
            if(fast == slow){
                break;
            }
        }
        // 无环直接返回null
        if(fast == null || fast.next == null){
            return null;
        }
        // 重新指向头结点
        slow = head;
        // 快慢指针同步前进，相交点就是环起点
        while (slow != fast) {
            fast = fast.next;
            slow = slow.next;
        }
        return slow;
    }
/*      public static class ListNode {
             int val;
             ListNode next;
             ListNode(int x) {
                 val = x;
                 next = null;
             }
         }
         public static void main(String[] args) {
             ListNode head = new ListNode(3);
             head.next = new ListNode(2);
             head.next.next = new ListNode(0);
             head.next.next.next = new ListNode(-4);
             head.next.next.next.next = head.next;
             Solution solution = new Solution();
             System.out.println(solution.detectCycle(head).val);
         } */
}
// @lc code=end

