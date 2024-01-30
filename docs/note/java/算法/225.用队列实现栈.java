/*
 * @lc app=leetcode.cn id=225 lang=java
 *
 * 
 * 请你仅使用两个队列实现一个后入先出（LIFO）的栈，并支持普通栈的全部四种操作
 * push、top、pop 和 empty）。
 * 实现 MyStack 类：
 * void push(int x) 将元素 x 压入栈顶。
 * int pop() 移除并返回栈顶元素。
 * int top() 返回栈顶元素。
 * boolean empty() 如果栈是空的，返回 true ；否则，返回 false 。
 * [225] 用队列实现栈
 */

// @lc code=start

import java.util.LinkedList;
import java.util.Queue;

class MyStack {
    private Queue<Integer> q1, q2;
    int top_elem;

    public MyStack() {
        q1 = new LinkedList<>();
        q2 = new LinkedList<>();
    }

    // 非空队列中放入元素
    public void push(int x) {
        if (q2.isEmpty()) {
            q1.offer(x);
        } else {
            q2.offer(x);
        }
        top_elem = x;
    }

    // 将非空队列中的元素依次出队并入队到另一个队列，直到只剩一个元素
    public int pop() {
        Queue<Integer> nonEmpty = q1.isEmpty() ? q2 : q1;
        Queue<Integer> empty = q1.isEmpty() ? q1 : q2;
        while (nonEmpty.size() > 1) {
            top_elem = nonEmpty.peek();
            empty.offer(nonEmpty.poll());
        }
        return nonEmpty.poll();
    }

    public int top() {
        return top_elem;
    }

    public boolean empty() {
        return q1.isEmpty() && q2.isEmpty();

    }
}

/**
 * // 使用一个队列实现
 * class MyStack {
    Queue<Integer> q = new LinkedList<>();
    int top_elem = 0;

    
    // 添加元素到栈顶
    public void push(int x) {
        // x 是队列的队尾，是栈的栈顶
        q.offer(x);
        top_elem = x;
    }

    //  返回栈顶元素
    public int top() {
        return top_elem;
    }

     // 删除栈顶的元素并返回
    public int pop() {
        int size = q.size();
        // 留下队尾 2 个元素
        while (size > 2) {
            q.offer(q.poll());
            size--;
        }
        // 记录新的队尾元素
        top_elem = q.peek();
        q.offer(q.poll());
        // 删除之前的队尾元素
        return q.poll();
    }

     // 判断栈是否为空
    public boolean empty() {
        return q.isEmpty();
    }
}
 * 
 */
/**
 * Your MyStack object will be instantiated and called as such:
 * MyStack obj = new MyStack();
 * obj.push(x);
 * int param_2 = obj.pop();
 * int param_3 = obj.top();
 * boolean param_4 = obj.empty();
 */
// @lc code=end
