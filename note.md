
### 利用redis的Bitmap，手写实现布隆过滤器

#### 原理分析
Redis的Bitmap
redis.set key value  value的存储是二进制。
可以使用setbit key offset 1 设置value对应bit上的0\1值。

Bitmap本质是string,是一串连续的2进制数字(0或1) ，每一位所在的位置为偏移(offset)。
string (Bitmap) 最大长度是512 MB,所以它们可以表示2 ^ 32=4294967296个不同的位。

bloom 过滤器，数组初始化全是0；
每设置一个值，经过三个hash函数计算，分别在数据下标处置为1；


#### SimpleBloomFilter
基于redis的Bitmap，实现布隆过滤器。
使用pipeline优化批量操作。
未实现元素删除功能。