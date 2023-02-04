package org.example.util;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;
import org.pcap4j.packet.namednumber.TcpPort;
import org.pcap4j.util.MacAddress;

import java.net.Inet4Address;
import java.util.ArrayList;

public class PacketUtil {
    public static void main(String[] args) {
        System.out.println(Short.MAX_VALUE);
        int x = 65535 * 1024;
        System.out.println((short)x);
    }

    public static TcpPacket.Builder TcpPacketBuilder_ACK(Inet4Address srcAddr, Inet4Address dstAddr, short srcPort, short dstPort, long sequenceNumber, long acknowledgmentNumber) {
        TcpPacket.Builder tcpBuilder = new TcpPacket.Builder();
        TcpMaximumSegmentSizeOption mss = new TcpMaximumSegmentSizeOption.Builder().maxSegSize((short) 65495)
                .correctLengthAtBuild(true)
                .build();
        TcpWindowScaleOption ws = new TcpWindowScaleOption.Builder().length((byte) 3).shiftCount((byte) 8).correctLengthAtBuild(true).build();
        ArrayList<TcpPacket.TcpOption> options = new ArrayList<>();
        options.add(mss);
        options.add(TcpNoOperationOption.getInstance());
        options.add(ws);
        options.add(TcpNoOperationOption.getInstance());
        options.add(TcpNoOperationOption.getInstance());
        options.add(TcpSackPermittedOption.getInstance());
        tcpBuilder//.payloadBuilder(new UnknownPacket.Builder().rawData(data))
                .srcAddr(srcAddr)
                .dstAddr(dstAddr)
                .ack(true)
                .sequenceNumber((int) sequenceNumber)
                .acknowledgmentNumber((int) acknowledgmentNumber)
                .window((short) (65535))
               // .options(options)
                .srcPort(TcpPort.getInstance(srcPort))
                .dstPort(TcpPort.getInstance(dstPort))
                .paddingAtBuild(true)
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true);
        return tcpBuilder;
    }
    public static TcpPacket.Builder TcpPacketBuilder_ACKPSH(Inet4Address srcAddr, Inet4Address dstAddr, short srcPort, short dstPort, long sequenceNumber, long acknowledgmentNumber, byte[] data) {
        TcpPacket.Builder tcpBuilder = new TcpPacket.Builder();
        TcpMaximumSegmentSizeOption mss = new TcpMaximumSegmentSizeOption.Builder().maxSegSize((short) 65495)
                .correctLengthAtBuild(true)
                .build();
        TcpWindowScaleOption ws = new TcpWindowScaleOption.Builder().length((byte) 3).shiftCount((byte) 8).correctLengthAtBuild(true).build();
        ArrayList<TcpPacket.TcpOption> options = new ArrayList<>();
        options.add(mss);
        options.add(TcpNoOperationOption.getInstance());
        options.add(ws);
        options.add(TcpNoOperationOption.getInstance());
        options.add(TcpNoOperationOption.getInstance());
        options.add(TcpSackPermittedOption.getInstance());

        tcpBuilder.payloadBuilder(new UnknownPacket.Builder().rawData(data))
                .srcAddr(srcAddr)
                .dstAddr(dstAddr)
                .psh(true)
                .ack(true)
                .sequenceNumber((int) sequenceNumber)
                .acknowledgmentNumber((int) acknowledgmentNumber)
                //.options(options)
                .srcPort(TcpPort.getInstance(srcPort))
                .dstPort(TcpPort.getInstance(dstPort))
                .window((short) 65535)
                .paddingAtBuild(true)
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true);
        return tcpBuilder;
    }

    public static IpV4Packet.Builder IpV4Packet(Packet.Builder payloadBuilder, Inet4Address srcAddr, Inet4Address dstAddr) {
        IpV4Packet.Builder ipBuilder = new IpV4Packet.Builder();
        ipBuilder.payloadBuilder(payloadBuilder)
                .version(IpVersion.IPV4)
                .tos((IpV4Packet.IpV4Tos) () -> (byte) 0)
                .protocol(IpNumber.TCP)
                .dontFragmentFlag(true)
                .ttl((byte) 128)
                .srcAddr(srcAddr)
                .dstAddr(dstAddr).correctChecksumAtBuild(true).correctLengthAtBuild(true);
        return ipBuilder;
    }

    public static EthernetPacket.Builder EthernetPacket(Packet.Builder payloadBuilder, MacAddress srcAddr, MacAddress dstAddr) {
        EthernetPacket.Builder etherBuilder = new EthernetPacket.Builder();
        etherBuilder
                .dstAddr(dstAddr)
                .srcAddr(srcAddr)
                .type(EtherType.IPV4)
                .paddingAtBuild(true)
                .payloadBuilder(payloadBuilder);
        return etherBuilder;
    }

}
