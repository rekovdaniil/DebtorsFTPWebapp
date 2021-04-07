package webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.StringEntity;

import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.nio.ch.IOUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by Daniil on 25.08.2020.
 */
@WebServlet(name = "FileDownloadServlet")
public class FileDownloadServlet extends HttpServlet {
    private static final String UPLOAD_DIRECTORY = "upload";


    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            PrintStream out = new PrintStream(System.out, true, "ISO8859_7");
            String paramsJSON = request.getParameter("params");
            System.out.print(paramsJSON);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> paramsMap = mapper.readValue(paramsJSON, Map.class);
            System.out.println(paramsMap);

            String hostName = (String)paramsMap.get("hostFTP");
            String userName = (String)paramsMap.get("userFTP");
            String password =  new String(DecryptUtil.decryptBase64EncodedWithManagedIV((String)paramsMap.get("passFTP"), "mRMjHmlC1C+1L/Dkz8EJuw=="), "UTF-8");
            String portFtp = (String)paramsMap.get("portFTP");
            String traceId = (String)paramsMap.get("traceId");
            String ftpFileName = (String)paramsMap.get("fileName");
            String ftpFilePath = (String)paramsMap.get("filePath");

            String callbackURL = (String)paramsMap.get("callbackURL");
            Integer batchSize = (Integer)paramsMap.get("batchSize");
            Integer chunkSleep = (Integer)paramsMap.get("chunkSleepTime");
            String fileSettingType = (String)paramsMap.get("fileSettingType");


            String uploadPath = getServletContext().getRealPath("")
                    + File.separator + UPLOAD_DIRECTORY+"\\Temp.csv";
            System.out.println(uploadPath);
            System.out.println(hostName);
            System.out.println(portFtp);
            System.out.println(traceId);
            System.out.println(userName);
            System.out.println(password);
            System.out.println(ftpFilePath+ftpFileName);
            System.out.println(callbackURL);
            System.out.println(batchSize);
            System.out.println(fileSettingType);


    Boolean isFileExists = FTPinteractionUtils.verifyFileExistsInFTP(hostName, userName,portFtp,password,ftpFileName,uploadPath);
    if(isFileExists) {
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                System.out.print("Tread START");

                FTPinteractionUtils.downloadFileFromFTP(hostName, userName, portFtp, password, ftpFilePath + ftpFileName, uploadPath);

                try (InputStreamReader in = new InputStreamReader(new FileInputStream(uploadPath),"ISO8859_7");) {

                   // String text = IOUtils.toString(in, StandardCharsets.ISO_8859_1.name());
                   // System.out.println(text);

                    CSV csv = new CSV(false, '|', in);
                    List<String> fieldNames = null;
                    if (csv.hasNext()) fieldNames = new ArrayList<>(csv.next());
                    List<Map<String, String>> list = new ArrayList<>();
                    JSONArray csvSerializedList = new JSONArray();
                    Integer index = 2;
                    while (csv.hasNext()) {
                        try {
                            System.out.println("Process CSV Record" + index);
                            List<String> x = csv.next();
                            JSONObject jsonObj = new JSONObject();
                            for (int i = 0; i < fieldNames.size(); i++) {
                                //System.out.println();
                                jsonObj.put(fieldNames.get(i), x.get(i));
                            }
                            out.println(jsonObj.toString());
                            csvSerializedList.put(jsonObj);
                            if (csvSerializedList.length() >= batchSize) {
                                //System.out.println("Sending chunk " + csvSerializedList.length() + " records...");
                                FileDownloadServlet.sendChunkToSF(callbackURL, csvSerializedList.toString(), traceId, fileSettingType, true);
                                Thread.sleep(chunkSleep);
                                csvSerializedList = new JSONArray();
                            }
                            index++;
                        } catch (Exception e) {
                           System.out.println("ERROR 3 in line" + index + " " + e.getCause() + e.getMessage());
                        }
                    }
                    if (csvSerializedList.length() > 0) {
                        //System.out.println("Sending chunk " + csvSerializedList.length() + " records...");
                        FileDownloadServlet.sendChunkToSF(callbackURL, csvSerializedList.toString(), traceId, fileSettingType, false);
                        Thread.sleep(chunkSleep);
                    }
                    PrintStream out = new PrintStream(System.out, true, "UTF-8");

                    System.out.println("Processing finished");
                } catch (Exception e) {
                    System.out.print("EX2");
                    System.out.println(e);
                    e.printStackTrace();
                }
            }
        });
        t1.start();
        FileDownloadServlet.sendResponce(response, "SUCCESS", "File Found", traceId);
        } else{
        System.out.print("EX File Not Exists");
        FileDownloadServlet.sendResponce(response, "ERROR", "Unable to access requested file", traceId);
        return;

    }

        }catch (Exception e){
            System.out.print("EX1");
            System.out.println("Error: " +e.getCause() + e.getStackTrace() + e.getMessage());
            FileDownloadServlet.sendResponce(response, "ERROR", e.getMessage(), null);
           return;

        }

    }

    private static void sendResponce(javax.servlet.http.HttpServletResponse response, String status, String messaage, String traceId) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("ISO8859_7");
        PrintWriter out = response.getWriter();
        JSONObject responseJSON = new JSONObject();
        responseJSON.put("Status", status);
        responseJSON.put("Message", messaage);
        responseJSON.put("TraceId", traceId);
        out.print(responseJSON.toString());
    }

    private static void sendChunkToSF(String url, String jsonBody, String traceId, String fileType, Boolean hasNext){
        try {
            URL urlEndpoint = new URL(url+"/?traceId="+traceId+"&fileType="+fileType+"&hasNext="+hasNext);

            URLConnection con = urlEndpoint.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST"); // PUT is another valid option
            http.setDoOutput(true);
            byte[] out = jsonBody.getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.connect();
            try(OutputStream os = http.getOutputStream()) {
                os.write(out);
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        } finally {
            System.out.println("SENT");
        }




       /* HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            //HttpPost request = new HttpPost("https://testtestdaniil.free.beeceptor.com");
            HttpPost request = new HttpPost(url+"/?traceId="+traceId+"&fileType="+fileType);

            //List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);
            //parameters.add(new BasicNameValuePair("body", jsonBody));
            //StringEntity params =new StringEntity(jsonBody);
            request.addHeader("content-type", "text/plain;charset=iso-8859-7");
            request.addHeader("Accept","application/json");
            request.setEntity(new StringEntity(jsonBody, Charset.forName("ISO8859_7")));
            HttpResponse response = httpClient.execute(request);

            // handle response here...
        }catch (Exception ex) {
            System.out.println(ex.getMessage());
            // handle exception here
        } finally {
            System.out.println("SENT");
        }*/

    }

}
