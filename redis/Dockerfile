# https://docs.docker.com/engine/reference/builder/
#hhhhiotwwg

# Use an official Python runtime as a parent image
# See https://hub.docker.com/ and search for python:2.7-slim
FROM redis:alpine

# Set the working directory to /app
# The WORKDIR instruction sets the working directory (inside the container) 
# for any RUN, CMD, ENTRYPOINT, COPY and ADD instructions that 
# follow it in the Dockerfile. 
WORKDIR /app

# Copy the current directory contents into the container at /app
ADD . /app

# Install any needed packages specified in requirements.txt
# RUN during image build
#RUN pip install --trusted-host pypi.python.org -r requirements.txt

RUN javac URLShortner.java
RUN ./URLShortner

# Make port 80 available to the world outside this container
EXPOSE 8086
