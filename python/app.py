import time

import redis
import psycopg2
from flask import Flask, request, redirect

app = Flask(__name__)
cache = redis.Redis(host='redis', port=6379)

postgres = psycopg2.connect(host="url",port=5432, database="linktable", user="urlshortner", password="arnold")
cur = postgres.cursor()
postgres.autocommit = True
cur.execute("CREATE TABLE IF NOT EXISTS linktable (shortURL text PRIMARY KEY, longURL text);")


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
    except:
        print("Redis write error")
    try:
        cur.execute("INSERT INTO linktable (shortURL, longURL) VALUES (%s, %s) ON CONFLICT (shortURL) DO UPDATE SET longURL = excluded.longURL", (short, longURL))
    except:
        print("PostgreSQL write error")


def get(short):
    try:
        longURL = cache.get(short)
        if longURL != None:
            return longURL
    except:
        print("Redis error occured")

    try:
        cur.execute("SELECT longURL from linkTable WHERE shortURL = %s", (short,))
        longURL = cur.fetchone()[0]
        if longURL != None:
            save(short, longURL)
            return longURL
    except:
        print("PostgreSQL error occured")
        


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
        
        

