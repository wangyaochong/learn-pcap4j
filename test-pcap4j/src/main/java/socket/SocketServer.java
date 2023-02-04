package socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {
    private int port = 9090;
    private ServerSocket serverSocket;

    public SocketServer() throws IOException {
        serverSocket = new ServerSocket(port, 3);    //连接请求队列的长度为3
        System.out.println("服务器启动");
    }

    public void service() {
        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();     //从连接请求队列中取出一个连接
                System.out.println("New connection accepted " +
                        socket.getInetAddress() + ":" + socket.getPort());
                while (true) {
                    if (socket.getInputStream().available() > 0) {
                        int available = socket.getInputStream().available();
                        byte[] b = new byte[available];
                        int read = socket.getInputStream().read(b);
                        System.out.println("read content = " + new String(b));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String args[]) throws Exception {
        SocketServer server = new SocketServer();
        Thread.sleep(60000 * 10);      //睡眠10分钟
        //server.service();
    }
}
