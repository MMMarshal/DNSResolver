# DNS Resolver
A DNS lookup server written in Java .

## Description
The server reads a client’s request and replies with an appropriate answer per DNS protocol. If an answer cannot be found in the local cache the server will forward the request to Google's public DNS server to recursively answer.

## Usage
Once running the DNS server will listen on port 8053 of the local host for client requests. Testing can be performed using the Unix tool dig with the -p argument to assign the port number as seen below.

```
dig example.com @127.0.0.1 -p 8053
```
