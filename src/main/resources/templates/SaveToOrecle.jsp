<%@  page language="java" import="org.apache.commons.fileupload.*,org.apache.commons.fileupload.disk.*,org.apache.commons.fileupload.servlet.*,org.apache.tomcat.util.http.fileupload.FileItem,org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory,org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload"%>
<%@ page import="java.io.ByteArrayInputStream" %>
<%@ page import="java.io.ByteArrayOutputStream" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.DriverManager" %>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="org.apache.tomcat.util.http.fileupload.RequestContext" %>

<%
// Prepare credentials for connectiong to Oracle, here we use Oracle express (XE)
String strDBUser = "dwtDB"; //database,schema name as well
String strDBPassword = "NotRealPWD";
String strDriverName = "oracle.jdbc.driver.OracleDriver";
String strConnString = "jdbc:oracle:thin:@127.0.0.1:1521:XE";
Connection conn=null;
// Test Database Connection
try
{
Class.forName(strDriverName).newInstance();
conn = DriverManager.getConnection(strConnString, strDBUser, strDBPassword);
conn.setAutoCommit(true);
}
catch(Exception e)
{
System.out.println("An exception occurred: " + e.getMessage());
}
String fileName = "";
long sizeInBytes = 0;
// Create a factory for disk-based file items
DiskFileItemFactory factory = new DiskFileItemFactory();
// Configure a repository (to ensure a secure temp location is used)
ServletContext servletContext = this.getServletConfig().getServletContext();
File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
// Set factory constraints
factory.setRepository(repository);
// Sets the size threshold beyond which files are written directly to disk.
factory.setSizeThreshold(1000000000);
// Create a new file upload handler
ServletFileUpload upload = new ServletFileUpload(factory);
// Set overall request size constraint
upload.setSizeMax(-1);
// Parse the request
List<FileItem> items = upload.parseRequest((RequestContext) request);
// Process the uploaded items
Iterator<FileItem> iter = items.iterator();
while (iter.hasNext()) {
try{
FileItem item = iter.next();
// Process a regular form field
if (item.isFormField()) {}
// Process a file upload
else {
fileName = item.getName();
sizeInBytes = item.getSize();
if(fileName != null && sizeInBytes != 0){
/**
* Get the input stream (the file)
*/
InputStream stream_Input = item.getInputStream();
byte[] buff = new byte[8000];
int bytesRead = 0;
ByteArrayOutputStream stream_BAO = new ByteArrayOutputStream();
while((bytesRead = stream_Input.read(buff)) != -1) {
stream_BAO.write(buff, 0, bytesRead);
}
byte[] data = stream_BAO.toByteArray();
ByteArrayInputStream stream_BAI = new ByteArrayInputStream(data);
stream_Input.close();
/**
* Save the stream into Oracle
*/
PreparedStatement preparedStatement = conn.prepareStatement("insert into dwtsample(id, document_name, document_data) values(s_tblImage.NextVal, ?, ?)");
preparedStatement.setString(1, fileName);
preparedStatement.setBinaryStream(2, stream_BAI, stream_BAI.available());
preparedStatement.executeUpdate();
preparedStatement.close();
conn.close();
}
}
}
catch(Exception e)
{
}
}
%>