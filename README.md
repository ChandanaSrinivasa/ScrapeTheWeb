# ScrapeTheWeb
Java Servlet which takes the URL and returns the KEYWORD that can be passed as a search Query for that page.
The servlet uses JSoup library for extracting data from the HTML DOM.
Targeted few Websites to extract the right keyword required which is then passed to Quixey's API to display DVC related to the search query. 
The servet is deployed on Heroku. https://qxy-url-scraping.herokuapp.com/?url=http://money.cnn.com
The url parameter in the heroku endpoint takes any website URL to return the search query.
