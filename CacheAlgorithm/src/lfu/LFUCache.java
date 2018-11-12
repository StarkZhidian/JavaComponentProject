package lfu;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import base.GetElementListener;

/**
 * Create by StarkZhidian on 30/10/2018
 * LFU 算法的实现：
 * 它是基于 “如果一个数据在最近一段时间内使用次数很少，那么在将来一段时间内被使用的可能性也很小” 的思路。
 * 数据依据访问次数降序排列，元素排序和遍历 {@link #entrySet()} 通过双向链表实现，
 * 随机访问 {@link #get(Object)} 通过自定义 Map 实现，通过连地址法（单向链表）来处理储存的元素的 key 的 hash 值出现冲突的情况，
 * 确保键的类型已经正确的实现了 equals 和 hashCode 方法，否则无法正确的通过键来得到对应的值
 */
@SuppressWarnings({"unchecked"})
public class LFUCache<K, V> {
    private static final boolean DEBUG = true;
    private static final int DEFAULT_MAX_CAPACITY = 32;
    protected ElementHashMap<K, V> elementsMap;

    public LFUCache() {
        this(DEFAULT_MAX_CAPACITY);
    }

    public LFUCache(int maxCapacity) {
        this(maxCapacity, null);
    }

    public LFUCache(int maxCapacity, GetElementListener<K, V> getElementListener) {
        this(ElementHashMap.DEFAULT_CAPACITY, maxCapacity, getElementListener);
    }

