## 一、引子

[并发编程](https://so.csdn.net/so/search?q=%E5%B9%B6%E5%8F%91%E7%BC%96%E7%A8%8B&spm=1001.2101.3001.7020)能充分利用CPU资源，提高用户响应时间，但同时共享内存模型也带来了线程安全问题。当两个线程同时对一个共享静态变量操作时，一个做自增操作，一个做自减操作，能达到想要的结果吗

```java
package com.sonny.classexercise.concurrent.synchronize;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Xionghaijun
 * @date 2022/11/3 22:15
 */
@Slf4j
public class SyncDemo {

    private static volatile int counter = 0;

    public static void increment() {
        counter++;
    }

    public static void decrement() {
        counter--;
    }

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50000; i++) {
                increment();
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 50000; i++) {
                decrement();
            }
        }, "t2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        //counter会是零嘛，volatile不能保证线程操作原子性，发生了竞态条件
        log.info("counter = {}", counter);


    }
}

-------------
21:35:07.371 [main] INFO com.sonny.classexercise.concurrent.synchronize.SyncDemo - counter = -2750

```

发现程序并没达到想要的结果零。

之前我们分析过，加了 volatile 关键字的变量能保证可见性，但此时多线程中并未达到“**可见性**”效果，我们在编译器中安装一个jclasslib，查看JVM中的字节码指令。  
![多线程操作共享变量](https://img-blog.csdnimg.cn/bbfdda5e5e3c45f78e2f2895d13495d3.jpeg#pic_center)  
increment方法的i++操作字节码指令：  
![increment字节码](https://img-blog.csdnimg.cn/4f82dbf8fba14569aa4b7cdf9f251dfa.jpeg#pic_center)

decrement方法的i–操作字节码指令：  
![decrement字节码](https://img-blog.csdnimg.cn/3a45d98f1fdf4feeb3d4ad20c19e8563.png)  
单线程下这些字节码指令顺序执行是没有问题的，但是多线程下 putstatic 操作交替执行就会出现问题，线程1更新了共享变量后，线程2当即又更新了变量，导致本来应该增大的变量变小了。

此时就引入了“竞态条件”问题，即多个线程在临界区内执行时，由于代码的执行序列不同而导致结果无法预测。

**解决方案：**

- **阻塞式：** Synchronized，Lock， Condition

- **非阻塞式：** 原子变量（Atomic）

    ```
      Tips：Java中互斥和同步都可用 Synchronized 关键字完成，但他们还是有区别的：
      互斥是保证临界区的竞态条件发生，同一时刻只能有一个线程只信你个临界区代码；
      同步是由于线程执行的先后顺序不同，需要一个线程等待其它线程裕兴到某个点（使用信号量）。
    ```

## 二、Synchronized

### 1 基本概念

**概述：** synchronized 同步块是 Java 提供的一种原子性内置锁，Java 中的每个对象都可以把它当作 一个同步锁来使用，这些 Java 内置的使用者看不到的锁被称为内置锁，也叫作监视器锁。

**实现细节：** 在JVM中都是基于进入和退出monitor对象来实现方法同步和代码块同步，具体的实现细节不同，但都是通过成对的 MonitorEnter 和 MonitorExit 指令实现。两个指令的执行是JVM通过调用操作系统的 互斥原语 mutex 来实现，被阻塞的线程会被挂起，等待锁释放后被重新唤起，此操作会导致用户态、内核态切换，对性能影响较大。

- 同步块中：MonitorEnter 指令插入在同步代码块的开始位置，当代码执行到 该指令时，将会尝试获取该对象 Monitor 的所有权，即尝试获得该对象的锁，而 monitorExit 指令则插入在方法结束处和异常处，JVM 保证每个 MonitorEnter 必须 有对应的 MonitorExit。
- 同步方法中：JVM 根据过方法中的access\_flags中设置ACC\_SYNCHRONIZED标志来实现方法的同步的：当方法被调用时，调用指令将 会检查方法的 ACC\_SYNCHRONIZED 访问标志是否被设置，如果设置了，执行线 程将先获取 monitor，获取成功之后才能执行方法体，方法执行完后再释放 monitor。在方法执行期间，其他任何线程都无法再获得同一个 monitor 对象。

Synchronized 加锁的位置不同，所得对象也不同：  
![加锁方式](https://img-blog.csdnimg.cn/24061045b3e7401599d17b0f1a4cbd3d.png)  
解决上面未达到预期效果问题  
方式一：方法锁

```java
    public static synchronized void increment() {
        counter++;
    }

    public static synchronized void decrement() {
        counter--;
    }

```

方式二：实例对象锁

```java
    private static String lock = "";

    public static void increment() {
        synchronized (lock) {
            counter++;
        }
    }

    public static void decrement() {
        synchronized (lock) {
            counter--;
        }
    }

```

### 2 底层原理

#### 2.1 对象的内存布局

Hotspot虚拟机中，对象在内存中可分为三块区域：对象头、实例数据、对齐填充。

- **对象头：** Hash码，GC分代年龄，对象锁，锁状态标志，偏向锁ID，偏向时间戳，数组长度等。
- **实例数据：** 存放类属性数据信息，包括父类的属性信息。
- **对齐填充：** 虚拟机要求对象起始地址必须是8bit的整数倍。填充数据不是必须存在的，仅是为了字节对齐。

其中对象头包括：

- **Mark Work：** 用于存储对象自身的运行时数据，如哈希码（HashCode）、GC分代年龄、锁状态标志、线 程持有的锁、偏向线程ID、偏向时间戳等，这部分数据的长度在32位和64位的虚拟机中分别为 32bit和64bit，官方称它为“Mark Word”。
- **Klass Pointer：** 即对象指向它的类元数据的指针，虚拟机通过这个指 针来确定这个对象是哪个类的实例。 32位4字节，64位开启指针压缩或最大堆内存<32g时4字 节，否则8字节。jdk1.8默认开启指针压缩后为4字节，当在JVM参数中关闭指针压缩（-XX:- UseCompressedOops）后，长度为8字节。
- **数组长度：** 如果对象是一个数组, 那在对象头中还必须有一块数据用于记录数组长度。 占4字节。

![对象头详解](https://img-blog.csdnimg.cn/66bda06ffd70482b81474a34f308ef62.jpeg#pic_center)  
Mark Word中锁标记枚举类

```java
enum { 
 locked_value = 0, //00 轻量级锁 
 unlocked_value = 1, //001 无锁 
 monitor_value = 2, //10 监视器锁，也叫膨胀锁，也叫重量级锁 
 marked_value = 3, //11 GC标记 
 biased_lock_pattern = 5 //101 偏向锁 
};

```

锁状态和对应的存储内容：  
![锁状态理解](https://img-blog.csdnimg.cn/9eb3444dd2084907800142e3fb533799.jpeg#pic_center)

#### 2.2 锁的状态

##### 偏向锁

偏向于第一个访问锁的线程，若在运行过程中，同步锁只有一个线程访问，不存在多线程抢占情况，则线程是不需要触发同步的，减少加锁、解锁的CAS操作（如等待队列的CAS操作），这种情况下，就会给线程加一个偏向锁。若在运行过程中，遇到其他线程抢占锁，则持有偏向锁的线程会被挂起，将锁升级为轻量级锁。

大多数情况下锁不仅不存在多线程竞争，而且总是由同一线程多次获得，为了来让线程获得锁的代价更低而引入了偏向锁，减少不必要的CAS操作。

**偏向锁获取过程：**

- 步骤 1、 访问 Mark Word 中偏向锁的标识是否设置成 1，锁标志位是否为 01，确认为可偏向状态。
- 步骤 2、 如果为可偏向状态，则测试线程 ID 是否指向当前线程，如果是， 进入步骤 5，否则进入步骤 3。
- 步骤 3、 如果线程 ID 并未指向当前线程，则通过 CAS 操作竞争锁。如果竞争成功，则将 Mark Word 中线程 ID 设置为当前线程 ID，然后执行 5；如果竞争 失败，执行 4。
- 步骤 4、 如果 CAS 获取偏向锁失败，则表示有竞争。当到达全局安全点 （safepoint）时获得偏向锁的线程被挂起，偏向锁升级为轻量级锁，然后被阻塞在安全点的线程继续往下执行同步代码。（撤销偏向锁的时候会导致 stop the word）
- 步骤 5、 执行同步代码。  
    ![偏向锁加锁撤销锁过程](https://img-blog.csdnimg.cn/3258b685bde84d92b0b58c07fd27f87d.jpeg#pic_center)

**偏向锁的释放**

偏向锁的撤销在上述第四步骤中有提到。偏向锁只有遇到其他线程尝试竞争偏向锁时，持有偏向锁的线程才会释放偏向锁，线程不会主动去释放偏向锁。偏向锁的撤销，需要等待全局安全点（在这个时间点上没有字节码正在执行），它会首先暂停拥有偏向锁的线程，判断锁对象是否处于被锁定状态，撤销偏向锁后恢复到未锁定（标志位为“01”）或轻量级锁（标志位为“00”）的状态。

**偏向锁的适用场景**

始终只有一个线程在执行同步块，在它没有执行完释放锁之前，没有其它线程去 执行同步块，在锁无竞争的情况下使用，一旦有了竞争就升级为轻量级锁，升级为轻量级锁的时候需要撤销偏向锁，撤销偏向锁的时候会导致 stop the word 操 作；在有锁的竞争时，偏向锁会多做很多额外操作，尤其是撤销偏向所的时候会导致 进入安全点，安全点会导致 stw，导致性能下降，这种情况下应当禁用。

##### 轻量级锁

轻量级锁是由偏向锁升级来的，偏向锁运行在一个线程进入同步块的情况下，当第二个线程加入锁争用的时候，偏向锁就会升级为轻量级锁。

**轻量级锁的加锁过程：**  
在代码进入同步块的时候，如果同步对象锁状态为无锁状态且不允许进行偏向 （锁标志位为“01”状态，是否为偏向锁为“0”），虚拟机首先将在当前线程 的栈帧中建立一个名为锁记录（Lock Record）的空间，用于存储锁对象目前的 Mark Word 的拷贝，官方称之为 Displaced Mark Word。 拷贝成功后，虚拟机将使用 CAS 操作尝试将对象的 Mark Word 更新为指向 Lock Record 的指针，并将 Lock record 里的 owner 指针指向 object mark word。如果更新成功，则执行步骤 4，否则执行步骤 5。 如果这个更新动作成功了，那么这个线程就拥有了该对象的锁，并且对象 Mark Word 的锁标志位设置为“00”，即表示此对象处于轻量级锁定状态 如果这个更新操作失败了，虚拟机首先会检查对象的 Mark Word 是否指向当前 线程的栈帧，如果是就说明当前线程已经拥有了这个对象的锁，那就可以直接进入同步块继续执行。否则说明多个线程竞争锁，当竞争线程尝试占用轻量级锁失 败多次之后，轻量级锁就会膨胀为重量级锁，重量级线程指针指向竞争线程，竞争线程也会阻塞，等待轻量级线程释放锁后唤醒他。锁标志的状态值变为“10”， Mark Word 中存储的就是指向重量级锁（互斥量）的指针，后面等待锁的线程也 要进入阻塞状态。

**jvm 开启/关闭偏向锁**

- 开启偏向锁：-XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0
- 关闭偏向锁：-XX:-UseBiasedLocking

**偏向锁升级到轻量级锁过程：**

```java
package com.sonny.classexercise.concurrent.synchronize;

import org.openjdk.jol.info.ClassLayout;

/**
 * 关闭指针压缩（-XX:-UseCompressedOops）
 *
 * @author Xionghaijun
 * @date 2022/11/5 22:44
 */
public class ObjectTest {
    public static void main(String[] args) throws InterruptedException {
        //HotSpot 虚拟机在启动后有个 4s 的延迟才会对每个新建的对象开启偏向锁模式
        Thread.sleep(5000);
        Object obj = new Object();
        //Object obj = new Integer[4];
        //obj.hashCode();
        //查看对象内部信息
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());

        new Thread(() -> {
            synchronized (obj) {
                System.out.println(Thread.currentThread().getName() + "\n" + ClassLayout.parseInstance(obj).toPrintable());
            }
            System.out.println(Thread.currentThread().getName() + "释放锁\n" + ClassLayout.parseInstance(obj).toPrintable());

            // jvm 优化
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, "Thread1").start();


        Thread.sleep(2000);


        new Thread(() -> {
            synchronized (obj) {
                System.out.println(Thread.currentThread().getName() + "\n" + ClassLayout.parseInstance(obj).toPrintable());
            }
            System.out.println(Thread.currentThread().getName() + "释放锁\n" + ClassLayout.parseInstance(obj).toPrintable());
        }, "Thread2").start();

    }
}

-----------
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           05 00 00 00 (00000101 00000000 00000000 00000000) (5)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           00 7c 95 29 (00000000 01111100 10010101 00101001) (697662464)
     12     4        (object header)                           02 00 00 00 (00000010 00000000 00000000 00000000) (2)
Instance size: 16 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total

Thread1
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           05 00 18 1f (00000101 00000000 00011000 00011111) (521666565)
      4     4        (object header)                           87 7f 00 00 (10000111 01111111 00000000 00000000) (32647)
      8     4        (object header)                           00 7c 95 29 (00000000 01111100 10010101 00101001) (697662464)
     12     4        (object header)                           02 00 00 00 (00000010 00000000 00000000 00000000) (2)
Instance size: 16 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total

Thread1释放锁
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           05 00 18 1f (00000101 00000000 00011000 00011111) (521666565)
      4     4        (object header)                           87 7f 00 00 (10000111 01111111 00000000 00000000) (32647)
      8     4        (object header)                           00 7c 95 29 (00000000 01111100 10010101 00101001) (697662464)
     12     4        (object header)                           02 00 00 00 (00000010 00000000 00000000 00000000) (2)
Instance size: 16 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total

Thread2
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           08 19 eb 0c (00001000 00011001 11101011 00001100) (216733960)
      4     4        (object header)                           00 70 00 00 (00000000 01110000 00000000 00000000) (28672)
      8     4        (object header)                           00 7c 95 29 (00000000 01111100 10010101 00101001) (697662464)
     12     4        (object header)                           02 00 00 00 (00000010 00000000 00000000 00000000) (2)
Instance size: 16 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total

Thread2释放锁
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           00 7c 95 29 (00000000 01111100 10010101 00101001) (697662464)
     12     4        (object header)                           02 00 00 00 (00000010 00000000 00000000 00000000) (2)
Instance size: 16 bytes
Space losses: 0 bytes internal + 0 bytes external = 0 bytes total

```

##### 重量级锁

JVM中又叫对象监视器（Monitor），底层依赖 Mutex(0|1) 互斥的功能实现，加锁、解锁会有用户态到内核态的切换，消耗大量系统资源。

**偏向锁，轻量级锁，重量级锁标记变化：**

```java
package com.sonny.classexercise.concurrent.synchronize;

import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.ClassLayout;

/**
 * 测试 偏向锁，轻量级锁，重量级锁标记变化
 * 关闭延迟开启偏向锁： -XX:BiasedLockingStartupDelay=0
 * 无锁 001
 * 偏向锁 101
 * 轻量级锁 00
 * 重量级锁 10
 *
 * @author Xionghaijun
 * @date 2022/11/6 20:45
 */
@Slf4j
public class LockEscalationDemo {

    public static void main(String[] args) throws InterruptedException {
        log.debug(ClassLayout.parseInstance(new Object()).toPrintable());
        //HotSpot 虚拟机在启动后有个 4s 的延迟才会对每个新建的对象开启偏向锁模式
        Thread.sleep(5000);
        Object obj = new Object();
        // 如果对象调用了hashCode,不会开启偏向锁，因为偏向锁中无法存储hashcode
//        obj.hashCode();
        log.debug(ClassLayout.parseInstance(obj).toPrintable());

        new Thread(() -> {
            log.debug(Thread.currentThread().getName() + "开始执行。。。\n"
                    + ClassLayout.parseInstance(obj).toPrintable());
            synchronized (obj) {
                //偏向锁执行过程中，调用hashcode会发生锁降级
//                obj.hashCode();
                //obj.notify();
//                    try {
//                        obj.wait(50);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }

                log.debug(Thread.currentThread().getName() + "获取锁执行中。。。\n"
                        + ClassLayout.parseInstance(obj).toPrintable());
            }
            log.debug(Thread.currentThread().getName() + "释放锁。。。\n"
                    + ClassLayout.parseInstance(obj).toPrintable());
        }, "thread1").start();

        //控制线程竞争时机
        Thread.sleep(1);

        new Thread(() -> {
            log.debug(Thread.currentThread().getName() + "开始执行。。。\n"
                    + ClassLayout.parseInstance(obj).toPrintable());
            synchronized (obj) {

                log.debug(Thread.currentThread().getName() + "获取锁执行中。。。\n"
                        + ClassLayout.parseInstance(obj).toPrintable());
            }
            log.debug(Thread.currentThread().getName() + "释放锁。。。\n"
                    + ClassLayout.parseInstance(obj).toPrintable());
        }, "thread2").start();


        new Thread(() -> {
            log.debug(Thread.currentThread().getName() + "开始执行。。。\n"
                    + ClassLayout.parseInstance(obj).toPrintable());
            synchronized (obj) {

                log.debug(Thread.currentThread().getName() + "获取锁执行中。。。\n"
                        + ClassLayout.parseInstance(obj).toPrintable());
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug(Thread.currentThread().getName() + "释放锁。。。\n"
                    + ClassLayout.parseInstance(obj).toPrintable());
        }, "thread3").start();


        Thread.sleep(5000);
        log.debug(ClassLayout.parseInstance(obj).toPrintable());
    }

}

--------------------
23:18:16.341 [main] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           05 00 00 00 (00000101 00000000 00000000 00000000) (5)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:21.351 [main] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           05 00 00 00 (00000101 00000000 00000000 00000000) (5)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:21.354 [thread1] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - thread1开始执行。。。
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           05 00 00 00 (00000101 00000000 00000000 00000000) (5)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:21.357 [thread1] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - thread1获取锁执行中。。。
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           05 a0 91 c4 (00000101 10100000 10010001 11000100) (-997089275)
      4     4        (object header)                           7b 7f 00 00 (01111011 01111111 00000000 00000000) (32635)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:21.357 [thread2] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - thread2开始执行。。。
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           05 a0 91 c4 (00000101 10100000 10010001 11000100) (-997089275)
      4     4        (object header)                           7b 7f 00 00 (01111011 01111111 00000000 00000000) (32635)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:21.359 [thread3] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - thread3开始执行。。。
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           05 a0 91 c4 (00000101 10100000 10010001 11000100) (-997089275)
      4     4        (object header)                           7b 7f 00 00 (01111011 01111111 00000000 00000000) (32635)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:21.360 [thread2] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - thread2获取锁执行中。。。
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           08 89 ed 0b (00001000 10001001 11101101 00001011) (200116488)
      4     4        (object header)                           00 70 00 00 (00000000 01110000 00000000 00000000) (28672)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:21.361 [thread1] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - thread1释放锁。。。
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           05 a0 91 c4 (00000101 10100000 10010001 11000100) (-997089275)
      4     4        (object header)                           7b 7f 00 00 (01111011 01111111 00000000 00000000) (32635)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:21.362 [thread2] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - thread2释放锁。。。
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           5a 9b 81 c5 (01011010 10011011 10000001 11000101) (-981361830)
      4     4        (object header)                           7b 7f 00 00 (01111011 01111111 00000000 00000000) (32635)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:21.363 [thread3] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - thread3获取锁执行中。。。
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           5a 9b 81 c5 (01011010 10011011 10000001 11000101) (-981361830)
      4     4        (object header)                           7b 7f 00 00 (01111011 01111111 00000000 00000000) (32635)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:23.369 [thread3] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - thread3释放锁。。。
java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

23:18:26.360 [main] DEBUG com.sonny.classexercise.concurrent.synchronize.LockEscalationDemo - java.lang.Object object internals:
 OFFSET  SIZE   TYPE DESCRIPTION                               VALUE
      0     4        (object header)                           01 00 00 00 (00000001 00000000 00000000 00000000) (1)
      4     4        (object header)                           00 00 00 00 (00000000 00000000 00000000 00000000) (0)
      8     4        (object header)                           e5 01 00 f8 (11100101 00000001 00000000 11111000) (-134217243)
     12     4        (loss due to the next object alignment)
Instance size: 16 bytes
Space losses: 0 bytes internal + 4 bytes external = 4 bytes total


```

##### 锁总结

![锁升降级总结](https://img-blog.csdnimg.cn/be22a5f28bf14d3aa3c0e705645386f8.jpeg#pic_center)
