package org.example;

import org.example.util.PacketUtil;
import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;
import org.pcap4j.util.MacAddress;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Test04TcpPacketCreate {
    public static void main(String[] args) throws IllegalRawDataException, UnknownHostException, PcapNativeException, NotOpenException {

        Inet4Address addr = (Inet4Address) Inet4Address.getByName("192.168.0.106");
        TcpPacket.Builder tcpBuilder = PacketUtil.TcpPacketBuilder_ACKPSH(addr, addr, (short) 13177, (short) 9090, 8210459L, 4273856588L, "hello tcp".getBytes(StandardCharsets.UTF_8));
        IpV4Packet.Builder ipV4PacketBuilder = PacketUtil.IpV4Packet(tcpBuilder, addr, addr);
        MacAddress macAddr = MacAddress.getByName("10-3D-1C-98-AE-D6");
        EthernetPacket.Builder builders = PacketUtil.EthernetPacket(ipV4PacketBuilder, macAddr, macAddr);
        EthernetPacket build = builders.build();


        InetAddress addr2 = InetAddress.getByName("192.168.0.106");
        PcapNetworkInterface nif = Pcaps.getDevByAddress(addr2);
        System.out.println(nif);
        PcapHandle sendHandle = nif.openLive(0, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 0);
//        sendHandle.sendPacket(ipV4Packet);
        sendHandle.sendPacket(build);
    }
}
