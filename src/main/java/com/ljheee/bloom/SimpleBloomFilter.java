package com.ljheee.bloom;

import com.google.common.hash.Funnels;
import com.google.common.hash.Hashing;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 利用redis的Bitmap，手写实现布隆过滤器
 */
public class SimpleBloomFilter {


    private JedisPool jedisPool = null;
    private Jedis jedis = null;


    // 预估插入量
    private long expectedInsertions = 20000L;

    // 可接受的错误率
    private double fpp = 0.01F;


    // 评估预算 所需bit数
    private long numBits = optimalNumOfBits(expectedInsertions, fpp);
    private int numHashFunctions = optimalNumOfHashFunctions(expectedInsertions, fpp);


    // 计算 所需bit数
    private long optimalNumOfBits(long n, double p) {
        if (p == 0F) {
            p = Double.MIN_VALUE;
        }
        return (long) (n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    // 计算 所需hash函数个数
    private int optimalNumOfHashFunctions(long n, double m) {
        return Math.max(1, (int) Math.round(m / n * Math.log(2)));
    }


    public SimpleBloomFilter() {
        init("localhost", 6379);
    }

    // 初始化jedis
    private void init(String host, int port) {
        jedisPool = new JedisPool(host, port);
        this.jedis = jedisPool.getResource();
    }

    public SimpleBloomFilter(long expectedInsertions, double fpp) {
        this.expectedInsertions = expectedInsertions;
        this.fpp = fpp;
        numBits = optimalNumOfBits(expectedInsertions, fpp);
        numHashFunctions = optimalNumOfHashFunctions(expectedInsertions, fpp);
        init("localhost", 6379);
    }

    public SimpleBloomFilter(long expectedInsertions, double fpp, String host, int port) {
        this.expectedInsertions = expectedInsertions;
        this.fpp = fpp;
        numBits = optimalNumOfBits(expectedInsertions, fpp);
        numHashFunctions = optimalNumOfHashFunctions(expectedInsertions, fpp);
        init(host, port);
    }

    // put元素
    public void put(String where, String key) throws IOException {
        long[] indexs = getIndexs(key);

        Pipeline pipelined = jedis.pipelined();
        try {
            for (long index : indexs) {
                pipelined.setbit(where, index, true);
            }
            pipelined.sync();
        } finally {
            pipelined.close();
        }
    }

    // 判断元素是否存在
    public boolean isExist(String where, String key) throws IOException {
        long[] indexs = getIndexs(key);
        boolean result;
        Pipeline pipelined = jedis.pipelined();
        try {
            for (long index : indexs) {
                pipelined.getbit(where, index);
            }
            result = !pipelined.syncAndReturnAll().contains(false);
        } finally {
            pipelined.close();
        }
        return result;
    }

    /**
     * 计算key 对应在bit数组中的下标
     * 经过 numHashFunctions 个hash函数计算后，返回numHashFunctions个位置下标
     *
     * @param key
     * @return
     */
    private long[] getIndexs(String key) {
        long hash1 = hash(key);
        long hash2 = hash1 >>> 16;

        long[] result = new long[numHashFunctions];
        for (int i = 0; i < numHashFunctions; i++) {
            long combinedHash = hash1 + i * hash2;
            if (combinedHash < 0) {
                combinedHash = ~combinedHash;
            }
            result[i] = combinedHash % numBits;
        }
        return result;
    }

    // murmur哈希函数
    private long hash(String key) {
        Charset charset = Charset.forName("UTF-8");
        return Hashing.murmur3_128().hashObject(key, Funnels.stringFunnel(charset)).asLong();
    }


}
