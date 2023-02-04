package socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BioServer {

    //可以使用telnet 127.0.0.1 6666 来连接服务器，按ctrl+]转入命令模式，使用send命令发送字符串

    public static void main(String[] args) throws IOException {
        System.out.println("cpu数量=" + Runtime.getRuntime().availableProcessors());

        ExecutorService executorService = Executors.newCachedThreadPool();
        ServerSocket serverSocket = new ServerSocket(9090);
        System.out.println("服务器启动成功了");
        while (true) {
            //每当有新的连接来临，会新建一个socket，否则一直阻塞
            final Socket socket = serverSocket.accept();
            System.out.println("连接到一个客户端了");
            executorService.execute(() -> {
                try {
                    byte[] bytes = new byte[1024];
                    InputStream inputStream = socket.getInputStream();
                    int read = 0;
                    while (read != -1) {
                        read = inputStream.read(bytes);
                        if (read > 0) {
                            System.out.println(Thread.currentThread().getId() + ":"
                                    + Thread.currentThread().getName() + ":"
                                    + new String(bytes, 0, read));
                        }
                    }
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }
}
