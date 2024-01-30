## 一、CAS介绍

### 1 什么是CAS

CAS（Compare And Swap，比较与交换），CAS的操作过程包含三个运算符：内存地址V，期望值A和新值B，操作时如果地址上的值等于期望值A，则将地址上的值赋为新值B，否则不做任何操作。  
![CAS过程](https://img-blog.csdnimg.cn/6f4d338ec98f49a7adfd9ad39a270493.jpeg#pic_center)

### 2 CAS方法示例

在Java底层代码中，CAS是由[Unsafe](https://so.csdn.net/so/search?q=Unsafe&spm=1001.2101.3001.7020)类提供支持的，该类定义了三种不同类型的变量的CAS操作。

```java
    public final native boolean compareAndSwapObject(Object var1, long var2, Object var4, Object var5);

    public final native boolean compareAndSwapInt(Object var1, long var2, int var4, int var5);

    public final native boolean compareAndSwapLong(Object var1, long var2, long var4, long var6);

```

调用的都是JVM封装的[native方法](https://so.csdn.net/so/search?q=native%E6%96%B9%E6%B3%95&spm=1001.2101.3001.7020)，由JVM提供具体实现，针对不同的操作系统，实现方式各有差异。其中var1，var2，var4，var5分别对应对象实例，内存偏移量，字段期望值，字段新值。

代码示例：

```java
package com.sonny.classexercise.concurrent;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeFactory {

    /**
     * 获取 Unsafe 对象
     * @return
     */
    public static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取字段的内存偏移量
     * @param unsafe
     * @param clazz
     * @param fieldName
     * @return
     */
    public static long getFieldOffset(Unsafe unsafe, Class clazz, String fieldName) {
        try {
            return unsafe.objectFieldOffset(clazz.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }


}

```

```java
package com.sonny.classexercise.concurrent;

import sun.misc.Unsafe;

/**
 * @author Xionghaijun
 * @date 2022/10/30 22:10
 */
public class CASTest {

    static class Entity{
        int x;
    }

    public static void main(String[] args) {
        Entity entity = new Entity();

        Unsafe unsafe = UnsafeFactory.getUnsafe();
        //获取字段内存偏移量
        long offset = UnsafeFactory.getFieldOffset(unsafe, Entity.class, "x");
        System.out.println(offset);
        boolean result;

        result = unsafe.compareAndSwapInt(entity, offset, 0, 1);
        System.out.println("结果：" + result + "\t" + entity.x);

        result = unsafe.compareAndSwapInt(entity, offset, 1, 2);
        System.out.println("结果：" + result + "\t" + entity.x);

        result = unsafe.compareAndSwapInt(entity, offset, 0, 3);
        System.out.println("结果：" + result + "\t" + entity.x);

    }
}


-------------------
12
结果：true 1
结果：true 2
结果：false 2
```

### 3 CAS底层源码分析

在[Hotspot](https://so.csdn.net/so/search?q=Hotspot&spm=1001.2101.3001.7020)虚拟机中对CAS方法实现

```cpp
#unsafe.cpp 
UNSAFE_ENTRY(jboolean, Unsafe_CompareAndSwapInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x)) UnsafeWrapper("Unsafe_CompareAndSwapInt"); 
oop p = JNIHandles::resolve(obj); 
// 根据偏移量，计算value的地址 
jint* addr = (jint *) index_oop_from_field_offset_long(p, offset); 
// Atomic::cmpxchg(x, addr, e) cas逻辑 x:要交换的值 e:要比较的值 
//cas成功，返回期望值e，等于e,此方法返回true 
//cas失败，返回内存中的value值，不等于e，此方法返回false 
return (jint)(Atomic::cmpxchg(x, addr, e)) == e; 
UNSAFE_END
```

核心逻辑在 Atomic::cmpxchg 方法中，这个根据不同操作系统和不同CPU会有不同的实现。以linux\_64x的为例，查看 Atomic::cmpxchg 的实现

```cpp
#atomic_linux_x86.inline.hpp 
inline jint Atomic::cmpxchg (jint exchange_value, volatile jint* dest, jint com pare_value) { 
//判断当前执行环境是否为多处理器环境 
int mp = os::is_MP(); 
//LOCK_IF_MP(%4) 在多处理器环境下，为 cmpxchgl 指令添加 lock 前缀，以达到内存屏障 的效果 
//cmpxchgl 指令是包含在 x86 架构及 IA‐64 架构中的一个原子条件指令， 
//它会首先比较 dest 指针指向的内存值是否和 compare_value 的值相等， 
//如果相等，则双向交换 dest 与 exchange_value，否则就单方面地将 dest 指向的内存值交 给exchange_value。
 //这条指令完成了整个 CAS 操作，因此它也被称为 CAS 指令。 
 __asm__ volatile (LOCK_IF_MP(%4) "cmpxchgl %1,(%3)"
 : "=a" (exchange_value) 
 : "r" (exchange_value), "a" (compare_value), "r" (dest), "r" (mp) 
 : "cc", "memory"); 
 return exchange_value; 
 }
```

**cmpxchgl的详细执行过程：**  
首先，输入是 “r” (exchange\_value), “a” (compare\_value), “r” (dest), “r” (mp)，表示compare\_value存入eax寄存器，而exchange\_value、dest、mp的值存入任意的通用寄存器。  
嵌入式汇编规定把输出和输入寄存器按统一顺序编号，顺序是从输出寄存器序列从左到右从上到下以“%0”开始，分别记为%0、%1∙∙∙%9。也就是说，输出的eax是%0，输入的exchange\_value、compare\_value、dest、mp分别是%1、%2、%3、%4。  
因此，cmpxchg %1,(%3)实际上表示cmpxchg exchange\_value,(dest) 需要注意的是cmpxchg有个隐含操作数eax，其实际过程是先比较eax的值(也就是compare\_value)和dest地址所存的值是否相等， 输出是 “=a” (exchange\_value)，表示把eax中存的值写入exchange\_value变量中。  
Atomic::cmpxchg这个函数最终返回值是exchange\_value，也就是说，如果cmpxchgl执行 时compare\_value和dest指针指向内存值相等则会使得dest指针指向内存值变成 exchange\_value，最终eax存的compare\_value赋值给了exchange\_value变量，即函数最 终返回的值是原先的compare\_value。  
此时Unsafe\_CompareAndSwapInt的返回值 (jint) (Atomic::cmpxchg(x, addr, e)) == e 就是true，表明CAS成功。  
如果cmpxchgl执行时 compare\_value和(dest)不等则会把当前dest指针指向内存的值写入eax，最终输出时赋值 给exchange\_value变量作为返回值，导致 (jint)(Atomic::cmpxchg(x, addr, e)) == e 得到 false，表明CAS失败。

___

现如今的处理器指令集架构基本上都会提供CAS指令，如 x86 和 IA-64 架构中的 cmpxchgl 指令 和 comxchgq 指令，sparc 架构中的 cas 指令和 casx 指令。

```
不管是 Hotspot 中的 Atomic::cmpxchg 方法，还是 Java 中的compareAndSwapInt 方法，
它们本质上都是对相应平台的 CAS 指令的一层简单封装。
CAS 指令作为一种硬件原语，有着天然 的原子性，这也正是 CAS 的价所在。
```

### 4 CAS缺陷

CAS虽然高效的解决了原子操作，但还是有三大缺陷：

#### 4.1 ABA问题

因为 CAS 需要在操作值的时候，检查值有没有发生变化，如果没有发生变化 则更新，但是如果一个值原来是 A，变成了 B，又变成了 A，那么使用 CAS 进行 检查时会发现它的值没有发生变化，但是实际上却变化了。 ABA 问题的解决思路就是使用版本号。在变量前面追加上版本号，每次变量 更新的时候把版本号加 1，那么 A→B→A 就会变成 1A→2B→3A。  
![ABA问题](https://img-blog.csdnimg.cn/ba37454de6724689be0443a5810f584f.gif)  
ABA问题解决方案

1. 使用AtomicStampedReference：使用 pair 的 int stamp 作为计数器，每次操作对stamp加一，记录的是改过几个版本。
2. 使用AtomicMarkableReference：使用的是boolean mark，记录的是是否更改过。

代码

```java
package com.sonny.classexercise.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.LockSupport;

/**
 * 经典转账案例：
 * 假如小熊卡里有100块钱存款，假定银行转账操作就是一个单纯的CAS命令，对比余额值看是否与当前值相同，如果相同则发生扣减、增加。我们看下会发生什么？
 * 第一步：小熊在ATM1机器上给小明转账100块，
 * 第二步：由于ATM1机器网络线路故障导致网络拥堵卡住了一直转圈圈无法返回结果，这时候小熊换了台ATM2机器给小明再次转账；
 * 第三步：这时候网络良好，执行了CAS(100, 0)，转账成功，此时小熊账号余额为0；
 * 第四步：然后小张刚好拿到工资，还了小熊100块，此时小熊账上余额为100块；
 * 第五步：此时ATM1机器网络恢复，继续执行CAS(100, 0)操作，此时执行成功了，小熊的账号余额又变成0了 (灬ꈍ ꈍ灬)；
 * 第六步：小张告诉小熊，100块钱还他了，询问是否收到了，小熊查看账号余额是0，表示没收到，此时就出现问题了，小张的转账钱没了。
 *
 * @author Xionghaijun
 * @date 2022/10/30 23:02
 */
@Slf4j
public class AtomicStampedReferenceTest {

    public static void main(String[] args) {
        // 定义AtomicStampedReference    Pair.reference值为1, Pair.stamp为1
        AtomicStampedReference atomicStampedReference = new AtomicStampedReference(100, 1);

        new Thread(() -> {
            int[] stampHolder = new int[1];
            int value = (int) atomicStampedReference.get(stampHolder);
            int stamp = stampHolder[0];
            log.debug("Thread1 read value: " + value + ", stamp: " + stamp);

            // 阻塞1s
            LockSupport.parkNanos(1000000000L);
            // Thread1通过CAS修改value值为3   stamp是版本，每次修改可以通过+1保证版本唯一性
            int newValue = 0;
            if (atomicStampedReference.compareAndSet(value, newValue, stamp, stamp + 1)) {
                log.debug("Thread1 update from " + value + " to " + newValue);
            } else {
                log.debug("Thread1 update from " + value + " to " + newValue + " fail!");
            }
        }, "Thread1").start();

        new Thread(() -> {
            int[] stampHolder = new int[1];
            int value = (int) atomicStampedReference.get(stampHolder);
            int stamp = stampHolder[0];
            log.debug("Thread2 read value: " + value + ", stamp: " + stamp);
            // Thread2通过CAS修改value值为2
            int newValue1 = 0;
            if (atomicStampedReference.compareAndSet(value, newValue1, stamp, stamp + 1)) {
                log.debug("Thread2 update from " + value + " to " + newValue1);

                // do something

                value = (int) atomicStampedReference.get(stampHolder);
                stamp = stampHolder[0];
                log.debug("Thread2 read value: " + value + ", stamp: " + stamp);
                // Thread2通过CAS修改value值为1
                int newValue2 = 100;
                if (atomicStampedReference.compareAndSet(value, newValue2, stamp, stamp + 1)) {
                    log.debug("Thread2 update from " + value + " to " + newValue2);
                }
            }
        }, "Thread2").start();
    }

}


-----------------------
22:00:07.212 [Thread1] DEBUG com.sonny.classexercise.concurrent.AtomicStampedReferenceTest - Thread1 read value: 100, stamp: 1
22:00:07.212 [Thread2] DEBUG com.sonny.classexercise.concurrent.AtomicStampedReferenceTest - Thread2 read value: 100, stamp: 1
22:00:07.219 [Thread2] DEBUG com.sonny.classexercise.concurrent.AtomicStampedReferenceTest - Thread2 update from 100 to 0
22:00:07.219 [Thread2] DEBUG com.sonny.classexercise.concurrent.AtomicStampedReferenceTest - Thread2 read value: 0, stamp: 2
22:00:07.219 [Thread2] DEBUG com.sonny.classexercise.concurrent.AtomicStampedReferenceTest - Thread2 update from 0 to 100
22:00:08.220 [Thread1] DEBUG com.sonny.classexercise.concurrent.AtomicStampedReferenceTest - Thread1 update from 100 to 0 fail!
```

#### 4.2 循环时间长开销大

自旋 CAS 如果长时间不成功，会给 CPU 带来非常大的执行开销。

#### 4.3 只能保证一个共享变量的原子操作

当对一个共享变量执行操作时，我们可以使用循环 CAS 的方式来保证原子操作，但是对多个共享变量操作时，循环 CAS 就无法保证操作的原子性，这个时候就可以用锁。

从 Java 1.5 开始，JDK 提供了 AtomicReference 类来保证引用对象之间的原子性，就可以把 多个变量放在一个对象里来进行 CAS 操作。
