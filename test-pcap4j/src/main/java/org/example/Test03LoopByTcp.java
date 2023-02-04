package org.example;

import org.example.util.PacketUtil;
import org.pcap4j.core.*;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.TcpPort;
import org.pcap4j.util.MacAddress;
import org.pcap4j.util.NifSelector;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Test03LoopByTcp {
    //    static InetAddress addr2 = null;
//    static PcapNetworkInterface nifSend = null;
//    static PcapHandle sendHandle = null;
//    static Inet4Address addr = null;
//    static MacAddress macAddr = null;
    static short clientPort = 11117;
    static short serverPort = 8090;

    static {
        try {
            InetAddress addr2 = InetAddress.getByName("192.168.0.106");
            PcapNetworkInterface nifSend = Pcaps.getDevByAddress(addr2);
            PcapHandle sendHandle = nifSend.openLive(0, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 0);
            Inet4Address addr = (Inet4Address) Inet4Address.getByName("192.168.0.106");
            MacAddress macAddr = MacAddress.getByName("10-3D-1C-98-AE-D6");
        } catch (UnknownHostException | PcapNativeException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws PcapNativeException, NotOpenException, IOException, InterruptedException, TimeoutException {
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
        LinkedBlockingQueue<TcpPacket> toSendQueue = new LinkedBlockingQueue<>();
        System.out.println(nif.getName() + "(" + nif.getDescription() + ")");

//        final PcapHandle handle = nif.openLive(0, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 0);
        PcapHandle handle = new PcapHandle.Builder(nif.getName())
//                .direction(PcapHandle.PcapDirection.INOUT)
                .immediateMode(true)
                .timeoutMillis(0)
                .promiscuousMode(PcapNetworkInterface.PromiscuousMode.PROMISCUOUS)
                .build();
//                and tcp src port 9999
        handle.setFilter("tcp dst port 8080", BpfProgram.BpfCompileMode.OPTIMIZE);
        new Thread(() -> {
//            handle.getNextPacketEx()
            try {
                handle.loop(0, (PacketListener) packet -> {
                    if (packet.contains(TcpPacket.class)) {
                        TcpPacket tcpPacket = packet.get(TcpPacket.class);
                        // tcpPacket.getHeader().getOptions()
                        Packet payload = tcpPacket.getPayload();
                        if (payload != null) {
                            if (payload.getRawData().length == 1) {
                                System.out.println("长度为1的包");
                            }
                            if (payload.getRawData().length > 1) {
                                toSendQueue.add(tcpPacket);
                            }

                        }
                    } else {
                        System.out.println("not tcp packet");
                    }
                }, Executors.newCachedThreadPool());
            } catch (PcapNativeException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (NotOpenException e) {
                throw new RuntimeException(e);
            }
        }).start();


        InetAddress addr2 = InetAddress.getByName("192.168.0.106");
        PcapNetworkInterface nifSend = Pcaps.getDevByAddress(addr2);
        PcapHandle sendHandle = nifSend.openLive(0, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 0);
        Inet4Address addr = (Inet4Address) Inet4Address.getByName("192.168.0.106");
        MacAddress macAddr = MacAddress.getByName("10-3D-1C-98-AE-D6");

        AtomicLong atomicSequenceNumber = new AtomicLong(-1);
        AtomicLong atomicAcknowledgmentNumber = new AtomicLong(-1);


        AtomicBoolean isFirstTime = new AtomicBoolean(true);
        new Thread(() -> {
            try {

                PcapHandle handle2 = new PcapHandle.Builder(nif.getName())
//                .direction(PcapHandle.PcapDirection.INOUT)
                        .immediateMode(true)
                        .promiscuousMode(PcapNetworkInterface.PromiscuousMode.PROMISCUOUS)
//                .bufferSize(4096)
                        .timeoutMillis(1000 * 60 * 60 * 24 * 20)//超时时间是20天
                        .build();
//                and tcp src port 9999
                handle2.setFilter("tcp port 8090", BpfProgram.BpfCompileMode.OPTIMIZE);
                handle2.loop(0, (PacketListener) packet -> {
                    if (packet.contains(TcpPacket.class)) {
                        TcpPacket tcpPacket = packet.get(TcpPacket.class);
                        TcpPacket.TcpHeader header = tcpPacket.getHeader();
                        TcpPort tcpSrcPort = header.getSrcPort();
                        TcpPort tcpDstPort = header.getDstPort();

                        if ((tcpSrcPort.value() != clientPort || tcpDstPort.value() != serverPort) && (tcpSrcPort.value() != serverPort || tcpDstPort.value() != clientPort)) {
                            return;
                        }

                        System.out.println(tcpPacket);

                        boolean fromClientPacket = tcpSrcPort.value() == clientPort;
                        if (isFirstTime.get() && fromClientPacket) {
                            if (header.getAck() && !header.getPsh() && !header.getSyn()) {//ack之后，就可以开始发送数据了
                                //因为是32为溢出了，肉眼不好看，所以需要移位
                                long sequenceNumberAsLong = tcpPacket.getHeader().getSequenceNumberAsLong();
                                long acknowledgmentNumberAsLong = tcpPacket.getHeader().getAcknowledgmentNumberAsLong();
                                System.out.println("sequenceNumber=" + sequenceNumberAsLong + ",acknowledgmentNumber=" + acknowledgmentNumberAsLong);
                                atomicSequenceNumber.set(sequenceNumberAsLong);//放到队列里面，没有就阻塞了
                                atomicAcknowledgmentNumber.set(acknowledgmentNumberAsLong);
                                isFirstTime.set(false);
                            }
                        }
                        boolean fromServerPacket = tcpSrcPort.value() == serverPort;
                        if (!isFirstTime.get() && fromServerPacket) {
                            if (header.getAck() && !header.getPsh()) {//ack之后，就可以开始发送数据了
                                //因为是32为溢出了，肉眼不好看，所以需要移位
                                long sequenceNumberAsLong = tcpPacket.getHeader().getSequenceNumberAsLong();
                                long acknowledgmentNumberAsLong = tcpPacket.getHeader().getAcknowledgmentNumberAsLong();
                                System.out.println("sequenceNumber2=" + sequenceNumberAsLong + ",acknowledgmentNumber2=" + acknowledgmentNumberAsLong);
                                atomicSequenceNumber.set(acknowledgmentNumberAsLong);//放到队列里面，没有就阻塞了
                                atomicAcknowledgmentNumber.set(sequenceNumberAsLong);
                                //如果有psh标记，需要返回ack

                                System.out.println("got server ack");
                            } else if (header.getAck() && header.getPsh()) {//如果服务端有响应，则需要发送ack
                                System.out.println("got server ack and psh");
                                long sequenceNumberAsLong2 = tcpPacket.getHeader().getSequenceNumberAsLong();
                                long acknowledgmentNumberAsLong2 = tcpPacket.getHeader().getAcknowledgmentNumberAsLong();

                                atomicSequenceNumber.set(acknowledgmentNumberAsLong2);//放到队列里面，没有就阻塞了
                                atomicAcknowledgmentNumber.set(sequenceNumberAsLong2 + tcpPacket.getPayload().getRawData().length);

                                System.out.println("seq=" + atomicSequenceNumber + ",ack=" + atomicAcknowledgmentNumber);
                                sendAck(sendHandle,addr,macAddr,clientPort, serverPort, atomicSequenceNumber.get(), atomicAcknowledgmentNumber.get());
                            }
                            if (header.getFin()) {

                            }
                        }

                    }
                });

            } catch (PcapNativeException | NotOpenException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        Thread.sleep(3000);
        Socket socket = new Socket();
        socket.bind(new InetSocketAddress(clientPort));//指定高端端口
        socket.connect(new InetSocketAddress("192.168.0.106", serverPort));
        if (socket.isConnected()) {
            System.out.println("网络连接成功");
        }


        new Thread(() -> {
            while (true) {
                TcpPacket take = null;
                try {
                    take = toSendQueue.take();
                    while (atomicSequenceNumber.get() == -1L) {
                        Thread.sleep(10);
                    }
                    Long sequenceNumber = atomicSequenceNumber.get();
                    Long acknowledgmentNumber = atomicAcknowledgmentNumber.get();
                    sendData(sendHandle, addr, macAddr, take.getPayload().getRawData(), clientPort, serverPort, sequenceNumber, acknowledgmentNumber);
                    atomicAcknowledgmentNumber.set(-1);
                    atomicSequenceNumber.set(-1);

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();


    }

    static void sendData(PcapHandle sendHandle, Inet4Address addr, MacAddress macAddr, byte[] data, short srcPort, short dstPort, Long sequenceNumber, Long acknowledgmentNumber) {
        TcpPacket.Builder tcpBuilder = PacketUtil.TcpPacketBuilder_ACKPSH(addr, addr, srcPort, dstPort, sequenceNumber, acknowledgmentNumber, data);
        IpV4Packet.Builder ipV4PacketBuilder = PacketUtil.IpV4Packet(tcpBuilder, addr, addr);
        EthernetPacket.Builder ethernetPacketBuilder = PacketUtil.EthernetPacket(ipV4PacketBuilder, macAddr, macAddr);
        try {
            sendHandle.sendPacket(ethernetPacketBuilder.build());
        } catch (PcapNativeException | NotOpenException e) {
            throw new RuntimeException(e);
        }
    }

    static void sendAck(PcapHandle sendHandle, Inet4Address addr, MacAddress macAddr, short srcPort, short dstPort, Long sequenceNumber, Long acknowledgmentNumber) {
        TcpPacket.Builder tcpBuilder = PacketUtil.TcpPacketBuilder_ACK(addr, addr, srcPort, dstPort, sequenceNumber, acknowledgmentNumber);
        IpV4Packet.Builder ipV4PacketBuilder = PacketUtil.IpV4Packet(tcpBuilder, addr, addr);
        EthernetPacket.Builder ethernetPacketBuilder = PacketUtil.EthernetPacket(ipV4PacketBuilder, macAddr, macAddr);
        try {
            sendHandle.sendPacket(ethernetPacketBuilder.build());
        } catch (PcapNativeException | NotOpenException e) {
            throw new RuntimeException(e);
        }
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


}
