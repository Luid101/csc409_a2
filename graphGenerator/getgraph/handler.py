from urllib.parse import urlparse
from ast import literal_eval
import asyncio
import requests
import json

def handle(req):

    data = json.loads(req)
    
    url = ""
    num = 0

    try:
        url = data["url"]
        num = int(data["num_links"])
    except Exception as e:
        return e

    #loop = asyncio.get_event_loop()
    
    graph = asyncio.run( getGraph( url, num ) )
    
    #loop.stop()
    
    return graph

async def getGraph(req, given_limit):

    limit = min( given_limit, 5000 )
    num_links = 0

    graph = {}

    links = await getLinks(req)
    links = literal_eval(links.content.decode("utf-8").strip())

    #print(links.content)
    graph[ ("Home", req) ] = links

    num_links += len(links)
    levels = 1

    while (num_links < limit) and links:
        levels += 1
        print(num_links)
        print("Level: " + str(levels))
        # do the next level of api calls
        async_calls = {}
        for link_pair in links:

            text, link = link_pair
            key = (text.strip(), link)

            if key not in graph:
                graph[key] = None
                
                # setup async
                async_calls[key] = getLinks(link)

        
        
        links = []
        for link in async_calls.keys():
            call_links = (await async_calls[link]).content.decode("utf-8").strip()
 
            graph[link] = literal_eval(call_links)
            
            num_links += len(async_calls)
            links.extend(call_links)

            if num_links > limit: 
                return graph

    return graph	

async def getLinks(req):
    return requests.request(method='get', url='http://gateway:8080/function/getlinks', data=req)

