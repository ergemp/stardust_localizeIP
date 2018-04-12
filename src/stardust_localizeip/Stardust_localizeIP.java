
package stardust_localizeip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Stardust_localizeIP {

    public static String sIPADDR = "NA";
    public static Double sLat = 0.0;
    public static Double sLong = 0.0;
    public static String sCountryName = "NA";
    public static String sCountryCode = "NA";
    public static String sCity = "NA";
    public static String sTimeZone = "NA";
    public static String sISP = "NA";
    
    public  static void main(String[] args) {
         //TODO code application logic here
         localize(args[0].toString());
         
         System.out.println("Latitude: " + Stardust_localizeIP.sLat);
         System.out.println("Longitude: " + Stardust_localizeIP.sLong);
         System.out.println("Country Name: " + Stardust_localizeIP.sCountryName);
         System.out.println("Country Code: " + Stardust_localizeIP.sCountryCode);
         System.out.println("City: " + Stardust_localizeIP.sCity);
         System.out.println("timeZone: " + Stardust_localizeIP.sTimeZone);
         System.out.println("ISP: " + Stardust_localizeIP.sISP);         
    }
        
    public static Boolean getLocalFromDB(String gIPADDR)
    {
    
        Boolean retVal = false;        
        sIPADDR = gIPADDR;         
        
        try
        {                        
            //get the connection string for postgres from the stardust_target API
            stardust_target.target postgresTarget = new stardust_target.target();
            postgresTarget.loadFromFile("stardust_postgres");            
            
            Class.forName("org.postgresql.Driver");
            
            String cnnStr = "jdbc:postgresql://" + postgresTarget.pIPADDR + ":" + postgresTarget.pPORT + "/" + postgresTarget.pDATABASE;
            Connection connection = null;
            connection = DriverManager.getConnection(cnnStr, postgresTarget.pUSERNAME, postgresTarget.pPASSWORD);                        
            Statement stmt = connection.createStatement();
            
            String strSQL = "select lat, long, country_code, country_name, city_name, timezone, isp_name  from strdst_localizeIP where substr(ipaddr,1, length(ipaddr) - strpos(reverse(ipaddr),'.'))='"+ gIPADDR.substring(0, gIPADDR.lastIndexOf(".")) + "'";
            ResultSet rSet = stmt.executeQuery(strSQL);        
            
            while (rSet.next()) {
                sLat = rSet.getDouble("lat");
                sLong = rSet.getDouble("long");
                sCountryName = rSet.getString("country_name");
                sCountryCode = rSet.getString("country_code");
                sCity = rSet.getString("city_name");
                sTimeZone = rSet.getString("timezone");
                sISP = rSet.getString("isp_name");   
                
                retVal = true;
            }                               
            
            if (!connection.isClosed())
            {
                connection.close();
                stmt.close();
            }
            //retVal = true;
        }
        catch(Exception ex)
        {
            retVal = false;
        }
        finally
        {
            return retVal;
        }
    }
        
    public static Boolean getLocalFromCDN(String gIPADDR)
    {
        Boolean retVal = true;
        final String USER_AGENT = "Mozilla";   
        sIPADDR = gIPADDR;
        
        try
        {
            URL url = new URL("https://tools.keycdn.com/geo.json?host=" + gIPADDR );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            //conn.setRequestProperty("Accept", "application/json");   
            conn.setRequestProperty("User-Agent", USER_AGENT);
            
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
                
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            
            String output = "";
            while ((output = br.readLine()) != null) 
            {
                org.json.simple.JSONObject jOut = new JSONObject();
                jOut = (JSONObject)JSONValue.parse(output);

                try
                {                        
                    org.json.simple.JSONObject jData = (JSONObject)JSONValue.parse(jOut.get("data").toString());                        
                    org.json.simple.JSONObject jGeo = (JSONObject)JSONValue.parse(jData.get("geo").toString());

                    /*
                    System.out.println(jOut.toString());
                    System.out.println(jGeo.get("latitude"));
                    System.out.println(jGeo.get("longitude"));
                    System.out.println(jGeo.get("country_code"));
                    System.out.println(jGeo.get("country_name"));
                    System.out.println(jGeo.get("city"));
                    System.out.println(jGeo.get("timezone"));
                    System.out.println(jGeo.get("isp"));  
                    */

                    sLat = Double.parseDouble(jGeo.get("latitude").toString());
                    sLong = Double.parseDouble(jGeo.get("longitude").toString());
                    sCountryName = jGeo.get("country_name").toString();
                    sCountryCode = jGeo.get("country_code").toString();
                    sCity = jGeo.get("city").toString();
                    sTimeZone = jGeo.get("timezone").toString();
                    sISP = jGeo.get("isp").toString();                                                

                }
                catch(Exception Ex)
                {
                }
            }   
             
            //then sleep for one sec.
            //CDN Service has this limit one request per second
            Thread.sleep(1000);
            
            retVal = true;
        }
        catch(Exception ex)
        {
            retVal = false;
        }
        finally
        {
            return retVal;
        }
        
    }
    
    public static Boolean saveLocaltoDB()
    {
        Boolean retVal = true;
        
        try
        {                   
            //get the connection string for postgres from the stardust_target API
            stardust_target.target postgresTarget = new stardust_target.target();
            postgresTarget.loadFromFile("stardust_postgres");            
            
            String cnnStr = "jdbc:postgresql://" + postgresTarget.pIPADDR + ":" + postgresTarget.pPORT + "/" + postgresTarget.pDATABASE;
            Connection connection = null;
            connection = DriverManager.getConnection(cnnStr, postgresTarget.pUSERNAME, postgresTarget.pPASSWORD);                        
            Statement stmt = connection.createStatement();
            
            stmt.executeUpdate(" insert into strdst_localizeIP (IPADDR, lat, long, country_code, country_name, city_name, timezone, isp_name) "+
                               " values ('" + sIPADDR + "', " + sLat + ", " + sLong + ", '" + sCountryCode + "', '" + sCountryName + "', '" + sCity + "' , '" + sTimeZone + "' , '" + sISP + "')");        
            
            connection.close();
            stmt.close();
            retVal = true;
            
        }
        catch(Exception ex)
        {
            retVal = false;
        }
        finally
        {
            return retVal;
        }
    }        
    
    public static Boolean localize(String gIPADDR)
    {
        Boolean retVal = true;        
        
        try
        {
            if (getLocalFromDB(gIPADDR))
            {
                return true;
            }
            else
            {
                if (getLocalFromCDN(gIPADDR))
                {
                    saveLocaltoDB();
                }
            }
                                             
            retVal = true;
        }
        catch(Exception ex)
        {
            retVal = false; 
            ex.printStackTrace();
        }
        finally
        {
            return retVal;            
        }
        
    }
    
}
