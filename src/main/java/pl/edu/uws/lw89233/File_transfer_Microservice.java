package pl.edu.uws.lw89233;

import pl.edu.uws.lw89233.managers.EnvManager;
import pl.edu.uws.lw89233.managers.MessageManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;

public class File_transfer_Microservice {

    private final int PORT = Integer.parseInt(EnvManager.getEnvVariable("FILE_TRANSFER_MICROSERVICE_PORT"));

    public void startService() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("File Transfer Microservice is running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting File Transfer Microservice: " + e.getMessage());
        }
    }

    private class ClientHandler extends Thread {

        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String request;
                while ((request = in.readLine()) != null) {
                    MessageManager requestDict = new MessageManager(request);
                    if (requestDict.getAttribute("type").equals("send_file_request")) {
                        handleSendFile(requestDict, in, out);
                    } else if (requestDict.getAttribute("type").equals("get_file_request")) {
                        handleGetFile(requestDict, in, out);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client request: " + e.getMessage());
            }
        }

        private void handleSendFile(MessageManager requestDict, BufferedReader in, PrintWriter out) {
            String fileName = requestDict.getAttribute("file_name");
            String userLogin = requestDict.getAttribute("login");
            String action = requestDict.getAttribute("action");
            String messageId = requestDict.getAttribute("message_id");

            File userDir = new File("files/" + userLogin);
            userDir.mkdirs();
            File file = new File(userDir, fileName);

            try {
                if ("init".equals(action)) {
                    new FileOutputStream(file).close();
                    out.println("type:send_file_response#message_id:" + messageId + "#status:200#");

                    while (true) {
                        String request = in.readLine();
                        if (request == null) {
                            break;
                        }

                        MessageManager packageRequest = new MessageManager(request);
                        action = packageRequest.getAttribute("action");

                        if ("finish".equals(action)) {
                            out.println("type:send_file_response#message_id:" + messageId + "#package_number:-1#status:200#");
                            break;
                        }

                        if ("package".equals(action)) {
                            String content = packageRequest.getAttribute("content");
                            int packageNumber = Integer.parseInt(packageRequest.getAttribute("package_number"));

                            try (FileOutputStream fos = new FileOutputStream(file, true)) {
                                byte[] partOfFileBytes = Base64.getDecoder().decode(content);
                                fos.write(partOfFileBytes);

                                out.println("type:send_file_response#message_id:" + messageId
                                        + "#package_number:" + packageNumber + "#status:200#");
                            } catch (IOException e) {
                                out.println("type:send_file_response#message_id:" + messageId
                                        + "#package_number:" + packageNumber
                                        + "#status:400#");
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                out.println("type:send_file_response#message_id:" + messageId
                        + "#status:400#" );
            }
        }

        private void handleGetFile(MessageManager requestDict, BufferedReader in, PrintWriter out) {
            String fileName = requestDict.getAttribute("file_name");
            String userLogin = requestDict.getAttribute("login");
            String messageId = requestDict.getAttribute("message_id");

            if (fileName == null || userLogin == null) {
                out.println("type:get_file_response#message_id:" + messageId + "#status:400#");
                return;
            }

            File file = new File("files/" + userLogin + "/" + fileName);
            if (!file.exists()) {
                out.println("type:get_file_response#message_id:" + messageId + "#status:400#");
                return;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[512];
                int bytesRead;
                int packageNumber = 0;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] partOfFileBytes = Arrays.copyOf(buffer, bytesRead);
                    String partOfFileString = Base64.getEncoder().encodeToString(partOfFileBytes);

                    String response = "type:get_file_response#message_id:" + messageId
                            + "#package_number:" + packageNumber
                            + "#content:" + partOfFileString
                            + "#status:200";

                    out.println(response);

                    String confirmation = in.readLine();
                    if (confirmation == null || !confirmation.contains("action:ack")) {
                        System.out.println("Failed to receive acknowledgment for package: " + packageNumber);
                        return;
                    }
                    packageNumber++;
                }

                out.println("type:get_file_response#message_id:" + messageId
                        + "#package_number:-1#"
                        + "status:200#");
            } catch (IOException e) {
                System.out.println("Error sending file: " + e.getMessage());
                out.println("type:get_file_response#message_id:" + messageId + "#status:400#");
            }
        }
    }

    public static void main(String[] args) {
        new File_transfer_Microservice().startService();
    }
}