package org.example;

import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;

import java.io.EOFException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Hello world!
 */
public class Test01GetOnePacket {
    public static void main(String[] args) throws UnknownHostException, PcapNativeException, NotOpenException, EOFException, TimeoutException {
        InetAddress addr = InetAddress.getByName("192.168.0.106");
        PcapNetworkInterface nif = Pcaps.getDevByAddress(addr);
        System.out.println(nif);

        int snapLen = 65536;
        PcapNetworkInterface.PromiscuousMode mode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS;
        int timeout = 1000;
        PcapHandle handle = nif.openLive(snapLen, mode, timeout);

        Packet packet = handle.getNextPacketEx();
        handle.close();

        IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
        byte[] rawData = ipV4Packet.getRawData();
        System.out.println(new String(rawData, StandardCharsets.UTF_8));
        System.out.println(ipV4Packet);
        Inet4Address srcAddr = ipV4Packet.getHeader().getSrcAddr();

        System.out.println(srcAddr);
    }
}
