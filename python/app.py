import time

import redis
import psycopg2
from flask import Flask, request, redirect

app = Flask(__name__)
cache = redis.Redis(host='redis', port=6379)

postgres = psycopg2.connect(host="url",port=5432, database="linktable", user="urlshortner", password="arnold")
cur = postgres.cursor()
cur.execute("CREATE TABLE IF NOT EXISTS linktable (shortURL text, longURL text);")


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

    try:
        cur.execute("INSERT INTO linktable (shortURL, longURL) VALUES (%s, %s)", (short, longURL))
    except e:
        print("PostgreSQL error")


def get(short):
    try:
        #cur.execute("SELECT * from linkTable")
        cur.execute("SELECT longURL from linkTable WHERE shortURL = %s", (short,))
        long = cur.fetchone()[0]
        return long
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
        
        

