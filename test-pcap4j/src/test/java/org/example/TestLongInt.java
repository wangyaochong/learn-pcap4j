package org.example;

public class TestLongInt {
    public static void main(String[] args) {
        long l = 4145588728L;
        int i = (int) l;
        System.out.println(i);
        long tmp=i;
        System.out.println(tmp<<32>>>32);
    }
}
