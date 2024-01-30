/*
 * @lc app=leetcode.cn id=232 lang=java
 *
 * [232] 用栈实现队列
 */

// @lc code=start

import java.util.Stack;

/* 
 * 我们使用两个栈 s1, s2 就能实现一个队列的功能。
 * 
 * 当调用 push 让元素入队时，只要把元素压入 s1 即可：
 * 使用 peek 或 pop 操作队头的元素时，若 s2 为空，可以把 s1 的所有元素取出再添加进 s2
 * ，这时候 s2 中元素就是先进先出顺序了：
 */
class MyQueue {
    private Stack<Integer> s1, s2;

    public MyQueue() {
        s1 = new Stack<>();
        s2 = new Stack<>();
    }

    /**
     * 添加元素到队尾
     */
    public void push(int x) {
        s1.push(x);
    }

    /**
     * 删除队头的元素并返回
     */
    public int pop() {
        // 先调用 peek 保证 s2 非空
        peek();
        return s2.pop();
    }

    /**
     * 返回队头元素
     */
    public int peek() {
        if (s2.isEmpty()) {
            while (!s1.isEmpty()) {
                s2.push(s1.pop());
            }
        }
        return s2.peek();
    }

    /**
     * 判断队列是否为空
     */
    public boolean empty() {
        return s1.isEmpty() && s2.isEmpty();
    }
}

/**
 * Your MyQueue object will be instantiated and called as such:
 * MyQueue obj = new MyQueue();
 * obj.push(x);
 * int param_2 = obj.pop();
 * int param_3 = obj.peek();
 * boolean param_4 = obj.empty();
 */
// @lc code=end
