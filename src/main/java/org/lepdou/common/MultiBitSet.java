package org.lepdou.common;

import java.io.*;
import java.util.BitSet;

/**
 *
 * 相比于Java API中的BitSet，MultiBitSet中的一位可以由多位bit表示。
 * ------------------------------------------------------------
 *             BitSet
 * ==> *|*|*|*|*|*|*|*|*|*|*|*
 *              VS
 *          MulitBitSet
 * ==> **..*|**..*|**..*|**..*|**..*|**..*
 * ------------------------------------------------------------
 * 优点：
 *   1.内存占用小
 *   2.解析速度快
 * 适用场景：
 *   1.有界的且数字不重复
 *   2.简单的 [k,v] 表示
 *   3.数据量大
 * ------------------------------------------------------------
 * 例如：
 *    假设有1亿个用户，用户类型包括4种：初中生、大学生、研究生、不是学生。
 *    需求：给定一批 5千万 userId进行分类。
 *    解决方案：
 *      方案一：
 *          每个userId，通过查询数据库判断其类型
 *      方案二：
 *          把userId 映射的 userType 存在hashmap。
 *          （假设userId和userType是int类型共需要越750M内存，
 *              且每次get时需计算hash，解决碰撞消耗CPU）
 *      方案三：
 *          利用MultiBitSet存储关系.2bit表示userType 00->初中生，01->大学生，10->研究生 11->不是学生
 *          假设一个BitSet：
 *          01 | 10 | 11 | 00 | 11 .... | 11 | ...
 *          1     2    3    4    5      88888
 *          则结果为：[userId,UserType] 为 [1,大学生],[2,研究生],[3,不是学生] ....[88888,不是学生]
 *          (共需内存：24MB,且无需计算，通过索引获取值速度更快)
 *
 *      很明显方案一是最笨的，方案二也可以，方案三最优
 *      方案三和方案二都需要初始化，这部分消耗大量的时间，
 *      鉴于此 提供writeObjectToFileSystem写入文件系统，
 *      并且通过initFromInputStream直接初始化对象。
 *      方案三由于文件小速度快，而方案二(750M）需要消耗大量时间。
 *
 *@Author lepdou 15.3.26
 */
public class MultiBitSet implements Serializable {
    public long serialVersionUID = 683651966784235010l;
    /**
     * bitSize个bit代表一个“bit”
     */
    private int bitSize;

    private int maxValuePerBit;
    /**
     * 用一个BitSet表示一维
     * ============================================================
     * 例：
     * ***|***|***|***|***|***|***|***|***|
     * 0   1   2   3  ....
     * =》
     * MultiBitSet[2] = bitSets[2][2] + bitSets[1][2] + bitSets[0][2]
     * *** = bitSets[2,1,0] 在一个Bit里低位用低数组的下标表示
     */
    private BitSet[] bitSets;

    /**
     * 创建指定维度的MultiBitSet
     *
     * @param bitSize
     */
    public MultiBitSet(int bitSize) {
        init(bitSize);
    }

    /**
     * 创建一个MultiBitSet 默认 bitsize = 1
     */
    public MultiBitSet() {
        init(1);
    }

    private void init(int bitSize) {
        if (bitSize <= 0) {
            throw new IllegalArgumentException("bitSize can not be negative:[bitSize=" + bitSize);
        }
        calMaxValuePerBit(bitSize);
        this.bitSize = bitSize;
        bitSets = new BitSet[bitSize];
        for (int i = 0; i < bitSize; i++)
            bitSets[i] = initBitSet(0);
    }

    /**
     * 计算“一位”最大的数值
     *
     * @param bitSize
     */
    private void calMaxValuePerBit(int bitSize) {
        int value = 1;
        for (int i = 0; i < bitSize; i++)
            value <<= 1;
        maxValuePerBit = value;
    }


    private BitSet initBitSet(int nbits) {
        BitSet bitSet = null;
        if (nbits <= 0)
            bitSet = new BitSet();
        else
            bitSet = new BitSet(nbits);
        return bitSet;
    }

    /**
     * 设置第 index位的值。
     *
     * @param index
     * @param value
     * @throws IllegalArgumentException value的值大于 bitsize能够表示的最大的值
     */
    public void set(int index, int value) {
        checkIndex(index);
        checkValue(value);
        Boolean[] bits = calBooleanPerBit(value);
        for (int i = 0; i < bitSize; i++)
            if (bits[i])
                bitSets[i].set(index);
            else
                bitSets[i].clear(index);
    }

    private void checkIndex(int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException("index:" + index);
    }

