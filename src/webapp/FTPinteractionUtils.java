package webapp;

import java.io.*;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * A program that demonstrates how to upload files from local computer
 * to a remote FTP server using Apache Commons Net API.
 * @author www.codejava.net
 */
public class FTPinteractionUtils {

    public static void uploadFileToFTP(String hostName, String portFtp,  String userName,
                                       String password, String hostDirectory,
                                       String localFilePath, String fileName) {

       System.out.println("Started FTP Upload");
        FTPUploader ftpUploader = null;
        try {
            ftpUploader = new FTPUploader(hostName, portFtp, userName, password);
            ftpUploader.uploadFile(localFilePath, fileName, hostDirectory);
            ftpUploader.disconnect();
            System.out.println("FTP Upload Done");
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*System.out.println("Started FTP Upload");
        FTPUploader ftpUploader = null;
        try {
            ftpUploader = new FTPUploader("ftp.journaldev.com", "ftpUser", "ftpPassword");
            //FTP server path is relative. So if FTP account HOME directory is "/home/pankaj/public_html/" and you need to upload
            // files to "/home/pankaj/public_html/wp-content/uploads/image2/", you should pass directory parameter as "/wp-content/uploads/image2/"
            ftpUploader.uploadFile("D:\\Pankaj\\images\\MyImage.png", "image.png", "/wp-content/uploads/image2/");
            ftpUploader.disconnect();
            System.out.println("FTP Upload Done");
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    public static void downloadFileFromFTP(String hostName, String userName, String portFtp,
                                           String password, String remoteFilePath,
                                           String localFilePath) {
        try {
            FTPDownloader ftpDownloader =
                    new FTPDownloader(hostName, portFtp, userName, password);
            ftpDownloader.downloadFile(remoteFilePath, localFilePath);
            System.out.println("FTP File downloaded successfully");
            ftpDownloader.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Boolean verifyFileExistsInFTP(String hostName, String userName, String portFtp,
                                           String password, String remoteFilePath,
                                           String localFilePath) {
        try {
            FTPDownloader ftpDownloader =
                    new FTPDownloader(hostName, portFtp, userName, password);
            Boolean isExists = ftpDownloader.checkfileExists(remoteFilePath);
            System.out.println("is Remote File exists?"+ remoteFilePath);
            ftpDownloader.disconnect();
            return isExists;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


        /*try {
            FTPDownloader ftpDownloader =
                    new FTPDownloader("ftp_server.journaldev.com", "ftp_user@journaldev.com", "ftpPassword");
            ftpDownloader.downloadFile("sitemap.xml", "/Users/pankaj/tmp/sitemap.xml");
            System.out.println("FTP File downloaded successfully");
            ftpDownloader.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

        public static void uploadFile() {
        String server = "www.myserver.com";
        int port = 21;
        String user = "user";
        String pass = "pass";

        FTPClient ftpClient = new FTPClient();
        try {

            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // APPROACH #1: uploads first file using an InputStream
            File firstLocalFile = new File("D:/Test/Projects.zip");

            String firstRemoteFile = "Projects.zip";
            InputStream inputStream = new FileInputStream(firstLocalFile);

            System.out.println("Start uploading first file");
            boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
            inputStream.close();
            if (done) {
                System.out.println("The first file is uploaded successfully.");
            }

            // APPROACH #2: uploads second file using an OutputStream
            File secondLocalFile = new File("E:/Test/Report.doc");
            String secondRemoteFile = "test/Report.doc";
            inputStream = new FileInputStream(secondLocalFile);

            System.out.println("Start uploading second file");
            OutputStream outputStream = ftpClient.storeFileStream(secondRemoteFile);
            byte[] bytesIn = new byte[4096];
            int read = 0;

            while ((read = inputStream.read(bytesIn)) != -1) {
                outputStream.write(bytesIn, 0, read);
            }
            inputStream.close();
            outputStream.close();

            boolean completed = ftpClient.completePendingCommand();
            if (completed) {
                System.out.println("The second file is uploaded successfully.");
            }

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}