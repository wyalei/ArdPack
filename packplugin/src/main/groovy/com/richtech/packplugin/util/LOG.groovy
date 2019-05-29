package com.richtech.packplugin.util;

/**
 * -------------------------------------
 * author      : wangyalei
 * time        : 2019-05-29
 * description :
 * history     :
 * -------------------------------------
 */
public class LOG {
    public static void log(String msg){
        println("pack2>>>: " + msg)
    }

    public static void info(String msg){
        println("pack2>>>: " + msg)
    }

    public static void info(String tag, String msg){
        println(tag + ">>>: " + msg)
    }
}
