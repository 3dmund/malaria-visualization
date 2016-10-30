import com.oracle.javafx.jmx.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by EdmundTian on 10/29/16.
 */
public class DataRetriever {

    /** ArrayList of ArrayList<Object>, each containing data about a location in the following indexes:
     *  0: latitude, 1: longitude, 2: deaths * .00005.
     * **/
    public static ArrayList<ArrayList<Object>> data = new ArrayList<>();

    /** Helper function for getJsonObject(). **/
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /** Get the JSONObject from a given url. **/
    public static JSONObject getJsonObject(String url) throws IOException, JSONException, ParseException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    /** Returns ArrayList of coordinates. Index 0 is latitude. Index 1 is longitude. **/
    public static ArrayList<Object> getCoordinates(String country) throws IOException, ParseException{
        String compatibleStr = country.replaceAll("\\s","+");
        String url = "http://maps.googleapis.com/maps/api/geocode/json?address=" + compatibleStr;
        JSONObject json = getJsonObject(url);
        JSONArray results = (JSONArray) json.get("results");
        JSONObject geometry = (JSONObject) ((JSONObject) results.get(0)).get("geometry");
        JSONObject location = (JSONObject) geometry.get("location");
        int latitude = ((Double) location.get("lat")).intValue();
        int longitude = ((Double) location.get("lng")).intValue();
        ArrayList<Object> coordinates = new ArrayList<>();
        coordinates.add(latitude);
        coordinates.add(longitude);
        System.out.println(latitude);
        System.out.println(longitude);
        return coordinates;
    }

    /** Parse the JSONArray and add data to data. **/
    public static void parse(JSONArray jsonArray) throws ParseException, IOException, InterruptedException {
        for(Object obj : jsonArray){
            try {
                JSONObject jsonObject = (JSONObject) obj;
                String country = (String) jsonObject.get("FIELD2");
                String deathsStr = (String) jsonObject.get("Underfive deaths due to Malaria");
                int deaths = (int) Double.parseDouble(deathsStr);
                System.out.println(country);
                System.out.println(deaths);
                ArrayList<Object> wrapper = getCoordinates(country);
                wrapper.add(deaths * .00005);
                data.add(wrapper);
                Thread.sleep(200); // To avoid exceeding rate-limit of Google Maps API
            } catch (Exception e) {
                System.out.println("Failed: " + ((String) ((JSONObject) obj).get("FIELD2")));
                continue;
            }
        }
    }

    /** Write data in ArrayList to json file **/
    public static void write() {
        JSONArray outer = new JSONArray();
        JSONArray all = new JSONArray();

        all.add("all");

        JSONArray magnitudes = new JSONArray();
        for (ArrayList<Object> wrapper : data) {
            magnitudes.add(wrapper.get(0));
            magnitudes.add(wrapper.get(1));
            magnitudes.add(wrapper.get(2));
        }
        all.add(magnitudes);
        outer.add(all);

        try {
            // Writing to a file
            File file = new File("data.json");
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            System.out.println("Writing JSON object to file");
            fileWriter.write(outer.toJSONString());
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String x[]){
        String FileName="malaria_deaths.json";
        try {
            JSONParser parser = new JSONParser();
            JSONArray array =  (JSONArray) parser.parse(new FileReader(FileName));
            parse(array);
            write();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }

}
