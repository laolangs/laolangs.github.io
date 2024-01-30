/*
 * @lc app=leetcode.cn id=160 lang=java
 *
 * [160] 相交链表
 */

// @lc code=start
/**
 * Definition for singly-linked list.
 * public class ListNode {
 * int val;
 * ListNode next;
 * ListNode(int x) {
 * val = x;
 * next = null;
 * }
 * }
 * 
 * 
 * 给你两个单链表的头节点 headA 和 headB ，请你找出并返回两个单链表相交的起始节点。
 * 如果两个链表不存在相交节点，返回 null 。
 * 
 * 方案一：把两个链表逻辑拼接后变成等长，循环判断相交节点
 * 方案二：比较两个链表长度，长的链表先走几步，然后同时走，相遇则相交
 * 
 * 测试用例中1不是相交节点，因为1的节点是new ListNode创建的，不是同一个对象
 * 
 *  测试用例构造两个相交链表
        ListNode commonNode = new ListNode(8);
        commonNode.next = new ListNode(4);
        commonNode.next.next = new ListNode(5);

        ListNode headA = new ListNode(4);
        headA.next = new ListNode(1);
        headA.next.next = commonNode;

        ListNode headB = new ListNode(5);
        headB.next = new ListNode(0);
        headB.next.next = new ListNode(1);
        headB.next.next.next = commonNode;
 * 
 */
public class Solution {
    // 方案一

    public ListNode getIntersectionNode(ListNode headA, ListNode headB) {
        if (headA == null || headB == null) {
            return null;
        }
        // p1 指向 A 链表头结点，p2 指向 B 链表头结点
        // 链表拉平合并成一个之后遍历
        ListNode p1 = headA;
        ListNode p2 = headB;
        while (p1 != p2) {
            // p1 走一步，如果走到 A 链表末尾，转到 B 链表
            p1 = p1 == null ? headB : p1.next;
            // p2 走一步，如果走到 B 链表末尾，转到 A 链表
            p2 = p2 == null ? headA : p2.next;
        }
        return p2;
    }

    // 方案二 比较两个链表长度，长的链表先走几步，然后同时走，相遇则相交
    public ListNode getIntersectionNodeV2(ListNode headA, ListNode headB) {
        ListNode p1 = headA, p2 = headB;
        int lenA = 0, lenB = 0;
        while (p1 != null) {
            p1 = p1.next;
            lenA++;
        }
        while (p2 != null) {
            p2 = p2.next;
            lenB++;
        }
        p1 = headA;
        p2 = headB;
        while (lenA > lenB) {
            p1 = p1.next;
            lenA--;
        }
        while (lenB > lenA) {
            p2 = p2.next;
            lenB--;
        }
        while (p1 != p2) {
            p1 = p1.next;
            p2 = p2.next;
        }
        return p1;
    }
}
// @lc code=end
