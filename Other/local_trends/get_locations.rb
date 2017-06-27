require 'twitter'

client = Twitter::REST::Client.new do |config|
  config.consumer_key        = "***REMOVED***"
  config.consumer_secret     = "***REMOVED***"
  config.access_token        = "***REMOVED***"
  config.access_token_secret = "***REMOVED***"
end

client.trends_available.each do |location|
	puts "#{location.name}, #{location.country} woeid: #{location.woeid}"
end