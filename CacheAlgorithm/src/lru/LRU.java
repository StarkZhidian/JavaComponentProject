package lru;

import java.util.Objects;

/**
 * 基于双向链表实现的 LRU 缓存，使用自定义类型作为键时，
 * 确保键的类型已经正确的实现了 equals 方法，否则无法正确的通过键来得到对应的值
 * @author StarkZhidian
 *
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
public class LRU<K, V> {

    private static final int DEFAULT_MAX_CAPACITY = 32;
    private int maxCapacity;
    private int count;
    private Element<K, V> head;
    private Element<K, V> tail;

    public LRU() {
        maxCapacity = DEFAULT_MAX_CAPACITY;
    }

    public LRU(int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("maxCapacity must greater than zero!");
        }
        this.maxCapacity = maxCapacity;
    }

    public Element<K, V> getHead() {
        return head;
    }

    public Element<K, V> getTail() {
        return tail;
    }

    public V get(K key) {
        Element<K, V> curNode = head;
        Element<K, V> prev = null;
        while (curNode != null) {
            // 命中，将当前节点移至表头
            if (Objects.equals(curNode.key, key)) {
                curNode.next = head;
                // 当前元素的前继节点不为 null，证明当前节点不是头结点，将节点移至表头
                if (prev != null) {
                    prev.next = curNode.next;
                    curNode.next = head;
                    head = curNode;
                }
                return curNode.value;
            }
            prev = curNode;
            curNode = curNode.next;
        }
        return null;
    }

    public V set(K key, V value) {
        Element<K, V> curNode = head;
        Element<K, V> prev = null;
        // 遍历当前链表，判断是否已经存在 key 等价的节点
        while (curNode != null) {
            // 命中，直接更新节点 value 并返回
            if (Objects.equals(curNode.key, key)) {
                V oldValue = curNode.value;
                curNode.value = value;
                // 命中的是尾节点并且当前链表节点数大于 1，则将尾节点引用前移
                if (tail == curNode && head != tail) {
                    tail = tail.prev;
                }
                // 移动当前节点至表头
                if (prev != null) {
                    prev.next = curNode.next;
                    if (curNode.next != null) {
                        curNode.next.prev = prev;
                    }
                    curNode.next = head;
                    curNode.prev = null;
                    head.prev = curNode;
                    head = curNode;
                }
                return oldValue;
            }
            prev = curNode;
            curNode = curNode.next;
        }
        return null;
    }

    public V removeLast() {
        V value = (tail == null ? null : tail.value);
        // 只有一个节点
        if (tail == head) {
            tail = head = null;
        } else if (tail != null) { // 多个节点
            if (tail.prev != null) {
                tail.prev.next = null;
            }
            Element<K, V> tailPrev = tail.prev;
            tail.prev = null;
            tail = tailPrev;
        }
        return value;
    }

    public V add(K key, V value) {
        V oldValue = set(key, value);
        // 命中，直接返回 oldValue
        if (oldValue != null) {
            return oldValue;
        }
        // 没命中，则插入一个新节点
        Element<K, V> newHead = new Element<K, V>(key, value);
        // 容量已满，淘汰尾节点
        if (count >= maxCapacity) {
            oldValue = removeLast();
        } else { // 容量未满，插入节点，节点数加一
            count++;
        }
        // 新节点插入链表头
        insertHead(newHead);
        return oldValue;
    }

    public int size() {
        return count;
    }

    public void clear() {
        count = 0;
        head = tail = null;
        System.gc();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        Element<K, V> curNode = head;
        if (curNode != null) {
            builder.append(curNode.toString());
            curNode = curNode.next;
        }
        while (curNode != null) {
            builder.append(", ").append(curNode.toString());
            curNode = curNode.next;
        }
        return builder.append("]").toString();
    }

    protected void insertHead(Element<K, V> newHead) {
        if (newHead == null) {
            return ;
        }
        if (head != null) {
            newHead.prev = null;
            head.prev = newHead;
        }
        newHead.next = head;
        head = newHead;
        tail = (tail == null ? head : tail);
    }

    /**
     * 描述 LRU 算法中链表元素的类
     * @author StarkZhidian
     * @param <K> 键的类型
     * @param <V> 值的类型
     */
    public static class Element<K, V> {
        K key;
        V value;
        Element<K, V> prev;
        Element<K, V> next;

        Element(K key, V value, Element<K, V> prev, Element<K, V> next) {
            this.key = key;
            this.value = value;
            this.prev = prev;
            this.next = next;
        }

        Element(K key, V value) {
            this(key, value, null, null);
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public String toString() {
            return "[" + key + ", " + value + "]";
        }
    }

    public static void main(String[] args) {
        LRU<Integer, String> lru;
        for (int x = 1; x < 12; x ++) {
            lru = new LRU<Integer, String>(x);
            for (int i = 0; i < 10; i++) {
                lru.add(i, String.valueOf(i));
                System.out.println(lru);
            }
            for (int i = 9; i >= 0; i--) {
                lru.add(i, String.valueOf(i));
                System.out.println(lru);
            }
            lru.clear();
            System.out.println("\n");
        }
    }
}
