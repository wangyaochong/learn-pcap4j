package org.example;

import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.util.NifSelector;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Comparator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TestLoopBySocket {
    public static void main(String[] args) throws PcapNativeException, NotOpenException, IOException, InterruptedException {

        PcapNetworkInterface nif;
        try {
            nif = new NifSelector().selectNetworkInterface(); // 这个方法提供用户输入网卡的序号
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (nif == null) {
            return;
        }
        System.out.println(nif.getName() + "(" + nif.getDescription() + ")");

        final PcapHandle handle = nif.openLive(0, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 0);
        final Socket socket = new Socket("127.0.0.1", 8090);
        if (socket.isConnected()) {
            System.out.println("网络连接成功");
        }
        final AtomicInteger total = new AtomicInteger(0);
        final Vector<TcpPacket> tcpPackets = new Vector<>();
        final int packetCacheSize = 256;

        ReentrantLock lock = new ReentrantLock();
        AtomicLong timeStamp = new AtomicLong(System.currentTimeMillis());
        new Thread(() -> {
            while (true) {
                if (timeStamp.get() + 60000 < System.currentTimeMillis()) {
                    System.out.println("超时时间到达，清空缓存");
                    try {
                        sendPacket(tcpPackets, total, socket, packetCacheSize);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    InputStream inputStream = null;
                    inputStream = socket.getInputStream();
                    int available = inputStream.available();
                    if (available > 0) {
                        byte[] bytes = new byte[available];
                        int read = inputStream.read(bytes);
                        System.out.println("------------------------response=" + new String(bytes));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        });
        thread.start();

        PacketListener listener =
                new PacketListener() {
                    @Override
                    public void gotPacket(Packet packet) {
                        lock.lock();
                        try {
                            timeStamp.set(System.currentTimeMillis());
                            if (packet.contains(TcpPacket.class)) {
                                TcpPacket tcpPacket = packet.get(TcpPacket.class);
                               // tcpPacket.getHeader().getOptions()
                                Packet payload = tcpPacket.getPayload();
                                if (payload != null) {
                                    if (payload.getRawData().length == 1) {
                                        System.out.println("长度为1的包");
                                    }
                                    if (payload.getRawData().length > 1) {
                                        tcpPackets.add(tcpPacket);
                                        total.addAndGet(tcpPacket.getPayload().getRawData().length);
                                        System.out.println("packet received, total size=" + total.get());
                                        if (total.get() >= packetCacheSize) {
                                            sendPacket(tcpPackets, total, socket, packetCacheSize);
                                        }
                                    }

                                }
                            } else {
                                System.out.println("not tcp packet");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                    }
                };

        ExecutorService pool = Executors.newCachedThreadPool();
        handle.setFilter("tcp dst port 8080", BpfProgram.BpfCompileMode.OPTIMIZE);
        handle.loop(0, listener, pool); // This is better than handle.loop(5, listener);
        pool.shutdown();
        handle.close();


    }

    static byte[] mergeByte(byte[]... bytes) {
        int length = 0;
        for (byte[] aByte : bytes) {
            length += aByte.length;
        }
        byte[] result = new byte[length];
        int index = 0;
        for (byte[] aByte : bytes) {
            System.arraycopy(aByte, 0, result, index, aByte.length);
            index += aByte.length;
        }
        return result;
    }

    private synchronized static void sendPacket(Vector<TcpPacket> tcpPackets, AtomicInteger total, Socket socket, int packetCache) throws IOException {
        System.out.println("start to send package size=" + total.get());
        tcpPackets.sort(Comparator.comparingInt(o -> o.getHeader().getSequenceNumber()));//重新排序
        double v = ((double) packetCache) / 3.0;
        System.out.println("threshold=" + v);
        byte[] toSend = new byte[0];
        while (toSend.length < 128 && total.get() > 128) {
            TcpPacket remove = tcpPackets.remove(0);
            toSend = mergeByte(toSend, remove.getPayload().getRawData());
        }
        System.out.println("one send size=" + toSend.length);
        System.out.println("content=" + new String(toSend));
        socket.getOutputStream().write(toSend);
        socket.getOutputStream().flush();
        total.addAndGet(-toSend.length);
        System.out.println("send finished package size=" + total.get());
    }
}
