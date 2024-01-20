import java.util.*;

/*
 * @lc app=leetcode.cn id=21 lang=java
 *
 * [21] 合并两个有序链表
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
 */
class Solution {
    //  public static class ListNode {
    //         int val;
    //          ListNode next;
    //          ListNode() {}
    //          ListNode(int val) { this.val = val; }
    //          ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    //      }
    public ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        // 虚拟头节点
        ListNode dummy = new ListNode(-1), p = dummy;
        while(l1 != null && l2 != null){
            // 比较l1和l2的值，较小的一个接到p的后面
            if(l1.val > l2.val){
                p.next = l2;
                l2 = l2.next;
            }else{
                p.next = l1;
                l1 = l1.next;
            }
            // p指针后移
            p = p.next;
        }
        if(l1 != null){
            p.next = l1;
        }
        if(l2 != null){
            p.next = l2;
        }
        return dummy.next;
    }
    // public static void main(String[] args) {
    //     ListNode l1 = new ListNode(1);
    //     l1.next = new ListNode(2);
    //     l1.next.next = new ListNode(4);
    //     ListNode l2 = new ListNode(2);
    //     l2.next = new ListNode(3);
    //     l2.next.next = new ListNode(6);
    //     l2.next.next.next = new ListNode(7);
    //     Solution s = new Solution();
    //     ListNode d = s.mergeTwoLists(l1, l2);    
    //     System.out.println(d);
    // }
}
// @lc code=end


