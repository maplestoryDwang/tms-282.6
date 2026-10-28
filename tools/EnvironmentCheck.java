import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Properties;
public final class EnvironmentCheck {
 public static void main(String[] args) {
  try {
   Properties p=new Properties();
   try(InputStream in=Files.newInputStream(Path.of("config/server.properties"))){p.load(in);}
   if(!"127.0.0.1".equals(p.getProperty("db.host")) || !p.getProperty("db.name","").matches("[A-Za-z0-9_]+"))throw new IllegalStateException();
   int port=Integer.parseInt(p.getProperty("db.port","3306"));
   if(port<1||port>65535)throw new IllegalStateException();
   Class.forName("org.mariadb.jdbc.Driver");
   String url="jdbc:mariadb://127.0.0.1:"+port+"/"+p.getProperty("db.name")+"?connectTimeout=3000&socketTimeout=3000";
   try(var con=DriverManager.getConnection(url,p.getProperty("db.user"),p.getProperty("db.password"))) {
    con.setReadOnly(true);
    for(String table:new String[]{"users","accounts","characters","user_recovery_codes"}) {
     try(var rows=con.getMetaData().getTables(con.getCatalog(),null,table,new String[]{"TABLE"})) {
      boolean found=false; while(rows.next())if(table.equals(rows.getString("TABLE_NAME")))found=true;
      if(!found)throw new IllegalStateException("Required table missing");
     }
    }
    try(var statement=con.createStatement();var rows=statement.executeQuery("SELECT 1")){if(!rows.next()||rows.getInt(1)!=1)throw new IllegalStateException();}
   }
   System.out.println("PASS: local database connection and account/character tables; read-only check, no data changed.");
  } catch(Exception e) {
   System.err.println("FAIL: local database settings, service or required tables are not ready. Check config/server.properties; credentials are not printed.");
   System.exit(1);
  }
 }
}