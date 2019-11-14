FROM python:3.7-alpine
WORKDIR /app
RUN apk update \
    && apk add \
    build-base \
    postgresql \
    postgresql-dev \
    libpq
ENV FLASK_APP app.py
ENV FLASK_RUN_HOST 0.0.0.0
RUN apk add --no-cache gcc musl-dev linux-headers
COPY requirements.txt requirements.txt
RUN pip install -r requirements.txt
COPY . .
CMD ["flask", "run"]