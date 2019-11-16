from bs4 import BeautifulSoup
import urllib3
from urllib.parse import urlparse
import re

def handle(req):
    http = urllib3.PoolManager()

    links = []
    base_url = "{0.scheme}://{0.netloc}/".format(urlparse(req))
    html_page = http.request('GET', req).data
    soup = BeautifulSoup(html_page, features="html.parser")

    for link in soup.findAll('a', attrs={'href': re.compile("^http://")}):
        #print link.get('href')
        links.append([ link.text, link.get('href') ] )
    for link in soup.findAll('a', attrs={'href': re.compile("^https://")}):
        #print link.get('href')
        links.append([ link.text, link.get('href') ] )
    for link in soup.findAll('a', attrs={'href': re.compile("^/")}):
        links.append([ link.text, base_url + link.get('href') ] )

    return links

