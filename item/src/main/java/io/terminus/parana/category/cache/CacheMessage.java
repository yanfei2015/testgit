/*
 *
 *  * Copyright (c) 2014 杭州端点网络科技有限公司
 *
 */

package io.terminus.parana.category.cache;

import com.google.common.base.Objects;

/**
 * Author: haolin
 * On: 11/28/14
 */
public enum CacheMessage {

    SYNC_FRONTEND(1, "前台类目同步消息"),
    SYNC_BACKEND(2, "后台类目同步消息");

    private int value;

    private String desc;

    private CacheMessage(int value, String desc){
        this.value = value;
        this.desc = desc;
    }

    public static CacheMessage from(int value) {
        for (CacheMessage status : CacheMessage.values()) {
            if (Objects.equal(status.value, value)) {
                return status;
            }
        }
        return null;
    }

    public static CacheMessage from(byte[] value) {
        return from(toInt(value));
    }

    public String desc(){
        return this.desc;
    }

    public static int toInt(byte[] b) {
        int mask = 0xff;
        int temp;
        int n = 0;
        for(int i=0; i<4; i++){
            n <<= 8;
            temp = b[i] & mask;
            n |= temp;
        }
        return n;
    }

    public static byte[] toBytes(CacheMessage m) {
        int value = m.value;
        return new byte[] {
                (byte) ((value >> 24) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }
}
