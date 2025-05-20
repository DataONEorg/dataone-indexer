#!/usr/bin/env python3
import pika
import sys

connection = pika.BlockingConnection(
    pika.ConnectionParameters(host='localhost'))
channel = connection.channel()

channel.queue_declare(queue='index', durable=True, arguments={'x-max-priority': 10})
channel.queue_bind(exchange='dataone-index',
                   queue='index',
                   routing_key='index')
properties = pika.BasicProperties(headers={'index_type': 'create', 'id': sys.argv[1:], 'docId': sys.argv[2:]})
message = ''
channel.basic_publish(
    exchange='dataone-index',
    routing_key='index',
    body=message,
    properties=pika.BasicProperties(
        delivery_mode=pika.DeliveryMode.Persistent
    ))
print(f" [x] Sent {message}")
connection.close()