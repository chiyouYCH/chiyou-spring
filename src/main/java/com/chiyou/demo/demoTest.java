package com.chiyou.demo;

import java.io.File;

public class demoTest {

    public static void main(String[] args) {
        File file = new File("E:\\chiyou\\Downloads\\apache-maven-3.6.3-bin.zip*\\conf");
        for (File listFile : file.listFiles()) {
            System.out.println(listFile.getName());
        }
    }
}
