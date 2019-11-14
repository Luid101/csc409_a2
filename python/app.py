import time

import redis
import psycopg2
from flask import Flask, request, redirect

app = Flask(__name__)
cache = redis.Redis(host='redis', port=6379)

postgres = psycopg2.connect(host="localhost",database="linktable", user="urlshortner", password="arnold")


def get_hit_count():
    retries = 5
    while True:
        try:
            return cache.incr('hits')
        except redis.exceptions.ConnectionError as exc:
            if retries == 0:
                raise exc
            retries -= 1
            time.sleep(0.5)

def save(short, longURL):
    try:
        cache.set(short, longURL)
    except redis.exceptions.ConnectionError as exc:
        if retries == 0:
            raise exc
        retries -= 1
        time.sleep(0.5)

def get(short):
    try:
        return cache.get(short)
    except redis.exceptions.ConnectionError as exc:
        if retries == 0:
            raise exc
        retries -= 1
        time.sleep(0.5)


#@app.route('/')
def hello():
    count = get_hit_count()
    print(request.path)
    return request.path
    return 'Hello Worldasdadsf! \n \n I have been seen {} times.\n'.format(count)

@app.route('/<short>', methods=['GET'])
def parse_request(short):
    longURL = get(short)
    if longURL == None:
        return "Didn't find " + short
    return redirect(longURL, 307)

    if (request.method == 'GET'):
        return requst.path
        short = request.args.get('short')
        return redirect(get(short), 307)
    else:
        
        longURL = request.args.get('long')
        short = request.args.get('short')
        save(short, longURL)
        return "Success"


@app.route('/', methods=['PUT'])
def parse_put():
    longURL = request.args.get('long')
    short = request.args.get('short')
    save(short, longURL)
    return "Success"
        
        

