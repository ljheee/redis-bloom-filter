package com.ljheee.bloom;

import java.io.IOException;

/**
 * Created by lijianhua04 on 2019/7/24.
 */
public class Main {


    public static void main(String[] args) throws IOException {
        SimpleBloomFilter filter = new SimpleBloomFilter();


        int ex_count = 0;
        int ne_count = 0;

        for (int i = 0; i < 15000; i++) {
            filter.put("bf:filter", 100 + i + "");

        }


        for (int i = 0; i < 15000; i++) {
            boolean exist = filter.isExist("bf:filter", 100 + i + "");
            if (exist) {
                ex_count++;
            } else {
                ne_count++;
            }
        }
        System.out.println(ex_count + "------" + ne_count);
    }


}
