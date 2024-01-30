## 一、JDK1.7下原理实现

**底层原理图：**  
![ConcurrentHashMap的JDK1.7实现](https://img-blog.csdnimg.cn/6c14edce6c75478d84ef865df712403d.jpeg#pic_center)  
[ConcurrentHashMap](https://so.csdn.net/so/search?q=ConcurrentHashMap&spm=1001.2101.3001.7020) 是由 Segment 数组结构和 HashEntry 数组结构组成。 Segment 是一种可重入锁（ReentrantLock），在 ConcurrentHashMap 里扮演锁的角色；HashEntry 则用于存储键值对数据。一个 ConcurrentHashMap 里包含一个 Segment 数组。Segment 的结构和 HashMap 类似，是一种数组和链表结构。一个 Segment 里包含一个 HashEntry 数组，每个 HashEntry 是一个链表结构的元素， 每个 Segment 守护着一个 HashEntry 数组里的元素，当对 HashEntry 数组的数据进行修改时，必须首先获得与它对应的 Segment 锁。

**构造方法：**

```java
public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
 //检验参数是否合法。值得说的是，并发级别一定要大于0，否则就没办法实现分段锁了。
 if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0)
  throw new IllegalArgumentException();
 //并发级别不能超过最大值
 if (concurrencyLevel > MAX_SEGMENTS)
  concurrencyLevel = MAX_SEGMENTS;
 // Find power-of-two sizes best matching arguments
 //偏移量，是为了对hash值做位移操作，计算元素所在的Segment下标，put方法详讲
 int sshift = 0;
 //用于设定最终Segment数组的长度，必须是2的n次幂
 int ssize = 1;
 //这里就是计算 sshift 和 ssize 值的过程  (1) 
 while (ssize < concurrencyLevel) {
  ++sshift;
  ssize <<= 1;
 }
 this.segmentShift = 32 - sshift;
 //Segment的掩码
 this.segmentMask = ssize - 1;
 if (initialCapacity > MAXIMUM_CAPACITY)
  initialCapacity = MAXIMUM_CAPACITY;
 //c用于辅助计算cap的值   (2)
 int c = initialCapacity / ssize;
 if (c * ssize < initialCapacity)
  ++c;
 // cap 用于确定某个Segment的容量，即Segment中HashEntry数组的长度
 int cap = MIN_SEGMENT_TABLE_CAPACITY;
 //(3)
 while (cap < c)
  cap <<= 1;
 // create segments and segments[0]
 //这里用 loadFactor做为加载因子，cap乘以加载因子作为扩容阈值，创建长度为cap的HashEntry数组，
 //三个参数，创建一个Segment对象，保存到S0对象中。后边在 ensureSegment 方法会用到S0作为原型对象去创建对应的Segment。
 Segment<K,V> s0 = new Segment<K,V>(loadFactor, (int)(cap * loadFactor), (HashEntry<K,V>[])new HashEntry[cap]);
 //创建出长度为 ssize 的一个 Segment数组
 Segment<K,V>[] ss = (Segment<K,V>[])new Segment[ssize];
 //把S0存到Segment数组中去。在这里，我们就可以发现，此时只是创建了一个Segment数组，
 //但是并没有把数组中的每个Segment对象创建出来，仅仅创建了一个Segment用来作为原型对象。
 UNSAFE.putOrderedObject(ss, SBASE, s0); // ordered write of segments[0]
 this.segments = ss;
}

```

ConcurrentHashMap 初始化方法是通过 initialCapacity、loadFactor 和 concurrencyLevel(参数 concurrencyLevel 是用户估计的并发级别，就是说你觉得最多有多少线程共同修改这个 map，根据这个来确定 Segment 数组的大小 concurrencyLevel 默认是 DEFAULT\_CONCURRENCY\_LEVEL = 16;)等几个参数来初始化 segment 数组、段偏移量 segmentShift、段[掩码](https://so.csdn.net/so/search?q=%E6%8E%A9%E7%A0%81&spm=1001.2101.3001.7020) segmentMask 和每个 segment 里的 HashEntry 数组来实现的。

并发级别可以理解为程序运行时能够同时更新 ConccurentHashMap 且不产生锁竞争的最大线程数，实际上就是 ConcurrentHashMap 中的分段锁个数，即 Segment\[\]的数组长度。ConcurrentHashMap 默认的并发度为 16，但用户也可以 在构造函数中设置并发度。当用户设置并发度时，ConcurrentHashMap 会使用大 于等于该值的最小 2 幂指数作为实际并发度（假如用户设置并发度为 17，实际并发度则为 32）。

如果并发度设置的过小，会带来严重的锁竞争问题；如果并发度设置的过大， 原本位于同一个 Segment 内的访问会扩散到不同的 Segment 中，CPU cache 命中率会下降，从而引起程序性能下降。（文档的说法是根据你并发的线程数量决定，太多会导性能降低）

segments 数组的长度 ssize 是通过 concurrencyLevel 计算得出的。为了能通 过按位与的[散列](https://so.csdn.net/so/search?q=%E6%95%A3%E5%88%97&spm=1001.2101.3001.7020)算法来定位 segments 数组的索引，必须保证 segments 数组的长 度是 2 的 N 次方（power-of-two size），所以必须计算出一个大于或等于 concurrencyLevel 的最小的 2 的 N 次方值来作为 segments 数组的长度。假如 concurrencyLevel 等于 14、15 或 16，ssize 都会等于 16，即容器里锁的个数也是 16。

```java
public V put(K key, V value) {
 Segment<K,V> s;
 //不支持value为空
 if (value == null)
   throw new NullPointerException();
 //通过 Wang/Jenkins 算法的一个变种算法，计算出当前key对应的hash值
 int hash = hash(key);
 //上边我们计算出的 segmentShift为28，因此hash值右移28位，说明此时用的是hash的高4位，
 //然后把它和掩码15进行与运算，得到的值一定是一个 0000 ~ 1111 范围内的值，即 0~15 。
 int j = (hash >>> segmentShift) & segmentMask;
 //这里是用Unsafe类的原子操作找到Segment数组中j下标的 Segment 对象
 if ((s = (Segment<K,V>)UNSAFE.getObject          // nonvolatile; recheck
   (segments, (j << SSHIFT) + SBASE)) == null) //  in ensureSegment
  //初始化j下标的Segment
  s = ensureSegment(j);
 //在此Segment中添加元素
 return s.put(key, hash, value, false);
}

```

ConcurrentHashMap 使用分段锁 Segment 来保护不同段的数据，那么在插入和获取元素的时候，必须先通过散列算法定位到 Segment。 ConcurrentHashMap 会首先使用 Wang/Jenkins hash 的变种算法对元素的 hashCode 进行一次再散列。

```java
//Segment中的 put 方法
final V put(K key, int hash, V value, boolean onlyIfAbsent) {
 //这里通过tryLock尝试加锁，如果加锁成功，返回null，否则执行 scanAndLockForPut方法
 //这里说明一下，tryLock 和 lock 是 ReentrantLock 中的方法，
 //区别是 tryLock 不会阻塞，抢锁成功就返回true，失败就立马返回false，
 //而 lock 方法是，抢锁成功则返回，失败则会进入同步队列，阻塞等待获取锁。
 HashEntry<K,V> node = tryLock() ? null : scanAndLockForPut(key, hash, value);
 V oldValue;
 try {
  //当前Segment的table数组
  HashEntry<K,V>[] tab = table;
  //这里就是通过hash值，与tab数组长度取模，找到其所在HashEntry数组的下标
  int index = (tab.length - 1) & hash;
  //当前下标位置的第一个HashEntry节点
  HashEntry<K,V> first = entryAt(tab, index);
  for (HashEntry<K,V> e = first;;) {
   //如果第一个节点不为空
   if (e != null) {
    K k;
    //并且第一个节点，就是要插入的节点，则替换value值，否则继续向后查找
    if ((k = e.key) == key ||
     (e.hash == hash && key.equals(k))) {
     //替换旧值
     oldValue = e.value;
     if (!onlyIfAbsent) {
      e.value = value;
      ++modCount;
     }
     break;
    }
    e = e.next;
   }
   //说明当前index位置不存在任何节点，此时first为null，
   //或者当前index存在一条链表，并且已经遍历完了还没找到相等的key，此时first就是链表第一个元素
   else {
  //如果node不为空，则直接头插
  if (node != null)
   node.setNext(first);
  //否则，创建一个新的node，并头插
  else
   node = new HashEntry<K,V>(hash, key, value, first);
  int c = count + 1;
  //如果当前Segment中的元素大于阈值，并且tab长度没有超过容量最大值，则扩容
  if (c > threshold && tab.length < MAXIMUM_CAPACITY)
   rehash(node);
  //否则，就把当前node设置为index下标位置新的头结点
  else
   setEntryAt(tab, index, node);
  ++modCount;
  //更新count值
  count = c;
  //这种情况说明旧值肯定为空
  oldValue = null;
  break;
   }
  }
 } finally {
  //需要注意ReentrantLock必须手动解锁
  unlock();
 }
 //返回旧值
 return oldValue;
}
```

ConcurrentHashMap 完全允许多个读操作并发进行，读操作并不需要加锁。 ConcurrentHashMap 实现技术是保证 HashEntry 几乎是不可变的以及 volatile 关键 字。

**rehash操作**  
扩容是新创建了数组，然后进行迁移数据，最后再将 newTable 设置给属性 table。

为了避免让所有的节点都进行复制操作：由于扩容是基于 2 的幂指来操作， 假设扩容前某 HashEntry 对应到 Segment 中数组的 index 为 i，数组的容量为 capacity，那么扩容后该 HashEntry 对应到新数组中的 index 只可能为 i 或者 i+capacity，因此很多 HashEntry 节点在扩容前后 index 可以保持不变。

## 二、JDK1.8下原理实现

**与JDK1.7区别：**

1. 取消 segments 字段，直接采用 transient volatile HashEntry<K,V>\[\] table 保存数据，采用 table 数组元素作为锁，从而实现了对缩小锁的粒度，进一 步减少并发冲突的概率，并大量使用了采用了 CAS + synchronized 来保证并发安全性。
2. 将原先 table 数组＋单向链表的数据结构，变更为 table 数组＋单 向链表＋红黑树的结构。对于 hash 表来说，最核心的能力在于将 key hash 之后 能均匀的分布在数组中。如果 hash 之后散列的很均匀，那么 table 数组中的每个 队列长度主要为 0 或者 1。但实际情况并非总是如此理想，虽然 ConcurrentHashMap 类默认的加载因子为 0.75，但是在数据量过大或者运气不佳的情况下，还是会存在一些队列长度过长的情况，如果还是采用单向列表方式， 那么查询某个节点的时间复杂度为 O(n)；因此，对于个数超过 8(默认值)的列表， jdk1.8 中采用了红黑树的结构，那么查询的时间复杂度可以降低到 O(logN)，可 以改进性能。 使用 Node（1.7 为 Entry） 作为链表的数据结点，仍然包含 key，value， hash 和 next 四个属性。 红黑树的情况使用的是 TreeNode（extends Node）。 根据数组元素中，第一个结点数据类型是 Node 还是 TreeNode 可以判断该位置下是链表还是红黑树。

用于判断是否需要将链表转换为红黑树的阈值：

```java

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2, and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     */
    static final int TREEIFY_THRESHOLD = 8;
    

```

用于判断是否需要将红黑树转换为链表的阈值:

```java

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     */
    static final int UNTREEIFY_THRESHOLD = 6;
    

```

核心数据结构和属性:

```java

    /* ---------------- Nodes -------------- */

    /**
     * Key-value entry.  This class is never exported out as a
     * user-mutable Map.Entry (i.e., one supporting setValue; see
     * MapEntry below), but can be used for read-only traversals used
     * in bulk tasks.  Subclasses of Node with a negative hash field
     * are special, and contain null keys and values (but are never
     * exported).  Otherwise, keys and vals are never null.
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        volatile V val;
        volatile Node<K,V> next;

        Node(int hash, K key, V val, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        public final K getKey()       { return key; }
        public final V getValue()     { return val; }
        public final int hashCode()   { return key.hashCode() ^ val.hashCode(); }
        public final String toString(){ return key + "=" + val; }
        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            Object k, v, u; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == (u = val) || v.equals(u)));
        }

        /**
         * Virtualized support for map.get(); overridden in subclasses.
         */
        Node<K,V> find(int h, Object k) {
            Node<K,V> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h &&
                        ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                } while ((e = e.next) != null);
            }
            return null;
        }
    }


```

定义基本和 1.7 中的 HashEntry 相同。而这个 map 本身所持有的也是一个 Node 型的数组 增加了一个 find 方法来用以辅助 map.get()方法。其实就是遍历链表，子类中会覆盖这个方法。

**TreeNode:**

```java

    /* ---------------- TreeNodes -------------- */

    /**
     * Nodes for use in TreeBins
     */
    static final class TreeNode<K,V> extends Node<K,V> {
        TreeNode<K,V> parent;  // red-black tree links
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        boolean red;

        TreeNode(int hash, K key, V val, Node<K,V> next,
                 TreeNode<K,V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        Node<K,V> find(int h, Object k) {
            return findTreeNode(h, k, null);
        }

        /**
         * Returns the TreeNode (or null if not found) for the given key
         * starting at given root.
         */
        final TreeNode<K,V> findTreeNode(int h, Object k, Class<?> kc) {
            if (k != null) {
                TreeNode<K,V> p = this;
                do  {
                    int ph, dir; K pk; TreeNode<K,V> q;
                    TreeNode<K,V> pl = p.left, pr = p.right;
                    if ((ph = p.hash) > h)
                        p = pl;
                    else if (ph < h)
                        p = pr;
                    else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                        return p;
                    else if (pl == null)
                        p = pr;
                    else if (pr == null)
                        p = pl;
                    else if ((kc != null ||
                              (kc = comparableClassFor(k)) != null) &&
                             (dir = compareComparables(kc, k, pk)) != 0)
                        p = (dir < 0) ? pl : pr;
                    else if ((q = pr.findTreeNode(h, k, kc)) != null)
                        return q;
                    else
                        p = pl;
                } while (p != null);
            }
            return null;
        }
    }


```

**与 1.8 中 HashMap 不同点：**

1. 它并不是直接转换为红黑树，而是把这些结点放在 TreeBin 对象中，由 TreeBin 完成对红黑树的包装。
2. TreeNode 在 ConcurrentHashMap 扩展自 Node 类，而并非 HashMap 中的 扩展自 LinkedHashMap.Entry<K,V>类，也就是说 TreeNode 带有 next 指针。

**TreeBin:**

```java
static final class TreeBin<K,V> extends Node<K,V> {
        TreeNode<K,V> root;
        volatile TreeNode<K,V> first;
        volatile Thread waiter;
        volatile int lockState;
        // values for lockState
        static final int WRITER = 1; // set while holding write lock
        static final int WAITER = 2; // set when waiting for write lock
        static final int READER = 4; // increment value for setting read lock
  
  //...
}

```

**核心方法：**

```java
 /**
 * 利用硬件级别的原子操作，获得在i位置上的Node节点，
 * Unsafe.getObjectVolatile可以直接获取指定内存的数据
 * 保证每次拿到的数据都是最新的
 */
    static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    }

 /**
 * 利用CAS操作设置i位置上Node节点
 */
    static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                        Node<K,V> c, Node<K,V> v) {
        return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }

 /**
 * 利用硬件级别的原子操作，设置在i位置上的Node节点，
 * Unsafe.pputObjjectVolatile可以直接设定指定内存的数据
 * 保证了其他线程访问的这个节点时一定可以看到最新的数据
 */
    static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
        U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
    }

```

**构造方法：**

```java

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}), table
     * density ({@code loadFactor}), and number of concurrently
     * updating threads ({@code concurrencyLevel}).
     *
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements,
     * given the specified load factor.
     * @param loadFactor the load factor (table density) for
     * establishing the initial table size
     * @param concurrencyLevel the estimated number of concurrently
     * updating threads. The implementation may use this value as
     * a sizing hint.
     * @throws IllegalArgumentException if the initial capacity is
     * negative or the load factor or concurrencyLevel are
     * nonpositive
     */
    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?
            MAXIMUM_CAPACITY : tableSizeFor((int)size);
        this.sizeCtl = cap;
    }
    
```

在 new 出一个 map 的实例时，并不会创建其中的数组等等相关的部件，只是进行简单的属性设置而已，同样的，table 的大小也被规定为必须 是 2 的乘方数。

真正的初始化在放在了是在向 ConcurrentHashMap 中插入元素的时候发生 的。如调用 put、computeIfAbsent、compute、merge 等方法的时候，调用时机 是检查 table==null。

**get操作：**

```java
/
    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key.equals(k)},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * @throws NullPointerException if the specified key is null
     */
    public V get(Object key) {
        Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
        int h = spread(key.hashCode());
        //根据hash值确定节点位置
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (e = tabAt(tab, (n - 1) & h)) != null) {
            //Node数组中的节点就是要找的节点
            if ((eh = e.hash) == h) {
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                    return e.val;
            }
            //eh < 0 说明这个节点在树上，调用树的 find 方法寻找
            else if (eh < 0)
                return (p = e.find(h, key)) != null ? p.val : null;
            //到这说明是一个链表，遍历链表找打对应的值并返回
            while ((e = e.next) != null) {
                if (e.hash == h &&
                    ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }

```

**put操作：**

```java

    /**
     * Maps the specified key to the specified value in this table.
     * Neither the key nor the value can be null.
     *
     * <p>The value can be retrieved by calling the {@code get} method
     * with a key that is equal to the original key.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key or value is null
     */
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    /** Implementation for put and putIfAbsent */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        //何时插入成功何时跳出
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            //如果 table 为空，初始化 table
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
             //Node 数组中的元素，这个位置没有值，使用CAS操作插入
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            else if ((fh = f.hash) == MOVED)
             //正在进行扩容，当前线程帮忙扩容
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                //锁 Node 数组中的元素，这个位置是 Hash 冲突组成链表的头节点，或者是红黑树的根节点
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                     //fh>0 说明这个节点是一个链表的节点，不是树的节点
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                //put 操作和 putIfAbsent 操作业务实现
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                //如果適历到了最后一个节点，使用尾插法，把它插入在链表尾部
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        //按照树的方式插入值
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                 //达到临界值 8，需要把链表转换成树结构
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        //Map的元素数量 + 1，并检查是否需要扩容
        addCount(1L, binCount);
        return null;
    }


```

```java

    /* ---------------- Conversion from/to TreeBins -------------- */

    /**
     * Replaces all linked nodes in bin at given index unless table is
     * too small, in which case resizes instead.
     */
    private final void treeifyBin(Node<K,V>[] tab, int index) {
        Node<K,V> b; int n, sc;
        if (tab != null) {
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
                tryPresize(n << 1);
            //利用硬件级别的原子操作，获得在 index 位置上的Node节点，hash位置大于零
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        TreeNode<K,V> hd = null, tl = null;
                        for (Node<K,V> e = b; e != null; e = e.next) {
                            TreeNode<K,V> p =
                                new TreeNode<K,V>(e.hash, e.key, e.val,
                                                  null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        //利用硬件级别的原子操作，设置在 index 位置上的Node节点
                        setTabAt(tab, index, new TreeBin<K,V>(hd));
                    }
                }
            }
        }
    }

```

总结来说，put 方法就是，沿用 HashMap 的 put 方法的思想，根据 hash 值 计算这个新插入的点在 table 中的位置 i，如果 i 位置是空的，直接放进去，否则进行判断，如果 i 位置是树节点，按照树的方式插入新的节点，否则把 i 插入到链表的末尾。

整体流程上，就是首先定义不允许 key 或 value 为 null 的情况放入 对于每 一个放入的值，首先利用 spread 方法对 key 的 hashcode 进行一次 hash 计算，由此来确定这个值在 table 中的位置。

如果这个位置是空的，那么直接放入，而且不需要加锁操作。

如果这个位置存在结点，说明发生了 hash 碰撞，首先判断这个节点的类型。 如果是链表节点,则得到的结点就是 hash 值相同的节点组成的链表的头节点。需要依次向后遍历确定这个新加入的值所在位置。如果遇到 hash 值与 key 值都与 新加入节点是一致的情况，则只需要更新 value 值即可。否则依次向后遍历，直到链表尾插入这个结点。如果加入这个节点以后链表长度大于 8，就把这个链表转换成红黑树。如果这个节点的类型已经是树节点的话，直接调用树节点的插入 方法进行插入新的值。

**初始化：**

```java

    /**
     * Initializes table, using the size recorded in sizeCtl.
     */
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {
         //小干 0 表示有其他线程正在进行初始化操作，把当前线程CPU时问让出来。因为对于 table 的初始化工作，只能有一个线程在进行
            if ((sc = sizeCtl) < 0)
                Thread.yield(); // lost initialization race; just spin
            //利用 CAS 操作把 sizectl 的值置为 -1,表示本线程正在进行初始化
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = tab = nt;
                        // n 右移 2 位本质上就是 n 变为 n 原值的 1/4，所以 sc = 0.75 * n
                        sc = n - (n >>> 2);
                    }
                } finally {
                 //设置成扩容的阈值
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }


```

**transfer：**

```java
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab)
```

当 ConcurrentHashMap 容量不足的时候，需要对 table 进行扩容。这个方法的基本思想跟 HashMap 是很像的，但是由于它是支持并发扩容的，所以要复杂的多。

整个扩容操作分为两个部分：

- 第一部分是构建一个 nextTable,它的容量是原来的 2 倍。
- 第二个部分就是将原来 table 中的元素复制到 nextTable 中，这里允许多线程进行操作。

**整个扩容流程就是遍历和复制：**

为 null 或者已经处理过的节点，会被设置为 forwardNode 节点，当线程准备 扩容时，发现节点是 forwardNode 节点，跳过这个节点，继续寻找未处理的节点， 找到了，对节点上锁；

如果这个位置是 Node 节点（fh>=0），说明它是一个链表，就构造一个反序链表，把他们分别放在 nextTable 的 i 和 i+n 的位置上；

如果这个位置是 TreeBin 节点（fh<0），也做一个反序处理，并且判断是否 需要红黑树转链表，把处理的结果分别放在 nextTable 的 i 和 i+n 的位置上；

遍历过所有的节点以后就完成了复制工作，这时让 nextTable 作为新的 table， 并且更新 sizeCtl 为新容量的 0.75 倍 ，完成扩容。

并发扩容其实就是将数据迁移任务拆分成多个小迁移任务，在实现上使用了 一个变量 stride 作为步长控制，每个线程每次负责迁移其中的一部分。

**remove：**

```java
    /**
     * Removes the key (and its corresponding value) from this map.
     * This method does nothing if the key is not in the map.
     *
     * @param  key the key that needs to be removed
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key is null
     */
    public V remove(Object key) {
        return replaceNode(key, null, null);
    }

```

移除方法的基本流程和 put 方法很类似，只不过操作由插入数据变为移除数 据而已，而且如果存在红黑树的情况下，会检查是否需要将红黑树转为链表的步 骤。不再重复讲述。

**treeifyBin：**

用于将过长的链表转换为 TreeBin 对象。但是他并不是直接转换，而是进行 一次容量判断，如果容量没有达到转换的要求，直接进行扩容操作并返回；如果 满足条件才将链表的结构转换为 TreeBin ，这与 HashMap 不同的是，它并没有 把 TreeNode 直接放入红黑树，而是利用了 TreeBin 这个小容器来封装所有的 TreeNode。
