# https://docs.docker.com/engine/reference/builder/

# Use an official Python runtime as a parent image
# See https://hub.docker.com/ and search for python:2.7-slim
FROM java:8

# Set the working directory to /app
# The WORKDIR instruction sets the working directory (inside the container) 
# for any RUN, CMD, ENTRYPOINT, COPY and ADD instructions that 
# follow it in the Dockerfile. 
WORKDIR /app

# Copy the current directory contents into the container at /app
ADD . /app

# Install any needed packages specified in requirements.txt
# RUN during image build
RUN java - jar jedis-2.1.0-sources.jar

RUN javac URLShortner.java

# Make port 80 available to the world outside this container
EXPOSE 8086

# Define environment variable
#ENV NAME World

# Run python app.py when the container launches
# This happens if no command is specified

CMD ["java", "URLShortner"]

