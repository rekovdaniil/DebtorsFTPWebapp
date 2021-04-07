package webapp;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;

/**
 * Created by Daniil on 25.08.2020.
 */
public class FTPUploader {

        FTPClient ftp = null;

        public FTPUploader(String host, String port, String user, String pwd) throws Exception{
            System.out.println("Trying to connect.");

            ftp = new FTPClient();
            ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            int reply;
            ftp.connect(host, Integer.valueOf(port));
            reply = ftp.getReplyCode();
            System.out.println("ReplyCode:"+reply);
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                throw new Exception("Exception in connecting to FTP Server");
            }
            ftp.login(user, pwd);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
        }
        public void uploadFile(String localFileFullName, String fileName, String hostDir)
                throws Exception {
            try(InputStream input = new FileInputStream(new File(localFileFullName))){
                this.ftp.storeFile(hostDir + fileName, input);
            }
        }

        public void disconnect(){
            if (this.ftp.isConnected()) {
                try {
                    this.ftp.logout();
                    this.ftp.disconnect();
                } catch (IOException f) {
                    // do nothing as file is already saved to server
                }
            }
        }
}
