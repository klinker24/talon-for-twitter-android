require 'twitter'

client = Twitter::REST::Client.new do |config|
  config.consumer_key        = "RXtk0w9pEDTg0ThHXsw7SHo34"
  config.consumer_secret     = "jOi5auTdlNXfLFOlR1Sd2bTGUoa37NXTCWLUDij1OoJA516vkH"
  config.access_token        = "604990177-mWVd1amjKS5T9fH61L5NiL0cfsn0guzFyt80Trdt"
  config.access_token_secret = "lG0ZQvVsfEht6WohVTvA0DjKJyNh5lIyzObGJEuw52bUc"
end

client.trends_available.each do |location|
	puts "#{location.name}, #{location.country} woeid: #{location.woeid}"
end