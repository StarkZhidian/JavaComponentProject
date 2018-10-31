package lfu;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Create by StarkZhidian on 30/10/2018
 * LFU 算法的实现：
 * 它是基于 “如果一个数据在最近一段时间内使用次数很少，那么在将来一段时间内被使用的可能性也很小” 的思路。
 * 即数据依据访问次数降序排列（排序通过双向链表实现），访问通过自定义 Map 实现
 *
 */
public class LFUCache<K, V> {
    private static final boolean DEBUG = true;
    private static final int DEFAULT_MAX_CAPACITY = 32;
    protected ElementHashMap<K, V> elementsMap;

    public LFUCache() {
        this(DEFAULT_MAX_CAPACITY);
    }

    public LFUCache(int maxCapacity) {
        elementsMap = new ElementHashMap<>(maxCapacity);
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
     * 储存和散列元素的 Map，tables 数组元素储存的是双向链表的节点，整个双向链表层通过访问频率降序，
     * 当容量已满时淘汰尾节点
     * @param <K> 键的类型
     * @param <V> 值的类型
     */
    static class ElementHashMap<K, V> extends AbstractMap<K, V> {
        static final float DEFAULT_FACTOR = 0.75F;
        int maxCapacity;
        int size;
        Element<K, V>[] tables;
        Element<K, V> head;
        Element<K, V> tail;
        HashMap<K, Integer> elementVisitTimes;
        EntrySet entrySet;

        ElementHashMap(int maxCapacity) {
            if (maxCapacity <= 0) {
                throw new IllegalArgumentException("The argument maxCapacity must greater than zero!");
            }
            this.maxCapacity = maxCapacity;
            elementVisitTimes = new HashMap<K, Integer>(maxCapacity);
            tables = new Element[(int) (maxCapacity / DEFAULT_FACTOR)];
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
            int hash = hash(key);
            Element<K, V> e;
            Integer visitTimes = elementVisitTimes.get(key);
            if (visitTimes == null) {
                return null;
            } else {
                // 访问次数 + 1，并重新移动元素
                elementVisitTimes.put((K) key, visitTimes + 1);
                return (e = moveElement(getElement(hash, key))) == null ? null : e.value;
            }
        }

        @Override
        public V put(K key, V value) {
            int hash = hash(key);
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
            } else {
                node = new Element<>(key, value, null, null, null);
                elementVisitTimes.put(key, 1);
                // 容量已满，淘汰尾节点
                if (size >= maxCapacity) {
                    removeLast();
                } else {
                    size++;
                }
                // 插入新节点
                addElement(hash, node);
            }
            // 移动节点至双向链表中合适的位置
            moveElement(node);
            display();
            return oldValue;
        }

        void removeLast() {
            if (tail != null) {
                Element<K, V> ele, elePrev = null;
                int hash = hash(tail.key);
                if ((ele = tables[hash]) != null) {
                    if (Objects.equals(ele.key, tables[hash].key)) {
                        tables[hash].hashConflictNext = null;
                        tables[hash] = ele.hashConflictNext;
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
                        System.out.println("Something was wrong!");
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
                    System.out.println("removeLast: something was wrong!");
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

        int hash(Object key) {
            int h;
            return ((key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)) % tables.length;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return entrySet == null ? (entrySet = new EntrySet()) : entrySet;
        }

        /**
         * 描述 ElementMap 储存的元素的类，通过链地址法处理处理 hash 冲突
         * @param <K>
         * @param <V>
         */
        static class Element<K, V> implements Map.Entry<K, V> {
            K key;
            V value;
            Element<K, V> prev;
            Element<K, V> next;
            // 采用
            Element<K, V> hashConflictNext;

            Element() {}

            Element(K key, V value, Element<K, V> prev, Element<K, V> next,
                           Element<K, V> hashConflictNext) {
                this.key = key;
                this.value = value;
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
            }
        }
    }

    public static void main(String[] args) {
        test2();
    }
}