    private void checkIndex(int fromIndex, int toIndex) {
        if (toIndex < fromIndex || toIndex < 0 | fromIndex < 0)
            throw new IndexOutOfBoundsException("[" +
                    "fromIndex,toIndex,value]=["
                    + fromIndex + "," + toIndex + "]");
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to the specified value.
     *
     * @param fromIndex index of the first bit to be set
     * @param toIndex   index after the last bit to be set
     * @param value     value to set the selected bits to
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    public void set(int fromIndex, int toIndex, int value) {
        checkIndex(fromIndex, toIndex);
        if (value < 0)
            throw new IllegalArgumentException("value cant be negative value:" + value);
        for (int i = fromIndex; i <= toIndex; i++)
            set(i, value);
    }

    public void set(int value, int... indexs) {
        checkValue(value);
        if (indexs != null || indexs.length > 0)
            for (int index : indexs)
                set(index, value);
    }

    /**
     * Sets the bit specified by the index to {@code false}.
     *
     * @param index the index of the bit to be cleared
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    public void clear(int index) {
        set(index, 0);
    }

    public void clear(int fromIndex, int toIndex) {
        set(fromIndex, toIndex, 0);
    }

    public void clear(int... indexs) {
        set(0, indexs);
    }

    /**
     * 解析value，得到每一位的 0|1
     *
     * @param value
     * @return
     */
    private Boolean[] calBooleanPerBit(int value) {
        Boolean[] sets = new Boolean[bitSize];
        int k = 1;
        for (int i = bitSize - 1; i >= 0; i--) {
            sets[i] = (value & k) >= 1;
            k <<= 1;
        }
        return sets;
    }

    /**
     * Returns the value of the bit with the specified index. The value
     * is {@code true} if the bit with the index {@code bitIndex}
     * is currently set in this {@code BitSet}; otherwise, the result
     * is {@code false}.
     *
     * @param index the bit index
     * @return the value of the bit with the specified index
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    public int get(int index) {
        checkIndex(index);
        Boolean[] bits = generateBits(index);
        return calValue(bits);
    }

    /**
     * Returns a new {@code BitSet} composed of bits from this {@code BitSet}
     * from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     *
     * @param fromIndex index of the first bit to include
     * @param toIndex   index after the last bit to include
     * @return a new {@code BitSet} from a range of this {@code BitSet}
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    public MultiBitSet get(int fromIndex, int toIndex) {
        checkIndex(fromIndex, toIndex);
        MultiBitSet subBitSet = new MultiBitSet(this.bitSize);
        BitSet[] copyedBitSet = new BitSet[this.bitSize];
        for (int i = 0; i < bitSize; i++)
            copyedBitSet[i] = bitSets[i].get(fromIndex, toIndex);
        subBitSet.bitSets = copyedBitSet;
        return subBitSet;
    }


    /**
     * 根据索引计算中间变量 Bits
     * 1101 = [true,true,false,true]
     *
     * @param index
     * @return
     */
    private Boolean[] generateBits(int index) {
        Boolean[] bits = new Boolean[bitSize];
        for (int i = bitSize - 1; i >= 0; i--)
            bits[i] = bitSets[i].get(index);
        return bits;
    }

    /**
     * [true,true,false,true] = 1101 = 13
     *
     * @param bits
     * @return
     */
    private int calValue(Boolean[] bits) {
        int k = 1;
        int value = 0;
        for (int i = bits.length - 1; i >= 0; i--) {
            if (bits[i]) {
                value += k;
            }
            k <<= 1;
        }
        return value;
    }

    private void checkValue(int value) {
        if (value < 0 || value > maxValuePerBit)
            throw new IllegalArgumentException("value = " + value);
    }

    /**
     * Returns the number of bits set to {@code true} in this {@code BitSet}.
     *
     * @return the number of bits set to {@code true} in this {@code BitSet}
     */
    public int cardinality() {
        int maxCardinality = 0;
        for (BitSet bitset : bitSets)
            maxCardinality = Math.max(maxCardinality, bitset.cardinality());
        return maxCardinality;
    }

    /**
     * Returns the "logical size" of this {@code BitSet}: the index of
     * the highest set bit in the {@code BitSet} plus one. Returns zero
     * if the {@code BitSet} contains no set bits.
     *
     * @return the logical size of this {@code BitSet}
     */
    public int length() {
        int maxLength = 0;
        for (BitSet bitset : bitSets)
            maxLength = Math.max(maxLength, bitset.length());
        return maxLength;
    }

    /**
     * Returns the index of the first bit that is set to {@code true}
     * that occurs on or after the specified starting index. If no such
     * bit exists then {@code -1} is returned.
     * <p/>
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     * <p/>
     * <pre> {@code
     * for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
     *     // operate on index i here
     * }}</pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the next set bit, or {@code -1} if there
     * is no such bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    public int nextSetBit(int fromIndex) {
        checkIndex(fromIndex);
        int nextSetBit = Integer.MAX_VALUE;
        int temp = -1;
        for (BitSet bitset : bitSets) {
            temp = bitset.nextSetBit(fromIndex);
            if (temp == fromIndex) {
                nextSetBit = temp;
                break;
            }
            if (temp > 0)
                nextSetBit = temp < nextSetBit ? temp : nextSetBit;
        }
        return nextSetBit == Integer.MAX_VALUE ? -1 : nextSetBit;
    }

    /**
     * Returns the index of the first bit that is set to {@code false}
     * that occurs on or after the specified starting index.
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the next clear bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    public int nextClearBit(int fromIndex) {
        checkIndex(fromIndex);
        int nextClearBit = -1;
        int maxClearBitIndex = -1;
        //获取最高位1
        for (BitSet bitset : bitSets)
            maxClearBitIndex = Math.max(maxClearBitIndex, bitset.nextClearBit(fromIndex));
        int len = length();
        for (int i = maxClearBitIndex; i < len; i++) {
            boolean hasFound = true;
            for (BitSet bitset : bitSets)
                if (bitset.get(i)) {
                    hasFound = false;
                    break;
                }
            if (hasFound) {
                nextClearBit = i;
                break;
            }
        }
        return nextClearBit;
    }

    /**
     * Returns the index of the nearest bit that is set to {@code false}
     * that occurs on or before the specified starting index.
     * If no such bit exists, or if {@code -1} is given as the
     * starting index, then {@code -1} is returned.
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the previous clear bit, or {@code -1} if there
     * is no such bit
     * @throws IndexOutOfBoundsException if the specified index is less
     *                                   than {@code -1}
     */
    public int previousClearBit(int fromIndex) {
        checkIndex(fromIndex);
        int previousClearBit = -1;
        int minClearBitIndex = Integer.MAX_VALUE;
        //获取最低位0
        for (BitSet bitset : bitSets)
            minClearBitIndex = Math.min(minClearBitIndex, bitset.previousClearBit(fromIndex));
        for (int i = minClearBitIndex; i >= 0; i++) {
            boolean hasFound = true;
            for (BitSet bitset : bitSets)
                if (bitset.get(i)) {
                    hasFound = false;
                    break;
                }
            if (hasFound) {
                previousClearBit = i;
                break;
            }
        }
        return previousClearBit;

    }

    /**
     * Returns the index of the nearest bit that is set to {@code true}
     * that occurs on or before the specified starting index.
     * If no such bit exists, or if {@code -1} is given as the
     * starting index, then {@code -1} is returned.
     * <p/>
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     * <p/>
     * <pre> {@code
     * for (int i = bs.length(); (i = bs.previousSetBit(i-1)) >= 0; ) {
     *     // operate on index i here
     * }}</pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the previous set bit, or {@code -1} if there
     * is no such bit
     * @throws IndexOutOfBoundsException if the specified index is less
     *                                   than {@code -1}
     */
    public int previousSetBit(int fromIndex) {
        checkIndex(fromIndex);
        int previousSetBit = -1;
        int temp = -1;
        for (BitSet bitset : bitSets) {
            temp = bitset.previousSetBit(fromIndex);
            if (temp == fromIndex) {
                previousSetBit = temp;
                break;
            }
            if (temp > 0)
                previousSetBit = temp > previousSetBit ? temp : previousSetBit;
        }
        return previousSetBit;
    }

    /**
     * Returns the number of bits of space actually in use by this
     * {@code BitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the number of bits currently in this bit set
     */
    public int size() {
        int maxSize = -1;
        for (BitSet bitset : bitSets)
            maxSize += bitset.size();
        return maxSize;
    }

    public int hashCode() {
        long hs = 0l;
        for (int i = 0; i < bitSize; i++)
            hs += bitSets[i].hashCode();
        return (int) ((hs >> 32) ^ hs);
    }

    public boolean equal(Object o) {
        if (!(o instanceof MultiBitSet))
            return false;
        MultiBitSet set = (MultiBitSet) o;
        if (this.bitSize != set.bitSize || set.bitSets.length != bitSize)
            return false;
        for (int i = 0; i < bitSize; i++)
            if (!this.bitSets[i].equals(set.bitSets[i]))
                return false;
        return true;

    }

    /**
     * @param out
     * @throws IOException
     */
    public void writeObject(OutputStream out) throws IOException {
        if(out == null)
            throw new NullPointerException();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);
        objectOutputStream.writeObject(this);
    }

    /**
     * 把对象存储在文件系统中，以便于下次初始化
     * @param path
     * @throws IOException
     */
    public void writeObjectToFileSystem(String path) throws IOException {
        if (path == null)
            throw new NullPointerException();
        FileOutputStream o = new FileOutputStream(path);
        writeObject(o);

    }

    /**
     * Reconstitute the {@code BitSet} instance from a stream (i.e.,
     * deserialize it).
     */
    public static MultiBitSet initFromInputStream(InputStream in)
            throws IOException, ClassNotFoundException {
        if (in == null)
            throw new NullPointerException();
        ObjectInputStream ois = new ObjectInputStream(in);
        Object o = ois.readObject();
        if (o != null)
            return (MultiBitSet) o;
        else
            return null;
    }

}
