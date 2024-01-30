## 一、HashMap底层结构和原理

**原理：** HashMap是由数据+链表+红黑树的形式组成的。  
数组：是一块连续的存储空间，存储的每一个元素都有自己的下标。

- 优点：寻址容易，通过下标可以快速的访问到元素，索引速度快 ；
- 缺点：大小固定，数组中的空间放满了，需要重新建立新数组，复制原有数组到新数组中，并且插入和删除困难。

链表：每一个元素存储下一个元素的内存地址，链式存储，存储元素离散。

- 优点：添加元素快速，只需要将上一个元素指向自己，自己存储下一个元素的地址，插入和删除容易；
- 缺点：没有索引下标，寻址困难，每次查找元素都需要从头部从新遍历，依次查找

HashMap底层的数据结构是hash[散列表](https://so.csdn.net/so/search?q=%E6%95%A3%E5%88%97%E8%A1%A8&spm=1001.2101.3001.7020)，结合了数组+链表两种数据结构。

采用Node（继承自Entry）数组来存储key-value的键值对，一个Node对应一个键值对，Node类是一个单向链表的结构，  
通过next（Node类型）指针连接下一个Node节点(解决hash冲突问题)。  
Node是HashMap的一个静态内部类，有key,value,hash,next是个属性。  
key,value即存储的是每个节点的键值对，  
hash 存储对应节点的hash值（并不是key的直接hash,而是(h = key.hashCode()) ^ (h >>> 16)，后边会对此方法详细讲解）  
next 是一个指针，用来记录下一个元素，当链表的长度超过8并且数组长度超过64之后链表将会转换为红黑树结构  
![HashMap底层数据结构](https://img-blog.csdnimg.cn/bde0b68dbd4044d99d59c382bb5ea7b8.jpeg#pic_center)

## 二、源码解析

### HashMap初始参数

```java

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2 and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * Should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts
     * between resizing and treeification thresholds.
     */
    static final int MIN_TREEIFY_CAPACITY = 64;
    

```

### 初始化构造方法

```java

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

```

构造方法中两个参数： initialCapacity 初始的[哈希表](https://so.csdn.net/so/search?q=%E5%93%88%E5%B8%8C%E8%A1%A8&spm=1001.2101.3001.7020)长度，loadFactor 负载因子。

### put源码解析

```java

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * Implements Map.put and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        // tab 引用当前hashmap的散列表
        // p 表示当前散列表的元素
        // n 表示当前数组的长度
        // i 表示路由寻址的结果
        Node<K,V>[] tab; Node<K,V> p; int n, i;

  // 如果table（散列表）为空或者里边没有元素，对散列表进行初始化（调用resize方法）
        // 在创建 HashMap 对象的时候并没有对table进行初始化，是为了避免创建HashMap之后，并不在里边放置元素，
        // 导致内存的浪费，所以在第一次调用putVal进行初始化最消耗内存的散列表
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
  // 最简单的情况，寻址找到的桶位（table的下标）正好是null，这个时候将key-value封装成Node对象，并放在i的位置
        // (n - 1) & hash 计算元素的桶位
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        else {
         // e node临时元素，不为null的话，找到了一个与要插入的key-value一致的key的元素
            // k 表示临时元素的key
            Node<K,V> e; K k;

   // 如果将要放置的元素的hash值和此桶位的元素一样，并且key的值相等，表示后续将要进行替换操作
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
                
   // 此桶位的结构是树形结构
            else if (p instanceof TreeNode)
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
   
   // 此桶位的结构是链表结构，并且链表的头元素和要插入的key不一致
            else {
             // 对链表进行循环，binCount表示此链表有多少元素
                for (int binCount = 0; ; ++binCount) {
                 // 如果循环到下一个元素是null，表示此时的元素是最后一个元素，将临时元素放在尾端
                    if ((e = p.next) == null) {
                     // 将临时元素放在尾端
                        p.next = newNode(hash, key, value, null);
                        // 如果此链表的长度达到了转换为树形结构的条件，尝试转换为红黑树。下标-1为第一个元素
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                         // 尝试转换为红黑树
                            treeifyBin(tab, hash);
                        break;
                    }
     
     // 如果将要放置的元素的hash值和此桶位的元素一样，并且key的值相等，说明找到了相同key的node元素，需要进行替换操作
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    // 将临时元素赋值给p，此时的e是p的下一个元素(e = p.next)
                    p = e;
                }
            }
            
            // e != null，表示此时的元素不是最后一个元素，即找到了一个相同key的node元素，需要进行value替换操作
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }

  // 增加散列表结构变化的次数
        ++modCount;
        // 插入新元素，size自增，如果自增后的值大于扩容阈值，则触发扩容
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
    }

```

### resize() 扩容方法源码解析

```java

    /**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
     * @return the table
     */
    final Node<K,V>[] resize() {
     // oldTab 引用扩容前的哈希表
        Node<K,V>[] oldTab = table;
        // oldCap 扩容之前的table的数组长度
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        // oldThr 表示扩容前的扩容阈值，触发本次扩容的阈值
        int oldThr = threshold;
        // newCap 扩容之后的table的数组长度
        // newThr 扩容之后的扩容阈值
        int newCap, newThr = 0;

  // 表示哈希表已经初始化过了，这是一次正常的扩容
        if (oldCap > 0) {
         // 扩容之前的数组大小已经达到了最大阈值后，则不扩容，且设置扩容条件为Integer.MAX_VALUE
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            // 如果oldCap左移一位实现数值翻倍，赋值给newCap，newCap小于最大的数组最大的长度，且扩容之前的数组长度小于16
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                // 新的扩容阈值翻倍
                newThr = oldThr << 1; // double threshold
        }
        // oldCap == 0，说明散列表是null，还没有进行初始化
        // 1. new HashMap(int initialCapacity, float loadFactor)；
        // 2. new HashMap(int initialCapacity)；
        // 3. new HashMap(Map<? extends K, ? extends V> m); 并且m有数据
        else if (oldThr > 0) // initial capacity was placed in threshold
         // 将扩容前的扩容阈值赋值给table的数组长度
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
         // 使用默认值
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        // newThr为 0，通过 newCap * loadFactor 计算出 newThr
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        // 创建一个更长更大的数组
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        // 说明哈希表本次扩容之前table不为null，已经进行过初始化
        if (oldTab != null) {
         // 对旧的哈希表进行循环
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                // 说明当前桶位上有数据，但是不确定是单个元素，还是链表，还是红黑树
                if ((e = oldTab[j]) != null) {
                 // 将老数组此桶位上的数据置空，方便JVM GC时回收，节省内存
                    oldTab[j] = null;
                    
                    // 如果当前node节点的下一个元素是空的，表示为单个元素
                    if (e.next == null)
                     // 从新计算此单个元素在新哈希表的桶位，并将此元素放置在新的哈希表对应的桶位上
                        newTab[e.hash & (newCap - 1)] = e;

     // 表示当前桶位上的是树形结构
                    else if (e instanceof TreeNode)
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);

     // 表示当前桶位上是链表结构
                    else { // preserve order
                     // 低位链表：存放在扩容之后的数组的下标位置，与当前数字的下标位置一致
                        Node<K,V> loHead = null, loTail = null;
                        // 高位链表：存档子啊扩容之后的数组的下标位置为 当前数组下标位置 + 扩容之前数组的长度
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                         // 将链表的下一个元素赋值给next变量
                            next = e.next;
                            // 此元素处于新表中的低位链表
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            // 此元素处于新表中的高位链表
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }


```

综合来说，HashMap 一次扩容的过程： 1、取当前 table 的 2 倍作为新 table 的大小 2、根据算出的新 table 的大小 new 出一个新的 Entry 数组来，名为 newTable 3、轮询原 table 的每一个位置，将每个位置上连接的 Entry，算出在新 table 上的位置，并以链表形式连接 4、原 table 上的所有 Entry 全部轮询完毕之后，意味着原 table 上面的所有 Entry 已经移到了新的 table 上，HashMap 中的 table 指向 newTable
