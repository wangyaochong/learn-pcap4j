package socket;

import java.io.IOException;
import java.net.Socket;

public class SocketClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        final Socket socket = new Socket("192.168.0.106", 9090);
//        final Socket socket = new Socket("64.91.248.15", 9090);
        if (socket.isConnected()) {
            System.out.println("网络连接成功");
        }
        byte[] bytes = "client hello 1".getBytes();
        socket.getOutputStream().write(bytes);
        Thread.sleep(3000);
        byte[] bytes1 = "client hello 2".getBytes();
        socket.getOutputStream().write(bytes1);
        byte[] bytes2 = "client hello 3".getBytes();
        socket.getOutputStream().write(bytes2);
        byte[] bytes3 = "client hello 4".getBytes();
        socket.getOutputStream().write(bytes3);
        byte[] bytes4 = "client hello 5".getBytes();
        socket.getOutputStream().write(bytes4);
        socket.getOutputStream().flush();
        Thread.sleep(3000);
        byte[] bytes5 = "client hello 20".getBytes();
        socket.getOutputStream().write(bytes5);
        System.out.println("总长度="+(bytes.length+bytes1.length+bytes2.length+bytes3.length+bytes4.length+bytes5.length));
        //总长度是可以开
        Thread.sleep(10000000);


    }
}
