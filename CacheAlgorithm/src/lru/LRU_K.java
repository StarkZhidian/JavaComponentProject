package lru;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Create by StarkZhidian on 2018/10/30
 *
 * 解决 LRU-1 时可能出现的数据污染问题
 * 在遍历 LRU 缓存表时，不要使用：
 * for (Object key : lru_k.keySet()) {
 *     System.out.println(key + ", " + lru_k.get(key));
 * }
 * 的遍历方式，原因有以下两点：
 * 1、这个方法基于 LinkedHashMap 提供的 LinkedKeyIterator 迭代器方式遍历， LinkedKeyIterator 提供了 expectedModCount 属性，
 * 而 lru_k.get(key) 方法会修改 modCount 的值，在通过 LinkedKeyIterator 的 nextNode() 方法获取下一个遍历的节点时，
 * 会比较 modCount 和 expectedModCount 的值，如果不一致则抛出 ConcurrentModificationException 异常，
 * 而 lru_k.get 方法只会更改 modCount 的值，不会更新迭代器中的 expectedModCount 值（事实上也没办法更新）；
 *
 * 2、 lru_k 设置了父类 LinkedHashMap 的 accessOrder 值为 true，则每次通过 get 方法取元素的时候，
 * 都会将得到的元素（如果不为 null）移至链表末尾，即更改了 LinkedHashMap 中元素的顺序，
 * 那么之后再通过迭代器对象获取下一个要遍历的节点的时候，可能会发生未知错误（元素重复遍历并且永远不会结束），
 * 具体请参考 LinkedHashMap 的相关源码。
 * 基于上述，建议使用 lru_k.entrySet() 方法来先得到整个键值对的集合迭代器，再进行键值遍历
 */
public class LRU_K<K, V> extends LinkedHashMap<K, V> {

    private static final int DEFAULT_K = 2;
    private static final int DEFAULT_MAX_CAPACITY = 32;
    private static final int DEFAULT_CACHE_QUEUE_MAX_CAPACITY = 32;
    private static final float DEFAULT_LOAD_FACTOR = 0.75F;
    private int maxCapacity;
    protected CacheQueue cacheQueue;

    /**
     * 储存每一个键值对出现次数信息的缓存队列，必要的时候将键值对元素存入 LRU 队列
     */
    class CacheQueue extends LinkedHashMap<Map.Entry<K, V>, Integer> {

        protected int k;
        protected int maxCapacity;

        public CacheQueue(int maxCapacity, int k) {
            super(maxCapacity, DEFAULT_LOAD_FACTOR, true);
            this.maxCapacity = maxCapacity;
            this.k = k;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Map.Entry<K, V>, Integer> eldest) {
            return size() > maxCapacity;
        }

        public Integer add(Map.Entry<K, V> element) {
            Integer curTime = get(element);
            return put(element, curTime == null ? 1 : curTime + 1);
        }

        @Override
        public Integer put(Map.Entry<K, V> element, Integer value) {
            if (value >= k) {
                addToLRU(element);
            } else {
                super.put(element, value);
            }
            return value;
        }

        protected void addToLRU(Map.Entry<K, V> element) {
            remove(element);
            LRU_K.this.put(element.getKey(), element.getValue());
        }

    }

    public LRU_K() {
        this(DEFAULT_MAX_CAPACITY, DEFAULT_K, DEFAULT_CACHE_QUEUE_MAX_CAPACITY);
    }

    public LRU_K(int maxCapacity, int k, int cacheQueueCapacity) {
        super(maxCapacity, DEFAULT_LOAD_FACTOR, true);
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("maxCapacity must greater than zero!");
        }
        this.maxCapacity = maxCapacity;
        cacheQueue = new CacheQueue(cacheQueueCapacity, k);
    }

    // 插入元素之后如果现有元素个数大于允许最大元素数，移除最老的元素
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;
    }

    public void add(K key, V value) {
        cacheQueue.add(new Element<>(key, value));
    }

    public void setCacheThreshold(int threshold) {
        cacheQueue.k = threshold;
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + cacheQueue.toString();
    }

    public static class Element<K, V> implements Map.Entry<K, V> {
        K key;
        V value;

        Element(K key, V value) {
            this.key = key;
            this.value = value;
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
        public String toString() {
            return "[" + key + ", " + value + "]";
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
        public int hashCode() {
            int h;
            return ((key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)) +
                    ((value == null) ? 0 : (h = value.hashCode()) ^ (h >>> 16));
        }
    }

    public static void main(String[] args) {
        LRU_K<Integer, String> lru_k = new LRU_K<Integer, String>(3, 3, 9);
        for (int i = 0; i < 10; i++) {
            lru_k.add(i, String.valueOf(i));
        }
        for (int i = 9; i >= 0; i--) {
            lru_k.add(i, String.valueOf(i));
        }
        for (int i = 8; i >= 0; i--) {
            lru_k.add(i, String.valueOf(i));
        }
        // !! 不建议采用的遍历元素方式
//        for (Object key : lru_k.keySet()) {
//            System.out.println(key + ", " + lru_k.get(key));
//        }
        // 建议采用的元素遍历方式
//        for (Map.Entry entry : lru_k.entrySet()) {
//            System.out.println(entry.getKey() + ", " + entry.getValue());
//        }
        System.out.println(lru_k);
    }
}
