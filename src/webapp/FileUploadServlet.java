package webapp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Created by Daniil on 19.08.2020.
 */
@javax.servlet.annotation.WebServlet(name = "FileUploadServlet")
public class FileUploadServlet extends javax.servlet.http.HttpServlet {

    private static final long serialVersionUID = 1L;

    // location to store file uploaded
    private static final String UPLOAD_DIRECTORY = "upload";

    // upload settings
    private static final int MEMORY_THRESHOLD   = 1024 * 1024 * 3;  // 3MB
    private static final int MAX_FILE_SIZE      = 1024 * 1024 * 40; // 40MB
    private static final int MAX_REQUEST_SIZE   = 1024 * 1024 * 50; // 50MB

    protected void doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {
        try {
            String paramsJSON = request.getParameter("params");

            System.out.print(paramsJSON);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> paramsMap = mapper.readValue(paramsJSON, Map.class);
            System.out.println(paramsMap);

            String hostName = paramsMap.get("hostFTP");
            String userName = paramsMap.get("userFTP");
            String password =  new String(DecryptUtil.decryptBase64EncodedWithManagedIV((String)paramsMap.get("passFTP"), "mRMjHmlC1C+1L/Dkz8EJuw=="), "UTF-8");
            String portFtp = paramsMap.get("portFTP");
            String traceId = paramsMap.get("traceId");
            //String ftpFileName = paramsMap.get("fileName");
            String ftpFilePath = paramsMap.get("filePath");

            //System.out.print(URLDecoder.decode(paramsJSON, StandardCharsets.UTF_8.toString()));
            System.out.println("FileUploadServlet");
            if (!ServletFileUpload.isMultipartContent(request)) {
                System.out.println("Not Multipart");
                FileUploadServlet.sendResponce(response, "ERROR", "File must be encrypted in multipart format.");
                return;
            }
            System.out.println("Is Multipart");
            // configures upload settings
            DiskFileItemFactory factory = new DiskFileItemFactory();
            // sets memory threshold - beyond which files are stored in disk
            factory.setSizeThreshold(MEMORY_THRESHOLD);
            // sets temporary location to store files
            factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
            ServletFileUpload upload = new ServletFileUpload(factory);
            // sets maximum size of upload file
            upload.setFileSizeMax(MAX_FILE_SIZE);
            // sets maximum size of request (include file + form data)
            upload.setSizeMax(MAX_REQUEST_SIZE);
            // constructs the directory path to store upload file
            // this path is relative to application's directory
            String uploadPath = getServletContext().getRealPath("")
                    + File.separator + UPLOAD_DIRECTORY;
            System.out.println("uploadPath" + uploadPath);
            // creates the directory if it does not exist
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdir();
            }
            try {
                // parses the request's content to extract file data
                @SuppressWarnings("unchecked")
                List<FileItem> formItems = upload.parseRequest(request);
                System.out.println("List<FileItem>" + formItems);
                if (formItems != null && formItems.size() > 0) {
                    // iterates over form's fields
                    for (FileItem item : formItems) {
                        // processes only fields that are not form fields
                        if (!item.isFormField()) {
                            String fileName = new File(item.getName()).getName();
                            String filePath = uploadPath + File.separator + fileName;
                            File storeFile = new File(filePath);
                            System.out.println("filePath: " + filePath);

                            // saves the file on disk
                            item.write(storeFile);
                            request.setAttribute("message",
                                    "Upload has been done successfully!");
                            System.out.println("Upload is made.");
                            FTPinteractionUtils.uploadFileToFTP(hostName, portFtp, userName, password, ftpFilePath, filePath, fileName);
                            try {
                                storeFile.delete();
                            }catch (Exception e){
                                System.out.println("Unable to delete temp file");
                            }
                        }
                    }
                } else {
                    FileUploadServlet.sendResponce(response, "ERROR", "File is not recognized");
                    return;
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
                FileUploadServlet.sendResponce(response, "ERROR", ex.getMessage());
                return;
            }
            FileUploadServlet.sendResponce(response, "SUCCESS", "File uploaded");
            return;
        }catch(Exception e){
            System.out.println("Error: " + e.getMessage());
            FileUploadServlet.sendResponce(response, "ERROR", e.getMessage());
            return;
        }
    }


    private static void sendResponce(javax.servlet.http.HttpServletResponse response, String status, String messaage) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        PrintWriter out = response.getWriter();
        JSONObject json = new JSONObject();
        json.put("Status", status);
        json.put("Message", messaage);
        out.print(json.toString());
    }

   // protected void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {

   // }
}