    public LFUCache(int initCapacity, int maxCapacity, GetElementListener<K, V> getElementListener) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("The argument maxCapacity must greater than zero!");
        }
        elementsMap = new ElementHashMap<>(initCapacity, maxCapacity, getElementListener);
    }

    public void setGetElementListener(GetElementListener<K, V> getElementListener) {
        elementsMap.setGetElementListener(getElementListener);
    }

    public int getMissCount() {
        return elementsMap.missCount;
    }

    public int getHitCount() {
        return elementsMap.hitCount;
    }

    // 获取缓存中元素的命中比例
    public float getHitPercent() {
        return elementsMap.getHitPercent();
    }

    public V get(K key) {
        return elementsMap.get(key);
    }

    public V put(K key, V value) {
        return elementsMap.put(key, value);
    }

    public int size() {
        return elementsMap.size();
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return elementsMap.entrySet();
    }

    @Override
    public String toString() {
        return elementsMap.toString();
    }

    /**
     * 储存和散列元素的 Map，tables 数组元素储存的是双向链表的节点，整个双向链表层通过访问频率降序排列，
     * 当容量已满时淘汰尾节点，元素之间的 hash 冲突通过单向链表来处理
     * @param <K> 键的类型
     * @param <V> 值的类型
     */
    static class ElementHashMap<K, V> extends AbstractMap<K, V> implements GetElementListener<K, V> {
        static final float DEFAULT_FACTOR = 0.75F;
        static final int MAX_CAPACITY = 1 << 30;
        static final int DEFAULT_CAPACITY = 16;
        int maxCapacity;
        int size;
        Element<K, V>[] tables;
        Element<K, V> head;
        Element<K, V> tail;
        HashMap<K, Integer> elementVisitTimes; // 记录当前 Cache 中元素访问次数的 map
        EntrySet entrySet;
        private GetElementListener<K, V> getElementListener;
        private int hitCount; // get 元素命中的次数
        private int missCount; // get 元素没有命中的次数

        ElementHashMap(int maxCapacity, GetElementListener<K, V> getElementListener) {
            this(DEFAULT_CAPACITY, maxCapacity, getElementListener);
        }

        ElementHashMap(int initCapacity, int maxCapacity, GetElementListener<K, V> getElementListener) {
            if (initCapacity <= 0 || maxCapacity <= 0) {
                throw new IllegalArgumentException(
                        "The argument initCapacity and maxCapacity must greater than zero!");
            }
            maxCapacity = maxCapacity > MAX_CAPACITY ? MAX_CAPACITY : maxCapacity;
            initCapacity = initCapacity > maxCapacity ? maxCapacity : initCapacity;
            this.maxCapacity = maxCapacity;
            initCapacity = tableSizeFor(initCapacity);
            elementVisitTimes = new HashMap<K, Integer>(initCapacity);
            tables = (Element<K, V>[]) new Element[initCapacity];
            this.getElementListener = getElementListener;
        }

        public void setGetElementListener(GetElementListener<K, V> getElementListener) {
            this.getElementListener = getElementListener;
        }

        public int getMissCount() {
            return missCount;
        }

        public int getHitCount() {
            return hitCount;
        }

        // 获取缓存中元素的命中比例
        public float getHitPercent() {
            int count = hitCount + missCount;
            return count == 0 ? 0 : (float) hitCount / (count);
        }

        public int size() {
            return size;
        }

        public void display() {
            System.out.println(this);
            System.out.println(elementVisitTimes + "\n");
        }

        @Override
        public V get(Object key) {
            int hash = hash((K) key, tables.length);
            Element<K, V> e;
            Integer visitTimes = elementVisitTimes.get(key);
            // 元素未命中
            if (visitTimes == null) {
                onMiss((K) key);
                return null;
            } else { // 元素命中
                Element<K, V> ele = getElement(hash, key);
                if (ele != null) {
                    onHit(ele.key, ele.value);
                } else {
                    String errMessage = "Get element: can not get element for key: "
                            + key + ", hash: " + hash + ", current table: " + this + "\n";
                    if (DEBUG) {
                        throw new IllegalStateException(errMessage);
                    } else {
                        System.err.println(errMessage);
                    }
                    onMiss((K) key);
                }
                // 访问次数 + 1，并重新移动元素
                elementVisitTimes.put((K) key, visitTimes + 1);
                return (e = moveElement(ele)) == null ? null : e.value;
            }
        }

        @Override
        public V put(K key, V value) {
            int hash = hash(key, tables.length);
            Element<K, V> node = getElement(hash, key);
            V oldValue = null;
            // 已有节点
            if (node != null) {
                if (DEBUG) {
                    Integer time = elementVisitTimes.get(key);
                    if (time == null) {
                        throw new IllegalStateException("The time of key: " + key + " should not be null!");
                    }
                }
                // 访问次数 + 1
                elementVisitTimes.put(key, elementVisitTimes.get(key) + 1);
                oldValue = node.value;
                node.value = value;
            } else { // 没有找到节点，插入新节点
                node = new Element<>(key, value, hash, null, null, null);
                elementVisitTimes.put(key, 1);
                // 容量已满，淘汰尾节点
                if (size >= maxCapacity) {
                    removeLast();
                } else {
                    size++;
                }
                // 插入新节点
                addElement(hash, node);
                // 如果当前的节点数达到一定阈值，那么进行扩容
                if (size >= tables.length * DEFAULT_FACTOR) {
                    resize();
                }
            }
            // 移动节点至双向链表中合适的位置
            moveElement(node);
            return oldValue;
        }

        public void setMaxCapacity(int newMaxCapacity) {
            if (newMaxCapacity < this.maxCapacity) {
                throw new IllegalArgumentException("The maxCapacity field can not shrink!");
            }
            this.maxCapacity = newMaxCapacity;
        }

        /**
         * 获取第一个不小于给定容量的 2 的次幂值
         * @param cap 给定的容量
         * @return 第一个不小于给定容量的 2 的次幂值
         */
        static int tableSizeFor(int cap) {
            int n = cap - 1;
            n |= n >>> 1;
            n |= n >>> 2;
            n |= n >>> 4;
            n |= n >>> 8;
            n |= n >>> 16;
            return (n < 0) ? 1 : (n >= MAX_CAPACITY) ? MAX_CAPACITY : n + 1;
        }

        void resize() {
            int newCap = tables.length << 1;
            if (newCap > MAX_CAPACITY || newCap <= 0) {
                newCap = MAX_CAPACITY;
            }
            if (newCap > tables.length) {
                Element<K, V>[] newTab = (Element<K, V>[]) new Element[newCap];
                Element<K, V> firstTemp = null;
                for (Element<K, V> ele : tables) {
                    if (ele != null) {
                        firstTemp = ele;
                        newTab[ele.hash = hash(ele.key, newCap)] = ele;
                        ele = ele.hashConflictNext;
                    }
                    // 处理 hash 冲突
                    while (ele != null) {
                        ele.hash = firstTemp.hash;
                        ele = ele.hashConflictNext;
                    }
                }
                tables = newTab;
            } else if (DEBUG) {
                System.out.println("The size of current table is maximum capacity!");
            }
        }

        void removeLast() {
            if (tail != null) {
                Element<K, V> ele, elePrev = null;
                if ((ele = tables[tail.hash]) != null) {
                    if (Objects.equals(ele.key, tables[tail.hash].key)) {
                        tables[tail.hash].hashConflictNext = null;
                        tables[tail.hash] = ele.hashConflictNext;
                    } else {
                        // 处理 elementMap 中的冲突
                        do {
                            if (Objects.equals(ele.key, tail.key)) {
                                if (elePrev != null) {
                                    elePrev.hashConflictNext = ele.hashConflictNext;
                                    ele.hashConflictNext = null;
                                    break;
                                }
                            }
                            elePrev = ele;
                        } while ((ele = ele.hashConflictNext) != null);
                    }
                }
                // 计数 map 移除对应元素
                if (elementVisitTimes.remove(tail.key) == null) {
                    if (DEBUG) {
                        throw new IllegalStateException("removeLast: can not get count of tail!");
                    } else {
                        System.out.println("Something was wrong when remove tail element!");
                    }
                }
                // 如果只有一个节点，那么 head 赋 null
                head = tail == head ? null : head;
                // 双向链表中移除节点
                if (tail.prev != null) {
                    tail.prev.next = null;
                }
                Element<K, V> tailPrev = tail.prev;
                tail.next = tail.prev = null;
                tail = tailPrev;
            } else {
                if (DEBUG) {
                    throw new IllegalStateException("removeLast: tail reference is null!");
                } else {
                    System.out.println("removeLast: tail reference is null!");
                }
            }
        }

        void addElement(int hash, Element<K, V> eleInsert) {
            if (eleInsert == null) {
                return ;
            }
            Element<K, V> ele;
            // 头插法建立冲突链表
            if ((ele = tables[hash]) != null) {
                eleInsert.hashConflictNext = ele;
            }
            tables[hash] = eleInsert;
            // 双向链表部分将节点插入到最后
            eleInsert.prev = tail;
            if (tail != null) {
                tail.next = eleInsert;
            }
            tail = eleInsert;
            head = (head == null ? tail : head);
        }

        Element<K, V> getElement(int hash, Object key) {
            Element<K, V> ele;
            // 处理可能存在的冲突
            if ((ele = tables[hash]) != null) {
                do {
                    if (Objects.equals(key, ele.key)) {
                        return ele;
                    }
                } while ((ele = ele.hashConflictNext) != null);
            }
            return null;
        }

        /**
         * 移动指定元素到合适位置（依据访问次数），返回要移动的元素
         * @param ele 要移动的元素
         * @return 要移动的元素
         */
        Element<K, V> moveElement(Element<K, V> ele) {
            if (ele == null || ele == head) {
                return ele;
            }
            Element<K, V> moveEle = ele;
            while (moveEle != null &&
                    elementVisitTimes.get(ele.key) >= elementVisitTimes.get(moveEle.key)) {
                moveEle = moveEle.prev;
            }
            // 移动的元素是尾节点，将尾节点前移
            if (ele == tail) {
                tail = tail.prev;
            }
            // 做好 ele 的前继节点和后继节点的连接
            if (ele.prev != null) {
                ele.prev.next = ele.next;
            }
            if (ele.next != null) {
                ele.next.prev = ele.prev;
            }
            // 移动 ele 到合适的位置
            if (moveEle != null) {
                ele.next = moveEle.next;
                moveEle.next = ele;
                ele.prev = moveEle;
            } else { // 移动到 head 之前
                ele.next = head;
                if (head != null) {
                    ele.prev = null;
                    head.prev = ele;
                }
                head = ele;
            }
            // 如果 ele.next 为 null，证明 ele 即为尾节点，将 ele 赋给 tail
            if (ele.next == null) {
                tail = ele;
            }
            return ele;
        }

        // 求出某个 Key 对象的 hash 值，如果 modLength 大于 0，那么 modLength 必须是 2 的次幂
        int hash(K key, int modLength) {
            int h = ((key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16));
            if (modLength <= 0) {
                return h;
            } else {
                if (DEBUG && (modLength & (modLength - 1)) != 0) {
                    throw new IllegalStateException("hash method: argument modLength must be the power of 2!");
                }
                return h & (modLength - 1);
            }
        }


        @Override
        public void onHit(K key, V value) {
            hitCount++;
            if (getElementListener != null) {
                getElementListener.onHit(key, value);
            }
        }

        @Override
        public void onMiss(K key) {
            missCount++;
            if (getElementListener != null) {
                getElementListener.onMiss(key);
            }
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return entrySet == null ? (entrySet = new EntrySet()) : entrySet;
        }

        /**
         * 描述 ElementMap 储存的元素的类，通过链地址法处理处理 hash 冲突
         * @param <K> 键的类型
         * @param <V> 值的类型
         */
        static class Element<K, V> implements Map.Entry<K, V> {
            int hash; // 键的 hash 值
            K key;
            V value;
            Element<K, V> prev;
            Element<K, V> next;
            // 采用单链表处理 hash 冲突，如果当前元素的 hash 值有冲突，
            // 那么 hashConflictNext 指向同 hash 值的下一个元素
            Element<K, V> hashConflictNext;

            Element() {}

            Element(K key, V value, int hash, Element<K, V> prev, Element<K, V> next,
                           Element<K, V> hashConflictNext) {
                this.key = key;
                this.value = value;
                this.hash = hash;
                this.prev = prev;
                this.next = next;
                this.hashConflictNext = hashConflictNext;
            }

            @Override
            public K getKey() {
                return key;
            }

            @Override
            public V getValue() {
                return value;
            }

            @Override
            public V setValue(V value) {
                V oldValue = this.value;
                this.value = value;
                return oldValue;
            }

            @Override
            public int hashCode() {
                int result = 1;
                int prime = 31;
                return prime * (prime * result + (key == null ? 0 : key.hashCode())) +
                        (value == null ? 0 : value.hashCode());
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof Element) {
                    return Objects.equals(((Element) obj).key, key) &&
                            Objects.equals(((Element) obj).value, value);
                }
                return super.equals(obj);
            }

            @Override
            public String toString() {
                return "{hash: " + hash + ", key: " + key + ", value: " +
                        value + ", conflictNext: " + hashConflictNext + "}\n";
            }
        }

        class EntrySet extends AbstractSet<Entry<K, V>> {

            Element<K, V> curPoint;

            EntrySet() {
                resetPoint();
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {
                    {
                        resetPoint();
                    }

                    @Override
                    public boolean hasNext() {
                        return curPoint != null;
                    }

                    @Override
                    public Entry<K, V> next() {
                        if (curPoint == null) {
                            throw new IllegalStateException(
                                    "Current point is null, there is no more elements!");
                        }
                        Element<K, V> ele = curPoint;
                        curPoint = curPoint.next;
                        return ele;
                    }
                };
            }

            @Override
            public int size() {
                return size;
            }

            public void resetPoint() {
                curPoint = head;
            }
        }
    }

    // 测试 1
    private static void test1() {
        LFUCache<String, String> lfuCache = new LFUCache<>(4);
        for (int i = 0; i < 10; i++) {
            lfuCache.put(String.valueOf(i), String.valueOf(i));
        }
        for (int i = 9; i >= 0; i--) {
            lfuCache.put(String.valueOf(i), String.valueOf(i));
        }
    }

    // 测试 2
    private static void test2() {
        LFUCache<String, String> lfuCache;
        for (int i = 1; i < 12; i++) {
            lfuCache = new LFUCache<>(i);
            for (int j = 0; j < 10; j++) {
                lfuCache.put(String.valueOf(j), String.valueOf(j));
                lfuCache.elementsMap.display();
            }
            for (int j = 0; j < 10; j++) {
                lfuCache.get(String.valueOf(j));
            }
            System.out.println(lfuCache.getHitPercent());
        }
    }

    public static void main(String[] args) {
        test2();
    }
}
