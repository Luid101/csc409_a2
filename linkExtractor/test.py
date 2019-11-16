import asyncio
import requests
from ast import literal_eval

async def handle(req):

    limit = 1000
    num_links = 0

    graph = {}

    links = await getLinks(req)
    links = literal_eval(links.content.decode("utf-8").strip())

    #print(links.content)
    graph[req] = links

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

            if link not in graph:
                graph[link] = None
                
                # setup async
                async_calls[link] = getLinks(link)

        
        
        links = []
        for link in async_calls.keys():
            call_links = (await async_calls[link]).content.decode("utf-8").strip()
            graph[link] = call_links
            num_links += len(async_calls)
            links.extend(call_links)

            if num_links > limit:
                break

            print(num_links)
    print(graph)
    print(num_links)
    print(levels)


async def getLinks(req):
    return requests.request(method='get', url='http://127.0.0.1:8080/function/getlinks', data=req)

loop = asyncio.get_event_loop()
loop.run_until_complete(handle("https://en.wikipedia.org/wiki/Function_as_a_service"))
#loop.run_until_complete(handle("https://edmondumolu.me"))
