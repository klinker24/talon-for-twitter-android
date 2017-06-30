require 'twitter'

client = Twitter::REST::Client.new do |config|
  config.consumer_key        = "***REMOVED***"
  config.consumer_secret     = "***REMOVED***"
  config.access_token        = "***REMOVED***"
  config.access_token_secret = "***REMOVED***"
end

locations = client.trends_available

puts "========== Countries String Array =========="
puts ""
puts "<string-array name=\"countries\">"

countries = locations.uniq { |x| x.country }
countries = countries.sort { |x, y| x.country <=> y.country }

countries.each do |country|
	puts "    <item>#{country.country}</item>"
end

puts "</string-array>"

puts "\n\n"
puts "========== Cities by Country =========="
puts ""

countries.each do |country|
	cities = locations.select { |location| location.country == country.country }
	cities = cities.sort { |x, y| x.name <=> y.name }

	name = country.country.downcase.gsub ' ', '_'
	puts "public static String[][] #{name} = {"
	cities.each do |city|
		if (city.name == country.country)
			puts "    { \"All Cities\", \"#{city.woeid}\" },"
		else
			puts "    { \"#{city.name}\", \"#{city.woeid}\" },"
		end
	end
	puts "};"
	puts "\n"
end

puts "\n\n"
puts "========== Return Statement to Convert String Array to Object =========="
puts ""

puts "public static String[][] getArray(String countryName) {"

countries.each do |country|
	puts "    } else if (countryName.equals(\"#{country.country}\")) {"

	name = country.country.downcase.gsub ' ', '_'
	puts "        return #{name};"
end

puts "    }"
puts "    return null;"
puts "}"

# locations.each do |location|
# 	puts "#{location.name}, #{location.country} woeid: #{location.woeid}"
# end