# Generic IoT Infrastructure

A web service that enables IoT manufacturers to gather data about their device’s usage. Written in Java, HTML, CSS and JavaScript. 
This project consists of two main components: a website and a Gateway server. The website serves as the frontend, created using HTML, CSS, and JavaScript, with Tomcat (servlets) utilized for the backend. The Gateway server, written in Java, acts as a concurrent, configurable, and scalable server. Website
The website provides a user interface for registration. It allows companies to register themselves and their products.
The Gateway server is the backbone of the IoT infrastructure, providing the necessary backend support for handling various functionalities. It serves as the intermediary for interacting with the company-specific database, facilitating all updates.

## Features

* Configurability: The server is easily configurable, allowing for the addition of new functionalities. The "Plug and Play" feature enables seamless addition of new functionalities by simply dropping a jar file containing the new functionality into a specified folder.
* Concurrency: the Gateway server utilizes a thread pool, enabling it to process requests concurrently
* Scalability: vertical scaling is enabled by the use of a thread pool, which by default creates as many threads as there are cores in the system.

## Functionalities

* Register Company: allows companies to register themselves to the IoT infrastructure. Company information is stored in an SQL database. Upon registration, a company - specific NoSQL database (MongoDB) is created for storing device usage data.
* Register Product: enables companies to register their products (models) to the service.
* Register IoT: allows registration of IoT devices belonging to registered products.
* Update: enables devices to send usage information to be stored within the system.

## How It Works

* Company Registration: Companies register via the website, providing necessary details.
* Product Registration: Registered companies can then register their products through the website.
* IoT Device Registration: Devices belonging to registered products can be registered by sending a request to the Gateway server.
* Data Update: Registered devices can start sending data updates to the Gateway server, which serves as the intermediary for interaction with the company-specific database. The server stores the information in the respective company's NoSQL database.

The following diagram illustrates the aforementioned usage flow:

![iot_drawio drawio](https://github.com/YonathanZzZ/GenericIoTInfrastructure/assets/101878675/0016bd4b-4269-43d1-8040-9b1af30078ab)


## Dependencies
* JRE 8 or later
* Tomcat 9 or later

## Usage
* Clone the project
```bash
git clone https://github.com/YonathanZzZ/GenericIoTInfrastructure.git
```

* Start a TomCat server and load the website (backend) server into it
* Start the Gateway server
* You can now use the website and send requests to the Gateway server
