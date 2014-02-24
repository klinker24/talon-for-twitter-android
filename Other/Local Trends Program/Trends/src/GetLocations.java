import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public class GetLocations {
	public static void main(String[] args) {
		String data = "";
		try {
			data = readFile("/home/luke/locations.txt");
			//System.out.print(data);
		} catch (Exception e) {
			System.out.print("error");
		}
		
		//printCountriesStringArray(data);
		printLocationArrays(data);
		//printReturnStatement(data);
	}
	
	public static void printCountriesStringArray(String data) {
		
		// get the list of countries
        ArrayList<String> countries = getCountries(data);
		
        // print out the formatted string array
		System.out.println("<string-array name=\"countries\">");
		
		for (String s: countries) {
			if (!s.equals("")) {
				System.out.println("<item>" + s + "</item>");
			}
		}
		
		System.out.println("</string-array>");
	}
	
	static class Country {
		public String name;
		public ArrayList<City> citys = new ArrayList<City>();
		
		public Country(String name) {
			this.name = name;
		}
	}
	
	static class City {
		public String name;
		public int woeid;
		
		public City(String name, int woeid) {
			this.name = name;
			this.woeid = woeid;
		}
	}
	
	public static void printLocationArrays(String data) {
		ArrayList<String> countries = getCountries(data);
		ArrayList<Country> array = new ArrayList<Country>();
		
		// set up the main array with the list of countries
		for (String s : countries) {
			array.add(new Country(s));
		}
		
		JSONArray jsonArray = new JSONArray(data);
		for (int i = 0; i < jsonArray.length(); i++) {
	        JSONObject jsonObject = jsonArray.getJSONObject(i);
	        String countName = jsonObject.getString("country");
	        
	        // add the city to the country
	        for (int j = 0; j < countries.size(); j++) {
	        	if (countName.equals(countries.get(j))) {
	        		String n = jsonObject.getString("name");
	        		if (n.equals(countName)) {
	        			n = "All Cities";
	        		}
	        		
	        		array.get(j).citys.add(new City(
	        				n, 
	        				jsonObject.getInt("woeid")));
	        	}
	        }
	    }
		
		for (Country c : array) {
			System.out.print("public static String[][] " + c.name.toLowerCase().replaceAll(" ", "_") + " = {\n");
			
			ArrayList<City> cities = c.citys;
			
			Collections.sort(cities, new Comparator<City>() {
	            public int compare(City result1, City result2) {
	                return result1.name.compareTo(result2.name);
	            }
	        });
			
			for (int i = 0; i < cities.size(); i++) {
				City s = cities.get(i);
				System.out.print("{\"" + s.name + "\", \"" + s.woeid + "\"}" + (i == cities.size() - 1 ? "\n" : ",\n"));
			}
			System.out.println("};\n");
		}
	}
	
	public static void printReturnStatement(String data) {
		ArrayList<String> countries = getCountries(data);
		
		for (String s : countries) {
			System.out.println("if (countryName.equals(\"" + s +"\")) {");
			System.out.print("return " + s.toLowerCase().replace(" ",  "_") + ";\n} else ");
		}
	}
	
	public static ArrayList<String> getCountries(String data) {
		ArrayList<String> countries = new ArrayList<String>();
		JSONArray jsonArray = new JSONArray(data);
		for (int i = 0; i < jsonArray.length(); i++) {
	        JSONObject jsonObject = jsonArray.getJSONObject(i);
	        countries.add(jsonObject.getString("country"));
	      }
		
		// add elements to al, including duplicates
		HashSet hs = new HashSet();
		hs.addAll(countries);
		countries.clear();
		countries.addAll(hs);
		
		Collections.sort(countries, new Comparator<String>() {
            public int compare(String result1, String result2) {
                return result1.compareTo(result2);
            }
        });
		return countries;
	}
	
	public static String readFile(String fileName) throws IOException {
	    BufferedReader br = new BufferedReader(new FileReader(fileName));
	    try {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            line = br.readLine();
	        }
	        return sb.toString();
	    } finally {
	        br.close();
	    }
	}
}
